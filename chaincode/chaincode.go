package main

import (
	"fmt"
	"reflect"
	"strconv"
	"strings"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

type CarChaincode struct {
}

const carIndexStr string = "_cars"
const insurerIndexStr string = "_insurers"

func (t *CarChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	fmt.Println("Car demo Init")

	var aval int
	var err error

	_, args := stub.GetFunctionAndParameters()
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1 integer to test chain.")
	}

	// initialize the chaincode
	aval, err = strconv.Atoi(args[0])
	if err != nil {
		return shim.Error("Expecting integer value for asset holding")
	}

	// write the state to the ledger
	// make a test var "abc" in order to able to query it and see if it worked
	err = stub.PutState("abc", []byte(strconv.Itoa(aval)))
	if err != nil {
		return shim.Error(err.Error())
	}

	// clear the car index
	err = clearCarIndex(carIndexStr, stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	// clear the insurer index
	err = clearInsurerIndex(insurerIndexStr, stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	fmt.Println("Init terminated")
	return shim.Success(nil)
}

/*
 * Invokes an action on the ledger.
 *
 * Expects 'username' and 'role' as first two parameters.
 * Unrestricted queries can only be done from test files.
 */
func (t *CarChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	function, args := stub.GetFunctionAndParameters()

	if len(args) < 2 {
		return shim.Error("Invoke expects 'username' and 'role' as first two args.")
	}

	username := args[0]
	role := args[1]
	args = args[2:]

	fmt.Printf("Invoke is running as user '%s' with role '%s'\n", username, role)
	fmt.Printf("Invoke is running function '%s' with args: %s\n", function, strings.Join(args, ", "))

	if function == "create" {
		if role != "garage" {
			return shim.Error("'create' expects you to be a garage user")
		}
		return t.create(stub, username, args)
	} else if function == "read" {
		if len(args) != 1 {
			return shim.Error("'read' expects a key to do the look up")
		} else if reflect.TypeOf(stub).String() != "*shim.MockStub" {
			// only allow unrestricted queries from the test files
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to do unrestricted queries on the ledger.", role))
		} else {
			return t.read(stub, args[0])
		}
	} else if function == "readCar" {
		if len(args) != 1 {
			return shim.Error("'readCar' expects a car vin to do the look up")
		} else {
			return t.readCar(stub, username, args[0])
		}
	} else if function == "register" {
		if len(args) != 1 {
			return shim.Error("'register' expects a car vin to register")
		} else if role != "dot" {
			// only the DOT is allowed to register new cars
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to register cars.", role))
		} else {
			return t.register(stub, username, args[0])
		}
	} else if function == "confirm" {
		if len(args) != 2 {
			return shim.Error("'confirm' expects a car vin to confirm a car")
		} else if role != "dot" {
			// only the DOT is allowed to confirm cars
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to confirm cars.", role))
		} else {
			return t.confirm(stub, username, args)
		}

	} else if function == "insureProposal" {
		if len(args) != 2 {
			return shim.Error("'insureProposal' expects a car vin and an insurance company")
		} else if role != "user" {
			// only normal users are allowed to do insurance proposals
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to create an insurance proposal.", role))
		} else {
			return t.insureProposal(stub, username, args[0], args[1])
		}
	} else if function == "insuranceAccept" {
		if len(args) != 2 {
			return shim.Error("'insuranceAccept' expects a car vin and an insurance company")
		} else if role != "insurer" {
			// only normal users are allowed to do insurance proposals
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to create an insurance proposal.", role))
		} else {
			return t.insuranceAccept(stub, username, args[0], args[1])
		}
	} else if function == "getInsurer" {
		if len(args) != 1 {
			return shim.Error("'getInsurer' expects an insurance company name")
		} else if role != "insurer" {
			// only normal users are allowed to do insurance proposals
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to create an insurance proposal.", role))
		} else {
			return t.getInsurer(stub, args[0])
		}
	}

	return shim.Error("Invoke did not find function: " + function)
}

/*
 * Reads ledger state from position 'key'.
 *
 * Can be any of:
 *  - Car   (expects car timestamp as key)
 *  - User  (expects user name as key)
 *  - or an index like '_cars'
 *
 * On success,
 * returns ledger state in bytes at position 'key'.
 */
func (t *CarChaincode) read(stub shim.ChaincodeStubInterface, key string) pb.Response {
	if key == "" {
		return shim.Error("'read' expects a non-empty key to do the look up")
	}

	valAsBytes, err := stub.GetState(key)
	if err != nil {
		return shim.Error("Failed to fetch value at key '" + key + "' from ledger")
	}

	return shim.Success(valAsBytes)
}

func main() {
	err := shim.Start(new(CarChaincode))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}
}
