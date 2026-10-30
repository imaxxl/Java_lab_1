package ru.university.socialnetwork.gui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.function.Consumer;

final class FriendshipDialog extends Dialog<int[]> {
    private static final double GAP = 10;

    private final TextField firstField = new TextField();
    private final TextField secondField = new TextField();
    private final TextField strengthField = new TextField("1");
    private final boolean withStrength;
    private int[] candidate;

    FriendshipDialog(Window owner, String title, String action, boolean withStrength, Consumer<int[]> validator) {
        this.withStrength = withStrength;
        initOwner(owner);
        setTitle(title);
        firstField.setId("firstField");
        secondField.setId("secondField");
        strengthField.setId("strengthField");
        GridPane grid = new GridPane();
        grid.setHgap(GAP);
        grid.setVgap(GAP);
        grid.setPadding(new Insets(GAP * 2, GAP * 2, GAP, GAP * 2));
        grid.addRow(0, new Label("ID первого:"), firstField);
        grid.addRow(1, new Label("ID второго:"), secondField);
        if (withStrength) grid.addRow(2, new Label("Сила связи:"), strengthField);
        getDialogPane().setContent(grid);
        ButtonType actionType = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(actionType, cancelType);
        Button button = (Button) getDialogPane().lookupButton(actionType);
        button.addEventFilter(ActionEvent.ACTION, event -> {
            candidate = null;
            try {
                int[] values = read();
                validator.accept(values);
                candidate = values;
            } catch (IllegalArgumentException e) {
                event.consume();
                GuiAlerts.show(getDialogPane().getScene().getWindow(), Alert.AlertType.ERROR,
                        "Некорректные данные", e.getMessage());
            }
        });
        setResultConverter(chosen -> chosen == actionType ? candidate : null);
    }

    private int[] read() {
        int first = number(firstField, "ID первого");
        int second = number(secondField, "ID второго");
        int strength = withStrength ? number(strengthField, "Сила связи") : 1;
        return new int[]{first, second, strength};
    }

    private static int number(TextField field, String label) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Поле «" + label + "» должно быть целым числом", e);
        }
    }
}
