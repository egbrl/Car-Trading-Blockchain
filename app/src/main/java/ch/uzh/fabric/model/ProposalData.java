package ch.uzh.fabric.model;

public class ProposalData {
    private String numberOfDoors;
    private Integer numberOfCylinders;
    private Integer numberOfAxis;
    private Integer maxSpeed;

    public ProposalData(String numberOfDoors, Integer numberOfCylinders, Integer numberOfAxis, Integer maxSpeed) {
        this.numberOfDoors = numberOfDoors;
        this.numberOfCylinders = numberOfCylinders;
        this.numberOfAxis = numberOfAxis;
        this.maxSpeed = maxSpeed;
    }
}
