package ch.uzh.fabric.model;

public class ProposalData {
    private String car;
    private String numberOfDoors;
    private Integer numberOfCylinders;
    private Integer numberOfAxis;
    private Integer maxSpeed;

    public ProposalData(String carVin, String numberOfDoors, Integer numberOfCylinders, Integer numberOfAxis, Integer maxSpeed) {
        this.car = carVin;
        this.numberOfDoors = numberOfDoors;
        this.numberOfCylinders = numberOfCylinders;
        this.numberOfAxis = numberOfAxis;
        this.maxSpeed = maxSpeed;
    }

    public Integer getMaxSpeed() {
        return maxSpeed;
    }

    public Integer getNumberOfAxis() {
        return numberOfAxis;
    }

    public Integer getNumberOfCylinders() {
        return numberOfCylinders;
    }

    public String getCar() {
        return car;
    }

    public String getNumberOfDoors() {
        return numberOfDoors;
    }

    public void setCar(String car) {
        this.car = car;
    }

    public void setMaxSpeed(Integer maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public void setNumberOfAxis(Integer numberOfAxis) {
        this.numberOfAxis = numberOfAxis;
    }

    public void setNumberOfCylinders(Integer numberOfCylinders) {
        this.numberOfCylinders = numberOfCylinders;
    }

    public void setNumberOfDoors(String numberOfDoors) {
        this.numberOfDoors = numberOfDoors;
    }

}
