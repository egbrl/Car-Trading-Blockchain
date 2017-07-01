package main

import (
	"encoding/json"
	"errors"
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

/*
 * Creates a new user
 *   - appends it to the user index
 *   - appends saves .
 * Returns an error if a user with the desired username already exists.
 *
 * Until we have an interface to stock up user credits,
 * every new user gets 100 credits for free to buy cars.
 *
 * On success,
 * returns the user.
 */
func (t *CarChaincode) createUser(stub shim.ChaincodeStubInterface, username string) pb.Response {

	// check if user with this username already exists
	userIndex, err := t.getUserIndex(stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	user, err := t.getUser(stub, username)

	if err != nil {
		fmt.Printf("User '%s' does not exist yet\nSaving new user with that username\n", username)
		// Create a new user,
		user = User{Name: username, Cars: []string{}, Balance: 100}
	} else {
		return shim.Error(fmt.Sprintf("User with username '%s' already exists. Choose another username.", username))
	}

	// map the user to the userIndex
	userIndex.append
	userIndex[username] = user
	fmt.Printf("Added user with Username '%s' to user index.\n", username)

	// write udpated user index back to ledger
	indexAsBytes, _ := json.Marshal(userIndex)
	err = stub.PutState(userIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing user index")
	}

	userAsBytes, _ := json.Marshal(user)

	// user creation successfull,
	// return the user
	return shim.Success(userAsBytes)
}

/*
 * Deletes a user from the ledger.
 *
 * Returns 'nil' on success.
 */
func (t *CarChaincode) deleteUser(stub shim.ChaincodeStubInterface, username string, remainingBalanceRecipient string) pb.Response {
	userIndexMap, err := t.getUserIndex(stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	//getting the user object
	user, remainingBalanceRecipientUserExisting := userIndexMap[remainingBalanceRecipient]

	//check if user doesn't own a car anymore
	numberOfCars := len(user.Cars)
	if numberOfCars != 0 {
		return shim.Error("User '" + username + "' still owns one or more car. Deletion of user therefore not possible.")
	}

	//transfer remaining balance to chosen recipient
	if remainingBalanceRecipientUserExisting {
		user.Balance += userIndexMap[username].Balance
		userIndexMap[remainingBalanceRecipient] = user
	}

	// delete user from user index
	delete(userIndexMap, username)

	// write udpated user index back to ledger
	indexAsBytes, _ := json.Marshal(userIndexMap)
	err = stub.PutState(userIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing user index")
	}

	fmt.Printf("Successfully deleted user with username: '%s'\n", username)
	return shim.Success(nil)
}

/*
 * Returns the user index
 */
func (t *CarChaincode) getUserIndex(stub shim.ChaincodeStubInterface) ([]string, error) {
	response := t.read(stub, userIndexStr)
	userIndex = []string{}
	err := json.Unmarshal(response.Payload, &userIndex)
	if err != nil {
		return nil, errors.New("Error parsing user index")
	}

	return userIndex, nil
}

/*
 * Reads a User from ledger
 */
func (t *CarChaincode) getUser(stub shim.ChaincodeStubInterface, username string) (User, error) {
	userString := "usr_" + username
	response := t.read(stub, userString)
	var user User
	err := json.Unmarshal(response.Payload, &user)
	if err != nil {
		return User{}, errors.New("Error parsing user index")
	}

	return user, err
}

/*
 * Saves user back to ledger
 */
func (t *CarChaincode) saveUser(stub shim.ChaincodeStubInterface, user User) error {
	response := t.read(stub, userIndexStr)
	userIndex := make(map[string]User)
	err := json.Unmarshal(response.Payload, &userIndex)
	if err != nil {
		return errors.New("Error parsing user index")
	}

	// overwriting existing user with updated user
	userIndex[user.Name] = user

	// write updated user back to ledger
	userAsBytes, _ := json.Marshal(user)
	err = stub.PutState(user.Name, userAsBytes)
	if err != nil {
		return errors.New("Error writing userIndex back to ledger")
	}

	return nil
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
	userIndex, _ := t.getUserIndex(stub)
	userIndex[username] = user
	userIndexAsBytes, _ := json.Marshal(userIndex)
	err = stub.PutState(userIndexStr, userIndexAsBytes)
	if err != nil {
		return User{}, errors.New("Error writing user, balance not updated")
	}

	return user, nil
}
