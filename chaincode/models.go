package main

type Car struct {
    Certificate    Certificate           `json:"certificate"`    // vehicle certificate issued by the DOT
    CreatedTs      int64                 `json:"created_ts"`     // birth date
    Vin            string                `json:"vin"`            // vehicle identification number
    UsageData      UsageData             `json:"usage_data"`     // car usage profile, interesting for car rentals
}

type UsageData struct {
    MileAge        int                   `json:"mile_age"`       // car mile age
    Repairs        string                `json:"repairs"`        //
                                                                 // tbd: what data does Mobility really collect?

    Contributions  []DataContribution    `json:"contributions"`  // who provided the data?
}

type DataContribution struct {
    User           string                `json:"user"`
    Metric         string                `json:"metric"`
}

type User struct {
    Name           string                `json:"name"`
    Cars           []string              `json:"cars"`
    Balance        int                   `json:"balance"`
}

type Insurer struct {
    Name           string                `json:"name"`
    Proposals      []InsureProposal      `json:"proposals"`
}

type InsureProposal struct {
    User           string                `json:"user"`
    Car            string                `json:"car"`
}

/*
 * Fahrzeugausweis
 *
 * The car certificate information is attested by the DOT
 */
type Certificate struct {
    Username        string `json:"username"`     // car owners name
    Insurer         string `json:"insurer"`      // the name of an insurance company
    Numberplate     string `json:"numberplate"`  // number plate ('AG 104 739')
    Vin             string `json:"vin"`          // vehicle identification number ('WVW ZZZ 6RZ HY26 0780')
    Color           string `json:"color"`
    Type            string `json:"type"`         // type: 'passenger car', 'truck', ...
    Brand           string `json:"brand"`
}

/*
 * Pruefungsbericht
 * (Form. 13.20 A)
 */
type RegistrationProposal struct {
    Car                   string     `json:"car"`
    NumberOfDoors         string     `json:"number_of_doors"`     // '4+1' for a passenger car
    NumberOfCylinders     int        `json:"number_of_cylinders"` // 3, 4, 6, 8 ?
    NumberOfAxis          int        `json:"number_of_axis"`      // typically 2
    MaxSpeed              int        `json:"max_speed"`           // maximum speed as tested
}