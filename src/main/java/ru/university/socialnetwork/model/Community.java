package ru.university.socialnetwork.model;

import java.util.ArrayList;
import java.util.List;

public class Community extends Profile implements Editable {
    private final String description;
    private final int administratorId;

    public Community(int id, String name, String city, int birthYear,
                     String description, int administratorId) {
        super(id, name, city, birthYear);
        this.description = description;
        this.administratorId = administratorId;
    }

    public String getDescription() { return description; }
    public int getAdministratorId() { return administratorId; }

    @Override
    public List<ValidationIssue> validationIssues() {
        List<ValidationIssue> errors = new ArrayList<>(super.validationIssues());
        checkText(errors, "description", "Описание", description, true);
        if (administratorId < 1) {
            errors.add(new ValidationIssue(ValidationIssue.Kind.INVALID_VALUE,
                    "administratorId", "ID администратора должен быть положительным"));
        } else if (administratorId == getId()) {
            errors.add(new ValidationIssue(ValidationIssue.Kind.INVALID_VALUE,
                    "administratorId", "Сообщество не может быть собственным администратором"));
        }
        return List.copyOf(errors);
    }

    @Override
    public String toString() {
        return super.toString() + ", сообщество: " + description
                + ", администратор: " + administratorId;
    }
}
