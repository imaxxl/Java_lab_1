package ru.university.socialnetwork.gui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.university.socialnetwork.csv.CsvLoader;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.store.ProfileRepository;

import java.io.BufferedWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MainWindowProgressTest {
    private static final int ROWS = 300_000;

    @TempDir Path directory;
    @BeforeAll static void startFx() throws Exception { FxTestSupport.initialize(); }

    private static Object field(MainWindow app, String name) throws Exception {
        Field field = MainWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(app);
    }

    private Path bigFile() throws Exception {
        Path file = directory.resolve("big.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(CsvLoader.header());
            writer.newLine();
            for (int id = 1; id <= ROWS; id++) {
                writer.write("PROFILE;" + id + ";Имя;Город;2000;;;");
                writer.newLine();
            }
        }
        return file;
    }

    private static MainWindow open() throws Exception {
        return FxTestSupport.run(() -> {
            MainWindow window = new MainWindow();
            window.start(new Stage());
            return window;
        });
    }

    private static CompletableFuture<Void> whenIdle(MainWindow app) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        FxTestSupport.run(() -> {
            BooleanProperty busy = (BooleanProperty) field(app, "busy");
            busy.addListener((observable, wasBusy, isBusy) -> {
                if (wasBusy && !isBusy) Platform.runLater(() -> done.complete(null));
            });
            return null;
        });
        return done;
    }

    @Test
    void largeFileLoadReportsGrowingProgress() throws Exception {
        Path file = bigFile();
        MainWindow app = open();
        try {
            List<Double> values = new ArrayList<>();
            CompletableFuture<Void> idle = whenIdle(app);
            FxTestSupport.run(() -> {
                TaskRunner runner = (TaskRunner) field(app, "runner");
                runner.progressProperty().addListener((observable, before, after) -> values.add(after.doubleValue()));
                app.loadFile(file);
                return null;
            });
            idle.get(60, TimeUnit.SECONDS);
            assertEquals(ROWS, FxTestSupport.run(() -> ((ProfileRepository) field(app, "repository")).size()));
            assertTrue(values.stream().anyMatch(value -> value > 0 && value < 1), "промежуточный прогресс не виден: " + values);
        } finally {
            FxTestSupport.run(() -> { ((Stage) field(app, "stage")).close(); app.stop(); return null; });
        }
    }

    @Test
    void cancelledLoadLeavesExistingDataUntouched() throws Exception {
        Path file = bigFile();
        MainWindow app = FxTestSupport.run(() -> {
            MainWindow window = new MainWindow();
            window.start(new Stage());
            ((ProfileRepository) field(window, "repository")).add(new EditableProfile(1, "Анна", "Город", 2000));
            return window;
        });
        try {
            CompletableFuture<Void> idle = whenIdle(app);
            FxTestSupport.run(() -> {
                app.loadFile(file);
                ((TaskRunner) field(app, "runner")).cancel();
                return null;
            });
            idle.get(60, TimeUnit.SECONDS);
            assertEquals(1, FxTestSupport.run(() -> ((ProfileRepository) field(app, "repository")).size()));
            String text = FxTestSupport.run(() -> ((Label) field(app, "status")).getText());
            assertTrue(text.contains("отменена"), text);
        } finally {
            FxTestSupport.run(() -> { ((Stage) field(app, "stage")).close(); app.stop(); return null; });
        }
    }
}
