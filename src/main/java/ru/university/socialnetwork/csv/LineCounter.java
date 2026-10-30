package ru.university.socialnetwork.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Предварительный проход по файлу: число строк нужно, чтобы показать прогресс загрузки в процентах. */
final class LineCounter {
    private LineCounter() { }

    static long count(Path file) throws IOException {
        long lines = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            while (reader.readLine() != null) lines++;
        }
        return lines;
    }
}
