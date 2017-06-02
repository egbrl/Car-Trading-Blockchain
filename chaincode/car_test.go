package main

import (
    "fmt"
    "encoding/json"
    "strconv"
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
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create"))

    // payload should contain the car index with the newly created car
    var carIndex []int64
    err := json.Unmarshal(response.Payload, &carIndex)
    if (err != nil) {
        t.Error(err.Error())
    }

    // print the car and the car index
    fmt.Println("CAR_TEST:\tSuccessfully created car with ts '" + strconv.FormatInt(carIndex[len(carIndex) - 1], 10) + "'")
    fmt.Println("CAR_TEST:\tCar index:")
    for i, carCreatedTs := range carIndex {
        fmt.Println("CAR_TEST:\t [" + strconv.Itoa(i) + "] " + strconv.FormatInt(carCreatedTs, 10))
    }

    // the car index should only contain one car
    if (len(carIndex) > 1) {
        t.Error("CAR_TEST:\tCar index should only contain one car")
    }
}