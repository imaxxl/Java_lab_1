package ru.university.socialnetwork.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class CsvFormat {
    public static final char SEPARATOR = ';';
    public static final List<String> HEADER_FIELDS = List.of(
            "type", "id", "name", "city", "birthYear", "description", "administratorId", "reason");
    public static final int FIELD_COUNT = HEADER_FIELDS.size();
    public static final int TYPE = 0;
    public static final int ID = 1;
    public static final int NAME = 2;
    public static final int CITY = 3;
    public static final int BIRTH_YEAR = 4;
    public static final int DESCRIPTION = 5;
    public static final int ADMINISTRATOR_ID = 6;
    public static final int REASON = 7;

    private CsvFormat() { }

    public static String header() { return String.join(String.valueOf(SEPARATOR), HEADER_FIELDS); }

    public static void requireHeader(String line, List<String> expected) throws CsvLoadException {
        if (line == null) {
            throw new CsvLoadException(CsvErrorCode.BAD_HEADER, 1, "Файл пуст: отсутствует заголовок CSV");
        }
        if (line.startsWith("\uFEFF")) line = line.substring(1);
        try {
            if (!parse(line).equals(expected)) {
                throw new CsvLoadException(CsvErrorCode.BAD_HEADER, 1,
                        "Ожидается заголовок: " + String.join(String.valueOf(SEPARATOR), expected));
            }
        } catch (ParseException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_HEADER, 1, e.getMessage(), e);
        }
    }

    public static List<String> parse(String line) throws ParseException {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        boolean afterQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        value.append('"');
                        i++;
                    } else {
                        quoted = false;
                        afterQuote = true;
                    }
                } else {
                    value.append(c);
                }
            } else if (c == SEPARATOR) {
                fields.add(value.toString());
                value.setLength(0);
                afterQuote = false;
            } else if (afterQuote) {
                throw new ParseException("После закрывающей кавычки допускается только разделитель");
            } else if (c == '"') {
                if (!value.isEmpty()) {
                    throw new ParseException("Кавычка внутри неэкранированного поля");
                }
                quoted = true;
            } else {
                value.append(c);
            }
        }
        if (quoted) {
            throw new ParseException("Незакрытая кавычка; многострочные поля не поддерживаются");
        }
        fields.add(value.toString());
        return List.copyOf(fields);
    }

    public static String format(List<String> fields) {
        return fields.stream().map(CsvFormat::escape)
                .collect(Collectors.joining(String.valueOf(SEPARATOR)));
    }

    private static String escape(String value) {
        if (value == null) throw new IllegalArgumentException("CSV-поле не должно быть null");
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("CSV-поле должно находиться в одной строке");
        }
        if (value.indexOf(SEPARATOR) >= 0 || value.indexOf('"') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    public static final class ParseException extends Exception {
        private static final long serialVersionUID = 1L;
        public ParseException(String message) { super(message); }
    }
}
