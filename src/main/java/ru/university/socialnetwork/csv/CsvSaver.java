package ru.university.socialnetwork.csv;

import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.model.ProfileType;
import ru.university.socialnetwork.store.ProfileRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class CsvSaver {
    public void save(Path file, List<Profile> profiles) throws IOException {
        List<Profile> snapshot;
        try {
            snapshot = List.copyOf(profiles);
            new ProfileRepository().replaceAll(snapshot);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IOException("Нельзя сохранить некорректные данные: " + e.getMessage(), e);
        }

        checkInterrupted();
        Path target = file.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".profiles-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write(CsvLoader.header());
                writer.newLine();
                for (Profile profile : snapshot) {
                    checkInterrupted();
                    writer.write(toCsv(profile));
                    writer.newLine();
                }
            }
            checkInterrupted();
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Сохранение отменено");
    }

    private static String toCsv(Profile profile) {
        String description = "";
        String administratorId = "";
        String reason = "";
        if (profile instanceof Community community) {
            description = community.getDescription();
            administratorId = String.valueOf(community.getAdministratorId());
        } else if (profile instanceof DeletedProfile deleted) {
            reason = deleted.getReason();
        }
        return CsvFormat.format(List.of(
                ProfileType.of(profile).name(), String.valueOf(profile.getId()),
                profile.getName(), profile.getCity(), String.valueOf(profile.getBirthYear()),
                description, administratorId, reason));
    }
}
