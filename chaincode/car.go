package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"time"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

/*
 * Returns the car index
 */
func (t *CarChaincode) getCarIndex(stub shim.ChaincodeStubInterface) (map[string]string, error) {
	response := t.read(stub, carIndexStr)
	carIndex := make(map[string]string)
	err := json.Unmarshal(response.Payload, &carIndex)
	if err != nil {
		return nil, errors.New("Error parsing car index")
	}

	return carIndex, nil
}

/*
 * Reads the car index at key 'vin'
 *
 * Returns username of car owner with VIN 'vin'.
 */
func (t *CarChaincode) getOwner(stub shim.ChaincodeStubInterface, vin string) (string, error) {
	carIndex, err := t.getCarIndex(stub)
	if err != nil {
		return "", err
	}
	return carIndex[vin], nil
}

/*
 * Creates a new, unregistered car with the current timestamp
 * and appends it to the car index. Returns an error if a
 * car with the desired VIN already exists.
 *
 * A registration proposal will be issued on successfull car creation.
 * For this proposal, optional registration data can be passed to
 * 'create' to create a tailored registration proposal.
 *
 * Expects 'args':
 *  Car with VIN                             json
 *  (optional) RegistrationProposal          json
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) create(stub shim.ChaincodeStubInterface, username string, args []string) pb.Response {
	if len(args) < 1 {
		return shim.Error("'create' expects Car with VIN as json")
	}

	// create new registration proposal for the DOT
	regProposal := RegistrationProposal{}

	// if provided, read additional registration data
	if len(args) > 1 {
		fmt.Printf("Received registration data: %s\n", args[1])
		err := json.Unmarshal([]byte(args[1]), &regProposal)
		if err != nil {
			fmt.Println("Unable to parse your registration data")
		}
	}

	// let the invoker know if his data was well formatted
	fmt.Printf("Creating car with parsed registration proposal: %v\n", regProposal)

	// create car from arguments
	car := Car{}
	err := json.Unmarshal([]byte(args[0]), &car)
	if err != nil {
		return shim.Error("Error parsing car data. Expecting Car with VIN as json.")
	}

	// add car birth date
	car.CreatedTs = time.Now().Unix()

	// create user from arguments
	user := User{Balance: 100}

	// check for existing garage user with that name
	response := t.read(stub, username)
	existingUser := User{}
	err = json.Unmarshal(response.Payload, &existingUser)
	if err == nil {
		user = existingUser
	} else {
		user.Name = username
	}

	// check for an existing car with that vin in the car index
	owner, err := t.getOwner(stub, car.Vin)
	if err != nil {
		return shim.Error(err.Error())
	} else if owner != "" {
		return shim.Error(fmt.Sprintf("Car with vin '%s' already exists. Choose another vin.", car.Vin))
	}

	// save car to ledger, the car vin serves
	// as the index to find the car again
	carAsBytes, _ := json.Marshal(car)
	err = stub.PutState(car.Vin, carAsBytes)
	if err != nil {
		return shim.Error("Error writing car")
	}

	// map the car to the users name
	carIndex, err := t.getCarIndex(stub)
	if err != nil {
		return shim.Error(err.Error())
	}
	carIndex[car.Vin] = user.Name
	fmt.Printf("Added car with VIN '%s' created at '%d' in garage '%s' to car index.\n",
		car.Vin, car.CreatedTs, user.Name)

	// write udpated car index back to ledger
	indexAsBytes, _ := json.Marshal(carIndex)
	err = stub.PutState(carIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing car index")
	}

	// hand over the car and write user to ledger
	user.Cars = append(user.Cars, car.Vin)
        userIndex, _ := t.getUserIndex(stub)
        userIndex[user.Name] = user
        userIndexAsBytes, _ := json.Marshal(userIndex)
        err = stub.PutState(userIndexStr, userIndexAsBytes)
	if err != nil {
		return shim.Error("Error saving user")
	}

	// load all proposals
	proposalIndex, err := t.getRegistrationProposals(stub)
	if err != nil {
		return shim.Error("Error loading registration proposal index")
	}

	// update the car vin in the registration proposal
	// and save the proposal for the DOT
	regProposal.Car = car.Vin
	proposalIndex[car.Vin] = regProposal

	// write udpated proposal index back to ledger
	// for the DOT to read and register the car
	indexAsBytes, _ = json.Marshal(proposalIndex)
	err = stub.PutState(registrationProposalIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing registration proposal index")
	}

	// car creation successfull,
	// return the car
	return shim.Success(carAsBytes)
}

/*
 * Reads a car.
 *
 * Only the car owner can read the car.
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) readCar(stub shim.ChaincodeStubInterface, username string, vin string) pb.Response {
	if vin == "" {
		return shim.Error("'readCar' expects a non-empty VIN to do the look up")
	}

	// fetch the car from the ledger
	carResponse := t.read(stub, vin)
	car := Car{}
	err := json.Unmarshal(carResponse.Payload, &car)
	if err != nil {
		return shim.Error("Failed to fetch car with vin '" + vin + "' from ledger")
	}

	// fetch the car index to check if the user owns the car
	owner, err := t.getOwner(stub, vin)
	if err != nil {
		return shim.Error(err.Error())
	} else if owner != username {
		return shim.Error("Forbidden: this is not your car")
	}

	return shim.Success(carResponse.Payload)
}

/*
 * Sell a car to a new owner (receiver).
 *
 * The car can only be sold if the buyer/receiver
 * has enough credits (balance sufficiently high)
 *
 * Arguments required:
 * [0] Price                       (int)
 * [1] VIN of the car to transfer  (string)
 * [2] Buyer username              (string)
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) sell(stub shim.ChaincodeStubInterface, seller string, args []string) pb.Response {
	price := args[0]
	priceAsInt, _ := strconv.Atoi(args[0])
	buyer := args[2]

	// price input sanitation
	if price == "" || priceAsInt < 0 {
		return shim.Error("'sell' expects a non-empty, positive price")
	}

	//////////////////////////////////////////////////////////
	//                     BUYER                            //
	//////////////////////////////////////////////////////////

	// fetch buyer and balance
	buyerAsUser, err := t.getUser(stub, buyer)
	if err != nil {
		// buyer does not exist yet
		// create and give her some credits to buy cars
		userResponse := t.createUser(stub, buyer)
                buyerAsUser = User {}
                err = json.Unmarshal(userResponse.Payload, &buyerAsUser)
                if err != nil {
                        return shim.Error("Error creating new buyer")
                }
	}

	// check buyer balance
	if buyerAsUser.Balance < priceAsInt {
		return shim.Error("Buyer has not enough credits")
	}

	// update buyer balance
	buyerAsUser, err = t.updateBalance(stub, buyer, buyerAsUser.Balance-priceAsInt)
	if err != nil {
		return shim.Error(err.Error())
	}

	fmt.Printf("Balance of user %s (buyer) updated, is now: %n\n", buyer, buyerAsUser.Balance)

	//////////////////////////////////////////////////////////
	//                     SELLER                           //
	//////////////////////////////////////////////////////////

	// fetch seller and balance
	sellerAsUser, err := t.getUser(stub, seller)
	if err != nil {
		return shim.Error("Error fetching seller")
	}

	// update sellers balance
	sellerAsUser, err = t.updateBalance(stub, seller, sellerAsUser.Balance+priceAsInt)
	if err != nil {
		// undo successful 'buyer' transaction
		buyerAsUser, err = t.updateBalance(stub, buyer, buyerAsUser.Balance+priceAsInt)
		if err != nil {
			return shim.Error("State corrupted")
		}

		return shim.Error(err.Error())
	}

	fmt.Printf("Balance of user %s (seller) updated, is now: %n\n", seller, sellerAsUser.Balance)

	//////////////////////////////////////////////////////////
	//                       CAR                            //
	//////////////////////////////////////////////////////////

	// remove price from args
	args = args[1:]

	// transfer car
	response := t.transfer(stub, seller, args)
	car := Car{}
	err = json.Unmarshal(response.Payload, &car)
	if err != nil {
		// undo SELLER and BUYER balance updates if unsucessfull
		// is there a 'hfc transaction' for automation of this scenario?
		buyerAsUser, err = t.updateBalance(stub, buyer, buyerAsUser.Balance+priceAsInt)
		if err != nil {
			return shim.Error("State corrupted")
		}

		sellerAsUser, err = t.updateBalance(stub, seller, sellerAsUser.Balance-priceAsInt)
		if err != nil {
			return shim.Error("State corrupted")
		}

		return shim.Error("Error transferring car, transaction not successfull")
	}

	return shim.Success(response.Payload)
}

/*
 * Transfers a car to a new owner (receiver)
 *
 * Arguments required:
 * [0] VIN of the car to transfer  (string)
 * [1] Username of ther receiver   (string)
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) transfer(stub shim.ChaincodeStubInterface, username string, args []string) pb.Response {
	vin := args[0]
	newCarOwner := args[1]

	if vin == "" {
		return shim.Error("'transfer' expects a non-empty VIN to do the transfer")
	}

	if newCarOwner == "" {
		return shim.Error("'transfer' expects a non-empty car receiver username to do the transfer")
	}

	// fetch the car from the ledger
	// this already checks for ownership
	carResponse := t.readCar(stub, username, vin)
	car := Car{}
	err := json.Unmarshal(carResponse.Payload, &car)
	if err != nil {
		return shim.Error("Failed to fetch car with vin '" + vin + "' from ledger")
	}

	// check if car is not confirmed anymore
	if IsConfirmed(&car) {
		return shim.Error("The car is still confirmed. It has to be revoked first in order to do the transfer")
	}

	// transfer:
	// change of ownership in the car certificate
	car.Certificate.Username = newCarOwner

	// write car with udpated certificate back to ledger
	carAsBytes, _ := json.Marshal(car)
	err = stub.PutState(vin, carAsBytes)
	if err != nil {
		return shim.Error("Error writing car")
	}

	// get the old car owner
	owner, err := t.getUser(stub, username)
	if err != nil {
		return shim.Error("Error fetching old car owner")
	}

	// go through all his cars
	// and remove the car we just transferred
	cars := owner.Cars
	var newCarList []string
	for i, carVin := range cars {
		newCarList = append(newCarList, carVin)
		if carVin == car.Vin {
			// remove car from new list
			newCarList = newCarList[:i]
		}
	}

	// save the new car list without the transferred
	// car to the old owner
	owner.Cars = newCarList

	// write the old owner back to state
        userIndex, _ := t.getUserIndex(stub)
        userIndex[username] = owner
	userIndexAsBytes, _ := json.Marshal(userIndex)
	err = stub.PutState(userIndexStr, userIndexAsBytes)
	if err != nil {
		return shim.Error("Error writing old owner")
	}

	// get the receiver of the car
	// (new car owner)
	owner, err = t.getUser(stub, newCarOwner)
	if err != nil {
		fmt.Println("New car owner (receiver) does not exist. Creating this user.")
		owner = User{}
		owner.Name = newCarOwner
	}

	// attach the car to the receiver (new car owner)
	owner.Cars = append(owner.Cars, car.Vin)

	// write back the new owner (reveiver) to state
        userIndex,_ = t.getUserIndex(stub)
        userIndex[newCarOwner] = owner
	userIndexAsBytes, _ = json.Marshal(userIndex)
	err = stub.PutState(userIndexStr, userIndexAsBytes)
	if err != nil {
		return shim.Error("Error writing new car owner (receiver)")
	}

	// get the car index
	carIndex, err := t.getCarIndex(stub)
	if err != nil {
		return shim.Error("Error fetching car index")
	}

	// update the car index to represent
	// the new ownership rights
	carIndex[car.Vin] = newCarOwner

	// write the car index back to ledger
	indexAsBytes, _ := json.Marshal(carIndex)
	err = stub.PutState(carIndexStr, indexAsBytes)
	if err != nil {
		return shim.Error("Error writing car index")
	}

	// car transfer successfull,
	// return the car
	return shim.Success(carAsBytes)
}
