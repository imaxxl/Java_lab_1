package ru.university.socialnetwork.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class SafeWrite {
    @FunctionalInterface
    interface Body {
        void write(BufferedWriter writer) throws IOException;
    }

    private SafeWrite() { }

    static void replaceFile(Path file, String temporaryPrefix, Body body) throws IOException {
        checkInterrupted();
        Path target = file.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), temporaryPrefix, ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                body.write(writer);
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

    static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Сохранение отменено");
    }
}
