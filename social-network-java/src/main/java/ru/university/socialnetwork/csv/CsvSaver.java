package ru.university.socialnetwork.csv;

import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.Profile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvSaver {
    public void save(Path file, List<Profile> profiles) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(CsvLoader.header());
            writer.newLine();

            for (Profile profile : profiles) {
                writer.write(toCsv(profile));
                writer.newLine();
            }
        }
    }

    private String toCsv(Profile profile) {
        if (profile instanceof Community community) {
            return String.join(
                    ";",
                    "COMMUNITY",
                    String.valueOf(community.getId()),
                    community.getName(),
                    community.getCity(),
                    String.valueOf(community.getBirthYear()),
                    community.getDescription(),
                    String.valueOf(community.getAdministratorId()),
                    ""
            );
        }

        if (profile instanceof DeletedProfile deletedProfile) {
            return String.join(
                    ";",
                    "DELETED",
                    String.valueOf(deletedProfile.getId()),
                    deletedProfile.getName(),
                    deletedProfile.getCity(),
                    String.valueOf(deletedProfile.getBirthYear()),
                    "",
                    "",
                    deletedProfile.getReason()
            );
        }

        return String.join(
                ";",
                "PROFILE",
                String.valueOf(profile.getId()),
                profile.getName(),
                profile.getCity(),
                String.valueOf(profile.getBirthYear()),
                "",
                "",
                ""
        );
    }
}
