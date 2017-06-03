package main

import (
    "fmt"
    "encoding/json"
    "strconv"

    "golang.org/x/crypto/nacl/box"
    "crypto/rand"
    "time"

    "github.com/hyperledger/fabric/core/chaincode/shim"
    pb "github.com/hyperledger/fabric/protos/peer"
)

/*
 * Checks for a valid car VIN.
 *
 * The car VIN is valid if the DOT certificate contains
 * the same information. Registration guarantees that
 * certificate VIN and a car VIN are equal and that
 * a certificate was issued by the DOT at least once.
 */
func isRegistered(car *Car) bool {
    // cannot be registered without certificate
    if (car.Certificate == nil) {
        fmt.Printf("Car created at ts '%d' is not yet registered\n", car.CreatedTs)
        return false
    }

    // validate car VIN
    carVin := car.Vin
    registered := car.Certificate.Vin == carVin

    if (registered) {
        fmt.Printf("Car created at ts '%d' is registered with VIN '%s'\n", car.CreatedTs, carVin)
    } else {
        fmt.Printf("Car created at ts '%d' is not yet registered\n", car.CreatedTs)
    }
    
    return registered
}

/*
 * Checks the car numberplate.
 *
 * The numberplate is handed out by the DOT.
 */
func isConfirmed(car *Car) bool {
    // cannot have a numberplate without car papers
    if (!isRegistered(car)) {
        return false
    }

    confirmed := car.Certificate.Numberplate != ""

    // because the car is registered, the car VIN can be trusted
    if (confirmed) {
        fmt.Printf("Car with VIN '%s' is confirmed\n", car.Vin)
    } else {
        fmt.Printf("Car with VIN '%s' has no valid numberplate\n", car.Vin)
    }
    
    return confirmed
}

/*
 * Checks for an active car insurance.
 *
 * A vehicle can be registered and confirmed by the DOT,
 * but still lack an insurance contract. This case can occur
 * when you change the insurer without changing the numberplate.
 * 
 * On the other hand the vehicle could already be insured,
 * but still be waiting for a valid numberplate. This case may
 * occur when changing numberplates.
 *
 * In any case, the car has to be registered before it can be insured.
 */
func isInsured(car *Car) bool {
    // cannot be insured without car papers
    if (!isRegistered(car)) {
        return false
    }

    insured := car.Certificate.Insurer != ""

    if (insured) {
        fmt.Printf("Car with VIN '%s' is insured by company '%s'\n", car.Vin, car.Certificate.Insurer)
    } else {
        fmt.Printf("Car with VIN '%s' is not insured\n", car.Vin)
    }
    
    return insured
}

/*
 * Reads ledger state from 'key'.
 *
 * Can be any of:
 *  - Car   (expects car timestamp as key)
 *  - User  (expects user name as key)
 *
 * On success,
 * returns bytes at ledger 'key'
 */
func (t *CarChaincode) read(stub shim.ChaincodeStubInterface, key string) pb.Response {
    if key == "" {
        return shim.Error("'read' expects a non-empty key to do the look up")
    }

    valAsBytes, err := stub.GetState(key)
    if err != nil {
        return shim.Error("Failed to fetch value at key '" + key + "' from ledger")
    }

    return shim.Success(valAsBytes)
}

/*
 * Creates a new, unregistered car with the current timestamp
 * and appends it to the car index.
 *
 * Expects arguments:
 *  [1] Car
 *  [2] User
 * 
 * On success,
 * returns the car keys to lock and unlock.
 */
func (t *CarChaincode) create(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    if len(args) != 2 {
        return shim.Error("'create' expects car and user data")
    }

    // create car from arguments
    car := Car {}
    err := json.Unmarshal([]byte(args[0]), &car)
    if err != nil {
        return shim.Error("Error parsing car data")
    }

    // create user from arguments
    user := User {}
    err = json.Unmarshal([]byte(args[1]), &user)
    if err != nil {
        return shim.Error("Error parsing user data")
    }

    // add car birth date
    car.CreatedTs = time.Now().Unix()

    // generate car keys
    publicKey, privateKey, _ := box.GenerateKey(rand.Reader)

    // the garage user gets the private key
    keyringEntry := KeyringEntry { PrivateKey: *privateKey,
                                   PublicKey:  *publicKey,
                                   CarTs:       car.CreatedTs }
    
    // find existing garage user with that name
    existingUserResponse := t.read(stub, user.Name)
    existingUser := User {}
    err = json.Unmarshal(existingUserResponse.Payload, &existingUser) 
    if err == nil {
        // use existing garage user
        user = existingUser
    }

    // hand over the keys and write user to ledger
    user.KeyringEntries = append(user.KeyringEntries, keyringEntry)
    userAsBytes, _ := json.Marshal(user)
    err = stub.PutState(user.Name, userAsBytes)
    if err != nil {
        return shim.Error("Error writing user")
    }

    // lock the car
    var nonce [24]byte
    carAsBytes, _ := json.Marshal(car)
    cryptedCar := box.Seal(nil, carAsBytes, &nonce, publicKey, privateKey)

    // save car to ledger, the car ts serves
    // as the index to find the car again
    err = stub.PutState(strconv.FormatInt(car.CreatedTs, 10), cryptedCar)
    if err != nil {
        return shim.Error("Error writing car")
    }

    // get the car index and map username
    carIndexResponse := t.read(stub, carIndexStr)
    index := make(map[int64]string)
    err = json.Unmarshal(carIndexResponse.Payload, &index)
    index[car.CreatedTs] = user.Name
    fmt.Printf("Added car with VIN '%s' created at '%d' in garage '%s' to car index.\n",
                car.Vin, car.CreatedTs, user.Name)

    // write udpated car index back to ledger
    indexAsBytes, _ := json.Marshal(index)
    err = stub.PutState(carIndexStr, indexAsBytes)
    if err != nil {
        return shim.Error("Error writing car index")
    }

    // car creation successfull,
    // return the car keys
    keysAsBytes, _ := json.Marshal(keyringEntry)
    return shim.Success(keysAsBytes)
}
