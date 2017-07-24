package ch.uzh.fabric.model;

public class Certificate {
    private String username;
    private String insurer;
    private String numberplate;
    private String vin;
    private String color;
    private String type;
    private String brand;

    public Certificate() {

    }

    public Certificate(String username, String insurer, String numberplate, String vin, String color, String type, String brand) {
        this();
        this.username = username;
        this.insurer = insurer;
        this.numberplate = numberplate;
        this.vin = vin;
        this.color = color;
        this.type = type;
        this.brand = brand;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInsurer() {
        return insurer;
    }

    public void setInsurer(String insurer) {
        this.insurer = insurer;
    }

    public String getNumberplate() {
        return numberplate;
    }

    public void setNumberplate(String numberplate) {
        this.numberplate = numberplate;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
