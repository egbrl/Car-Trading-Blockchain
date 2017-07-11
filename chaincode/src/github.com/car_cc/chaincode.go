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

// uuid for test mocks
const uuid string = "1"

// indexes
const carIndexStr string = "_cars"
const userIndexStr string = "_users"
const insurerIndexStr string = "_insurers"
const registrationProposalIndexStr string = "_registrationProposals"
const revocationProposalIndexStr string = "_revocationProposals"

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
	err = clearStringIndex(carIndexStr, stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	// clear the user index
	err = clearStringIndex(userIndexStr, stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	// clear the revocation proposal index
	err = clearStringIndex(revocationProposalIndexStr, stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	// clear the insurer index
	err = clearInsurerIndex(insurerIndexStr, stub)
	if err != nil {
		return shim.Error(err.Error())
	}

	// clear the registration proposal index
	err = clearRegistrationProposalIndex(registrationProposalIndexStr, stub)
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

	switch function {

	// GENERAL FUNCTIONS
	case "read":
		if len(args) != 1 {
			return shim.Error("'read' expects a key to do the look up")
		} else if reflect.TypeOf(stub).String() != "*shim.MockStub" {
			// only allow unrestricted queries from the test files
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to do unrestricted queries on the ledger.", role))
		} else {
			return t.read(stub, args[0])
		}

	case "readCar":
		if len(args) != 1 {
			return shim.Error("'readCar' expects a car vin to do the look up")
		}
		return t.readCar(stub, username, args[0])

	// USER FUNCTIONS
	case "createUser":
		if len(args) != 0 {
			return shim.Error("'creatUser' expects a username to create a new user")
		}
		return t.createUser(stub, args[0])

	case "deleteUser":
		if len(args) != 2 {
			return shim.Error("'deleteUser' expects a username and a remainingBalanceRecipient username")
		}
		return t.deleteUser(stub, args[0], args[1])

	case "transfer":
		if len(args) != 2 {
			return shim.Error("'transfer' expects a car vin and name of the new owner to transfer a car")
		} else if role == "user" || role == "garage" {
			// only allow users and garage users to transer cars
			return t.transfer(stub, username, args)
		} else {
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to transfer cars.", role))
		}

	case "revocationProposal":
		if len(args) != 1 {
			return shim.Error("'revocationProposal' expects a car vin to revoke a car")
		} else if role != "user" {
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to create a revocation proposal.", role))
		} else {
			return t.revocationProposal(stub, username, args[0])
		}

	case "insureProposal":
		if len(args) != 2 {
			return shim.Error("'insureProposal' expects a car vin and an insurance company")
		} else if role != "user" {
			// only normal users are allowed to do insurance proposals
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to create an insurance proposal.", role))
		} else {
			return t.insureProposal(stub, username, args[0], args[1])
		}

	case "sell":
		if len(args) != 3 {
			return shim.Error("'sell' expects a price, car vin and buyer name to transfer a car")
		} else if role == "user" || role == "garage" {
			// only allow users and garage users to transer cars
			return t.sell(stub, username, args)
		} else {
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to sell cars.", role))
		}

	case "updateBalance":
		if len(args) != 1 {
			return shim.Error("'updateBalance' expects only one argument")
		} else if role != "user" {
			// only a user is allowed to update balance
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to update the balance of a user.", role))
		} else {
			/* TODO
			newBalance64, err := strconv.ParseInt(args[0], 10, 64)
			var newBalance int
			newBalance = int(newBalance64)
			if err != nil {
				return shim.Error("Error converting string to int.")
			}
			return t.updateBalance(shim, username, newBalance)
			*/
		}

	// GARAGE FUNCTIONS
	case "create":
		if role != "garage" {
			return shim.Error("'create' expects you to be a garage user")
		}
		return t.createCar(stub, username, args)

	// DOT FUNCTIONS
	case "revoke":
		if len(args) != 1 {
			return shim.Error("'revoke' expects a car vin to revoke a car")
		} else if role != "dot" {
			// only the DOT is allowed to revoke cars
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to revoke cars.", role))
		} else {
			return t.revoke(stub, username, args[0])
		}

	case "delete":
		if len(args) != 1 {
			return shim.Error("'delete' expects a car vin to delete a car")
		} else if role != "dot" {
			// only the DOT is allowed to delete cars
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to delete cars.", role))
		} else {
			return t.deleteCar(stub, args[0])
		}

	case "readRegistrationProposals":
		if role != "dot" {
			// only the DOT is allowed to read registration proposals
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to read reigistration proposals.", role))
		} else {
			return t.readRegistrationProposals(stub)
		}

	case "register":
		if len(args) != 1 {
			return shim.Error("'register' expects a car vin to register")
		} else if role != "dot" {
			// only the DOT is allowed to register new cars
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to register cars.", role))
		} else {
			return t.registerCar(stub, username, args[0])
		}

	case "confirm":
		if len(args) != 2 {
			return shim.Error(fmt.Sprintf("'confirm' expects a car vin and numberplate to confirm a car.\n You can choose your numberplate yourself."))
		} else if role != "dot" {
			// only the DOT is allowed to confirm cars
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to confirm cars.", role))
		} else {
			return t.confirmCar(stub, username, args)
		}

	case "getRevocationProposals":
		if role != "dot" {
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to query revocation proposals.", role))
		} else {
			return t.getRevocationProposals(stub)
		}

	// INSURANCE FUNCTIONS
	case "insuranceAccept":
		if len(args) != 2 {
			return shim.Error("'insuranceAccept' expects a car vin and an insurance company")
		} else if role != "insurer" {
			// only insurers are allowed to create insurance contracts
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to create an insurance proposal.", role))
		} else {
			return t.insuranceAccept(stub, username, args[0], args[1])
		}

	case "getInsurer":
		if len(args) != 1 {
			return shim.Error("'getInsurer' expects an insurance company name")
		} else if role != "insurer" {
			// only insurers are allowed to read their insurance proposals
			return shim.Error(fmt.Sprintf("Sorry, role '%s' is not allowed to create an insurance proposal.", role))
		} else {
			return t.getInsurer(stub, args[0])
		}

	default:

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
		fmt.Printf("Error starting Car chaincode: %s", err)
	}
}
