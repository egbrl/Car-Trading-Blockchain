package ch.uzh.fabric.model;

public class ProposalAndCar {
    private ProposalData proposalData;
    private Car car;

    public ProposalAndCar(ProposalData proposalData, Car car) {
        this.proposalData = proposalData;
        this.car = car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    public ProposalData getProposalData() {
        return proposalData;
    }

    public void setProposalData(ProposalData proposalData) {
        this.proposalData = proposalData;
    }

}
