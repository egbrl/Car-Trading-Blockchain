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
 * A vehicle can be registered and confirmed by the DOT,
 * but still lack an insurance contract. This case can occur
 * when you change the insurer without changing the numberplate.
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