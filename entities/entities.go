package entities

type CarOwner struct {
	OwnerID   	string 	`json:"userID"`
	Name 			string 	`json:"name"`
	Address 	string 	`json:"address"`
	LicenseID string 	`json:"licenseID"`
	Telephone string 	`json:"telephone"`
	Verified  bool    `json:"telephone"`
}

type Car struct {
	CarID      	string 	`json:"carID"`
}

type TestData struct {
	CarOwners	[]CarOwner 	 `json:"carOwners"`
	Cars 		[]Car  				`json:"cars"`
}
