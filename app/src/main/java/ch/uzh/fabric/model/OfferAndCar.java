package ch.uzh.fabric.model;

public class OfferAndCar {
    private Offer offer;
    private Car car;

    public OfferAndCar(Offer offer, Car car) {
        this.offer = offer;
        this.car = car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    public Offer getOffer() {
        return offer;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }
}
