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

    // make an insurance proposal for AXA
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insureProposal", username, "user", vin, "axa"))
    proposal := InsureProposal {}
    err = json.Unmarshal(response.Payload, &proposal)
    if (err != nil) {
        t.Error("Error while creating insurance proposal")
    }

    fmt.Println(proposal)
}

func TestGetInsurerAndInsuranceAccept(t *testing.T) {
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

    // make an insurance proposal for AXA
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insureProposal", username, "user", vin, "axa"))
    proposal := InsureProposal {}
    err = json.Unmarshal(response.Payload, &proposal)
    if (err != nil) {
        t.Error("Error while creating insurance proposal")
    }

    fmt.Println(proposal)

    // accept the proposal as axa insurance company
    // this would be allowed, but the car is not registered yet
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insuranceAccept", username, "insurer", vin, "axa"))
    err = json.Unmarshal(response.Payload, &proposal)
    if (err == nil) {
        t.Error("Insuring a car before registration is impossible. How could you possibly trust this VIN in the certificate?")
    }

    // the DOT registers the car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", vin))
    err = json.Unmarshal(response.Payload, &car)
    if (err != nil) {
        t.Error(response.Message)
    }

    // accept my own proposal as user
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insuranceAccept", username, "user", vin, "axa"))
    err = json.Unmarshal(response.Payload, &proposal)
    if (err == nil) {
        t.Error("Normal user should not be allowed to accept his own insurance proposals")
    }

    // accept the proposal as axa insurance company
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insuranceAccept", username, "insurer", vin, "axa"))
    err = json.Unmarshal(response.Payload, &proposal)
    if (err != nil) {
        t.Error("Error creating insurance contract")
    }

    // the list of proposals for AXA should be empty by now
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("getInsurer", username, "insurer", "axa"))
    insurer := Insurer {}
    err = json.Unmarshal(response.Payload, &insurer)
    if (err != nil) {
        t.Error("Error fetching insurance records")
    }

    fmt.Println(insurer)

    if len(insurer.Proposals) != 0 {
        t.Error("After creating an insurance contract, the proposal should be removed from the list of open insurance proposals")
    }
}