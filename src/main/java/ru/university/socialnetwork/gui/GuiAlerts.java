package ru.university.socialnetwork.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import ru.university.socialnetwork.csv.CsvLoader;


public final class GuiAlerts {
    private static final int MAX_SHOWN_ERRORS = 50;

    private GuiAlerts() { }

    public static void show(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirmDiscard(Window owner) {
        ButtonType discard = new ButtonType("Продолжить без сохранения");
        ButtonType cancel = new ButtonType("Отмена", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Есть несохранённые изменения. Продолжить?", discard, cancel);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Несохранённые изменения");
        alert.setHeaderText(null);
        alert.getDialogPane().lookupButton(discard).setStyle("-fx-font-weight: normal;");
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(discard)).setDefaultButton(false);
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        return alert.showAndWait().filter(discard::equals).isPresent();
    }

    public static void showCsvErrors(Window owner, CsvLoader.LoadResult result, boolean retainedOldData) {
        if (result.errors().isEmpty()) return;
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Ошибки загрузки CSV");
        String summary = "Корректных профилей в файле: " + result.profiles().size()
                + ". Ошибок: " + result.errors().size() + ".";
        if (retainedOldData) summary += " Текущие данные сохранены.";
        if (result.errors().size() > MAX_SHOWN_ERRORS) {
            summary += " Ниже первые " + MAX_SHOWN_ERRORS + " ошибок.";
        }
        alert.setHeaderText(summary);
        StringBuilder details = new StringBuilder();
        result.errors().stream().limit(MAX_SHOWN_ERRORS).forEach(error -> details
                .append("Строка ").append(error.getLineNumber()).append(": ")
                .append(error.getCode()).append(" — ").append(error.getMessage()).append('\n'));
        TextArea text = new TextArea(details.toString());
        text.setEditable(false);
        text.setWrapText(true);
        text.setPrefColumnCount(75);
        text.setPrefRowCount(8);
        alert.getDialogPane().setContent(text);
        alert.setResizable(true);
        alert.showAndWait();
    }
}
