package entities

type CarOwner struct {
	UserID   	string 	`json:"userID"`
	Name 			string 	`json:"name"`
	Address 	string 	`json:"address"`
	LicenseID string 	`json:"licenseID"`
	Telephone string 	`json:"telephone"`
	Verified  bool    `json:"telephone"`
}

type Car struct {
	CarID      	string 	`json:"carID"`
}
