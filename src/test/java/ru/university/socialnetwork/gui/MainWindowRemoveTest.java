package ru.university.socialnetwork.gui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.*;
import ru.university.socialnetwork.store.ProfileRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

class MainWindowRemoveTest {
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
    private static void withWindow(Scenario scenario) throws Exception {
        FxTestSupport.run((Callable<Void>) () -> {
            MainWindow app = new MainWindow();
            Stage stage = new Stage();
            try {
                app.start(stage);
                scenario.run(app, (ProfileRepository) field(app, "repository"), (TableView<Profile>) field(app, "table"));
            } finally { stage.close(); app.stop(); }
            return null;
        });
    }

    private interface Scenario {
        void run(MainWindow app, ProfileRepository repository, TableView<Profile> table) throws Exception;
    }

    @Test
    void removeButtonFollowsEditableLikeEditButton() throws Exception {
        withWindow((app, repository, table) -> {
            repository.replaceAll(List.of(new EditableProfile(1, "Анна", "Город", 2000),
                    new DeletedProfile(2, "Архив", "Город", 2000, "")));
            refresh(app);
            Button remove = (Button) field(app, "removeButton");
            assertTrue(remove.isDisabled());
            table.getSelectionModel().select(repository.findById(1));
            assertFalse(remove.isDisabled());
            table.getSelectionModel().select(repository.findById(2));
            assertTrue(remove.isDisabled());
        });
    }

    @Test
    void removingProfileDeletesItFromTableAndTakesFriendshipsAway() throws Exception {
        withWindow((app, repository, table) -> {
            repository.add(new EditableProfile(1, "Анна", "Город", 2000));
            repository.add(new EditableProfile(2, "Иван", "Город", 2000));
            repository.addFriendship(new Friendship(1, 2, 3));
            refresh(app);
            table.getSelectionModel().select(repository.findById(1));
            ((Button) field(app, "removeButton")).fire();
            assertNull(repository.findById(1));
            assertEquals(0, repository.friendshipCount());
            assertEquals(1, table.getItems().size());
        });
    }

    @Test
    void administratorOfCommunityStaysInPlace() throws Exception {
        withWindow((app, repository, table) -> {
            repository.add(new EditableProfile(1, "Анна", "Город", 2000));
            repository.add(new Community(2, "Клуб", "Город", 2020, "Java", 1));
            refresh(app);
            table.getSelectionModel().select(repository.findById(1));
            Platform.runLater(() -> FxTestSupport.dismissDialogs(null));
            ((Button) field(app, "removeButton")).fire();
            assertNotNull(repository.findById(1));
            assertEquals(2, table.getItems().size());
        });
    }

    @Test
    void windowTitleMentionsSecondLab() throws Exception {
        withWindow((app, repository, table) ->
                assertTrue(((Stage) field(app, "stage")).getTitle().contains("лабораторная 2")));
    }
}
