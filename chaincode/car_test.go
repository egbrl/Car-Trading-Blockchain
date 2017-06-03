package main

import (
    "fmt"
    "encoding/json"
    "testing"
    "strconv"

    "golang.org/x/crypto/nacl/box"
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

func TestInit(t *testing.T) {
    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

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
}

func TestCreateAndReadCar(t *testing.T) {
    username := "amag"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    // create a new car
    carData := `{ "vin": "WVW ZZZ 6RZ HY26 0780" }`
    userData := `{ "name": "` + username + `" }`
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", carData, userData))

    // payload should contain the car key
    var carKeys KeyringEntry
    err := json.Unmarshal(response.Payload, &carKeys)
    if (err != nil) {
        fmt.Println(response)
        t.Error(err.Error())
    }

    fmt.Printf("Successfully created car with ts '%d'\n", carKeys.CarTs)

    // the user should only have one car by now
    // fetch the actual car and unlock it
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", strconv.FormatInt(carKeys.CarTs, 10)))
    var nonce [24]byte
    carAsBytes, ok := box.Open(nil, response.Payload, &nonce, &carKeys.PublicKey, &carKeys.PrivateKey)
    if !ok {
        t.Fatalf("Failed to unlock car")
    }

    car := Car{}
    err = json.Unmarshal(carAsBytes, &car)
    if (err != nil) {
        t.Error(err.Error())
    }

    fmt.Println(car)
}