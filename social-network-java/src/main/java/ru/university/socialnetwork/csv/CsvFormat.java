package ru.university.socialnetwork.csv;

import java.util.ArrayList;
import java.util.List;

/** Диалект CSV: UTF-8, разделитель ';', кавычки '"', одна запись на строку. */
public final class CsvFormat {
    public static final char SEPARATOR = ';';
    public static final List<String> HEADER_FIELDS = List.of(
            "type", "id", "name", "city", "birthYear", "description", "administratorId", "reason");
    public static final int FIELD_COUNT = HEADER_FIELDS.size();

    private CsvFormat() { }

    public static String header() { return String.join(String.valueOf(SEPARATOR), HEADER_FIELDS); }

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
                .collect(java.util.stream.Collectors.joining(String.valueOf(SEPARATOR)));
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
