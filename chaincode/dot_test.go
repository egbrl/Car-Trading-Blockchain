package main

import (
    "fmt"
    "encoding/json"
    "testing"

    "github.com/hyperledger/fabric/core/chaincode/shim"
    "github.com/hyperledger/fabric/common/util"
)

// default test input arguments
var username string = "amag"
var vin string      = "WVW ZZZ 6RZ HY26 0780"
var carData string  = `{ "vin": "` + vin + `" }`

func TestIsConfirmed(t *testing.T) {
    // create a new car without numberplate
    car := &Car{}

    if (IsConfirmed(car)) {
        t.Error("Car should not be confirmed initially")
    }
}

func TestIsRegistered(t *testing.T) {
    // create a new car without certificate
    car := &Car{}

    if (IsRegistered(car)) {
        t.Error("Car should not be registered initially")
    }
}

func TestReadRegistrationProposalsAndRegisterCar(t *testing.T) {
    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

    // payload should contain the car...
    car := Car {}
    err := json.Unmarshal(response.Payload, &car)
    if (err != nil) {
        t.Error(err.Error())
    }

    // ...but without certificate
    if IsRegistered(&car) {
        t.Error("Car should not be registered yet!")
    }

    fmt.Printf("Successfully created car with ts '%d'\n", car.CreatedTs)

    // read all registration proposals as DOT user
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readRegistrationProposals", "TESTING", "dot"))
    proposals := make(map[string]RegistrationProposal)
    err = json.Unmarshal(response.Payload, &proposals)
    if err != nil {
        t.Error("Error reading proposal index")
    }

    // there should be a registration proposal for the DOT now
    if len(proposals) != 1 {
        t.Error("Error: 'create' should also create a new registration proposal for the DOT")
    }

    // check if the registration proposal got saved for the right car
    fmt.Printf("The registration proposal: %v\n", proposals[car.Vin])

    // registering a car as garage user should be forbidden
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "garage", vin))
    err = json.Unmarshal(response.Payload, &car)
    if err == nil {
        t.Error("Registering a car as 'garage' user should not be possible")
    }

    // Registering a random car for which no open registration proposal exists
    // should return an error. This car should not even be found on the ledger.
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", "someRandomVIN"))
    err = json.Unmarshal(response.Payload, &car)
    if err == nil {
        t.Error("Registering an unsaved car is not possible")
    }

    // register the car again as DOT user, should be allowed
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", vin))
    err = json.Unmarshal(response.Payload, &car)
    if (err != nil) {
        t.Error(response.Message)
    }

    if !IsRegistered(&car) {
        t.Error("Car should now be registered!")
    }

    // check out the proposals again and ensure
    // that the just registered car is removed
    // from the list of open registration proposals
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readRegistrationProposals", "TESTING", "dot"))
    proposals = make(map[string]RegistrationProposal)
    err = json.Unmarshal(response.Payload, &proposals)
    if err != nil {
        t.Error("Error reading proposal index")
    }

    if len(proposals) != 0 {
        t.Error("Error: Registration proposal for that car should no longer be on the list of open proposals!")
    }

    // Registering the car twice should return an error,
    // because no open registration proposal exists.
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", vin))
    err = json.Unmarshal(response.Payload, &car)
    if err == nil {
        t.Error("Registering a car without registration proposal should not be possible")
    }

    fmt.Println(car.Certificate)
}

func TestConfirm(t *testing.T) {
    numberplate      := "ZH 7878"
    insuranceCompany := "axa"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

    // payload should contain the car...
    car := Car {}
    err := json.Unmarshal(response.Payload, &car)
    if (err != nil) {
        t.Error("Error creating car")
    }

    // ...but not yet confirmed
    if IsConfirmed(&car) {
        t.Error("Car should not be confirmed yet!")
    }

    fmt.Printf("Successfully created car with ts '%d'\n", car.CreatedTs)

    // register the car as DOT user
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", vin))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error(response.Message)
    }

    if !IsRegistered(&car) {
        t.Error("Car should now be registered!")
    }

    fmt.Println(car.Certificate)

    // getting a numberplate (getting the car confirmed)
    // without insurance contract should not be allowed
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("confirm", username, "dot", vin, numberplate))
    err = json.Unmarshal(response.Payload, &car)
    if err == nil {
        t.Error("Car should not get confirmed without insurance contract")
    }

    // make an insurance proposal for AXA
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insureProposal", username, "user", vin, insuranceCompany))
    proposal := InsureProposal {}
    err = json.Unmarshal(response.Payload, &proposal)
    if (err != nil) {
        t.Error("Error while creating insurance proposal")
    }

    // and let axa insure the car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insuranceAccept", username, "insurer", vin, insuranceCompany))
    err = json.Unmarshal(response.Payload, &proposal)
    if (err != nil) {
        t.Error("Error while accepting insurance proposal")
    }

    fmt.Println(proposal)

    // fetch the car a new to check for insurance
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readCar", username, "TESTING", car.Vin))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error("Failed to fetch car")
    } else if (car.Certificate.Insurer != insuranceCompany) {
        t.Error("Insurer does not match")
    }

    fmt.Println(car.Certificate.Insurer)

    if !IsInsured(&car) {
        t.Error("Error insuring car")
    }

    // get a numberplate
    // with a valid insurance contract this is now possible
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("confirm", username, "dot", vin, numberplate))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error("Error assigning numberplate")
    }

    if !IsConfirmed(&car) {
        t.Error("Car should be confirmed by now")
    }

    if car.Certificate.Numberplate != numberplate {
        t.Error("Car has wrong numberplate")
    }

    fmt.Println(car.Certificate.Numberplate)
}