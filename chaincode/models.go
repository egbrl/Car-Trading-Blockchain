package main

type Car struct {
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