package ru.university.socialnetwork.model;

public final class Friendship {
    private final int firstId;
    private final int secondId;
    private int strength;

    public Friendship(int firstId, int secondId, int strength) {
        if (firstId < 1 || secondId < 1) {
            throw new IllegalArgumentException("ID участников дружбы должны быть положительными");
        }
        if (firstId == secondId) {
            throw new IllegalArgumentException("Нельзя добавить дружбу с самим собой");
        }
        this.firstId = firstId;
        this.secondId = secondId;
        setStrength(strength);
    }

    public int getFirstId() { return firstId; }
    public int getSecondId() { return secondId; }
    public int getStrength() { return strength; }

    public void setStrength(int strength) {
        if (strength < 1) {
            throw new IllegalArgumentException("Сила связи должна быть положительной");
        }
        this.strength = strength;
    }

    public int getOtherId(int id) {
        if (id == firstId) return secondId;
        if (id == secondId) return firstId;
        throw new IllegalArgumentException("ID " + id + " не входит в эту связь");
    }
}
