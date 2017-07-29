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
* Queries the car history.
*
* On sucess,
* Returns a map[in64]Car with modification timestamp as key
* and Car object (from that point in time) as value
 */
func (t *CarChaincode) getHistory(stub shim.ChaincodeStubInterface, vin string) pb.Response {
	hist, err := stub.GetHistoryForKey(vin)
	if err != nil {
		return shim.Error(err.Error())
	}

	carHistory := make(map[int64]Car)
	for hist.HasNext() {
		mod, _ := hist.Next()
		var car Car
		err := json.Unmarshal(mod.GetValue(), &car)
		if err != nil {
			return shim.Error(err.Error())
		}

		carHistory[mod.GetTimestamp().GetSeconds()] = car
	}

	carHistoryAsBytes, _ := json.Marshal(carHistory)
	return shim.Success(carHistoryAsBytes)
}

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
	car.CreatedTs = time.Now().Unix()

	// check for existing garage user with that name
	user, err := t.getUser(stub, username)
	if err != nil {
		userResponse := t.createUser(stub, username)
		user = User{}
		err := json.Unmarshal(userResponse.Payload, &user)
		if err != nil {
			return shim.Error(err.Error())
		}
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

	// update the car vin and the username
	// in the registration proposal
	// and save the proposal for the DOT
	regProposal.Car = car.Vin
	regProposal.Username = username
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
 * Reads a car as DOT
 *
 * The DOT is allowed to read all cars
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) getCarAsDot(stub shim.ChaincodeStubInterface, vin string) (Car, error) {
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
 * Reads a car as DOT
 *
 * Extra function for DOT to read cars
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) readCarAsDot(stub shim.ChaincodeStubInterface, username string, vin string) pb.Response {
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

	return shim.Success(carResponse.Payload)
}

/*
 * Creates selling offer.
 *
 * Arguments required:
 * [0] Price                       (int)
 * [1] VIN of the car to transfer  (string)
 * [2] Buyer username              (string)
 *
 * On success,
 * returns the offer.
 */
func (t *CarChaincode) createSellingOffer(stub shim.ChaincodeStubInterface, seller string, args []string) pb.Response {
	price, _ := strconv.Atoi(args[0])
	vin := args[1]
	buyer := args[2]

	// price input sanitation
	if args[0] == "" || price < 0 {
		return shim.Error("'sell' expects a non-empty, positive price")
	}

	// create new selling offer
	offer := Offer{
		Seller: seller,
		Buyer:  buyer,
		Vin:    vin,
		Price:  price}

	// updating buyer object
	buyerAsObject, err := t.getUser(stub, buyer)
	if err != nil {
		return shim.Error("Error: Could not find buyer in database.")
	}
	// allow only one selling offer per car toa  user
	for _, offer := range buyerAsObject.Offers {
		if offer.Vin == vin {
			return shim.Error("Error: It exists already an offer for car '" + vin + "and prospective buyer '" + buyer + "'.")
		}
	}
	buyerAsObject.Offers = append(buyerAsObject.Offers, offer)
	err = t.saveUser(stub, buyerAsObject)
	if err != nil {
		return shim.Error(err.Error())
	}

	offerAsBytes, _ := json.Marshal(offer)

	return shim.Success(offerAsBytes)
}

/*
 * Sell a car to a new owner (receiver).
 *
 * No balance checks are performed before selling a car,
 * cars are bought on credit.
 *
 * Arguments required:
 * [0] VIN of the car to transfer  (string)
 * [1] Buyer username              (string)
 *
 * On success,
 * returns the car.
 */
func (t *CarChaincode) sell(stub shim.ChaincodeStubInterface, seller string, args []string) pb.Response {
	vin := args[0]
	buyer := args[1]

	// input sanitation
	if vin == "" {
		return shim.Error("'sell' expects a non-empty VIN to do the transfer")
	}

	// check for sales offer,
	// you cannot buy a car without the sellers agreement
	buyerAsUser, err := t.getUser(stub, buyer)
	if err != nil {
		return shim.Error(err.Error())
	}

	var salesOffer Offer
	for _, o := range buyerAsUser.Offers {
		if o.Vin == vin {
			salesOffer = o
		}
	}

	if salesOffer == (Offer{}) {
		return shim.Error("No sale without sales offer. Request an offer from the seller first.")
	}

	// fetch the car from the ledger
	// this already checks for ownership
	car, err := t.getCar(stub, seller, vin)
	if err != nil {
		return shim.Error("Failed to fetch car with vin '" + vin + "' from ledger")
	}

	// check if car is not confirmed anymore
	if IsConfirmed(&car) {
		return shim.Error("The car is still confirmed. It has to be revoked first in order to do the transfer.")
	}

	// change of ownership in the car certificate
	car.Certificate.Username = buyer

	// write car with udpated certificate back to ledger
	carAsBytes, _ := json.Marshal(car)
	err = stub.PutState(car.Vin, carAsBytes)
	if err != nil {
		return shim.Error("Error writing car")
	}

	// remove all selling offers for this car from all users
	// this has to happen in the same transaction as 'sell'
	// to ensure no orphans are left in the system
	// and that no car is bought/sold twice
	userIndex, err := t.getUserIndex(stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	for username, _ := range userIndex {
		user, err := t.getUser(stub, username)
		if err != nil {
			return shim.Error(err.Error())
		}

		var newOffers []Offer
		for _, offer := range user.Offers {
			// check that user is allowed to remove his selling offers
			// only the seller should be allowed to remove an offer
			// he created
			if offer.Vin != vin || offer.Seller != seller {
				newOffers = append(newOffers, offer)
			}
		}

		user.Offers = newOffers

		// if buyer/seller update balances
		// also update car lists and the car index
		if user.Name == buyer {
			user.Balance -= salesOffer.Price

			// attach the car to the buyer
			user.Cars = append(user.Cars, car.Vin)

			// get the car index
			carIndex, err := t.getCarIndex(stub)
			if err != nil {
				return shim.Error("Error fetching car index")
			}

			// update the car index to represent
			// the new ownership rights
			carIndex[car.Vin] = user.Name

			// write the car index back to ledger
			indexAsBytes, _ := json.Marshal(carIndex)
			err = stub.PutState(carIndexStr, indexAsBytes)
			if err != nil {
				return shim.Error("Error writing car index")
			}
		} else if user.Name == seller {
			user.Balance += salesOffer.Price

			// go through all his cars
			// and remove the car we just transferred
			var newCarList []string
			for _, carVin := range user.Cars {
				if carVin != car.Vin {
					newCarList = append(newCarList, carVin)
				}
			}

			user.Cars = newCarList
		}

		// write the user back to ledger
		err = t.saveUser(stub, user)
		if err != nil {
			return shim.Error(err.Error())
		}
	}

	return shim.Success(carAsBytes)
}