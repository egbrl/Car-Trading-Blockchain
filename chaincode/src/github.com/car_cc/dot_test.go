package main

import (
	"encoding/json"
	"fmt"
	"testing"

	"github.com/hyperledger/fabric/common/util"
	"github.com/hyperledger/fabric/core/chaincode/shim"
)

func TestIsConfirmed(t *testing.T) {
	// create a new car without numberplate
	car := &Car{}

	if IsConfirmed(car) {
		t.Error("Car should not be confirmed initially")
	}
}

func TestIsRegistered(t *testing.T) {
	// create a new car without certificate
	car := &Car{}

	if IsRegistered(car) {
		t.Error("Car should not be registered initially")
	}
}

func TestReadRegistrationProposalsAndRegisterCar(t *testing.T) {
	var username string = "amag"
	var vin string = "WVW ZZZ 6RZ HY26 0780"
	var carData string = `{ "vin": "` + vin + `" }`

	// create and name a new chaincode mock
	carChaincode := &CarChaincode{}
	stub := shim.NewMockStub("car", carChaincode)

	ccSetup(t, stub)

	// create a new car
	response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

	// payload should contain the car...
	car := Car{}
	err := json.Unmarshal(response.Payload, &car)
	if err != nil {
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
	if err != nil {
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

func TestRevocationIndex(t *testing.T) {
	var username string = "amag"
	var vin string = "WVW ZZZ 6RZ HY26 0780"
	var carData string = `{ "vin": "` + vin + `" }`
	var numberplate string = "ZH 7878"
	var insuranceCompany string = "axa"

	// create and name a new chaincode mock
	carChaincode := &CarChaincode{}
	stub := shim.NewMockStub("car", carChaincode)

	ccSetup(t, stub)

	// create a new car
	response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

	// payload should contain the car...
	car := Car{}
	err := json.Unmarshal(response.Payload, &car)
	if err != nil {
		t.Error("Error creating car")
	}

	fmt.Printf("Successfully created car with ts '%d'\n", car.CreatedTs)

	// register the car as DOT user
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("register", username, "dot", vin))
	err = json.Unmarshal(response.Payload, &car)
	if err != nil {
		t.Error(response.Message)
	}

	// make an insurance proposal for AXA
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insureProposal", username, "user", vin, insuranceCompany))
	proposal := InsureProposal{}
	err = json.Unmarshal(response.Payload, &proposal)
	if err != nil {
		t.Error("Error while creating insurance proposal")
	}

	// and let axa insure the car
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insuranceAccept", "insurance-username-test-xyz", "insurer", username, vin, insuranceCompany))
	err = json.Unmarshal(response.Payload, &proposal)
	if err != nil {
		t.Error("Error while accepting insurance proposal")
	}

	// get a numberplate (confirmation)
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("confirm", username, "dot", vin, numberplate))
	err = json.Unmarshal(response.Payload, &car)
	if err != nil {
		t.Error("Error assigning numberplate")
	}

	if !IsConfirmed(&car) {
		t.Error("Car should be confirmed by now")
	}

	// checkout revocation proposals, should have none
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("getRevocationProposals", username, "dot"))
	index := make(map[string]string)
	err = json.Unmarshal(response.Payload, &index)

	if len(index) != 0 {
		t.Error("There should not be any revocation proposals")
	}

	// create a proposal
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("revocationProposal", username, "user", vin))
	if response.Payload != nil {
		t.Error("Error creating revocation proposal")
	}

	// read proposals again
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("getRevocationProposals", username, "dot"))
	err = json.Unmarshal(response.Payload, &index)
	if err != nil {
		t.Error("Error reading revocation proposals")
	}

	if len(index) != 1 {
		t.Error("There should be a revocation proposal now")
	}

	if index[car.Vin] != username {
		t.Error("The revocation proposal was intended for another car/username")
	}

	fmt.Println("Current revocation proposals:")
	fmt.Println(index)

	// revoke numberplate
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("revoke", username, "dot", vin))
	err = json.Unmarshal(response.Payload, &car)
	if err != nil {
		t.Error("Error revoking numberplate")
	}

	if IsConfirmed(&car) {
		t.Error("Car should be revoked by now")
	}

	// read proposals again
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("getRevocationProposals", username, "dot"))
	index = make(map[string]string)
	err = json.Unmarshal(response.Payload, &index)

	fmt.Println("Revocation proposal index after revocation:")
	fmt.Println(index)

	if len(index) != 0 {
		t.Error("The revocation proposal should get deleted after revocation")
	}
}

func TestConfirmRevokeAndDelete(t *testing.T) {
	var username string = "amag"
	var vin string = "WVW ZZZ 6RZ HY26 0780"
	var carData string = `{ "vin": "` + vin + `" }`
	var numberplate string = "ZH 7878"
	var insuranceCompany string = "axa"

	// create and name a new chaincode mock
	carChaincode := &CarChaincode{}
	stub := shim.NewMockStub("car", carChaincode)

	ccSetup(t, stub)

	// create a new car
	response := stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

	// payload should contain the car...
	car := Car{}
	err := json.Unmarshal(response.Payload, &car)
	if err != nil {
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
	proposal := InsureProposal{}
	err = json.Unmarshal(response.Payload, &proposal)
	if err != nil {
		t.Error("Error while creating insurance proposal")
	}

	// and let axa insure the car
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("insuranceAccept", "insurance-username-test-xyz", "insurer", username, vin, insuranceCompany))
	err = json.Unmarshal(response.Payload, &proposal)
	if err != nil {
		t.Error("Error while accepting insurance proposal")
	}

	fmt.Println(proposal)

	// fetch the car a new to check for insurance
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readCar", username, "TESTING", car.Vin))
	err = json.Unmarshal(response.Payload, &car)
	if err != nil {
		t.Error("Failed to fetch car")
	} else if car.Certificate.Insurer != insuranceCompany {
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

	// revoke numberplate
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("revoke", username, "dot", vin))
	err = json.Unmarshal(response.Payload, &car)
	if err != nil {
		t.Error("Error revoking numberplate")
	}

	if IsConfirmed(&car) {
		t.Error("Car should be revoked by now")
	} else if car.Certificate.Insurer != "" {
		t.Error("Revocation includes cancelation of the insurance contract")
	} else if car.Certificate.Numberplate != "" {
		t.Error("Revocation includes removal of the numberplate")
	}

	fmt.Println(car.Certificate)

	// delete the car from the ledger
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("delete", username, "dot", vin))
	if response.Payload != nil {
		t.Error("Car deletion unsuccessfull")
	}

	// try to fetch the delete car from the ledger
	// (should be impossible)
	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readCar", username, "TESTING", car.Vin))
	err = json.Unmarshal(response.Payload, &car)
	if err == nil {
		t.Error("Failed to delete car")
	}
}

func TestReadRegistrationProposals(t *testing.T) {
	username         := "test"
    vin              := "WVW ZZZ 6RZ HY26 0780"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    carData := `{ "vin": "` + vin + `" }`
    stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

    // read proposals as map
    response := stub.MockInvoke(uuid, util.ToChaincodeArgs("readRegistrationProposals", "dot-user", "dot"))
	index := make(map[string]RegistrationProposal)
	err := json.Unmarshal(response.Payload, &index)

	if err != nil {
		t.Error("Error reading registration proposals as map")
		return
	}

	if len(index) != 1 {
		t.Error("Registration proposal not added during car creation")
		return
	}

	// read proposals as list
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readRegistrationProposalsAsList", "dot-user", "dot"))
	var proposalList []RegistrationProposal
	err = json.Unmarshal(response.Payload, &proposalList)

	if err != nil {
		t.Error("Error reading registration proposals as list")
		return
	}

	if len(proposalList) != 1 {
		t.Error("Registration proposal not added during car creation")
		return
	}

	// read single proposal
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readRegistrationProposal", "dot-user", "dot", vin))
	var proposal RegistrationProposal
	err = json.Unmarshal(response.Payload, &proposal)

	if err != nil {
		t.Error("Error reading registration proposal")
		return
	}

	if proposal.Car != vin {
		t.Error("Read wrong proposal")
		return
	}

	// register car
	stub.MockInvoke(uuid, util.ToChaincodeArgs("register", "dot-user", "dot", vin))

    // read proposals again
    response = stub.MockInvoke(uuid, util.ToChaincodeArgs("readRegistrationProposals", "dot-user", "dot"))
	index = make(map[string]RegistrationProposal)
	err = json.Unmarshal(response.Payload, &index)

	if len(index) > 0 {
		t.Error("Registration proposal not removed after registration")
		return
	}
}

func TestCarsToConfirmList(t *testing.T) {
	username         := "test"
    vin              := "WVW ZZZ 6RZ HY26 0780"
    insuranceCompany := "axa"

    // create and name a new chaincode mock
    carChaincode := &CarChaincode{}
    stub := shim.NewMockStub("car", carChaincode)

    ccSetup(t, stub)

    // create a new car
    carData := `{ "vin": "` + vin + `" }`
    stub.MockInvoke(uuid, util.ToChaincodeArgs("create", username, "garage", carData))

    // make an insurance proposal for AXA
    stub.MockInvoke(uuid, util.ToChaincodeArgs("insureProposal", username, "user", vin, insuranceCompany))

	response := stub.MockInvoke(uuid, util.ToChaincodeArgs("getCarsToConfirmAsList", "dot-user", "dot"))
	var cars []Car
	err := json.Unmarshal(response.Payload, &cars)
	fmt.Println(cars)
	if err != nil {
		t.Error("Error getting cars to confirm")
		return
	}

	if len(cars) > 0 {
		t.Error("Unregistered car should not be confirmed")
		return
	}

	// register car
	stub.MockInvoke(uuid, util.ToChaincodeArgs("register", "dot-user", "dot", vin))

	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("getCarsToConfirmAsList", "dot-user", "dot"))
	err = json.Unmarshal(response.Payload, &cars)
	if err != nil {
		t.Error("Error getting cars to confirm")
		return
	}

	if len(cars) > 0 {
		t.Error("Uninsured car should not be confirmed")
		return
	}

	// accept insurance
	stub.MockInvoke(uuid, util.ToChaincodeArgs("insuranceAccept", "insurance-user", "insurer", username, vin, insuranceCompany))

	response = stub.MockInvoke(uuid, util.ToChaincodeArgs("getCarsToConfirmAsList", "dot-user", "dot"))
	err = json.Unmarshal(response.Payload, &cars)
	if err != nil {
		t.Error("Error getting cars to confirm")
		return
	}

	if len(cars) != 1 && cars[0].Vin != vin {
		t.Error("Function does not return the right cars ready for confirmation")
		return
	}
}