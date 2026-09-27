package ru.university.socialnetwork.model;

public class DeletedProfile extends Profile {
    private final String reason;

    public DeletedProfile(int id, String name, String city, int birthYear, String reason) {
        super(id, name, city, birthYear);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return super.toString() + ", удален: " + reason;
    }
}
