package main

import (
    "fmt"
    "encoding/json"
    "strconv"
    "time"

    "github.com/hyperledger/fabric/core/chaincode/shim"
    pb "github.com/hyperledger/fabric/protos/peer"
)

/*
 * Checks if a car has a valid certificate.
 * The certificate can only be issued by the DOT.
 */
func isConfirmed(car *Car) bool {
    confirmed := car.Certificate != nil

    if (confirmed) {
        fmt.Println("Car with ts '" + strconv.FormatInt(car.CreatedTs, 10) + "' is confirmed")
    } else {
        fmt.Println("Car with ts '" + strconv.FormatInt(car.CreatedTs, 10) + "' is unconfirmed")
    }
    
    return confirmed
}

/*
 * Creates a new, unconfirmed car with the current timestamp
 * and appends it to the car index.
 *
 * Returns the full car index on success, including the new car.
 */
func (t *CarChaincode) create(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    if len(args) > 0 {
        return shim.Error("'create' does not accept any arguments")
    }

    // create a new car
    car := Car{ CreatedTs: time.Now().Unix() }

    // get the car index
    indexAsBytes, err := stub.GetState(carIndexStr)
    if err != nil {
        return shim.Error("Failed to get car index")
    }

    var index []int64
    json.Unmarshal(indexAsBytes, &index)

    // append car to index
    index = append(index, car.CreatedTs)
    fmt.Println("Appended car with ts '" + strconv.FormatInt(car.CreatedTs, 10) + "' to car index.")
    indexAsBytes, _ = json.Marshal(index)
    err = stub.PutState(carIndexStr, indexAsBytes)

    return shim.Success(indexAsBytes)
}
