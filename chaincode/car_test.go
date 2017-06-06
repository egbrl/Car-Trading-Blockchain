package main

import (
    "fmt"
    "encoding/json"
    "testing"
    "strconv"

    "github.com/hyperledger/fabric/core/chaincode/shim"
    "github.com/hyperledger/fabric/common/util"
)

const uuid string = "1"

func ccSetup(t *testing.T, stub *shim.MockStub) {
    // a successfull init should not return any errors
    response := stub.MockInit(uuid, util.ToChaincodeArgs("init", "999"))
    if (response.Payload != nil) {
        t.Error(response.Payload)
    }

    // init should write a test on the ledger
    testAsBytes, err := stub.GetState("abc")
    if err != nil {
        t.Error("Failed to read test var from ledger")
    }

    var aval int
    json.Unmarshal(testAsBytes, &aval)

    if (aval != 999) {
        t.Error("Aval for testing should be '999', but is '%d'", aval)
    }

    // check out the empty car index
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", carIndexStr))
    carIndex := make(map[string]string)
    err = json.Unmarshal(response.Payload, &carIndex)

    if err != nil {
        t.Error(err.Error())
    }

    fmt.Printf("Empty car index:\t%v\n", carIndex)
    fmt.Printf("Car index length:\t%v\n", len(carIndex))

    if len(carIndex) != 0 {
        t.Error("Car index should be empty")
    }
}

func TestIsConfirmed(t *testing.T) {
    // create a new car without timestamp
    car := &Car{}

    if (isConfirmed(car)) {
        t.Error("Car should be not confirmed initially")
    }
}

func TestInit(t *testing.T) {
    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)
}

func TestCreateAndReadCar(t *testing.T) {
    username := "amag"
    vin      := "WVW ZZZ 6RZ HY26 0780"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    carData := `{ "vin": "` + vin + `" }`
    userData := `{ "name": "` + username + `" }`
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", "amag", "garage", carData, userData))

    // payload should contain the car
    car := Car {}
    err := json.Unmarshal(response.Payload, &car)
    if (err != nil) {
        t.Error(err.Error())
    }

    fmt.Printf("Successfully created car with ts '%d'\n", car.CreatedTs)

    // fetch car again to check if the car was saved correctly
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", strconv.FormatInt(car.CreatedTs, 10)))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error("Failed to fetch car")
    }

    fmt.Println(car)
    if (car.Vin != vin) {
        t.Error("Car VIN does not match")
    }

    // check out the car index, should contain one car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", carIndexStr))
    carIndex := make(map[string]string)
    err = json.Unmarshal(response.Payload, &carIndex)

    if err != nil {
        t.Error("Failed to fetch car index")
    }

    if (carIndex[strconv.FormatInt(car.CreatedTs, 10)] != username) {
        t.Error("This is not the car '" + username + "' created")
    }

    // the user should only have one car by now
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", username))
    user := User {}
    err = json.Unmarshal(response.Payload, &user)
    if err != nil {
        t.Error("Failed to fetch user")
    }

    if (user.Cars[0] != car.CreatedTs) {
        t.Error("There was an error handing over the car")
    }
}