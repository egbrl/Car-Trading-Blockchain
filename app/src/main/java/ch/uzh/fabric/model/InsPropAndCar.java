package ch.uzh.fabric.model;

public class InsPropAndCar {
    private InsureProposal insureProposal;
    private Car car;

    public InsPropAndCar(InsureProposal insureProposal, Car car) {
        this.insureProposal = insureProposal;
        this.car = car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    public InsureProposal getInsureProposal() {
        return insureProposal;
    }

    public void setInsureProposal(InsureProposal insureProposal) {
        this.insureProposal = insureProposal;
    }
}
