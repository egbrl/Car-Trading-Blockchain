package ch.uzh.fabric.model;

import java.util.ArrayList;

public class User {
    private String name;
    private ArrayList<String> cars;
    private Integer balance;

    public User(String name, ArrayList<String> cars, Integer balance) {
        this.name = name;
        this.cars = cars;
        this.balance = balance;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getCars() {
        return cars;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCars(ArrayList<String> cars) {
        this.cars = cars;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }
}
