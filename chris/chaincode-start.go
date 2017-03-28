package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/hyperledger/fabric/core/chaincode/shim"
)

// SimpleChaincode example simple Chaincode implementation
type SimpleChaincode struct {
}

var carIndexStr = "_cars"         //name for the key/value that will store a list of all known cars
var openTradesStr = "_opentrades" //name for the key/value that will store all open trades
var bookingIndexStr = "_bookings" //name for the key/value taht will store a list of all bookings

type Car struct {
	Name      string `json:"name"`
	Color     string `json:"color"`
	Size      int    `json:"size"`
	User      string `json:"user"`
	Available bool   `json:"available"`
}

type customEvent struct {
	Type        string `json:"type"`
	Description string `json:"description"`
}

type Description struct {
	Color string `json:"color"`
	Size  int    `json:"size"`
}

type Booking struct {
	CarName string `json:"carName"`
	Start   int64  `json:"start"`
	End     int64  `json:"end"`
	User    string `json:"user"`
}

type AnOpenTrade struct {
	User      string        `json:"user"`      //user who created the open trade order
	Timestamp int64         `json:"timestamp"` //utc timestamp of creation
	Want      Description   `json:"want"`      //description of desired car
	Willing   []Description `json:"willing"`   //array of car willing to trade away
}

type AllTrades struct {
	OpenTrades []AnOpenTrade `json:"open_trades"`
}

// ======================================================================================================================
// Main
// ============================================================================================================================
func main() {
	err := shim.Start(new(SimpleChaincode))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}
}

// ============================================================================================================================
// Init - reset all the things
// ============================================================================================================================
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	var Aval int
	var err error

	if len(args) != 1 {
		return nil, errors.New("Incorrect number of arguments. Expecting 1")
	}

	// Initialize the chaincode
	Aval, err = strconv.Atoi(args[0])
	if err != nil {
		return nil, errors.New("Expecting integer value for asset holding")
	}

	// Write the state to the ledger
	err = stub.PutState("abc", []byte(strconv.Itoa(Aval))) //making a test var "abc" in order to able to query it and see if it worked
	if err != nil {
		return nil, err
	}

	var empty []string
	jsonAsBytes, _ := json.Marshal(empty) //marshal an emtpy array of strings to clear the index
	err = stub.PutState(carIndexStr, jsonAsBytes)
	if err != nil {
		return nil, err
	}

	var empty2 []string
	jsonAsBytes2, _ := json.Marshal(empty2) //marshal an emtpy array of strings to clear the index
	err = stub.PutState(bookingIndexStr, jsonAsBytes2)
	if err != nil {
		return nil, err
	}

	var trades AllTrades
	jsonAsBytes, _ = json.Marshal(trades) //clear the open trade struct
	err = stub.PutState(openTradesStr, jsonAsBytes)
	if err != nil {
		return nil, err
	}

	fmt.Println("Init is running: " + function)

	return nil, nil
}

// ============================================================================================================================
// Invoke - Our entry point for Invocations
// ============================================================================================================================
func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	fmt.Println("invoke is running " + function)

	// Handle different functions
	if function == "init" { //initialize the chaincode state, used as reset
		return t.Init(stub, "init", args)
	} else if function == "delete" { //deletes an entity from its state
		res, err := t.Delete(stub, args)
		cleanTrades(stub) //lets make sure all open trades are still valid
		return res, err
	} else if function == "write" { //writes a value to the chaincode state
		return t.Write(stub, args)
	} else if function == "change_availability" { //writes a value to the chaincode state
		return t.change_availability(stub, args)
	} else if function == "init_car" { //create a new car
		return t.init_car(stub, args)
	} else if function == "update_car" { //update car
		return t.update_car(stub, args)
	} else if function == "set_user" { //change owner of a car
		res, err := t.set_user(stub, args)
		cleanTrades(stub) //lets make sure all open trades are still valid
		return res, err
	} else if function == "open_trade" { //create a new trade order
		return t.open_trade(stub, args)
	} else if function == "perform_trade" { //forfill an open trade order
		res, err := t.perform_trade(stub, args)
		cleanTrades(stub) //lets clean just in case
		return res, err
	} else if function == "create_booking" {
		return t.create_booking(stub, args)
	} else if function == "remove_trade" { //cancel an open trade order
		return t.remove_trade(stub, args)
	}
	fmt.Println("invoke did not find func: " + function) //error

	return nil, errors.New("Received unknown function invocation")
}

// ============================================================================================================================
// Query - Our entry point for Queries
// ============================================================================================================================
func (t *SimpleChaincode) Query(stub shim.ChaincodeStubInterface, function string, args []string) ([]byte, error) {
	fmt.Println("query is running " + function)

	// Handle different functions
	if function == "read" { //read a variable
		return t.read(stub, args)
	}
	fmt.Println("query did not find func: " + function) //error

	return nil, errors.New("Received unknown function query")
}

// ============================================================================================================================
// Read - read a variable from chaincode state
// ============================================================================================================================
func (t *SimpleChaincode) read(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	var name, jsonResp string
	var err error

	if len(args) != 1 {
		return nil, errors.New("Incorrect number of arguments. Expecting name of the var to query")
	}

	name = args[0]
	valAsbytes, err := stub.GetState(name) //get the var from chaincode state
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + name + "\"}"
		return nil, errors.New(jsonResp)
	}

	return valAsbytes, nil //send it onward
}

// ============================================================================================================================
// Create Booking
// ============================================================================================================================
func (t *SimpleChaincode) create_booking(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	if len(args) != 4 {
		return nil, errors.New("Incorrect number of arguments")
	}

	var booking Booking

	booking.CarName = args[0]
	bookingStart, _ := strconv.Atoi(args[1])
	booking.Start = int64(bookingStart)
	bookingEnd, _ := strconv.Atoi(args[2])
	booking.End = int64(bookingEnd)
	booking.User = args[3]

	bookingAsbytes, err := stub.GetState(bookingIndexStr)
	if err != nil {
		var jsonResp string
		jsonResp = "{\"Error\":\"Failed to get booking index}"
		return nil, errors.New(jsonResp)
	}

	var bookingIndex []Booking
	json.Unmarshal(bookingAsbytes, &bookingIndex) //un stringify it aka JSON.parse()

	bookingIndex = append(bookingIndex, booking)
	jsonAsBytes, _ := json.Marshal(bookingIndex) //save new index

	stub.PutState(bookingIndexStr, jsonAsBytes)

	return jsonAsBytes, nil //send it onward
}

// ============================================================================================================================
// Delete - remove a key/value pair from state
// ============================================================================================================================
func (t *SimpleChaincode) Delete(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	if len(args) != 1 {
		return nil, errors.New("Incorrect number of arguments. Expecting 1")
	}

	name := args[0]
	err := stub.DelState(name) //remove the key from chaincode state
	if err != nil {
		return nil, errors.New("Failed to delete state")
	}

	//get the car index
	carAsBytes, err := stub.GetState(carIndexStr)
	if err != nil {
		return nil, errors.New("Failed to get car index")
	}
	var carIndex []string
	json.Unmarshal(carAsBytes, &carIndex) //un stringify it aka JSON.parse()

	//remove car from index
	for i, val := range carIndex {
		fmt.Println(strconv.Itoa(i) + " - looking at " + val + " for " + name)
		if val == name { //find the correct car
			fmt.Println("found car")
			carIndex = append(carIndex[:i], carIndex[i+1:]...) //remove it
			for x := range carIndex {                          //debug prints...
				fmt.Println(string(x) + " - " + carIndex[x])
			}
			break
		}
	}
	jsonAsBytes, _ := json.Marshal(carIndex) //save new index
	err = stub.PutState(carIndexStr, jsonAsBytes)
	return nil, nil
}

// ============================================================================================================================
// Write - write variable into chaincode state
// ============================================================================================================================
func (t *SimpleChaincode) Write(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	var name, value string // Entities
	var err error
	fmt.Println("running write()")

	if len(args) != 2 {
		return nil, errors.New("Incorrect number of arguments. Expecting 2. name of the variable and value to set")
	}

	name = args[0] //rename for funsies
	value = args[1]
	err = stub.PutState(name, []byte(value)) //write the variable into the chaincode state
	if err != nil {
		return nil, err
	}
	return nil, nil
}

// ============================================================================================================================
// Init Car - create a new car, store into chaincode state
// ============================================================================================================================
func (t *SimpleChaincode) init_car(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	var err error

	//   0       1       2     3		4
	// "asdf", "blue", "35", "bob" 	  "true"
	// Name    Color   Size  Owner  Availability
	if len(args) != 5 {
		return nil, errors.New("Incorrect number of arguments. Expecting 4")
	}

	//input sanitation
	fmt.Println("- start init car")
	if len(args[0]) <= 0 {
		return nil, errors.New("1st argument must be a non-empty string")
	}
	if len(args[1]) <= 0 {
		return nil, errors.New("2nd argument must be a non-empty string")
	}
	if len(args[2]) <= 0 {
		return nil, errors.New("3rd argument must be a non-empty string")
	}
	if len(args[3]) <= 0 {
		return nil, errors.New("4th argument must be a non-empty string")
	}
	if len(args[4]) <= 0 {
		return nil, errors.New("5th argument must be a non-empty string")
	}
	name := args[0]
	color := strings.ToLower(args[1])
	user := strings.ToLower(args[3])
	size, err := strconv.Atoi(args[2])

	if err != nil {
		return nil, errors.New("3rd argument must be a numeric string")
	}

	available, err := strconv.ParseBool(args[4])

	if err != nil {
		return nil, errors.New("4rd argument must be a boolean string ex. 1, t, T, True, TRUE, true...")
	}

	//check if car already exists
	marbleAsBytes, err := stub.GetState(name)
	if err != nil {
		return nil, errors.New("Failed to get car name")
	}
	res := Car{}
	json.Unmarshal(marbleAsBytes, &res)
	if res.Name == name {
		fmt.Println("This car arleady exists: " + name)
		fmt.Println(res)
		return nil, errors.New("This car arleady exists") //all stop a car by this name exists
	}

	//build the car json string manually
	str := `{"name": "` + name + `", "color": "` + color + `", "size": ` + strconv.Itoa(size) + `, "user": "` + user + `, "available": "` + strconv.FormatBool(available) + `" }`
	err = stub.PutState(name, []byte(str)) //store car with id as key
	if err != nil {
		return nil, err
	}

	//get the car index
	carAsBytes, err := stub.GetState(carIndexStr)
	if err != nil {
		return nil, errors.New("Failed to get car index")
	}
	var carIndex []string
	json.Unmarshal(carAsBytes, &carIndex) //un stringify it aka JSON.parse()

	//append
	carIndex = append(carIndex, name) //add car name to index list
	fmt.Println("! car index: ", carIndex)
	jsonAsBytes, _ := json.Marshal(carIndex)
	err = stub.PutState(carIndexStr, jsonAsBytes) //store name of car

	fmt.Println("- end init car")
	return nil, nil
}

// ============================================================================================================================
// Update Car - update an existing car, store into chaincode state
// ============================================================================================================================
func (t *SimpleChaincode) update_car(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	var err error

	if len(args) != 4 {
		return nil, errors.New("Incorrect number of arguments. Expecting 4")
	}

	//input sanitation
	fmt.Println("- starting car update")
	if len(args[0]) <= 0 {
		return nil, errors.New("1st argument (car name) must be a non-empty string")
	}
	if len(args[1]) <= 0 {
		return nil, errors.New("2nd argument (car color) must be a non-empty string")
	}
	if len(args[2]) <= 0 {
		return nil, errors.New("3rd argument (car owner) must be a non-empty string")
	}
	if len(args[3]) <= 0 {
		return nil, errors.New("4th argument (car size) must be a non-empty string")
	}
	if len(args[4]) <= 0 {
		return nil, errors.New("5th argument (car availability) must be a non-empty string")
	}
	name := args[0]
	color := strings.ToLower(args[1])
	user := strings.ToLower(args[3])
	size, err := strconv.Atoi(args[2])
	if err != nil {
		return nil, errors.New("3rd argument (car owner) must be a numeric string")
	}

	available, err := strconv.ParseBool(args[4])

	if err != nil {
		return nil, errors.New("4rd argument must be a boolean string ex. 1, t, T, True, TRUE, true...")
	}

	// find an existing car
	carAsBytes, err := stub.GetState(name)
	if err != nil {
		return nil, errors.New("Car " + name + " does not exist yet")
	}

	// update car
	res := Car{}
	json.Unmarshal(carAsBytes, &res)
	if res.Name == name {
		fmt.Println("Updating car " + name)
		// build the car json string
		str := `{"name": "` + name + `", "color": "` + color + `", "size": ` + strconv.Itoa(size) + `, "user": "` + user + `, "available": "` + strconv.FormatBool(available) + `"}`
		err = stub.PutState(name, []byte(str))
		if err != nil {
			return nil, err
		}
	}

	fmt.Println("- end update car")
	return nil, nil
}

// ============================================================================================================================
// Change availability of cars
// ============================================================================================================================
func (t *SimpleChaincode) change_availability(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	if len(args) != 1 {
		return nil, errors.New("Incorrect number of arguments. Expecting car name")
	}

	if len(args[0]) <= 0 {
		return nil, errors.New("Car name must be a non-empty string")
	}

	name := args[0]

	// find an existing car
	carAsBytes, err := stub.GetState(name)
	if err != nil {
		return nil, errors.New("Car " + name + " does not exist yet")
	}

	// update car
	res := Car{}
	json.Unmarshal(carAsBytes, &res)
	if res.Name == name {
		fmt.Println("Updating car " + name)
		// build the car json string
		str := `{"name": "` + name + `", "color": "` + res.Color + `", "size": ` + strconv.Itoa(res.Size) + `, "user": "` + res.User
		if (res.Available) {
			str = str + `, "available": "false"}`
		} else {
			str = str + `, "available": "true"}`
		}
		fmt.Println(str)
		err = stub.PutState(name, []byte(str))
		if err != nil {
			return nil, err
		}
	}

	return nil, nil
}


// ============================================================================================================================
// Set User Permission on car
// ============================================================================================================================
func (t *SimpleChaincode) set_user(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	var err error

	//   0       1
	// "name", "bob"
	if len(args) < 2 {
		return nil, errors.New("Incorrect number of arguments. Expecting 2")
	}

	fmt.Println("- start set user")
	fmt.Println(args[0] + " - " + args[1])
	carAsBytes, err := stub.GetState(args[0])
	if err != nil {
		return nil, errors.New("Failed to get thing")
	}
	res := Car{}
	json.Unmarshal(carAsBytes, &res) //un stringify it aka JSON.parse()
	res.User = args[1]               //change the user

	jsonAsBytes, _ := json.Marshal(res)
	err = stub.PutState(args[0], jsonAsBytes) //rewrite the car with id as key
	if err != nil {
		return nil, err
	}

	fmt.Println("- end set user")
	return nil, nil
}

// ============================================================================================================================
// Open Trade - create an open trade for a car you want with cars you have
// ============================================================================================================================
func (t *SimpleChaincode) open_trade(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	var err error
	var will_size int
	var trade_away Description

	//	0        1      2     3      4      5       6
	//["bob", "blue", "16", "red", "16"] *"blue", "35*
	if len(args) < 5 {
		return nil, errors.New("Incorrect number of arguments. Expecting like 5?")
	}
	if len(args)%2 == 0 {
		return nil, errors.New("Incorrect number of arguments. Expecting an odd number")
	}

	size1, err := strconv.Atoi(args[2])
	if err != nil {
		return nil, errors.New("3rd argument must be a numeric string")
	}

	open := AnOpenTrade{}
	open.User = args[0]
	open.Timestamp = makeTimestamp() //use timestamp as an ID
	open.Want.Color = args[1]
	open.Want.Size = size1
	fmt.Println("- start open trade")
	jsonAsBytes, _ := json.Marshal(open)
	err = stub.PutState("_debug1", jsonAsBytes)

	for i := 3; i < len(args); i++ { //create and append each willing trade
		will_size, err = strconv.Atoi(args[i+1])
		if err != nil {
			msg := "is not a numeric string " + args[i+1]
			fmt.Println(msg)
			return nil, errors.New(msg)
		}

		trade_away = Description{}
		trade_away.Color = args[i]
		trade_away.Size = will_size
		fmt.Println("! created trade_away: " + args[i])
		jsonAsBytes, _ = json.Marshal(trade_away)
		err = stub.PutState("_debug2", jsonAsBytes)

		open.Willing = append(open.Willing, trade_away)
		fmt.Println("! appended willing to open")
		i++
	}

	//get the open trade struct
	tradesAsBytes, err := stub.GetState(openTradesStr)
	if err != nil {
		return nil, errors.New("Failed to get opentrades")
	}
	var trades AllTrades
	json.Unmarshal(tradesAsBytes, &trades) //un stringify it aka JSON.parse()

	trades.OpenTrades = append(trades.OpenTrades, open) //append to open trades
	fmt.Println("! appended open to trades")
	jsonAsBytes, _ = json.Marshal(trades)
	err = stub.PutState(openTradesStr, jsonAsBytes) //rewrite open orders
	if err != nil {
		return nil, err
	}
	fmt.Println("- end open trade")
	return nil, nil
}

// ============================================================================================================================
// Perform Trade - close an open trade and move ownership
// ============================================================================================================================
func (t *SimpleChaincode) perform_trade(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	var err error

	//	0		1					2					3				4					5
	//[data.id, data.closer.user, data.closer.name, data.opener.user, data.opener.color, data.opener.size]
	if len(args) < 6 {
		return nil, errors.New("Incorrect number of arguments. Expecting 6")
	}

	fmt.Println("- start close trade")
	timestamp, err := strconv.ParseInt(args[0], 10, 64)
	if err != nil {
		return nil, errors.New("1st argument must be a numeric string")
	}

	size, err := strconv.Atoi(args[5])
	if err != nil {
		return nil, errors.New("6th argument must be a numeric string")
	}

	//get the open trade struct
	tradesAsBytes, err := stub.GetState(openTradesStr)
	if err != nil {
		return nil, errors.New("Failed to get opentrades")
	}
	var trades AllTrades
	json.Unmarshal(tradesAsBytes, &trades) //un stringify it aka JSON.parse()

	for i := range trades.OpenTrades { //look for the trade
		fmt.Println("looking at " + strconv.FormatInt(trades.OpenTrades[i].Timestamp, 10) + " for " + strconv.FormatInt(timestamp, 10))
		if trades.OpenTrades[i].Timestamp == timestamp {
			fmt.Println("found the trade")

			marbleAsBytes, err := stub.GetState(args[2])
			if err != nil {
				return nil, errors.New("Failed to get thing")
			}
			closersCar := Car{}
			json.Unmarshal(marbleAsBytes, &closersCar) //un stringify it aka JSON.parse()

			//verify if car meets trade requirements
			if closersCar.Color != trades.OpenTrades[i].Want.Color || closersCar.Size != trades.OpenTrades[i].Want.Size {
				msg := "car in input does not meet trade requriements"
				fmt.Println(msg)
				return nil, errors.New(msg)
			}

			marble, e := findMarble4Trade(stub, trades.OpenTrades[i].User, args[4], size) //find a car that is suitable from opener
			if e == nil {
				fmt.Println("! no errors, proceeding")

				t.set_user(stub, []string{args[2], trades.OpenTrades[i].User}) //change owner of selected Car, closer -> opener
				t.set_user(stub, []string{marble.Name, args[1]})               //change owner of selected Car, opener -> closer

				trades.OpenTrades = append(trades.OpenTrades[:i], trades.OpenTrades[i+1:]...) //remove trade
				jsonAsBytes, _ := json.Marshal(trades)
				err = stub.PutState(openTradesStr, jsonAsBytes) //rewrite open orders
				if err != nil {
					return nil, err
				}
			}
		}
	}
	fmt.Println("- end close trade")
	return nil, nil
}

// ============================================================================================================================
// findMarble4Trade - look for a matching car that this user owns and return it
// ============================================================================================================================
func findMarble4Trade(stub shim.ChaincodeStubInterface, user string, color string, size int) (m Car, err error) {
	var fail Car
	fmt.Println("- start find car 4 trade")
	fmt.Println("looking for " + user + ", " + color + ", " + strconv.Itoa(size))

	//get the car index
	carAsBytes, err := stub.GetState(carIndexStr)
	if err != nil {
		return fail, errors.New("Failed to get car index")
	}
	var carIndex []string
	json.Unmarshal(carAsBytes, &carIndex) //un stringify it aka JSON.parse()

	for i := range carIndex { //iter through all the car
		//fmt.Println("looking @ car name: " + carIndex[i]);

		carAsBytes, err := stub.GetState(carIndex[i]) //grab this car
		if err != nil {
			return fail, errors.New("Failed to get car")
		}
		res := Car{}
		json.Unmarshal(carAsBytes, &res) //un stringify it aka JSON.parse()
		//fmt.Println("looking @ " + res.User + ", " + res.Color + ", " + strconv.Itoa(res.Size));

		//check for user && color && size
		if strings.ToLower(res.User) == strings.ToLower(user) && strings.ToLower(res.Color) == strings.ToLower(color) && res.Size == size {
			fmt.Println("found a car: " + res.Name)
			fmt.Println("! end find car 4 trade")
			return res, nil
		}
	}

	fmt.Println("- end find car 4 trade - error")
	return fail, errors.New("Did not find car to use in this trade")
}

// ============================================================================================================================
// Make Timestamp - create a timestamp in ms
// ============================================================================================================================
func makeTimestamp() int64 {
	return time.Now().UnixNano() / (int64(time.Millisecond) / int64(time.Nanosecond))
}

// ============================================================================================================================
// Remove Open Trade - close an open trade
// ============================================================================================================================
func (t *SimpleChaincode) remove_trade(stub shim.ChaincodeStubInterface, args []string) ([]byte, error) {
	var err error

	//	0
	//[data.id]
	if len(args) < 1 {
		return nil, errors.New("Incorrect number of arguments. Expecting 1")
	}

	fmt.Println("- start remove trade")
	timestamp, err := strconv.ParseInt(args[0], 10, 64)
	if err != nil {
		return nil, errors.New("1st argument must be a numeric string")
	}

	//get the open trade struct
	tradesAsBytes, err := stub.GetState(openTradesStr)
	if err != nil {
		return nil, errors.New("Failed to get opentrades")
	}
	var trades AllTrades
	json.Unmarshal(tradesAsBytes, &trades) //un stringify it aka JSON.parse()

	for i := range trades.OpenTrades { //look for the trade
		//fmt.Println("looking at " + strconv.FormatInt(trades.OpenTrades[i].Timestamp, 10) + " for " + strconv.FormatInt(timestamp, 10))
		if trades.OpenTrades[i].Timestamp == timestamp {
			fmt.Println("found the trade")
			trades.OpenTrades = append(trades.OpenTrades[:i], trades.OpenTrades[i+1:]...) //remove this trade
			jsonAsBytes, _ := json.Marshal(trades)
			err = stub.PutState(openTradesStr, jsonAsBytes) //rewrite open orders
			if err != nil {
				return nil, err
			}
			break
		}
	}

	fmt.Println("- end remove trade")
	return nil, nil
}

// ============================================================================================================================
// Clean Up Open Trades - make sure open trades are still possible, remove choices that are no longer possible, remove trades that have no valid choices
// ============================================================================================================================
func cleanTrades(stub shim.ChaincodeStubInterface) (err error) {
	var didWork = false
	fmt.Println("- start clean trades")

	//get the open trade struct
	tradesAsBytes, err := stub.GetState(openTradesStr)
	if err != nil {
		return errors.New("Failed to get opentrades")
	}
	var trades AllTrades
	json.Unmarshal(tradesAsBytes, &trades) //un stringify it aka JSON.parse()

	fmt.Println("# trades " + strconv.Itoa(len(trades.OpenTrades)))
	for i := 0; i < len(trades.OpenTrades); { //iter over all the known open trades
		fmt.Println(strconv.Itoa(i) + ": looking at trade " + strconv.FormatInt(trades.OpenTrades[i].Timestamp, 10))

		fmt.Println("# options " + strconv.Itoa(len(trades.OpenTrades[i].Willing)))
		for x := 0; x < len(trades.OpenTrades[i].Willing); { //find a car that is suitable
			fmt.Println("! on next option " + strconv.Itoa(i) + ":" + strconv.Itoa(x))
			_, e := findMarble4Trade(stub, trades.OpenTrades[i].User, trades.OpenTrades[i].Willing[x].Color, trades.OpenTrades[i].Willing[x].Size)
			if e != nil {
				fmt.Println("! errors with this option, removing option")
				didWork = true
				trades.OpenTrades[i].Willing = append(trades.OpenTrades[i].Willing[:x], trades.OpenTrades[i].Willing[x+1:]...) //remove this option
				x--
			} else {
				fmt.Println("! this option is fine")
			}

			x++
			fmt.Println("! x:" + strconv.Itoa(x))
			if x >= len(trades.OpenTrades[i].Willing) { //things might have shifted, recalcuate
				break
			}
		}

		if len(trades.OpenTrades[i].Willing) == 0 {
			fmt.Println("! no more options for this trade, removing trade")
			didWork = true
			trades.OpenTrades = append(trades.OpenTrades[:i], trades.OpenTrades[i+1:]...) //remove this trade
			i--
		}

		i++
		fmt.Println("! i:" + strconv.Itoa(i))
		if i >= len(trades.OpenTrades) { //things might have shifted, recalcuate
			break
		}
	}

	if didWork {
		fmt.Println("! saving open trade changes")
		jsonAsBytes, _ := json.Marshal(trades)
		err = stub.PutState(openTradesStr, jsonAsBytes) //rewrite open orders
		if err != nil {
			return err
		}
	} else {
		fmt.Println("! all open trades are fine")
	}

	fmt.Println("- end clean trades")
	return nil
}
