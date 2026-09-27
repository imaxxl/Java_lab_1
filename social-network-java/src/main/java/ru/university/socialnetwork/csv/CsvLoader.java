package ru.university.socialnetwork.csv;

import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CsvLoader {

    public static String header() {
        return "type;id;name;city;birthYear;description;administratorId;reason";
    }

    public LoadResult load(Path file) throws IOException {
        List<Profile> profiles = new ArrayList<>();
        List<CsvLoadException> errors = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();

        List<String> lines = Files.readAllLines(file);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.isBlank()) {
                continue;
            }

            String[] fields = line.split(";", -1);
            String type = fields[0];

            int id;

            try {
                id = Integer.parseInt(fields[1]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                errors.add(new CsvLoadException(
                        CsvErrorCode.BAD_NUMBER,
                        i + 1,
                        "Некорректный ID"
                ));
                continue;
            }

            if (ids.contains(id)) {
                errors.add(new CsvLoadException(
                        CsvErrorCode.DUPLICATE_ID,
                        i + 1,
                        "Дублирующийся ID: " + id
                ));
                continue;
            }

            try {
                switch (type) {
                    case "PROFILE" -> {
                        int birthYear = Integer.parseInt(fields[4]);

                        profiles.add(new Profile(
                                id,
                                fields[2],
                                fields[3],
                                birthYear
                        ));

                        ids.add(id);
                    }

                    case "DELETED" -> {
                        int birthYear = Integer.parseInt(fields[4]);

                        profiles.add(new DeletedProfile(
                                id,
                                fields[2],
                                fields[3],
                                birthYear,
                                fields.length > 7 ? fields[7] : ""
                        ));

                        ids.add(id);
                    }

                    case "COMMUNITY" -> {
                        int birthYear = Integer.parseInt(fields[4]);
                        int administratorId = Integer.parseInt(fields[6]);

                        profiles.add(new Community(
                                id,
                                fields[2],
                                fields[3],
                                birthYear,
                                fields[5],
                                administratorId
                        ));

                        ids.add(id);
                    }

                    default -> errors.add(new CsvLoadException(
                            CsvErrorCode.UNKNOWN_TYPE,
                            i + 1,
                            "Неизвестный тип: " + type
                    ));
                }

            } catch (NumberFormatException e) {
                errors.add(new CsvLoadException(
                        CsvErrorCode.BAD_NUMBER,
                        i + 1,
                        "Некорректное число"
                ));
            } catch (ArrayIndexOutOfBoundsException e) {
                errors.add(new CsvLoadException(
                        CsvErrorCode.WRONG_FIELD_COUNT,
                        i + 1,
                        "Недостаточно полей"
                ));
            }
        } 
        
        return new LoadResult(profiles, errors);
    }

    public record LoadResult(
            List<Profile> profiles,
            List<CsvLoadException> errors
    ) {
    }
}