package main

import (
    "encoding/json"
    "testing"
    "strconv"
    "github.com/hyperledger/fabric/common/util"
    "github.com/hyperledger/fabric/core/chaincode/shim"
)

func TestCreateUserAndUpdateBalance(t *testing.T) {
    var root string = "test"
    var user string = "test2"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create user 'test2'
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("createUser", root, "user", user))

    userObject := User {}
    err := json.Unmarshal(response.Payload, &userObject)
    if err != nil {
        t.Error("Error creating test user")
    }

    if userObject.Balance != 0 {
        t.Error("New user should start with balance 0")
    }

    // update balance of user
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("updateBalance", user, "user", "5"))
    updatedBalance, _ := strconv.Atoi(string(response.Payload))

    if updatedBalance != 5 {
        t.Error("Wrong balance")
    }

    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("updateBalance", user, "user", "-10"))
    updatedBalance, _ = strconv.Atoi(string(response.Payload))

    if updatedBalance != -5 {
        t.Error("Wrong balance")
    }   
}