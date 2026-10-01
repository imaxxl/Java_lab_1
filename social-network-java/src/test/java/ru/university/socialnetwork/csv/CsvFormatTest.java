package ru.university.socialnetwork.csv;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CsvFormatTest {
    @ParameterizedTest
    @ValueSource(strings = {"", "Анна", "Java; Kotlin", "Он сказал \"привет\"", "\"", "\";\"", "  имя  "})
    void escapedFieldRoundTrips(String value) throws Exception {
        List<String> fields = List.of(value, "", "7", "");
        assertEquals(fields, CsvFormat.parse(CsvFormat.format(fields)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"не закрыто", "a\"b", "\"abc\"x", "\"a\" \"b\""})
    void rejectsMalformedQuotes(String line) {
        assertThrows(CsvFormat.ParseException.class, () -> CsvFormat.parse(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r", "x\nx", "x\r\nx"})
    void refusesMultilineValuesInsteadOfCorruptingFile(String value) {
        assertThrows(IllegalArgumentException.class, () -> CsvFormat.format(List.of(value)));
    }
}
