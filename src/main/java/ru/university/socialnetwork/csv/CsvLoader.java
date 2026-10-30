package ru.university.socialnetwork.csv;

import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.model.ProfileType;
import ru.university.socialnetwork.model.ValidationIssue;
import ru.university.socialnetwork.store.ProfileRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CsvLoader {
    public static String header() { return CsvFormat.header(); }

    private static final int PROGRESS_STEP = 200;

    public LoadResult load(Path file) throws IOException {
        return load(file, null);
    }

    public LoadResult load(Path file, LoadProgress progress) throws IOException {
        long total = progress == null ? 0 : LineCounter.count(file);
        List<Row> candidates = new ArrayList<>();
        List<CsvLoadException> errors = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            try {
                checkHeader(firstLine);
            } catch (CsvLoadException e) {
                return new LoadResult(List.of(), List.of(e));
            }
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException("Загрузка отменена");
                }
                if (progress != null && lineNumber % PROGRESS_STEP == 0) progress.update(lineNumber, total);
                if (line.isBlank()) continue;
                try {
                    Profile profile = parseProfile(line, lineNumber);
                    candidates.add(new Row(profile, lineNumber));
                } catch (CsvLoadException e) {
                    errors.add(e);
                }
            }
        }

        if (progress != null) progress.update(total, total);
        Set<Integer> possibleAdministrators = new HashSet<>();
        for (Row row : candidates) {
            if (ProfileRepository.canBeAdministrator(row.profile())) {
                possibleAdministrators.add(row.profile().getId());
            }
        }
        Map<Integer, Row> rows = new LinkedHashMap<>();
        for (Row row : candidates) {
            if (row.profile() instanceof Community community
                    && !possibleAdministrators.contains(community.getAdministratorId())) {
                errors.add(new CsvLoadException(CsvErrorCode.INVALID_REFERENCE, row.lineNumber(),
                        "Администратор " + community.getAdministratorId() + " не найден"));
            } else if (rows.putIfAbsent(row.profile().getId(), row) != null) {
                errors.add(new CsvLoadException(CsvErrorCode.DUPLICATE_ID, row.lineNumber(),
                        "Дублирующийся ID: " + row.profile().getId()));
            }
        }
        Set<Integer> activeProfiles = new HashSet<>();
        for (Row row : rows.values()) {
            if (ProfileRepository.canBeAdministrator(row.profile())) {
                activeProfiles.add(row.profile().getId());
            }
        }
        rows.values().removeIf(row -> {
            if (row.profile() instanceof Community community
                    && !activeProfiles.contains(community.getAdministratorId())) {
                errors.add(new CsvLoadException(CsvErrorCode.INVALID_REFERENCE, row.lineNumber(),
                        "Администратор " + community.getAdministratorId()
                                + " не является существующим обычным, не удалённым профилем"));
                return true;
            }
            return false;
        });
        errors.sort(Comparator.comparingInt(CsvLoadException::getLineNumber));
        return new LoadResult(rows.values().stream().map(Row::profile).toList(), errors);
    }

    private static void checkHeader(String line) throws CsvLoadException {
        CsvFormat.requireHeader(line, CsvFormat.HEADER_FIELDS);
    }

    private static Profile parseProfile(String line, int lineNumber) throws CsvLoadException {
        List<String> fields;
        try {
            fields = CsvFormat.parse(line);
        } catch (CsvFormat.ParseException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_CSV_FORMAT, lineNumber, e.getMessage(), e);
        }
        if (fields.size() != CsvFormat.FIELD_COUNT) {
            throw new CsvLoadException(CsvErrorCode.WRONG_FIELD_COUNT, lineNumber,
                    "Ожидается " + CsvFormat.FIELD_COUNT + " полей, получено " + fields.size());
        }
        requireField(fields, 0, lineNumber);
        ProfileType type;
        try {
            type = ProfileType.valueOf(fields.get(0).trim());
        } catch (IllegalArgumentException e) {
            throw new CsvLoadException(CsvErrorCode.UNKNOWN_TYPE, lineNumber,
                    "Неизвестный тип: " + fields.get(0), e);
        }
        for (int index : new int[]{1, 2, 3, 4}) requireField(fields, index, lineNumber);
        if (type == ProfileType.COMMUNITY) {
            requireField(fields, 5, lineNumber);
            requireField(fields, 6, lineNumber);
        }
        checkUnusedFields(fields, type, lineNumber);
        int id = number(fields, 1, lineNumber);
        int birthYear = number(fields, 4, lineNumber);
        Profile profile = switch (type) {
            case PROFILE -> new EditableProfile(id, fields.get(2), fields.get(3), birthYear);
            case DELETED -> new DeletedProfile(id, fields.get(2), fields.get(3), birthYear, fields.get(7));
            case COMMUNITY -> new Community(id, fields.get(2), fields.get(3), birthYear,
                    fields.get(5), number(fields, 6, lineNumber));
        };
        List<ValidationIssue> issues = profile.validationIssues();
        if (!issues.isEmpty()) {
            CsvErrorCode code = issues.getFirst().kind() == ValidationIssue.Kind.EMPTY_FIELD
                    ? CsvErrorCode.EMPTY_FIELD : CsvErrorCode.INVALID_VALUE;
            throw new CsvLoadException(code, lineNumber,
                    String.join("; ", issues.stream().map(ValidationIssue::message).toList()));
        }
        return profile;
    }

    private static void requireField(List<String> fields, int index, int lineNumber) throws CsvLoadException {
        if (fields.get(index).isBlank()) {
            throw new CsvLoadException(CsvErrorCode.EMPTY_FIELD, lineNumber,
                    "Не заполнено поле " + CsvFormat.HEADER_FIELDS.get(index));
        }
    }

    private static int number(List<String> fields, int index, int lineNumber) throws CsvLoadException {
        try {
            return Integer.parseInt(fields.get(index).trim());
        } catch (NumberFormatException e) {
            throw new CsvLoadException(CsvErrorCode.BAD_NUMBER, lineNumber,
                    "Поле " + CsvFormat.HEADER_FIELDS.get(index) + " должно быть целым числом", e);
        }
    }

    private static void checkUnusedFields(List<String> fields, ProfileType type,
                                          int lineNumber) throws CsvLoadException {
        int[] unused = switch (type) {
            case PROFILE -> new int[]{5, 6, 7};
            case DELETED -> new int[]{5, 6};
            case COMMUNITY -> new int[]{7};
        };
        for (int index : unused) {
            if (!fields.get(index).isBlank()) {
                throw new CsvLoadException(CsvErrorCode.INVALID_VALUE, lineNumber,
                        "Поле " + CsvFormat.HEADER_FIELDS.get(index) + " не используется типом " + type.name());
            }
        }
    }

    private record Row(Profile profile, int lineNumber) { }

    public record LoadResult(List<Profile> profiles, List<CsvLoadException> errors) {
        public LoadResult {
            profiles = List.copyOf(profiles);
            errors = List.copyOf(errors);
        }
    }
}
