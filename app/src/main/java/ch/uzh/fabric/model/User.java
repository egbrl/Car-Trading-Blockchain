package ch.uzh.fabric.model;

import java.util.ArrayList;

public class User {
    private String name;
    private Integer balance;
    private ArrayList<String> cars;
    private ArrayList<Offer> offers;

    public User(String name, ArrayList<String> cars, Integer balance, ArrayList<Offer> offers) {
        this.name = name;
        this.cars = cars;
        this.balance = balance;
        this.offers = offers;
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

    public ArrayList<Offer> getOffers() {
        return offers;
    }

    public void setOffers(ArrayList<Offer> offers) {
        this.offers = offers;
    }

}
