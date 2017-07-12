package ch.uzh.fabric.model;

import java.sql.Timestamp;
import java.util.Date;

public class Car {
    private Certificate certificate;
    private Date created_ts;
    private String vin;

    public Car(Certificate certificate, Date created_ts, String vin) {
        this.certificate = certificate;
        this.created_ts = created_ts;
        this.vin = vin;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public Date getCreated_ts() {
        return created_ts;
    }

    public String getVin() {
        return vin;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public void setCreated_ts(Date created_ts) {
        this.created_ts = created_ts;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }
}
