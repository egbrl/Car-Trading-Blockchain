package ch.uzh.fabric.model;

import org.springframework.beans.factory.annotation.Autowired;

public class CarData {

    private String vin;

    public CarData(String vin) {
        this.vin = vin;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }
}
