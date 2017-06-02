package main

import (
    "fmt"
    "encoding/json"
    "strconv"
    "strings"
    "time"
    "errors"

    "github.com/hyperledger/fabric/core/chaincode/shim"
    pb "github.com/hyperledger/fabric/protos/peer"
)

type CarChaincode struct {
}

var carIndexStr = "_cars"         //name for the key/value that will store a list of all known cars
var openTradesStr = "_opentrades" //name for the key/value that will store all open trades

func (t *CarChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
    fmt.Println("Car demo Init")
    
    var Aval int
    var err error

    _, args := stub.GetFunctionAndParameters()
    if len(args) != 1 {
        return shim.Error("Incorrect number of arguments. Expecting 1 integer to test chain.")
    }

    // Initialize the chaincode
    Aval, err = strconv.Atoi(args[0])
    if err != nil {
        return shim.Error("Expecting integer value for asset holding")
    }

    // Write the state to the ledger
    // Make a test var "abc" in order to able to query it and see if it worked
    err = stub.PutState("abc", []byte(strconv.Itoa(Aval)))
    if err != nil {
        return shim.Error(err.Error())
    }

    var empty []string
    
    // Clear the car index
    jsonAsBytes, _ := json.Marshal(empty)
    err = stub.PutState(carIndexStr, jsonAsBytes)
    if err != nil {
        return shim.Error(err.Error())
    }

    fmt.Println("Init terminated")
    return shim.Success(nil)
}

func (t *CarChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
    function, args := stub.GetFunctionAndParameters()
    fmt.Println("Invoke is running function '" + function + "' with args: " + strings.Join(args, ", "))

    // Handle different functions
    if function == "create" {
        return t.create(stub, args)
    } else if function == "delete" {
        res := t.Delete(stub, args)
        cleanTrades(stub)
        return res
    } else if function == "delete" {
        res := t.Delete(stub, args)
        cleanTrades(stub)
        return res
    } else if function == "read"{
        return t.read(stub, args)
    } else if function == "write" {
        return t.Write(stub, args)
    } else if function == "change_availability" {
        return t.change_availability(stub, args)
    } else if function == "init_car" {
        return t.init_car(stub, args)
    } else if function == "update_car" {
        return t.update_car(stub, args)
    } else if function == "set_user" {
        res := t.set_user(stub, args)
        cleanTrades(stub)
        return res
    } else if function == "open_trade" {
        return t.open_trade(stub, args)
    }

    fmt.Println("Invoke did not find function: " + function)
    return shim.Error("Received unknown function invocation")
}

func (t *CarChaincode) read(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var name, jsonResp string
    var err error

    if len(args) != 1 {
        return shim.Error("Incorrect number of arguments. Expecting name of the var to query")
    }

    name = args[0]
    valAsbytes, err := stub.GetState(name) //get the var from chaincode state
    if err != nil {
        jsonResp = "{\"Error\":\"Failed to get state for " + name + "\"}"
        return shim.Error(jsonResp)
    }

    return shim.Success(valAsbytes)
}

func (t *CarChaincode) Delete(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    if len(args) != 1 {
        return shim.Error("Incorrect number of arguments. Expecting 1")
    }

    name := args[0]
    err := stub.DelState(name) //remove the key from chaincode state
    if err != nil {
        return shim.Error("Failed to delete state")
    }

    //get the car index
    carAsBytes, err := stub.GetState(carIndexStr)
    if err != nil {
        return shim.Error("Failed to get car index")
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
    return shim.Success(nil)
}

func (t *CarChaincode) Write(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var name, value string // Entities
    var err error
    fmt.Println("running write()")

    if len(args) != 2 {
        return shim.Error("Incorrect number of arguments. Expecting 2. name of the variable and value to set")
    }

    name = args[0] //rename for funsies
    value = args[1]
    err = stub.PutState(name, []byte(value)) //write the variable into the chaincode state
    if err != nil {
        return shim.Error(err.Error())
    }
    return shim.Success(nil)
}

func (t *CarChaincode) init_car(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var err error

    //   0       1       2     3        4
    // "asdf", "blue", "35", "bob"    "true"
    // Name    Color   Size  Owner  Availability
    if len(args) != 5 {
        return shim.Error("Incorrect number of arguments. Expecting 4")
    }

    //input sanitation
    fmt.Println("- start init car")
    if len(args[0]) <= 0 {
        return shim.Error("1st argument must be a non-empty string")
    }
    if len(args[1]) <= 0 {
        return shim.Error("2nd argument must be a non-empty string")
    }
    if len(args[2]) <= 0 {
        return shim.Error("3rd argument must be a non-empty string")
    }
    if len(args[3]) <= 0 {
        return shim.Error("4th argument must be a non-empty string")
    }
    if len(args[4]) <= 0 {
        return shim.Error("5th argument must be a non-empty string")
    }
    name := args[0]
    color := strings.ToLower(args[1])
    user := strings.ToLower(args[3])
    size, err := strconv.Atoi(args[2])

    if err != nil {
        return shim.Error("3rd argument must be a numeric string")
    }

    available, err := strconv.ParseBool(args[4])

    if err != nil {
        return shim.Error("4rd argument must be a boolean string ex. 1, t, T, True, TRUE, true...")
    }

    //check if car already exists
    marbleAsBytes, err := stub.GetState(name)
    if err != nil {
        return shim.Error("Failed to get car name")
    }
    res := Car{}
    json.Unmarshal(marbleAsBytes, &res)
    if res.Name == name {
        fmt.Println("This car arleady exists: " + name)
        fmt.Println(res)
        return shim.Error("This car arleady exists") //all stop a car by this name exists
    }

    //build the car json string manually
    str := `{"name": "` + name + `", "color": "` + color + `", "size": ` + strconv.Itoa(size) + `, "user": "` + user + `, "available": "` + strconv.FormatBool(available) + `" }`
    err = stub.PutState(name, []byte(str)) //store car with id as key
    if err != nil {
        return shim.Error(err.Error())
    }

    //get the car index
    carAsBytes, err := stub.GetState(carIndexStr)
    if err != nil {
        return shim.Error("Failed to get car index")
    }
    var carIndex []string
    json.Unmarshal(carAsBytes, &carIndex) //un stringify it aka JSON.parse()

    //append
    carIndex = append(carIndex, name) //add car name to index list
    fmt.Println("! car index: ", carIndex)
    jsonAsBytes, _ := json.Marshal(carIndex)
    err = stub.PutState(carIndexStr, jsonAsBytes) //store name of car

    fmt.Println("- end init car")
    return shim.Success(nil)
}

func (t *CarChaincode) update_car(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var err error

    if len(args) != 4 {
        return shim.Error("Incorrect number of arguments. Expecting 4")
    }

    //input sanitation
    fmt.Println("- starting car update")
    if len(args[0]) <= 0 {
        return shim.Error("1st argument (car name) must be a non-empty string")
    }
    if len(args[1]) <= 0 {
        return shim.Error("2nd argument (car color) must be a non-empty string")
    }
    if len(args[2]) <= 0 {
        return shim.Error("3rd argument (car owner) must be a non-empty string")
    }
    if len(args[3]) <= 0 {
        return shim.Error("4th argument (car size) must be a non-empty string")
    }
    if len(args[4]) <= 0 {
        return shim.Error("5th argument (car availability) must be a non-empty string")
    }
    name := args[0]
    color := strings.ToLower(args[1])
    user := strings.ToLower(args[3])
    size, err := strconv.Atoi(args[2])
    if err != nil {
        return shim.Error("3rd argument (car owner) must be a numeric string")
    }

    available, err := strconv.ParseBool(args[4])

    if err != nil {
        return shim.Error("4rd argument must be a boolean string ex. 1, t, T, True, TRUE, true...")
    }

    // find an existing car
    carAsBytes, err := stub.GetState(name)
    if err != nil {
        return shim.Error("Car " + name + " does not exist yet")
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
            return shim.Error(err.Error())
        }
    }

    fmt.Println("- end update car")
    return shim.Success(nil)
}

func (t *CarChaincode) change_availability(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    if len(args) != 1 {
        return shim.Error("Incorrect number of arguments. Expecting car name")
    }

    if len(args[0]) <= 0 {
        return shim.Error("Car name must be a non-empty string")
    }

    name := args[0]

    // find an existing car
    carAsBytes, err := stub.GetState(name)
    if err != nil {
        return shim.Error("Car " + name + " does not exist yet")
    }

    // update car
    res := Car{}
    json.Unmarshal(carAsBytes, &res)
    fmt.Println("Updating car " + name)

    if (res.Available) {
        res.Available = false
    } else {
        res.Available = true
    }
    
    jsonAsBytes, _ := json.Marshal(res)
    err = stub.PutState(args[0], jsonAsBytes) //rewrite the car with id as key

    if err != nil {
        return shim.Error(err.Error())
    }
    return shim.Success(nil)
}

func (t *CarChaincode) set_user(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var err error

    //   0       1
    // "name", "bob"
    if len(args) < 2 {
        return shim.Error("Incorrect number of arguments. Expecting 2")
    }

    fmt.Println("- start set user")
    fmt.Println(args[0] + " - " + args[1])
    carAsBytes, err := stub.GetState(args[0])
    if err != nil {
        return shim.Error("Failed to get thing")
    }
    res := Car{}
    json.Unmarshal(carAsBytes, &res) //un stringify it aka JSON.parse()
    res.User = args[1]               //change the user

    jsonAsBytes, _ := json.Marshal(res)
    err = stub.PutState(args[0], jsonAsBytes) //rewrite the car with id as key
    if err != nil {
        return shim.Error(err.Error())
    }

    fmt.Println("- end set user")
    return shim.Success(nil)
}

func (t *CarChaincode) open_trade(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var err error
    var will_size int
    var trade_away Description

    //  0        1      2     3      4      5       6
    //["bob", "blue", "16", "red", "16"] *"blue", "35*
    if len(args) < 5 {
        return shim.Error("Incorrect number of arguments. Expecting like 5?")
    }
    if len(args)%2 == 0 {
        return shim.Error("Incorrect number of arguments. Expecting an odd number")
    }

    size1, err := strconv.Atoi(args[2])
    if err != nil {
        return shim.Error("3rd argument must be a numeric string")
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
            return shim.Error(msg)
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
        return shim.Error("Failed to get opentrades")
    }
    var trades AllTrades
    json.Unmarshal(tradesAsBytes, &trades) //un stringify it aka JSON.parse()

    trades.OpenTrades = append(trades.OpenTrades, open) //append to open trades
    fmt.Println("! appended open to trades")
    jsonAsBytes, _ = json.Marshal(trades)
    err = stub.PutState(openTradesStr, jsonAsBytes) //rewrite open orders
    if err != nil {
        return shim.Error(err.Error())
    }
    fmt.Println("- end open trade")
    return shim.Success(nil)
}

func (t *CarChaincode) perform_trade(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var err error

    //  0       1                   2                   3               4                   5
    //[data.id, data.closer.user, data.closer.name, data.opener.user, data.opener.color, data.opener.size]
    if len(args) < 6 {
        return shim.Error("Incorrect number of arguments. Expecting 6")
    }

    fmt.Println("- start close trade")
    timestamp, err := strconv.ParseInt(args[0], 10, 64)
    if err != nil {
        return shim.Error("1st argument must be a numeric string")
    }

    size, err := strconv.Atoi(args[5])
    if err != nil {
        return shim.Error("6th argument must be a numeric string")
    }

    //get the open trade struct
    tradesAsBytes, err := stub.GetState(openTradesStr)
    if err != nil {
        return shim.Error("Failed to get opentrades")
    }
    var trades AllTrades
    json.Unmarshal(tradesAsBytes, &trades) //un stringify it aka JSON.parse()

    for i := range trades.OpenTrades { //look for the trade
        fmt.Println("looking at " + strconv.FormatInt(trades.OpenTrades[i].Timestamp, 10) + " for " + strconv.FormatInt(timestamp, 10))
        if trades.OpenTrades[i].Timestamp == timestamp {
            fmt.Println("found the trade")

            marbleAsBytes, err := stub.GetState(args[2])
            if err != nil {
                return shim.Error("Failed to get thing")
            }
            closersCar := Car{}
            json.Unmarshal(marbleAsBytes, &closersCar) //un stringify it aka JSON.parse()

            //verify if car meets trade requirements
            if closersCar.Color != trades.OpenTrades[i].Want.Color || closersCar.Size != trades.OpenTrades[i].Want.Size {
                msg := "car in input does not meet trade requriements"
                fmt.Println(msg)
                return shim.Error(msg)
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
                    return shim.Error(err.Error())
                }
            }
        }
    }
    fmt.Println("- end close trade")
    return shim.Success(nil)
}

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

func makeTimestamp() int64 {
    return time.Now().UnixNano() / (int64(time.Millisecond) / int64(time.Nanosecond))
}

func (t *CarChaincode) remove_trade(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var err error

    //  0
    //[data.id]
    if len(args) < 1 {
        return shim.Error("Incorrect number of arguments. Expecting 1")
    }

    fmt.Println("- start remove trade")
    timestamp, err := strconv.ParseInt(args[0], 10, 64)
    if err != nil {
        return shim.Error("1st argument must be a numeric string")
    }

    //get the open trade struct
    tradesAsBytes, err := stub.GetState(openTradesStr)
    if err != nil {
        return shim.Error("Failed to get opentrades")
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
                return shim.Error(err.Error())
            }
            break
        }
    }

    fmt.Println("- end remove trade")
    return shim.Success(nil)
}

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




func main() {
    err := shim.Start(new(CarChaincode))
    if err != nil {
        fmt.Printf("Error starting Simple chaincode: %s", err)
    }
}