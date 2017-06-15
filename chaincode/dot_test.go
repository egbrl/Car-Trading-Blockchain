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

func TestReadRegistrationProposalsAndRegisterCar(t *testing.T) {
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