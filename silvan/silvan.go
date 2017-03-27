package main

import (
	"errors"
	"fmt"

	"github.com/hyperledger/fabric/core/chaincode/shim"
)

type SampleChaincode struct {
}

type CarOwner struct {
	UserID    int    `json:"userid"`
	Firstname string `json:"firstname"`
	Lastname  string `json:"lastname"`
	Address   string `json:"address"`
	LicenseID string `json:"licenceid"`
	Verified  bool   `json:"verified"`
}

type Car struct {
	CarID     int `json:"carid"`
	Location  int `json:"location"`
	CarOwnerb int `json:"owner"`
}

type CarRenter struct {
	UserID   int  `json:"userid"`
	Name     int  `json:"name"`
	Verified bool `json:"verified"`
}

type InsuranceCompany struct {
	CarID       int `json:"carid"`
	InsuranceID int `json:"insuranceid"`
}

type Transaction struct {
	TransactionID int `json:"transactionid"`
	OwnerID       int `json:"ownerid"`
	RenterID      int `json:"renterid"`
	CarID         int `json:"carid"`
	MoneyAmount   int `json:"moneyamount"`
}

func (t *SampleChaincode) Init(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	return nil, nil
}

func (t *SampleChaincode) Query(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	return nil, nil
}

func (t *SampleChaincode) Invoke(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	if len(args) == 0 {
		return nil, errors.New("Incorrect number of arguments. Expecting >0")
	}

	if function == "CreateCarOwner" { //initialize the chaincode state, used as reset
		return CreateCarOwner(stub, args)
	}
	// Create test cars

	return nil, nil
}

func main() {
	err := shim.Start(new(SampleChaincode))
	if err != nil {
		fmt.Println("Could not start SampleChaincode")
	} else {
		fmt.Println("SampleChaincode successfully started")
	}

}

func CreateCarOwner(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	fmt.Println("Entering CreateOwnerApplication")

	if len(args) < 2 {
		fmt.Println("Invalid number of args")
		return nil, errors.New("Expected at least two arguments for Car Owner Creation")
	}

	var OwnerID = args[0]
	var OwnerInput = args[1]

	err := stub.PutState(OwnerID, []byte(OwnerInput))
	if err != nil {
		fmt.Println("Could not save car owner to ledger", err)
		return nil, err
	}

	fmt.Println("Successfully saved loan application")
	return nil, nil
}

func GetCarOwner(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	fmt.Println("Entering GetCarOwnerApplication")

	if len(args) < 1 {
		fmt.Println("Invalid number of arguments")
		return nil, errors.New("Missing car owner ID")
	}
	var carOwnerID = args[0]
	bytes, err := stub.GetState(carOwnerID)
	if err != nil {
		fmt.Println("Could not fetch car owner with id "+carOwnerID+" from ledger", err)
		return nil, err
	}
	return bytes, nil
}
