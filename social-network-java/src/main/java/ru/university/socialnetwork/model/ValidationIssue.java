package ru.university.socialnetwork.model;

import java.util.Objects;

/** Ошибка модели, не зависящая от GUI и формата файла. */
public record ValidationIssue(Kind kind, String field, String message) {
    public enum Kind { EMPTY_FIELD, INVALID_VALUE }

    public ValidationIssue {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(field);
        Objects.requireNonNull(message);
    }
}
