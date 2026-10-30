package ru.university.socialnetwork.gui;

import javafx.application.Platform;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FriendshipDialogTest {
    @BeforeAll static void startFx() throws Exception { FxTestSupport.initialize(); }

    private static void fill(DialogPane pane, String first, String second, String strength) {
        ((TextField) pane.lookup("#firstField")).setText(first);
        ((TextField) pane.lookup("#secondField")).setText(second);
        TextField field = (TextField) pane.lookup("#strengthField");
        if (field != null) field.setText(strength);
    }

    private static Button action(DialogPane pane) {
        var type = pane.getButtonTypes().stream()
                .filter(t -> t.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow();
        return (Button) pane.lookupButton(type);
    }

    @Test
    void validValuesAreReturned() throws Exception {
        FxTestSupport.run(() -> {
            FriendshipDialog dialog = new FriendshipDialog(null, "Новая дружба", "Добавить", true, values -> { });
            try {
                dialog.show();
                fill(dialog.getDialogPane(), "1", "2", "7");
                action(dialog.getDialogPane()).fire();
                assertArrayEquals(new int[]{1, 2, 7}, dialog.getResult());
            } finally { dialog.close(); }
            return null;
        });
    }

    @Test
    void notANumberKeepsDialogOpen() throws Exception {
        FxTestSupport.run(() -> {
            FriendshipDialog dialog = new FriendshipDialog(null, "Новая дружба", "Добавить", true, values -> { });
            try {
                dialog.show();
                fill(dialog.getDialogPane(), "один", "2", "7");
                Platform.runLater(() -> FxTestSupport.dismissDialogs(dialog.getDialogPane()));
                action(dialog.getDialogPane()).fire();
                assertTrue(dialog.isShowing());
                assertNull(dialog.getResult());
                assertEquals("2", ((TextField) dialog.getDialogPane().lookup("#secondField")).getText());
            } finally { dialog.close(); }
            return null;
        });
    }

    @Test
    void ruleViolationFromValidatorKeepsDialogOpen() throws Exception {
        FxTestSupport.run(() -> {
            FriendshipDialog dialog = new FriendshipDialog(null, "Новая дружба", "Добавить", true, values -> {
                throw new IllegalArgumentException("Такая дружба уже есть");
            });
            try {
                dialog.show();
                fill(dialog.getDialogPane(), "1", "2", "7");
                Platform.runLater(() -> FxTestSupport.dismissDialogs(dialog.getDialogPane()));
                action(dialog.getDialogPane()).fire();
                assertTrue(dialog.isShowing());
                assertNull(dialog.getResult());
            } finally { dialog.close(); }
            return null;
        });
    }

    @Test
    void removalDialogAsksOnlyForTwoProfiles() throws Exception {
        FxTestSupport.run(() -> {
            FriendshipDialog dialog = new FriendshipDialog(null, "Удаление дружбы", "Удалить", false, values -> { });
            try {
                dialog.show();
                assertNull(dialog.getDialogPane().lookup("#strengthField"));
                fill(dialog.getDialogPane(), "3", "4", "");
                action(dialog.getDialogPane()).fire();
                assertEquals(3, dialog.getResult()[0]);
                assertEquals(4, dialog.getResult()[1]);
            } finally { dialog.close(); }
            return null;
        });
    }
}
