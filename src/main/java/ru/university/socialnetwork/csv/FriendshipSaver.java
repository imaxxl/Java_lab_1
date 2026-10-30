package ru.university.socialnetwork.csv;

import ru.university.socialnetwork.model.Friendship;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class FriendshipSaver {
    public void save(Path file, List<Friendship> friendships) throws IOException {
        List<Friendship> snapshot = List.copyOf(friendships);
        SafeWrite.replaceFile(file, ".friendships-", writer -> {
            writer.write(FriendshipLoader.header());
            writer.newLine();
            for (Friendship friendship : snapshot) {
                SafeWrite.checkInterrupted();
                writer.write(CsvFormat.format(List.of(String.valueOf(friendship.getFirstId()),
                        String.valueOf(friendship.getSecondId()), String.valueOf(friendship.getStrength()))));
                writer.newLine();
            }
        });
    }
}
