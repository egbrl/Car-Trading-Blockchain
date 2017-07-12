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
 * 'createCar' to createCar a tailored registration proposal.
 *
 * Expects 'args':
 *  Car with VIN                             json
 *  (optional) RegistrationProposal          json
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) createCar(stub shim.ChaincodeStubInterface, username string, args []string) pb.Response {
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
	car.CreatedTs = strconv.FormatInt(time.Now().Unix(), 10)

	// check for existing garage user with that name
	user, err := t.getUser(stub, username)
	if err != nil {
		user = User{Name: username, Cars: []string{}, Balance: 100}
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
		return shim.Error("Error writing car to ledger")
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
	err = t.saveUser(stub, user)
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
 * Reads a car and checks for ownership
 *
 * Only the car owner can read the car.
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) getCar(stub shim.ChaincodeStubInterface, username string, vin string) (Car, error) {
	if vin == "" {
		return Car{}, errors.New("'readCar' expects a non-empty VIN to do the look up")
	}

	// fetch the car from the ledger
	carResponse := t.read(stub, vin)
	car := Car{}
	err := json.Unmarshal(carResponse.Payload, &car)
	if err != nil {
		return Car{}, errors.New("Failed to fetch car with vin '" + vin + "' from ledger")
	}

	// fetch the car index to check if the user owns the car
	owner, err := t.getOwner(stub, vin)
	if err != nil {
		return Car{}, errors.New(err.Error())
	} else if owner != username {
		return Car{}, errors.New("Forbidden: this is not your car")
	}

	return car, nil
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
	vin := args[1]
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
		buyerAsUser = User{}
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
	buyerAsUser, err = t.setBalance(stub, buyer, buyerAsUser.Balance-priceAsInt)
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
		// Temporary fix for tests (ToDo: Fix User creation in tests)
		fmt.Printf("Error fetching old car owner. Creating new one.")
		userAsBytes := t.createUser(stub, seller)
		err := json.Unmarshal(userAsBytes.Payload, &sellerAsUser)
		if err != nil {
			return shim.Error("Error unmarshaling user payload.")
		}
		// return shim.Error("Error fetching seller")
	}

	// update sellers balance
	sellerAsUser, err = t.setBalance(stub, seller, sellerAsUser.Balance+priceAsInt)
	if err != nil {
		// undo successful 'buyer' transaction
		buyerAsUser, err = t.setBalance(stub, buyer, buyerAsUser.Balance+priceAsInt)
		if err != nil {
			return shim.Error("State corrupted")
		}

		return shim.Error(err.Error())
	}

	fmt.Printf("Balance of user %s (seller) updated, is now: %n\n", seller, sellerAsUser.Balance)

	//////////////////////////////////////////////////////////
	//           WRITING USER CHANGES TO LEDGER             //
	//////////////////////////////////////////////////////////

	// Writing updated buyer back to ledger
	err = t.saveUser(stub, buyerAsUser)
	if err != nil {
		return shim.Error("Error saving updated buyer!")
	}

	// writing updated seller back to ledger
	err = t.saveUser(stub, sellerAsUser)
	if err != nil {
		return shim.Error("Error saving updated seller!")
	}

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
		buyerAsUser, err = t.setBalance(stub, buyer, buyerAsUser.Balance+priceAsInt)
		if err != nil {
			return shim.Error("State corrupted")
		}

		sellerAsUser, err = t.setBalance(stub, seller, sellerAsUser.Balance-priceAsInt)
		if err != nil {
			return shim.Error("State corrupted")
		}

		return shim.Error("Error transferring car, transaction not successfull")
	}

	//////////////////////////////////////////////////////////
	//           WRITING CAR CHANGES TO LEDGER              //
	//////////////////////////////////////////////////////////

	// write updated car back to ledger
	carAsBytes, _ := json.Marshal(car)
	err = stub.PutState(vin, carAsBytes)
	if err != nil {
		return shim.Error("Error writing updated car to ledger")
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
	newCarOwnerUsername := args[1]

	if vin == "" {
		return shim.Error("'transfer' expects a non-empty VIN to do the transfer")
	}

	if newCarOwnerUsername == "" {
		return shim.Error("'transfer' expects a non-empty car receiver username to do the transfer")
	}

	// fetch the car from the ledger
	// this already checks for ownership
	car, err := t.getCar(stub, username, vin)
	if err != nil {
		return shim.Error("Failed to fetch car with vin '" + vin + "' from ledger")
	}

	// check if car is not confirmed anymore
	if IsConfirmed(&car) {
		return shim.Error("The car is still confirmed. It has to be revoked first in order to do the transfer")
	}

	// transfer:
	// change of ownership in the car certificate
	car.Certificate.Username = newCarOwnerUsername

	// write car with udpated certificate back to ledger
	carAsBytes, _ := json.Marshal(car)
	err = stub.PutState(vin, carAsBytes)
	if err != nil {
		return shim.Error("Error writing car")
	}

	// get the old car owner
	oldOwner, err := t.getUser(stub, username)

	// go through all his cars
	// and remove the car we just transferred
	cars := oldOwner.Cars
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
	oldOwner.Cars = newCarList

	// write the old owner back to state
	err = t.saveUser(stub, oldOwner)
	if err != nil {
		return shim.Error("Error writing old owner")
	}

	// get the receiver of the car
	// (new car owner)
	newOwner, err := t.getUser(stub, newCarOwnerUsername)

	if err != nil {
		fmt.Println("New car owner (receiver) does not exist. Creating this user.")
		userResponse := t.createUser(stub, newCarOwnerUsername)
		newOwner = User{}
		err = json.Unmarshal(userResponse.Payload, &newOwner)
		if err != nil {
			return shim.Error("Error creating new car owner")
		}
	}

	// attach the car to the receiver (new car owner)
	newOwner.Cars = append(newOwner.Cars, car.Vin)


	// write back the new owner (reveiver) to state
	err = t.saveUser(stub, newOwner)
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
	carIndex[car.Vin] = newOwner.Name

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
