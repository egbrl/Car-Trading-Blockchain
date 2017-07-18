package ch.uzh.fabric.model;

public class InsureProposal {
    private String user;
    private String car;
    private boolean registered;

    public InsureProposal() {
        this.user = "";
        this.car = "";
        this.registered = false;
    }

    public InsureProposal(String user, String car) {
        this();
        this.user = user;
        this.car = car;
    }

    public InsureProposal(String user, String car, boolean registered) {
        this(user, car);
        this.registered = registered;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getCar() {
        return car;
    }

    public void setCar(String car) {
        this.car = car;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
}
