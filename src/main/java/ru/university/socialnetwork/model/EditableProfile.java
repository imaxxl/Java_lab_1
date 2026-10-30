package ru.university.socialnetwork.model;

public class EditableProfile extends Profile implements Editable {
    public EditableProfile(int id, String name, String city, int birthYear) {
        super(id, name, city, birthYear);
    }
}
