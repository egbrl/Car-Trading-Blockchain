package ch.uzh.fabric.model;

public class InsureProposal {
    private String user;
    private String car;

    public InsureProposal(String user, String car) {
        this.user = user;
        this.car = car;
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
}
