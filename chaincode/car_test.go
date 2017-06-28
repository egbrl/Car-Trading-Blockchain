package main

import (
    "encoding/json"
    "fmt"
    "testing"

    "github.com/hyperledger/fabric/common/util"
    "github.com/hyperledger/fabric/core/chaincode/shim"
)

func ccSetup(t *testing.T, stub *shim.MockStub) {
    // a successfull init should not return any errors
    response := stub.MockInit(uuid, util.ToChaincodeArgs("init", "999"))
    if response.Payload != nil {
        t.Error(response.Payload)
    }

    // init should write a test on the ledger
    testAsBytes, err := stub.GetState("abc")
    if err != nil {
        t.Error("Failed to read test var from ledger")
    }

    var aval int
    json.Unmarshal(testAsBytes, &aval)

    if aval != 999 {
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

func TestInit(t *testing.T) {
    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)
}

func TestTransferCar(t *testing.T) {
    var username string = "amag"
    var receiver string = "bobby"
    var vin string = "WVW ZZZ 6RZ HY26 0780"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    carData := `{ "vin": "` + vin + `" }`
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

    // payload should contain the car
    car := Car{}
    err := json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error(err.Error())
    }

    fmt.Printf("Successfully created car with ts '%d'\n", car.CreatedTs)

    // register the car as DOT user
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", vin))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error("Error registering the car")
    }

    if !IsRegistered(&car) {
        t.Error("Car should now be registered!")
    }

    // transfer the car
    // the new car owner (receiver 'bobby') will get created
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("transfer", username, "garage", vin, receiver))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        fmt.Println(response.Message)
        t.Error("Error transferring car")
    }

    // check that the old owner has no longer access to the car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readCar", username, "TESTING", car.Vin))
    err = json.Unmarshal(response.Payload, &car)
    if err == nil {
        fmt.Println(response.Message)
        t.Error("The old car owner should no longer have access to the car")
    }

    // check that bobby has access to the car now
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readCar", receiver, "TESTING", car.Vin))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error("Error transferring car ownership in the cars certificate")
    }

    // checkout bobbys user record
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", userIndexStr))
        userIndex := make(map[string]User)
        err = json.Unmarshal(response.Payload, &userIndex)
    if err != nil {
        t.Error("Error fetching user index")
    }
    receiverAsUser := userIndex[receiver]

    fmt.Printf("New owner/receiver with cars: %v\n", receiverAsUser)

    if receiverAsUser.Cars[0] != vin {
        t.Error("Car transfer unsuccessfull")
    }

    // checkout the old owners user record
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", userIndexStr))
        userIndex = make(map[string]User)
    err = json.Unmarshal(response.Payload, &userIndex)
    if err != nil {
        t.Error("Error fetching user index")
    }
        oldOwnerAsUser := userIndex[username]

    fmt.Printf("Old owner with cars: %v\n", oldOwnerAsUser)

    // the old owner should be left with 0 cars
    if len(oldOwnerAsUser.Cars) != 0 {
        t.Error("Car transfer unsuccessfull")
    }

    // check out the new car index and see
    // that ownership righs are registered properly
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", carIndexStr))
    carIndex := make(map[string]string)
    err = json.Unmarshal(response.Payload, &carIndex)

    fmt.Printf("Car index after transfer: %v\n", carIndex)

    if carIndex[vin] != receiver {
        t.Error("Car transfer unsuccessfull")
    }
}

func TestSellCar(t *testing.T) {
    var username string = "amag"
    var receiver string = "bobby"
    var vin string = "WVW ZZZ 6RZ HY26 0780"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    carData := `{ "vin": "` + vin + `" }`
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

    // payload should contain the car
    car := Car{}
    err := json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error(err.Error())
    }

    fmt.Printf("Successfully created car with ts '%d'\n", car.CreatedTs)

    // register the car as DOT user
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", vin))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error("Error registering the car")
    }

    if !IsRegistered(&car) {
        t.Error("Car should now be registered!")
    }

    // sell the car, but for less than 100 credits
    // the new car owner (receiver 'bobby') will get created
    // with a balance of 100
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("sell", username, "garage", "99", vin, receiver))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error("Error selling car")
    }

    // check that the old owner has no longer access to the car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readCar", username, "TESTING", car.Vin))
    err = json.Unmarshal(response.Payload, &car)
    if err == nil {
        fmt.Println(response.Message)
        t.Error("The old car owner should no longer have access to the car")
    }

    // check that bobby has access to the car now
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readCar", receiver, "TESTING", car.Vin))
    err = json.Unmarshal(response.Payload, &car)
    if err != nil {
        t.Error("Error transferring car ownership in the cars certificate")
    }

    // checkout bobbys user record
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", userIndexStr))
        userIndex := make(map[string]User)
    err = json.Unmarshal(response.Payload, &userIndex)
    if err != nil {
        t.Error("Error fetching user index")
    }
        receiverAsUser := userIndex[receiver]

    fmt.Printf("New owner/receiver with cars: %v\n", receiverAsUser)

    if receiverAsUser.Cars[0] != vin {
        t.Error("Car transfer unsuccessfull")
    }

    // checkout the old owners user record
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", userIndexStr))
        userIndex = make(map[string]User)
    err = json.Unmarshal(response.Payload, &userIndex)
    if err != nil {
        t.Error("Error fetching user index")
    }
    oldOwnerAsUser := userIndex[username]

    fmt.Printf("Old owner with cars: %v\n", oldOwnerAsUser)

    // the old owner should be left with 0 cars
    if len(oldOwnerAsUser.Cars) != 0 {
        t.Error("Car transfer unsuccessfull")
    }

    // check out the new car index and see
    // that ownership righs are registered properly
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", carIndexStr))
    carIndex := make(map[string]string)
    err = json.Unmarshal(response.Payload, &carIndex)

    fmt.Printf("Car index after transfer: %v\n", carIndex)

    if carIndex[vin] != receiver {
        t.Error("Car transfer unsuccessfull")
    }

    // check new balances of seller (old owner)
    if oldOwnerAsUser.Balance != 199 {
        t.Error("Sellers balance not updated")
    }

    // check new balances of buyer
    if receiverAsUser.Balance != 1 {
        t.Error("Buyers balance not updated")
    }
}

func TestCreateAndReadCar(t *testing.T) {
    username := "amag"
    vin := "WVW ZZZ 6RZ HY26 0780"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    // and provide additional registration data for the DOT
    carData := `{ "vin": "` + vin + `" }`
    registrationData := `{ "number_of_doors":     "4+1",
                           "number_of_cylinders":  4,
                           "number_of_axis":       2,
                           "max_speed":            200 }`
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData, registrationData))

    // payload should contain the car
    carCreated := Car{}
    err := json.Unmarshal(response.Payload, &carCreated)
    if err != nil {
        t.Error(err.Error())
    }

    fmt.Printf("Successfully created car with ts '%d'\n", carCreated.CreatedTs)

    // check out the car index, should contain one car
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("read", "TESTING", "TESTING", carIndexStr))
    carIndex := make(map[string]string)
    err = json.Unmarshal(response.Payload, &carIndex)

    if err != nil {
        t.Error("Failed to fetch car index")
    } else if len(carIndex) > 1 {
        t.Error("The car index should only contain one car by now")
    } else if carIndex[carCreated.Vin] != username {
        t.Error("This is not the car '" + username + "' created")
    }

    // the user should only have one car by now
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readCar", username, "TESTING", carCreated.Vin))
    carFetched := Car{}
    err = json.Unmarshal(response.Payload, &carFetched)
    if err != nil {
        t.Error("Failed to fetch car")
    } else if carFetched.Vin != carCreated.Vin {
        t.Error("Car VIN does not match")
    } else if carFetched.CreatedTs != carCreated.CreatedTs {
        t.Error("This is not the car you created before")
    }

    // create a car with the same vin
    // should get rejected with an error msg
    // also tests to create cars without the additional registration data
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))
    err = json.Unmarshal(response.Payload, &carCreated)
    if err == nil {
        t.Error(fmt.Sprintf("Only one car with vin '%s' can exist", vin))
    }

    fmt.Println(carFetched)
}
