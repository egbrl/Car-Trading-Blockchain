package main

import (
    "fmt"
    "encoding/json"

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
func IsRegistered(car *Car) bool {
    // cannot be registered without certificate
    if (car.Certificate.Vin == "") {
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
 * Registers a car.
 *
 * Registration guarantees that certificate VIN
 * and a car VIN are equal and that a certificate
 * was issued by the DOT at least once.
 * 
 * On success,
 * returns the car with certificate.
 */
func (t *CarChaincode) register(stub shim.ChaincodeStubInterface, username string, vin string) pb.Response {
    // this already checks that the user 
    // is the actual owner of the car
    carResponse := t.readCar(stub, username, vin)
    car := Car {}
    json.Unmarshal(carResponse.Payload, &car)
    if vin != car.Vin {
        return shim.Error(fmt.Sprintf("Cannot register, invalid VIN.\nCar VIN is '%s' and you want to register VIN '%s'", car.Vin, vin))
    }

    // create a certificate, approve vin
    // and update the car in the ledger
    cert := Certificate { Username: username,
                          Vin:      vin }
    car.Certificate = cert
    carAsBytes, _ := json.Marshal(car)
    err := stub.PutState(car.Vin, carAsBytes)
    if err != nil {
        return shim.Error("Error writing car")
    }

    fmt.Printf("Successfully registered car created at ts '%d' with VIN '%s'\n", car.CreatedTs, vin)
    
    return shim.Success(carAsBytes)
}