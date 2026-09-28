package ru.university.socialnetwork.csv;

public enum CsvErrorCode {
    BAD_HEADER,
    BAD_CSV_FORMAT,
    BAD_NUMBER,
    WRONG_FIELD_COUNT,
    EMPTY_FIELD,
    UNKNOWN_TYPE,
    DUPLICATE_ID,
    INVALID_VALUE,
    INVALID_REFERENCE
}
