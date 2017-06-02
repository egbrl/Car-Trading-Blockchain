package main

import (
    "fmt"
    "encoding/json"
    "time"
    "strconv"

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
        fmt.Println("Car with VIN '%s' has no valid numberplate\n", car.Vin)
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
        fmt.Println("Car with VIN '%s' is not insured\n", car.Vin)
    }
    
    return insured
}

/*
 * Creates a new, unregistered car with the current timestamp
 * and appends it to the car index.
 *
 * Expects (as json):
 * {
 *   user: "<GARAGE>",
 *   vin:  "<VEHICLE IDENTIFICATION NUMBER>"
 * }
 *  
 * Returns the full car index on success, including the new car.
 */
func (t *CarChaincode) create(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    if len(args) != 1 {
        return shim.Error("'create' expects car data")
    }

    // create a new car from json argument list
    car := Car {}
    err := json.Unmarshal([]byte(args[0]), &car)
    if err != nil {
        return shim.Error("Error parsing car json")
    }

    // add car birth date and save car to ledger
    // the car ts serves as the index to find the car again
    car.CreatedTs = time.Now().Unix()
    carAsBytes, _ := json.Marshal(car)
    err = stub.PutState(strconv.FormatInt(car.CreatedTs, 10), carAsBytes)
    if err != nil {
        return shim.Error("Error saving car data")
    }

    // get the car index
    indexAsBytes, err := stub.GetState(carIndexStr)
    if err != nil {
        return shim.Error("Failed to get car index")
    }

    var index []int64
    json.Unmarshal(indexAsBytes, &index)

    // append car to index
    index = append(index, car.CreatedTs)
    fmt.Printf("Appended car with VIN '%s' created at '%d' in garage '%s' to car index.\n",
                car.Vin, car.CreatedTs, car.User)
    indexAsBytes, _ = json.Marshal(index)

    // write udpated car index back to ledger
    err = stub.PutState(carIndexStr, indexAsBytes)
    if (err != nil) {
        return shim.Error(err.Error())
    }

    return shim.Success(indexAsBytes)
}
