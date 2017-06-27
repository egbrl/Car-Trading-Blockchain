package main

import (
	"encoding/json"
	"errors"

	"github.com/hyperledger/fabric/core/chaincode/shim"
)

/*
 * Create new user
 */

/*
 * Delete user
 */

/*
 * Returns the user index
 */
func (t *CarChaincode) getUserIndex(stub shim.ChaincodeStubInterface) (map[string]string, error) {
	response := t.read(stub, userIndexStr)
	userIndex := make(map[string]string)
	err := json.Unmarshal(response.Payload, &userIndex)
	if err != nil {
		return nil, errors.New("Error parsing car index")
	}

	return userIndex, nil
}

/*
 * Reads a User from ledger
 */
func (t *CarChaincode) getUser(stub shim.ChaincodeStubInterface, username string) (User, error) {
	response := t.read(stub, username)
	user := User{}
	err := json.Unmarshal(response.Payload, &user)
	if err != nil {
		return User{}, errors.New("Error fetching User")
	}

	return user, nil
}

/*
 * Updates User balance
 */
func (t *CarChaincode) updateBalance(stub shim.ChaincodeStubInterface, username string, balance int) (User, error) {
	// fetch user
	user, err := t.getUser(stub, username)
	if err != nil {
		return User{}, errors.New("Error fetching user, balance not updated")
	}

	// updbalanceate user balance
	user.Balance = balance

	// write user balance back to ledger
	userAsBytes, _ := json.Marshal(user)
	err = stub.PutState(username, userAsBytes)
	if err != nil {
		return User{}, errors.New("Error writing user, balance not updated")
	}

	return user, nil
}
