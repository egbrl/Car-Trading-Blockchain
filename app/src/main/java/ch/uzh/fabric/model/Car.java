package ch.uzh.fabric.model;

import ch.uzh.fabric.controller.AppController;

import java.util.Date;

public class Car {
    private Certificate certificate;
    private int createdTs;
    private String vin;

    public Car() {
    }

    public Car(Certificate certificate, int createdTs, String vin) {
        this();
        this.certificate = certificate;
        this.createdTs = createdTs;
        this.vin = vin;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public int getCreatedTs() {
        return createdTs;
    }

    public String getCreatedTime() {
        Date d = new Date(createdTs*1000L);
        return AppController.timeFormat.format(d);
    }

    public String getVin() {
        return vin;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public void setCreatedTs(int createdTs) {
        this.createdTs = createdTs;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public boolean isRegistered() {
        return !this.certificate.getVin().isEmpty();
    }

    public boolean isConfirmed() {
        return !this.certificate.getNumberplate().isEmpty();
    }

    public boolean isInsured() {
        return !this.certificate.getInsurer().isEmpty();
    }

}
