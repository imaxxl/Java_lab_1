package ru.university.socialnetwork.csv;

import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.store.ProfileRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FriendshipLoader {
    public static final List<String> HEADER_FIELDS = List.of("firstId", "secondId", "strength");
    private static final int PROGRESS_STEP = 200;
    private static final int ID_SHIFT = 32;

    public static String header() { return String.join(String.valueOf(CsvFormat.SEPARATOR), HEADER_FIELDS); }

    public LoadResult load(Path file, ProfileRepository repository, LoadProgress progress) throws IOException {
        long total = progress == null ? 0 : LineCounter.count(file);
        List<Friendship> friendships = new ArrayList<>();
        List<CsvLoadException> errors = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            try {
                CsvFormat.requireHeader(reader.readLine(), HEADER_FIELDS);
            } catch (CsvLoadException e) {
                return new LoadResult(List.of(), List.of(e));
            }
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Загрузка отменена");
                if (progress != null && lineNumber % PROGRESS_STEP == 0) progress.update(lineNumber, total);
                if (line.isBlank()) continue;
                try {
                    friendships.add(parse(line, lineNumber, repository, seen));
                } catch (CsvLoadException e) {
                    errors.add(e);
                }
            }
        }
        if (progress != null) progress.update(total, total);
        return new LoadResult(friendships, errors);
    }

    private static Friendship parse(String line, int lineNumber, ProfileRepository repository,
                                    Set<Long> seen) throws CsvLoadException {
        List<String> fields;
        try {
            fields = CsvFormat.parse(line);
        } catch (CsvFormat.ParseException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_CSV_FORMAT, lineNumber, e.getMessage(), e);
        }
        if (fields.size() != HEADER_FIELDS.size()) {
            throw new CsvLoadException(CsvErrorCode.WRONG_FIELD_COUNT, lineNumber,
                    "Ожидается " + HEADER_FIELDS.size() + " поля, получено " + fields.size());
        }
        for (int index = 0; index < fields.size(); index++) {
            if (fields.get(index).isBlank()) {
                throw new CsvLoadException(CsvErrorCode.EMPTY_FIELD, lineNumber,
                        "Не заполнено поле " + HEADER_FIELDS.get(index));
            }
        }
        int first = number(fields, 0, lineNumber);
        int second = number(fields, 1, lineNumber);
        int strength = number(fields, 2, lineNumber);
        Friendship friendship;
        try {
            friendship = new Friendship(first, second, strength);
        } catch (IllegalArgumentException e) {
            throw new CsvLoadException(CsvErrorCode.INVALID_VALUE, lineNumber, e.getMessage(), e);
        }
        for (int id : new int[]{first, second}) {
            if (repository.findById(id) == null) {
                throw new CsvLoadException(CsvErrorCode.INVALID_REFERENCE, lineNumber, "Профиль " + id + " не найден");
            }
        }
        long pair = ((long) Math.min(first, second) << ID_SHIFT) | Math.max(first, second);
        if (repository.friendshipStrength(first, second) > 0 || !seen.add(pair)) {
            throw new CsvLoadException(CsvErrorCode.DUPLICATE_FRIENDSHIP, lineNumber,
                    "Дружба " + first + " — " + second + " уже существует");
        }
        return friendship;
    }

    private static int number(List<String> fields, int index, int lineNumber) throws CsvLoadException {
        try {
            return Integer.parseInt(fields.get(index).trim());
        } catch (NumberFormatException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_NUMBER, lineNumber,
                    "Поле " + HEADER_FIELDS.get(index) + " должно быть целым числом", e);
        }
    }

    public record LoadResult(List<Friendship> friendships, List<CsvLoadException> errors) {
        public LoadResult {
            friendships = List.copyOf(friendships);
            errors = List.copyOf(errors);
        }
    }
}
