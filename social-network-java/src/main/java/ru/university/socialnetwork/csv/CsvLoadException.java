package ru.university.socialnetwork.csv;

import java.util.Objects;

public class CsvLoadException extends Exception {
    private static final long serialVersionUID = 1L;
    private final CsvErrorCode code;
    private final int lineNumber;

    public CsvLoadException(CsvErrorCode code, int lineNumber, String message) {
        this(code, lineNumber, message, null);
    }

    public CsvLoadException(CsvErrorCode code, int lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code);
        this.lineNumber = lineNumber;
    }

    public CsvErrorCode getCode() { return code; }
    public int getLineNumber() { return lineNumber; }
}
