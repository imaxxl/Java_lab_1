package ru.university.socialnetwork.model;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/** Общая неизменяемая часть всех профилей. Редактирование GUI заменяет объект. */
public class Profile {
    public static final int MIN_BIRTH_YEAR = 1900;

    private final int id;
    private final String name;
    private final String city;
    private final int birthYear;

    public Profile(int id, String name, String city, int birthYear) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.birthYear = birthYear;
    }

    public final int getId() { return id; }
    public final String getName() { return name; }
    public final String getCity() { return city; }
    public final int getBirthYear() { return birthYear; }

    public List<ValidationIssue> validationIssues() {
        List<ValidationIssue> errors = new ArrayList<>();
        if (id < 1) {
            errors.add(new ValidationIssue(ValidationIssue.Kind.INVALID_VALUE,
                    "id", "ID должен быть положительным"));
        }
        checkText(errors, "name", "Имя", name, true);
        checkText(errors, "city", "Город", city, true);
        int currentYear = Year.now().getValue();
        if (birthYear < MIN_BIRTH_YEAR || birthYear > currentYear) {
            errors.add(new ValidationIssue(ValidationIssue.Kind.INVALID_VALUE,
                    "birthYear", "Год должен быть от " + MIN_BIRTH_YEAR + " до " + currentYear));
        }
        return List.copyOf(errors);
    }

    public List<String> validate() {
        return validationIssues().stream().map(ValidationIssue::message).toList();
    }

    /** CSV и формы этой лабораторной используют однострочные текстовые поля. */
    protected static void checkText(List<ValidationIssue> errors, String field,
                                    String label, String value, boolean required) {
        if (value != null && (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)) {
            errors.add(new ValidationIssue(ValidationIssue.Kind.INVALID_VALUE,
                    field, "Поле «" + label + "» должно находиться в одной строке"));
        }
        if (required && (value == null || value.isBlank())) {
            errors.add(new ValidationIssue(ValidationIssue.Kind.EMPTY_FIELD,
                    field, "Поле «" + label + "» не должно быть пустым"));
        }
    }

    @Override
    public String toString() {
        return id + ": " + name + ", " + city + ", " + birthYear;
    }
}
