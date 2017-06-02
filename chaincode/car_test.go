package main

import (
    "fmt"
    "encoding/json"
    "testing"

    "github.com/hyperledger/fabric/core/chaincode/shim"
    "github.com/hyperledger/fabric/common/util"
)

const uuid string = "1"

func TestIsConfirmed(t *testing.T) {
    // create a new car without timestamp
    car := &Car{}

    if (isConfirmed(car)) {
        t.Error("Car should be not confirmed initially")
    }
}

func TestCreate(t *testing.T) {
    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    // create a new car
    carData := `{"vin": "WVW ZZZ 6RZ HY26 0780", "user": "amag"}`
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", carData))

    // payload should contain the car index with the newly created car
    var carIndex []int64
    err := json.Unmarshal(response.Payload, &carIndex)
    if (err != nil) {
        t.Error(err.Error())
    }

    // print the car and the car index
    fmt.Printf("CAR_TEST:\tSuccessfully created car with ts '%d'\n", carIndex[0])
    fmt.Println("CAR_TEST:\tCar index:")
    for i, carCreatedTs := range carIndex {
        fmt.Printf("CAR_TEST:\t [%d] %d\n", i, carCreatedTs)
    }

    // the car index should only contain one car
    if (len(carIndex) > 1) {
        t.Error("CAR_TEST:\tCar index should only contain one car")
    }
}