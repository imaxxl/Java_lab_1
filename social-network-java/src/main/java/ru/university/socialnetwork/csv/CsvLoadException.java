package ru.university.socialnetwork.csv;

public class CsvLoadException extends Exception {
    private final CsvErrorCode code;
    private final int lineNumber;

    public CsvLoadException(CsvErrorCode code, int lineNumber, String message) {
        super(message);
        this.code = code;
        this.lineNumber = lineNumber;
    }

    public CsvErrorCode getCode() {
        return code;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
