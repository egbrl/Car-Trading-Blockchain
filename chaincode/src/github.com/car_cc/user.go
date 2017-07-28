package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"strconv"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

/*
 * Creates a new user and appends it to the user index.
 * Returns an error if a user with the desired username already exists.
 *
 * On success,
 * returns the user.
 */
func (t *CarChaincode) createUser(stub shim.ChaincodeStubInterface, username string) pb.Response {
	// check if user with this username already exists
	_, err := t.getUser(stub, username)
	if err == nil {
		return shim.Error(fmt.Sprintf("User with username '%s' already exists. Choose another username.", username))
	}

	// user does not exist yet,
	// create user
	fmt.Printf("User '%s' does not exist yet\nSaving new user with that username\n", username)
	user := User{Name: username, Cars: []string{}, Balance: 0, Offers: []Offer{}}

	userIndex, err := t.getUserIndex(stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	// map the user to the userIndex
	userIndex[username] = username
	fmt.Printf("Added user with Username '%s' to user index.\n", username)

	// write udpated user index back to ledger
	indexAsBytes, _ := json.Marshal(userIndex)
	err = stub.PutState(userIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing updated user index to ledger")
	}

	// write new user to ledger
	err = t.saveUser(stub, user)
	if err != nil {
		return shim.Error(err.Error())
	}

	// user creation successfull,
	// return the user
	userAsBytes, _ := json.Marshal(user)
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

	// getting user which shall be deleted
	userToDelete, err := t.getUser(stub, username)
	if err != nil {
		return shim.Error("User to delete does not exist. Username: '" + username + "'")
	}

	// getting the user which receives the remaining balance
	balanceRecipient, err := t.getUser(stub, remainingBalanceRecipient)
	if err != nil {
		return shim.Error("User does not exist. Username: '" + username + "'")
	}

	// check if user doesn't own a car anymore
	if len(userToDelete.Cars) != 0 {
		return shim.Error("Deletion of user not possible. User '" + username + "' still owns '" + string(len(userToDelete.Cars)) + "' cars.")
	}

	// transfer remaining balance to chosen recipient
	balanceRecipient.Balance += userToDelete.Balance

	// delete user from user index
	delete(userIndexMap, userToDelete.Name)

	// write udpated user index back to ledger
	indexAsBytes, _ := json.Marshal(userIndexMap)
	err = stub.PutState(userIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing user index")
	}

	// Delete the user key from the state in ledger
	err = stub.DelState("usr_" + userToDelete.Name)
	if err != nil {
		return shim.Error("Failed to delete user from state")
	}

	fmt.Printf("Successfully deleted user with username: '%s'\n", userToDelete.Name)
	return shim.Success(nil)
}

/*
 * Returns the user index
 */
func (t *CarChaincode) getUserIndex(stub shim.ChaincodeStubInterface) (map[string]string, error) {
	response := t.read(stub, userIndexStr)
	userIndex := make(map[string]string)
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
	response := t.read(stub, "usr_"+username)
	var user User
	err := json.Unmarshal(response.Payload, &user)
	if err != nil {
		return User{}, errors.New("Error parsing user index")
	}

	return user, nil
}

/*
 * Reads a User from ledger
 */
func (t *CarChaincode) readUser(stub shim.ChaincodeStubInterface, username string) pb.Response {
	user, err := t.getUser(stub, username)

	if err != nil {
		return shim.Error("Error reading user (function: readUser, file: user.go)")
	}

	userAsBytes, err := json.Marshal(user)

	if err != nil {
		return shim.Error("Invalid User format")
	}
	return shim.Success(userAsBytes)
}

/*
 * Writes updated user back to ledger
 */
func (t *CarChaincode) saveUser(stub shim.ChaincodeStubInterface, user User) error {
	userAsBytes, err := json.Marshal(user)

	if err != nil {
		fmt.Println(user)
		return errors.New("User has wrong format")
	}

	err = stub.PutState("usr_"+user.Name, userAsBytes)
	if err != nil {
		return errors.New("Error writing user back to ledger")
	}

	return nil
}

/*
 * Updates User balance
 *
 * The update amount (can be positive or negative)
 * is added to the user balance.
 *
 * Expects 'args':
 *  username              string
 *  updateAmount          string
 *
 * On success,
 * returns updated user balance
 */
func (t *CarChaincode) updateBalance(stub shim.ChaincodeStubInterface, username string, updateAmount string) pb.Response {
	amount, _ := strconv.Atoi(updateAmount)

	// fetch user
	user, err := t.getUser(stub, username)
	if err != nil {
		fmt.Println(err.Error())

		return shim.Error("Error fetching user, balance not updated")
	}

	// update user balance
	user.Balance = user.Balance + amount

	// save updated user
	err = t.saveUser(stub, user)
	if err != nil {
		return shim.Error("Error writing user, balance not updated")
	}

	fmt.Printf("Balance of user '" + user.Name + "' successfully updated\n")

	balanceAsBytes := []byte(strconv.Itoa(user.Balance))
	return shim.Success(balanceAsBytes)
}
