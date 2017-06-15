package main

import (
	"fmt"
	"encoding/json"
	"errors"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

/*
 * Checks for an active car insurance.
 *
 * A vehicle can be registered by the DOT, but still lack
 * an insurance contract. This is the case if the car has no
 * numberplate (is not confirmed) yet.
 * 
 * On the other hand the vehicle could already be insured,
 * but still be waiting for a valid numberplate. This case may
 * occur when changing numberplates.
 *
 * In any case, the car has to be registered before it can be insured.
 */
func IsInsured(car *Car) bool {
	// cannot be insured without car papers
	if (!IsRegistered(car)) {
		return false
	}

	insured := car.Certificate.Insurer != ""

	if (insured) {
		fmt.Printf("Car with VIN '%s' is insured by company '%s'\n", car.Vin, car.Certificate.Insurer)
	} else {
		fmt.Printf("Car with VIN '%s' is not insured\n", car.Vin)
	}
	
	return insured
}

/*
 * Returns the insurer index
 */
func (t *CarChaincode) getInsurerIndex(stub shim.ChaincodeStubInterface) (map[string]Insurer, error) {
	response := t.read(stub, insurerIndexStr)
	insurerIndex := make(map[string]Insurer)
	err := json.Unmarshal(response.Payload, &insurerIndex)
	if err != nil {
		return nil, errors.New("Error parsing insurer index")
	}

	return insurerIndex, nil
}

/*
 * Returns an insurer with a list of insurance proposals.
 */
func (t *CarChaincode) getInsurer(stub shim.ChaincodeStubInterface, company string) pb.Response {
	// load all insurers
	insurerIndex, err := t.getInsurerIndex(stub)
	if err != nil {
		return shim.Error("Error reading insurer index")
	}

	ret := insurerIndex[company]
	retAsBytes, _ := json.Marshal(ret)
	return shim.Success(retAsBytes)
}

/*
 * Accpets an insurance proposal for a car
 * and creates an insurance contract. The proposal
 * will be removed from the ledger afterwards.
 *
 * The car needs to be registered.
 * A car numberplate (confirmation) is not required.
 *
 * On success,
 * returns the removed insurance proposal
 */
func (t *CarChaincode) insuranceAccept(stub shim.ChaincodeStubInterface, username string, vin string, company string) pb.Response {
	carResponse := t.readCar(stub, username, vin)
	car := Car {}
	json.Unmarshal(carResponse.Payload, &car)

	insurerIndex, err := t.getInsurerIndex(stub)
	if err != nil {
		return shim.Error("Error fetching insurer index")
	}

	insurer := insurerIndex[company]
	proposals := insurer.Proposals
	validProposal := InsureProposal {}
	var newProposals []InsureProposal
	for i := 0; i < len(proposals); i++ {
		newProposals = append(newProposals, proposals[i])

		if proposals[i].Car == vin && proposals[i].User == username {
			// check if we can create an insurance contract
			// we can only create an insurance contract,
			// if we are sure the car VIN is approved by the DOT
			// and the car has a valid certificate
			if !IsRegistered(&car) {
				return shim.Error("Go register your car first")
			}

			// insure the car
			car.Certificate.Insurer = company
			carAsBytes, err := json.Marshal(car)
			err = stub.PutState(car.Vin, carAsBytes)
			if err != nil {
				return shim.Error("Error writing car")
			}

			// remove proposal
			validProposal = proposals[i]
			newProposals = newProposals[:i]
		}
	}

	// write udpated insurer index back to ledger
	insurer.Proposals = newProposals
	insurerIndex[company] = insurer
	indexAsBytes, _ := json.Marshal(insurerIndex)
	err = stub.PutState(insurerIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing insurer index")
	}

	propAsBytes, _ := json.Marshal(validProposal)
	return shim.Success(propAsBytes)
}

/*
 * Creates an insurance proposal for an insurance
 * company 'company' and a car with 'vin'.
 *
 * The car does not need to be registered.
 * A car numberplate is not required.
 * The proposal will be recorded even if no
 * insurance company with that name exists.
 *
 * On success,
 * returns the insurance proposal
 */
func (t *CarChaincode) insureProposal(stub shim.ChaincodeStubInterface, username string, vin string, company string) pb.Response {
	carResponse := t.readCar(stub, username, vin)
	car := Car {}
	json.Unmarshal(carResponse.Payload, &car)

	// load all insurers
	insurerIndex, err := t.getInsurerIndex(stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	// check if this insurance company even exists
	// if not, just save the proposal anyway
	insurer := insurerIndex[company]
	if insurer.Name == "" {
		fmt.Printf("Insurance company '%s' does not exist yet\nSaving your proposal anyway\n", company)
		// Create a new insurer,
		// mainly just to save the proposal somewhere
		insurer = Insurer { Name: company }
	}

	// create the proposal
	proposal := InsureProposal { User: username,
								 Car:  vin }
	
	// inform the insurer of the new proposal
	insurer.Proposals = append(insurer.Proposals, proposal)
	insurerIndex[company] = insurer

	// write udpated insurer index back to ledger
	indexAsBytes, _ := json.Marshal(insurerIndex)
	err = stub.PutState(insurerIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing insurer index")
	}

	proposalAsBytes, _ := json.Marshal(proposal)
	return shim.Success(proposalAsBytes)
}