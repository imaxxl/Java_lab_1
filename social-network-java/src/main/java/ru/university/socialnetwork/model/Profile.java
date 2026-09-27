package ru.university.socialnetwork.model;

public class Profile {
    private final int id;
    private String name;
    private String city;
    private int birthYear;

    public Profile(int id, String name, String city, int birthYear) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.birthYear = birthYear;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    @Override
    public String toString() {
        return id + ": " + name + ", " + city + ", " + birthYear;
    }
}
