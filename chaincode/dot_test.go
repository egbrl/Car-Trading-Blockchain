package main

import (
    "fmt"
    "encoding/json"
    "testing"

    "github.com/hyperledger/fabric/core/chaincode/shim"
    "github.com/hyperledger/fabric/common/util"
)

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

func TestRegisterCar(t *testing.T) {
    username := "amag"
    vin      := "WVW ZZZ 6RZ HY26 0780"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    carData := `{ "vin": "` + vin + `" }`
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

    // registering a car as garage user should be forbidden
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "garage", vin))
    err = json.Unmarshal(response.Payload, &car)
    if err == nil {
        t.Error("Registering a car as 'garage' user should not be possible")
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

    fmt.Println(car.Certificate)
}