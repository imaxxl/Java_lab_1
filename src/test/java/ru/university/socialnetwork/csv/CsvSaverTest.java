package ru.university.socialnetwork.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.university.socialnetwork.model.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CsvSaverTest {
    @TempDir Path directory;

    @Test
    void roundTripsAllTypesWithSemicolonsAndQuotes() throws Exception {
        List<Profile> original = List.of(
                new EditableProfile(1, "Анна; Мария", "Город \"N\"", 2000),
                new DeletedProfile(2, "Пётр", "Омск", 1999, "Архив; навсегда \"ok\""),
                new Community(3, "Java Club", "Москва", 2020, "Java; Kotlin \"JVM\"", 1));
        Path file = directory.resolve("saved.csv");
        new CsvSaver().save(file, original);
        var loaded = new CsvLoader().load(file);
        assertTrue(loaded.errors().isEmpty());
        assertEquals(original.size(), loaded.profiles().size());
        for (int i = 0; i < original.size(); i++) {
            Profile a = original.get(i), b = loaded.profiles().get(i);
            assertEquals(ProfileType.of(a), ProfileType.of(b));
            assertEquals(a.getId(), b.getId());
            assertEquals(a.getName(), b.getName());
            assertEquals(a.getCity(), b.getCity());
            assertEquals(a.getBirthYear(), b.getBirthYear());
        }
        assertEquals(((DeletedProfile) original.get(1)).getReason(),
                ((DeletedProfile) loaded.profiles().get(1)).getReason());
        assertEquals(((Community) original.get(2)).getDescription(),
                ((Community) loaded.profiles().get(2)).getDescription());
        assertEquals(1, ((Community) loaded.profiles().get(2)).getAdministratorId());
    }

    static Stream<Arguments> invalidData() {
        return Stream.of(
                Arguments.of(List.of(new EditableProfile(0, "Анна", "Город", 2000))),
                Arguments.of(List.of(new EditableProfile(1, "Анна\nМария", "Город", 2000))),
                Arguments.of(List.of(new DeletedProfile(1, "Анна", "Город", 2000, "\n"))),
                Arguments.of(List.of(new EditableProfile(1, "Анна", "Город", 2000),
                        new EditableProfile(1, "Дубль", "Город", 2000))),
                Arguments.of(List.of(new Community(2, "Клуб", "Город", 2020, "Java", 99))));
    }

    @ParameterizedTest
    @MethodSource("invalidData")
    void invalidDataDoesNotChangeExistingFile(List<Profile> profiles) throws Exception {
        Path file = directory.resolve("existing.csv");
        Files.writeString(file, "original content");
        assertThrows(IOException.class, () -> new CsvSaver().save(file, profiles));
        assertEquals("original content", Files.readString(file));
    }

    @Test
    void failedMoveKeepsTargetAndCleansTemporaryFile() throws Exception {
        Path target = directory.resolve("existing-directory");
        Files.createDirectory(target);
        Files.writeString(target.resolve("original.txt"), "keep");
        assertThrows(IOException.class, () -> new CsvSaver().save(target,
                List.of(new EditableProfile(1, "Анна", "Город", 2000))));
        assertEquals("keep", Files.readString(target.resolve("original.txt")));
        try (var paths = Files.list(directory)) {
            assertFalse(paths.anyMatch(p -> p.getFileName().toString().startsWith(".profiles-")));
        }
    }

    @Test
    void interruptedSaveKeepsOriginalFile() throws Exception {
        Path file = directory.resolve("interrupted.csv");
        Files.writeString(file, "keep");
        Thread.currentThread().interrupt();
        try {
            assertThrows(IOException.class, () -> new CsvSaver().save(file,
                    List.of(new EditableProfile(1, "Анна", "Город", 2000))));
        } finally {
            Thread.interrupted();
        }
        assertEquals("keep", Files.readString(file));
    }

    @Test
    void emptyDatasetWritesValidHeader() throws Exception {
        Path file = directory.resolve("empty.csv");
        new CsvSaver().save(file, List.of());
        var result = new CsvLoader().load(file);
        assertTrue(result.profiles().isEmpty());
        assertTrue(result.errors().isEmpty());
    }
}
