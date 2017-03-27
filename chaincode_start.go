package main

import (
	"encoding/json"
	"errors"
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
)

// DATA MODEL

type CarOwner struct {
	OwnerID string `json:"userID"`
	Name    string `json:"name"`
}

type Car struct {
	CarID string `json:"carID"`
}

type TestData struct {
	CarOwners []CarOwner `json:"carOwners"`
	Cars      []Car      `json:"cars"`
}

// SimpleChaincode example simple Chaincode implementation
type SimpleChaincode struct {
}

// ============================================================================================================================
// Main
// ============================================================================================================================
func main() {
	err := shim.Start(new(SimpleChaincode))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}
}

// Init resets all the things
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	if len(args) != 1 {
		return nil, errors.New("Incorrect number of arguments. Expecting 1")
	}

	errors.New(args[0])

	// Create test cars
	t.addTestdata(stub, args[0])

	return nil, nil
}

func (t *SimpleChaincode) addTestdata(stub shim.ChaincodeStubInterface, testDataAsJson string) error {
	var testData TestData
	err := json.Unmarshal([]byte(testDataAsJson), &testData)
	if err != nil {
		return errors.New("Error while unmarshalling testdata")
	}

	for _, carOwner := range testData.CarOwners {
		carOwnerAsBytes, err := json.Marshal(carOwner)
		if err != nil {
			return errors.New("Error marshalling testCarOwner, reason: " + err.Error())
		}

		err = StoreObjectInChain(stub, carOwner.OwnerID, "_owners", carOwnerAsBytes)
		if err != nil {
			return errors.New("error in storing object, reason: " + err.Error())
		}
	}

	for _, car := range testData.Cars {
		carAsBytes, err := json.Marshal(car)
		if err != nil {
			return errors.New("Error marshalling testCar, reason: " + err.Error())
		}

		err = StoreObjectInChain(stub, car.CarID, "_cars", carAsBytes)
		if err != nil {
			return errors.New("error in storing object, reason: " + err.Error())
		}
	}

	return nil
}

func getTestData(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	fmt.Println("Retrieving Owner Name")

	if len(args) < 1 {
		fmt.Println("Invalid number of arguments")
		return nil, errors.New("Missing owner ID")
	}

	var ownerID = args[0]
	bytes, err := stub.GetState(ownerID)
	if err != nil {
		fmt.Println("Could not fetch owner id "+ownerID+" from ledger", err)
		return nil, err
	}
	return bytes, nil
}

func StoreObjectInChain(stub shim.ChaincodeStubInterface, objectID string, indexName string, object []byte) error {
	ID, err := WriteIDToBlockchainIndex(stub, indexName, objectID)
	if err != nil {
		return errors.New("Writing ID to index: " + indexName + "Reason: " + err.Error())
	}

	fmt.Println("adding: ", string(object))

	err = stub.PutState(string(ID), object)
	if err != nil {
		return errors.New("Putstate error: " + err.Error())
	}

	return nil
}

func WriteIDToBlockchainIndex(stub shim.ChaincodeStubInterface, indexName string, id string) ([]byte, error) {
	index, err := GetIndex(stub, indexName)
	if err != nil {
		return nil, err
	}

	index = append(index, id)

	jsonAsBytes, err := json.Marshal(index)
	if err != nil {
		return nil, errors.New("Error marshalling index '" + indexName + "': " + err.Error())
	}

	err = stub.PutState(indexName, jsonAsBytes)
	if err != nil {
		return nil, errors.New("Error storing new " + indexName + " into ledger")
	}

	return []byte(id), nil
}

func GetIndex(stub shim.ChaincodeStubInterface, indexName string) ([]string, error) {
	indexAsBytes, err := stub.GetState(indexName)
	if err != nil {
		return nil, errors.New("Failed to get " + indexName)
	}

	var index []string
	err = json.Unmarshal(indexAsBytes, &index)
	if err != nil {
		return nil, errors.New("Error unmarshalling index '" + indexName + "': " + err.Error())
	}

	return index, nil
}

// Invoke is our entry point to invoke a chaincode function
func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	fmt.Println("invoke is running " + function)

	// Handle different functions
	if function == "init" { //initialize the chaincode state, used as reset
		return t.Init(stub, "init", args)
	}
	fmt.Println("invoke did not find func: " + function) //error

	return nil, errors.New("Received unknown function invocation: " + function)
}

// Query is our entry point for queries
func (t *SimpleChaincode) Query(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	fmt.Println("query is running " + function)
	// Handle different functions
	if function == "dummy_query" { //read a variable
		fmt.Println("hi there " + function) //error
		return nil, nil
	} else if function == "getTestData" {
		fmt.Println("Starting the function " + function)
		return getTestData(stub, args)
	}
	fmt.Println("query did not find func: " + function) //error

	return nil, errors.New("Received unknown function query: " + function)
}
