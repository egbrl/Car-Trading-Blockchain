package main

import (
    "encoding/json"
    "testing"
    "strconv"
    "fmt"
    "github.com/hyperledger/fabric/common/util"
    "github.com/hyperledger/fabric/core/chaincode/shim"
)

func TestUser(t *testing.T) {
    var root string = "test"
    var user string = "test2"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create user 'test' and 'test2'
    stub.MockInvoke(uuid, util.ToChaincodeArgs("createUser", root, "user", root))
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("createUser", root, "user", user))

    userObject := User {}
    err := json.Unmarshal(response.Payload, &userObject)
    if err != nil {
        t.Error("Error creating test user")
    }

    if userObject.Balance != 0 {
        t.Error("New user should start with balance 0")
    }

    // read the user again
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readUser", user, "user"))
    userObject = User {}
    err = json.Unmarshal(response.Payload, &userObject)
    if err != nil {
        fmt.Println(err.Error())
        t.Error("Error reading test user")
        return
    }

    if userObject.Name != user {
        t.Error("User creation error")
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

    // delete user 'test2'
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("deleteUser", user, "user", user, root))
    if response.Payload != nil {
        t.Error("Error deleting user")
        return
    }

    // check that user was deleted
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readUser", user, "user"))
    if response.Status != 500 {
        t.Error("User not deleted")
        return
    }

    // read the root user
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readUser", root, "user"))
    userObject = User {}
    err = json.Unmarshal(response.Payload, &userObject)
    if err != nil {
        fmt.Println(response.Payload)
        t.Error("Error reading root user")
        return
    }

    if userObject.Balance != -5 {
        fmt.Println(userObject.Balance)
        t.Error("User balance not transferred successfully after deleting user")
    }
}