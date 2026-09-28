package ru.university.socialnetwork.model;

/** Обычный профиль, который разрешено создавать и редактировать в GUI. */
public class EditableProfile extends Profile implements Editable {
    public EditableProfile(int id, String name, String city, int birthYear) {
        super(id, name, city, birthYear);
    }
}
