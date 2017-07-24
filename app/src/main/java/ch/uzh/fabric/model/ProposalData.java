package ch.uzh.fabric.model;

public class ProposalData {
    private String car;
    private String username;
    private String numberOfDoors;
    private Integer numberOfCylinders;
    private Integer numberOfAxis;
    private Integer maxSpeed;

    public ProposalData() {
    }

    public ProposalData(String car, String numberOfDoors, Integer numberOfCylinders, Integer numberOfAxis, Integer maxSpeed) {
        this(numberOfDoors, numberOfCylinders, numberOfAxis, maxSpeed);
        this.car = car;
    }

    public ProposalData(String car, String username, String numberOfDoors, Integer numberOfCylinders, Integer numberOfAxis, Integer maxSpeed) {
        this(car, numberOfDoors, numberOfCylinders, numberOfAxis, maxSpeed);
        this.username = username;
    }

    public ProposalData(String numberOfDoors, Integer numberOfCylinders, Integer numberOfAxis, Integer maxSpeed) {
        this();
        this.numberOfDoors = numberOfDoors;
        this.numberOfCylinders = numberOfCylinders;
        this.numberOfAxis = numberOfAxis;
        this.maxSpeed = maxSpeed;
    }

    public String getCar() {
        return car;
    }

    public void setCar(String car) {
        this.car = car;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNumberOfDoors() {
        return numberOfDoors;
    }

    public void setNumberOfDoors(String numberOfDoors) {
        this.numberOfDoors = numberOfDoors;
    }

    public Integer getNumberOfCylinders() {
        return numberOfCylinders;
    }

    public void setNumberOfCylinders(Integer numberOfCylinders) {
        this.numberOfCylinders = numberOfCylinders;
    }

    public Integer getNumberOfAxis() {
        return numberOfAxis;
    }

    public void setNumberOfAxis(Integer numberOfAxis) {
        this.numberOfAxis = numberOfAxis;
    }

    public Integer getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(Integer maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
}
