package ru.university.socialnetwork.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import ru.university.socialnetwork.csv.CsvLoadException;
import ru.university.socialnetwork.csv.CsvLoader;

import java.util.List;

public final class GuiAlerts {
    private static final int MAX_SHOWN_ERRORS = 50;
    private static final int ERRORS_COLUMNS = 75;
    private static final int ERRORS_ROWS = 8;

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
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Есть несохранённые изменения. Продолжить?", discard, cancel);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Несохранённые изменения");
        alert.setHeaderText(null);
        alert.getDialogPane().lookupButton(discard).setStyle("-fx-font-weight: normal;");
        ((Button) alert.getDialogPane().lookupButton(discard)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        return alert.showAndWait().filter(discard::equals).isPresent();
    }

    public static void showCsvErrors(Window owner, CsvLoader.LoadResult result, boolean retainedOldData) {
        showCsvErrors(owner, "профилей", result.profiles().size(), result.errors(), retainedOldData);
    }

    public static void showCsvErrors(Window owner, String noun, int valid, List<CsvLoadException> errors,
                                     boolean retainedOldData) {
        if (errors.isEmpty()) return;
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Ошибки загрузки CSV");
        String summary = "Корректных " + noun + " в файле: " + valid + ". Ошибок: " + errors.size() + ".";
        if (retainedOldData) summary += " Текущие данные сохранены.";
        if (errors.size() > MAX_SHOWN_ERRORS) {
            summary += " Ниже первые " + MAX_SHOWN_ERRORS + " ошибок.";
        }
        alert.setHeaderText(summary);
        StringBuilder details = new StringBuilder();
        errors.stream().limit(MAX_SHOWN_ERRORS).forEach(error -> details
                .append("Строка ").append(error.getLineNumber()).append(": ")
                .append(error.getCode()).append(" — ").append(error.getMessage()).append('\n'));
        TextArea text = new TextArea(details.toString());
        text.setEditable(false);
        text.setWrapText(true);
        text.setPrefColumnCount(ERRORS_COLUMNS);
        text.setPrefRowCount(ERRORS_ROWS);
        alert.getDialogPane().setContent(text);
        alert.setResizable(true);
        alert.showAndWait();
    }
}
