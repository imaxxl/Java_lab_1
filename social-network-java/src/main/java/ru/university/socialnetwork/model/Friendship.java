package ru.university.socialnetwork.model;

public class Friendship {
    private final int firstId;
    private final int secondId;
    private int strength;

    public Friendship(int firstId, int secondId, int strength) {
        this.firstId = firstId;
        this.secondId = secondId;
        this.strength = strength;
    }

    public int getFirstId() {
        return firstId;
    }

    public int getSecondId() {
        return secondId;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getOtherId(int id) {
        if (id == firstId) {
            return secondId;
        }
        return firstId;
    }
}
