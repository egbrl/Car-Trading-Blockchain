package main

/*
 * Fahrzeugausweis
 */
type Certificate struct {
    User            string `json:"user"`            // the name of a user (garage or private person)
    Insurer         string `json:"insurer"`         // the name of an insurance company
    Number_Plate    string `json:"number_plate"`    // number plate like 'AG 104 739'
    Serial_Number   string `json:"serial_number"`   // serial number like 'WVW ZZZ 6RZ HY26 0780'
    Color           string `json:"color"`
    Type            string `json:"type"`            // type, like 'passenger car' or 'truck'
    Brand           string `json:"brand"`
}

/*
 * Pruefungsbericht
 * (Form. 13.20 A)
 */
type CarAudit struct {
    Car                     Car `json:"car"`
    Number_of_Doors         string `json:"number_of_doors"`      // '4+1' for a standard passenger car
    Number_of_Cylinders     int `json:"number_of_cylinders"`     // 3, 4, 6, 8 ?
    Number_of_Axis          int `json:"number_of_axis"`          // typically 2
    Max_Speed               int `json:"max_speed"`               // maximum speed as tested
}

type Car struct {
    Certificate     Certificate `json:"certificate"`
    Name      string `json:"name"`
    Color     string `json:"color"`
    Size      int    `json:"size"`
    User      string `json:"user"`
    Available bool   `json:"available"`
}

type Description struct {
    Color string `json:"color"`
    Size  int    `json:"size"`
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