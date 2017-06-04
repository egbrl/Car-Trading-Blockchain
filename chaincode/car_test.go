package main

import (
    "fmt"
    "encoding/json"
    "testing"
    "strconv"
    "crypto/rsa"
    "crypto/rand"
    "crypto/sha256"

    "github.com/minio/minio-go/pkg/encrypt"
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
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", carIndexStr))
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

    // check out the empty key index
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", keyIndexStr))
    keyIndex := make(map[string][]byte)
    err = json.Unmarshal(response.Payload, &keyIndex)

    if err != nil {
        t.Error(err.Error())
    }

    fmt.Printf("Empty key index:\t%v\n", keyIndex)
    fmt.Printf("Key index length:\t%v\n", len(keyIndex))

    if len(keyIndex) != 0 {
        t.Error("Key index should be empty")
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
    secret   := "my-secret-key-00"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    carData := `{ "vin": "` + vin + `" }`
    userData := `{ "name": "` + username + `" }`
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", carData, userData, secret))

    // payload should contain the car key
    var carKeys KeyringEntry
    err := json.Unmarshal(response.Payload, &carKeys)
    if (err != nil) {
        t.Error(err.Error())
    }

    fmt.Printf("Successfully created car with ts '%d'\n", carKeys.CarTs)

    // the user should only have one car by now
    // fetch the key index to get the crypted car key
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", keyIndexStr))
    keyIndex := make(map[string][]byte)
    err = json.Unmarshal(response.Payload, &keyIndex)
    cryptedCarKey := keyIndex[strconv.FormatInt(carKeys.CarTs, 10)]

    // encrypt the secret
    carKey, err := rsa.DecryptOAEP(sha256.New(), rand.Reader, &carKeys.PrivateKey, cryptedCarKey, nil)
    if err != nil {
        t.Error("Error decrypting car secret")
    }

    fmt.Printf("Pssssst! Car with ts '%d' has secret '%s'\n", carKeys.CarTs, string(carKey))

    // fetch car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", strconv.FormatInt(carKeys.CarTs, 10)))
    var lockedCar []byte
    err = json.Unmarshal(response.Payload, &lockedCar)
    if err != nil {
        t.Error("Failed to fetch car")
    }

    // unlock car
    symmetricKey := encrypt.NewSymmetricKey(carKey)
    carAsBytes, err := symmetricKey.Decrypt(lockedCar)
    if err != nil {
        t.Error("Error unlocking car")
    }

    car := Car{}
    err = json.Unmarshal(carAsBytes, &car)
    if (err != nil) {
        t.Error(err.Error())
    }

    fmt.Println(car)
    if (car.Vin != vin) {
        t.Error("Car VIN does not match")
    }

    // check out the car index, should contain one car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", carIndexStr))
    carIndex := make(map[string]string)
    err = json.Unmarshal(response.Payload, &carIndex)

    if err != nil {
        t.Error(err.Error())
    }

    if (carIndex[strconv.FormatInt(car.CreatedTs, 10)] != username) {
        t.Error("This is not the car '" + username + "' created")
    }
}