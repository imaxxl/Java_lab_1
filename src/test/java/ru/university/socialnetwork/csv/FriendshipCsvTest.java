package ru.university.socialnetwork.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.store.ProfileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FriendshipCsvTest {
    @TempDir Path directory;

    private static ProfileRepository repository() {
        ProfileRepository repository = new ProfileRepository();
        for (int id = 1; id <= 5; id++) repository.add(new EditableProfile(id, "Имя" + id, "Город", 2000));
        return repository;
    }

    private Path write(String... lines) throws IOException {
        Path file = directory.resolve("friendships.csv");
        Files.writeString(file, String.join("\n", lines) + "\n");
        return file;
    }

    private static List<CsvErrorCode> codes(FriendshipLoader.LoadResult result) {
        return result.errors().stream().map(CsvLoadException::getCode).toList();
    }

    @Test
    void loadsCorrectRows() throws Exception {
        FriendshipLoader.LoadResult result = new FriendshipLoader().load(
                write(FriendshipLoader.header(), "1;2;5", "2;3;7"), repository(), null);
        assertTrue(result.errors().isEmpty());
        assertEquals(2, result.friendships().size());
        assertEquals(7, result.friendships().get(1).getStrength());
    }

    @Test
    void skipsBrokenRowsAndReportsLineNumbersAndCodes() throws Exception {
        FriendshipLoader.LoadResult result = new FriendshipLoader().load(write(FriendshipLoader.header(),
                "1;2;5",
                "1;x;5",
                "1;3",
                "1;;5",
                "2;2;5",
                "1;3;0",
                "1;99;5",
                "2;1;3",
                "3;4;9"), repository(), null);
        assertEquals(2, result.friendships().size());
        assertEquals(List.of(CsvErrorCode.BAD_NUMBER, CsvErrorCode.WRONG_FIELD_COUNT, CsvErrorCode.EMPTY_FIELD,
                CsvErrorCode.INVALID_VALUE, CsvErrorCode.INVALID_VALUE, CsvErrorCode.INVALID_REFERENCE,
                CsvErrorCode.DUPLICATE_FRIENDSHIP), codes(result));
        assertEquals(3, result.errors().get(0).getLineNumber());
        assertEquals(9, result.errors().get(6).getLineNumber());
    }

    @Test
    void friendshipAlreadyInRepositoryIsDuplicate() throws Exception {
        ProfileRepository repository = repository();
        repository.addFriendship(new Friendship(1, 2, 4));
        FriendshipLoader.LoadResult result = new FriendshipLoader().load(
                write(FriendshipLoader.header(), "2;1;9"), repository, null);
        assertEquals(List.of(CsvErrorCode.DUPLICATE_FRIENDSHIP), codes(result));
    }

    @Test
    void wrongHeaderStopsLoading() throws Exception {
        FriendshipLoader.LoadResult result = new FriendshipLoader().load(write("a;b;c", "1;2;3"), repository(), null);
        assertTrue(result.friendships().isEmpty());
        assertEquals(List.of(CsvErrorCode.BAD_HEADER), codes(result));
    }

    @Test
    void emptyFileReportsMissingHeader() throws Exception {
        Path file = directory.resolve("empty.csv");
        Files.writeString(file, "");
        assertEquals(List.of(CsvErrorCode.BAD_HEADER), codes(new FriendshipLoader().load(file, repository(), null)));
    }

    @Test
    void byteOrderMarkBeforeHeaderIsAllowed() throws Exception {
        Path file = directory.resolve("bom.csv");
        Files.writeString(file, "\uFEFF" + FriendshipLoader.header() + "\n1;2;3\n");
        assertTrue(new FriendshipLoader().load(file, repository(), null).errors().isEmpty());
    }

    @Test
    void reportsProgressUpToTheEnd() throws Exception {
        ProfileRepository repository = new ProfileRepository();
        List<String> lines = new ArrayList<>();
        lines.add(FriendshipLoader.header());
        for (int id = 1; id <= 1001; id++) repository.add(new EditableProfile(id, "Имя", "Город", 2000));
        for (int id = 1; id <= 1000; id++) lines.add(id + ";" + (id + 1) + ";1");
        List<Long> done = new ArrayList<>();
        long[] total = {0};
        new FriendshipLoader().load(write(lines.toArray(new String[0])), repository, (current, all) -> {
            done.add(current);
            total[0] = all;
        });
        assertEquals(1001, total[0]);
        assertEquals(1001L, done.get(done.size() - 1));
        assertTrue(done.size() >= 5);
        for (int i = 1; i < done.size(); i++) assertTrue(done.get(i) >= done.get(i - 1));
    }

    @Test
    void roundTripKeepsFriendships() throws Exception {
        List<Friendship> original = List.of(new Friendship(1, 2, 5), new Friendship(3, 1, 8));
        Path file = directory.resolve("saved.csv");
        new FriendshipSaver().save(file, original);
        FriendshipLoader.LoadResult loaded = new FriendshipLoader().load(file, repository(), null);
        assertTrue(loaded.errors().isEmpty());
        assertEquals(2, loaded.friendships().size());
        assertEquals(8, loaded.friendships().get(1).getStrength());
        try (var paths = Files.list(directory)) {
            assertFalse(paths.anyMatch(p -> p.getFileName().toString().startsWith(".friendships-")));
        }
    }

    @Test
    void interruptedSaveKeepsOriginalFile() throws Exception {
        Path file = directory.resolve("kept.csv");
        Files.writeString(file, "старое содержимое");
        Thread.currentThread().interrupt();
        try {
            assertThrows(IOException.class, () -> new FriendshipSaver().save(file, List.of(new Friendship(1, 2, 3))));
        } finally {
            Thread.interrupted();
        }
        assertEquals("старое содержимое", Files.readString(file));
    }

    @Test
    void profileLoaderReportsProgressToo() throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add(CsvLoader.header());
        for (int id = 1; id <= 800; id++) lines.add("PROFILE;" + id + ";Имя;Город;2000;;;");
        Path file = directory.resolve("profiles.csv");
        Files.write(file, lines);
        List<Long> done = new ArrayList<>();
        CsvLoader.LoadResult result = new CsvLoader().load(file, (current, total) -> done.add(current * 1000 / total));
        assertEquals(800, result.profiles().size());
        assertEquals(1000L, done.get(done.size() - 1));
        assertTrue(done.size() >= 4);
    }
}
