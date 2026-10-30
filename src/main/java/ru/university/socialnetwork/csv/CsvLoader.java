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
    private static final int PROGRESS_STEP = 200;

    public static String header() { return CsvFormat.header(); }

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
        requireField(fields, CsvFormat.TYPE, lineNumber);
        ProfileType type;
        try {
            type = ProfileType.valueOf(fields.get(CsvFormat.TYPE).trim());
        } catch (IllegalArgumentException e) {
            throw new CsvLoadException(CsvErrorCode.UNKNOWN_TYPE, lineNumber,
                    "Неизвестный тип: " + fields.get(CsvFormat.TYPE), e);
        }
        for (int index : new int[]{CsvFormat.ID, CsvFormat.NAME, CsvFormat.CITY, CsvFormat.BIRTH_YEAR}) {
            requireField(fields, index, lineNumber);
        }
        if (type == ProfileType.COMMUNITY) {
            requireField(fields, CsvFormat.DESCRIPTION, lineNumber);
            requireField(fields, CsvFormat.ADMINISTRATOR_ID, lineNumber);
        }
        checkUnusedFields(fields, type, lineNumber);
        int id = number(fields, CsvFormat.ID, lineNumber);
        int birthYear = number(fields, CsvFormat.BIRTH_YEAR, lineNumber);
        String name = fields.get(CsvFormat.NAME);
        String city = fields.get(CsvFormat.CITY);
        Profile profile = switch (type) {
            case PROFILE -> new EditableProfile(id, name, city, birthYear);
            case DELETED -> new DeletedProfile(id, name, city, birthYear, fields.get(CsvFormat.REASON));
            case COMMUNITY -> new Community(id, name, city, birthYear, fields.get(CsvFormat.DESCRIPTION),
                    number(fields, CsvFormat.ADMINISTRATOR_ID, lineNumber));
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
            case PROFILE -> new int[]{CsvFormat.DESCRIPTION, CsvFormat.ADMINISTRATOR_ID, CsvFormat.REASON};
            case DELETED -> new int[]{CsvFormat.DESCRIPTION, CsvFormat.ADMINISTRATOR_ID};
            case COMMUNITY -> new int[]{CsvFormat.REASON};
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
