package ch.uzh.fabric.model;

public class ProposalData {
    private String numberOfDoors;
    private Integer numberOfCylinders;
    private Integer numberOfAxis;
    private Integer maxSpeed;

    public ProposalData() {
        
    }

    public ProposalData(String numberOfDoors, Integer numberOfCylinders, Integer numberOfAxis, Integer maxSpeed) {
        this.numberOfDoors = numberOfDoors;
        this.numberOfCylinders = numberOfCylinders;
        this.numberOfAxis = numberOfAxis;
        this.maxSpeed = maxSpeed;
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
