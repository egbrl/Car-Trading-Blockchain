package main

import (
    "fmt"
    "encoding/json"
    "testing"

    "github.com/hyperledger/fabric/core/chaincode/shim"
    "github.com/hyperledger/fabric/common/util"
)

func TestIsInsured(t *testing.T) {
    // create a new car without insurance
    car := &Car{}

    if (IsInsured(car)) {
        t.Error("Car should not be insured initially")
    }
}

func TestInsureProposal(t *testing.T) {
    username := "amag"
    vin      := "WVW ZZZ 6RZ HY26 0780"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    carData := `{ "vin": "` + vin + `" }`
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

    // payload should contain the car
    car := Car {}
    err := json.Unmarshal(response.Payload, &car)
    if (err != nil) {
        t.Error(err.Error())
    }

    fmt.Printf("Successfully created car with ts '%d'\n", car.CreatedTs)

    // register the car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", vin))
    err = json.Unmarshal(response.Payload, &car)
    if (err != nil) {
        t.Error(response.Message)
    }

    // make an insurance proposal for AXA
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insureProposal", username, "user", vin, "axa"))
}