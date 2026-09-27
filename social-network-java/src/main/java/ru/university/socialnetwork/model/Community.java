package ru.university.socialnetwork.model;

import java.util.ArrayList;
import java.util.List;

public class Community extends Profile implements Editable {
    private String description;
    private int administratorId;

    public Community(int id, String name, String city, int birthYear, String description, int administratorId) {
        super(id, name, city, birthYear);
        this.description = description;
        this.administratorId = administratorId;
    }

    public String getDescription() {
        return description;
    }

    public int getAdministratorId() {
        return administratorId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAdministratorId(int administratorId) {
        this.administratorId = administratorId;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (getName() == null || getName().isBlank()) {
            errors.add("Имя не должно быть пустым");
        }

        if (getCity() == null || getCity().isBlank()) {
            errors.add("Город не должен быть пустым");
        }

        if (getBirthYear() < 1900 || getBirthYear() > 2026) {
            errors.add("Год должен быть от 1900 до 2026");
        }

        if (description == null || description.isBlank()) {
            errors.add("Описание не должно быть пустым");
        }

        if (administratorId < 1) {
            errors.add("ID администратора должен быть положительным");
        }

        return errors;
    }

    @Override
    public String toString() {
        return super.toString() + ", сообщество: " + description + ", администратор: " + administratorId;
    }
}
