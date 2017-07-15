package ch.uzh.fabric.model;

import java.sql.Timestamp;
import java.util.Date;

public class Car {
    private Certificate certificate;
    private Date createdTs;
    private String vin;

    public Car() {

    }

    public Car(Certificate certificate, Date createdTs, String vin) {
        this.certificate = certificate;
        this.createdTs = createdTs;
        this.vin = vin;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public Date getCreated_ts() {
        return createdTs;
    }

    public String getVin() {
        return vin;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public void setCreated_ts(Date created_ts) {
        this.createdTs = created_ts;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public boolean isConfirmed() {
        return !this.certificate.getVin().isEmpty();
    }

    public boolean isRegistered() {
        return !this.certificate.getNumberplate().isEmpty();
    }

    public boolean isInsured() {
        return !this.certificate.getInsurer().isEmpty();
    }

}
