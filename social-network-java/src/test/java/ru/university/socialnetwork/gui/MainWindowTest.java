package ru.university.socialnetwork.gui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.university.socialnetwork.csv.CsvLoader;
import ru.university.socialnetwork.model.*;
import ru.university.socialnetwork.store.ProfileRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class MainWindowTest {
    @TempDir Path directory;
    @BeforeAll static void startFx() throws Exception { FxTestSupport.initialize(); }

    private static Object field(MainWindow app, String name) throws Exception {
        Field field = MainWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(app);
    }

    private static void refresh(MainWindow app) throws Exception {
        Method method = MainWindow.class.getDeclaredMethod("updateView");
        method.setAccessible(true);
        method.invoke(app);
    }

    @SuppressWarnings("unchecked")
    @Test
    void editButtonUsesEditableAndDeletedRemainsBlocked() throws Exception {
        FxTestSupport.run(() -> {
            MainWindow app = new MainWindow();
            Stage stage = new Stage();
            try {
                app.start(stage);
                ProfileRepository repository = (ProfileRepository) field(app, "repository");
                repository.replaceAll(List.of(new EditableProfile(1, "Анна", "Город", 2000),
                        new DeletedProfile(2, "Архив", "Город", 2000, "")));
                refresh(app);
                TableView<Profile> table = (TableView<Profile>) field(app, "table");
                Button edit = (Button) field(app, "editButton");
                table.getSelectionModel().select(repository.findById(1));
                assertFalse(edit.isDisabled());
                table.getSelectionModel().select(repository.findById(2));
                assertTrue(edit.isDisabled());
                table.getSelectionModel().clearSelection();
                assertTrue(edit.isDisabled());
            } finally { stage.close(); app.stop(); }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void idSortIsNumeric() throws Exception {
        FxTestSupport.run(() -> {
            MainWindow app = new MainWindow();
            Stage stage = new Stage();
            try {
                app.start(stage);
                ProfileRepository repository = (ProfileRepository) field(app, "repository");
                repository.replaceAll(List.of(new EditableProfile(10, "А", "Город", 2000),
                        new EditableProfile(2, "Б", "Город", 2000), new EditableProfile(1, "В", "Город", 2000)));
                refresh(app);
                TableView<Profile> table = (TableView<Profile>) field(app, "table");
                var column = table.getColumns().get(1);
                column.setSortType(TableColumn.SortType.ASCENDING);
                table.getSortOrder().add(column);
                table.sort();
                assertEquals(List.of(1, 2, 10), table.getItems().stream().map(Profile::getId).toList());
            } finally { stage.close(); app.stop(); }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void editableTypeOtherThanCommunityActuallyOpensAndSavesEditor() throws Exception {
        FxTestSupport.run(() -> {
            class OtherEditableProfile extends Profile implements Editable {
                OtherEditableProfile() { super(1, "Исходное имя", "Москва", 2000); }
            }
            MainWindow app = new MainWindow();
            Stage stage = new Stage();
            try {
                app.start(stage);
                ProfileRepository repository = (ProfileRepository) field(app, "repository");
                repository.add(new OtherEditableProfile());
                refresh(app);
                TableView<Profile> table = (TableView<Profile>) field(app, "table");
                table.getSelectionModel().select(repository.findById(1));
                Platform.runLater(() -> {
                    DialogPane pane = Window.getWindows().stream()
                            .filter(w -> w.isShowing() && w.getScene().getRoot() instanceof DialogPane)
                            .map(w -> (DialogPane) w.getScene().getRoot()).findFirst().orElseThrow();
                    ((TextField) pane.lookup("#nameField")).setText("Новое имя");
                    ProfileEditorDialogTest.save(pane).fire();
                });
                ((Button) field(app, "editButton")).fire();
                assertEquals("Новое имя", repository.findById(1).getName());
            } finally { stage.close(); app.stop(); }
            return null;
        });
    }

    @Test
    void csvLoadAndSaveRunAsBackgroundTasks() throws Exception {
        Path input = directory.resolve("input.csv");
        Path output = directory.resolve("output.csv");
        Files.writeString(input, CsvLoader.header() + "\nPROFILE;1;Анна;Москва;2000;;;\n");
        MainWindow app = FxTestSupport.run(() -> {
            MainWindow window = new MainWindow();
            window.start(new Stage());
            return window;
        });
        try {
            awaitOperation(app, () -> app.loadFile(input));
            assertEquals(1, FxTestSupport.run(() -> ((ProfileRepository) field(app, "repository")).size()));
            awaitOperation(app, () -> app.saveFile(output));
            assertEquals(1, new CsvLoader().load(output).profiles().size());
        } finally { close(app); }
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a header\n", "HEADER_ONLY"})
    void emptyOrInvalidLoadDoesNotEraseExistingRows(String content) throws Exception {
        Path file = directory.resolve("empty.csv");
        Files.writeString(file, content.equals("HEADER_ONLY") ? CsvLoader.header() + "\n" : content);
        MainWindow app = FxTestSupport.run(() -> {
            MainWindow window = new MainWindow();
            window.start(new Stage());
            ((ProfileRepository) field(window, "repository")).add(new EditableProfile(1, "Анна", "Город", 2000));
            refresh(window);
            return window;
        });
        try {
            awaitOperation(app, () -> app.loadFile(file));
            assertEquals("Анна", FxTestSupport.run(() -> ((ProfileRepository) field(app, "repository")).findById(1).getName()));
        } finally { close(app); }
    }

    private static void awaitOperation(MainWindow app, Runnable start) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        FxTestSupport.run(() -> {
            BooleanProperty busy = (BooleanProperty) field(app, "busy");
            busy.addListener((obs, wasBusy, isBusy) -> {
                if (wasBusy && !isBusy) Platform.runLater(() -> {
                    FxTestSupport.dismissDialogs(null);
                    done.complete(null);
                });
            });
            start.run();
            assertTrue(busy.get()); // Метод уже вернулся, а I/O ещё выполняется отдельно.
            return null;
        });
        done.get(15, TimeUnit.SECONDS);
    }

    private static void close(MainWindow app) throws Exception {
        FxTestSupport.run(() -> { ((Stage) field(app, "stage")).close(); app.stop(); return null; });
    }
}
