package main

import (
    "encoding/json"
    "github.com/hyperledger/fabric/core/chaincode/shim"
)

/*
 * Clears an index on the ledger
 */
func clearIndex(indexStr string, stub shim.ChaincodeStubInterface) error {
    index := make(map[int64]string)

    jsonAsBytes, _ := json.Marshal(index)
    return stub.PutState(indexStr, jsonAsBytes)
}