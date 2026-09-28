package ru.university.socialnetwork.model;

import java.util.ArrayList;
import java.util.List;

/** Архивная запись: нет Editable, сеттеров и изменяемых полей. */
public final class DeletedProfile extends Profile {
    private final String reason;

    public DeletedProfile(int id, String name, String city, int birthYear, String reason) {
        super(id, name, city, birthYear);
        this.reason = reason == null ? "" : reason;
    }

    public String getReason() { return reason; }

    @Override
    public List<ValidationIssue> validationIssues() {
        List<ValidationIssue> errors = new ArrayList<>(super.validationIssues());
        checkText(errors, "reason", "Причина удаления", reason, false);
        return List.copyOf(errors);
    }

    @Override
    public String toString() { return super.toString() + ", удалён: " + reason; }
}
