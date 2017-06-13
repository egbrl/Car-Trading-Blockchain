package main

import (
    "encoding/json"
    "github.com/hyperledger/fabric/core/chaincode/shim"
)

/*
 * Clears an index of type 'map[string]string' on the ledger
 */
func clearCarIndex(indexStr string, stub shim.ChaincodeStubInterface) error {
    index := make(map[string]string)

    jsonAsBytes, err := json.Marshal(index)
    if err != nil {
        return err
    }

    return stub.PutState(indexStr, jsonAsBytes)
}

/*
 * Clears an index of type 'map[string]Insurer' on the ledger
 */
func clearInsurerIndex(indexStr string, stub shim.ChaincodeStubInterface) error {
    index := make(map[string]Insurer)

    jsonAsBytes, err := json.Marshal(index)
    if err != nil {
        return err
    }

    return stub.PutState(indexStr, jsonAsBytes)
}

/*
 * Clears an index of type 'map[string]RegistrationProposal' on the ledger
 */
func clearRegistrationProposalIndex(indexStr string, stub shim.ChaincodeStubInterface) error {
    index := make(map[string]RegistrationProposal)

    jsonAsBytes, err := json.Marshal(index)
    if err != nil {
        return err
    }

    return stub.PutState(indexStr, jsonAsBytes)
}