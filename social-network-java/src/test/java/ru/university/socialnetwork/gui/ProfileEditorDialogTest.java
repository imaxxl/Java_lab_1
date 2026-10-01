package ru.university.socialnetwork.gui;

import javafx.application.Platform;
import javafx.scene.control.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.*;
import ru.university.socialnetwork.store.ProfileRepository;
import static org.junit.jupiter.api.Assertions.*;

class ProfileEditorDialogTest {
    @BeforeAll static void startFx() throws Exception { FxTestSupport.initialize(); }

    static void fill(DialogPane pane, String id, String name) {
        ((TextField) pane.lookup("#idField")).setText(id);
        ((TextField) pane.lookup("#nameField")).setText(name);
        ((TextField) pane.lookup("#cityField")).setText("Москва");
        ((TextField) pane.lookup("#yearField")).setText("2000");
    }

    static Button save(DialogPane pane) {
        var type = pane.getButtonTypes().stream()
                .filter(t -> t.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow();
        return (Button) pane.lookupButton(type);
    }

    @Test
    void invalidInputKeepsEditorAndEnteredValues() throws Exception {
        FxTestSupport.run(() -> {
            ProfileEditorDialog dialog = new ProfileEditorDialog(null, null, profile -> { });
            try {
                dialog.show();
                fill(dialog.getDialogPane(), "1", "");
                Platform.runLater(() -> FxTestSupport.dismissDialogs(dialog.getDialogPane()));
                save(dialog.getDialogPane()).fire();
                assertTrue(dialog.isShowing());
                assertNull(dialog.getResult());
                assertEquals("Москва", ((TextField) dialog.getDialogPane().lookup("#cityField")).getText());
            } finally { dialog.close(); }
            return null;
        });
    }

    @Test
    void duplicateIdKeepsEditorOpen() throws Exception {
        FxTestSupport.run(() -> {
            ProfileRepository repository = new ProfileRepository();
            repository.add(new EditableProfile(1, "Первый", "Москва", 2000));
            ProfileEditorDialog dialog = new ProfileEditorDialog(null, null, repository::validateForAdd);
            try {
                dialog.show();
                fill(dialog.getDialogPane(), "1", "Дубль");
                Platform.runLater(() -> FxTestSupport.dismissDialogs(dialog.getDialogPane()));
                save(dialog.getDialogPane()).fire();
                assertTrue(dialog.isShowing());
                assertEquals(1, repository.size());
            } finally { dialog.close(); }
            return null;
        });
    }

    @Test
    void addCreatesOrdinaryEditableProfile() throws Exception {
        FxTestSupport.run(() -> {
            ProfileEditorDialog dialog = new ProfileEditorDialog(null, null, profile -> { });
            dialog.show();
            fill(dialog.getDialogPane(), "1", "Анна");
            save(dialog.getDialogPane()).fire();
            assertFalse(dialog.isShowing());
            assertInstanceOf(EditableProfile.class, dialog.getResult());
            assertEquals("Анна", dialog.getResult().getName());
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void addOffersNoDeletedTypeAndCanCreateCommunity() throws Exception {
        FxTestSupport.run(() -> {
            ProfileRepository repository = new ProfileRepository();
            repository.add(new EditableProfile(1, "Администратор", "Москва", 2000));
            ProfileEditorDialog dialog = new ProfileEditorDialog(null, null, repository::validateForAdd);
            dialog.show();
            ComboBox<ProfileType> types = (ComboBox<ProfileType>) dialog.getDialogPane().lookup("#typeField");
            assertFalse(types.getItems().contains(ProfileType.DELETED));
            types.setValue(ProfileType.COMMUNITY);
            fill(dialog.getDialogPane(), "2", "Java Club");
            ((TextField) dialog.getDialogPane().lookup("#descriptionField")).setText("Java; Kotlin");
            ((TextField) dialog.getDialogPane().lookup("#administratorField")).setText("1");
            save(dialog.getDialogPane()).fire();
            assertInstanceOf(Community.class, dialog.getResult());
            assertEquals("Java; Kotlin", ((Community) dialog.getResult()).getDescription());
            return null;
        });
    }

    @Test
    void deletedProfileCannotOpenEditor() throws Exception {
        FxTestSupport.run(() -> {
            assertThrows(IllegalArgumentException.class, () -> new ProfileEditorDialog(null,
                    new DeletedProfile(1, "Архив", "Город", 2000, "Причина"), profile -> { }));
            return null;
        });
    }
}
