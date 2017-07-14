package ch.uzh.fabric.model;

import org.springframework.beans.factory.annotation.Autowired;

public class CarData {

    private String vin;
    private String createdTs;

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
