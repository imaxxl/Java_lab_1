package ru.university.socialnetwork.gui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.Editable;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.model.ProfileType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Проверяет ввод до закрытия окна; не изменяет исходный объект. */
public final class ProfileEditorDialog extends Dialog<Profile> {
    private final ComboBox<ProfileType> typeField = new ComboBox<>();
    private final TextField idField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField cityField = new TextField();
    private final TextField yearField = new TextField();
    private final TextField descriptionField = new TextField();
    private final TextField administratorField = new TextField();
    private final ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
    private Profile candidate;

    public ProfileEditorDialog(Window owner, Profile source, Consumer<Profile> contextValidator) {
        Objects.requireNonNull(contextValidator);
        if (source != null && !(source instanceof Editable)) {
            throw new IllegalArgumentException("Read-only профиль нельзя редактировать");
        }
        if (owner != null) initOwner(owner);
        setTitle(source == null ? "Добавить профиль" : "Изменить профиль");
        getDialogPane().getButtonTypes().addAll(saveType,
                new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE));
        typeField.getItems().setAll(Arrays.stream(ProfileType.values())
                .filter(ProfileType::isEditableType).toList());
        typeField.setValue(source == null ? ProfileType.PROFILE : ProfileType.of(source));
        typeField.setDisable(source != null);
        typeField.setId("typeField");
        idField.setId("idField");
        nameField.setId("nameField");
        cityField.setId("cityField");
        yearField.setId("yearField");
        descriptionField.setId("descriptionField");
        administratorField.setId("administratorField");
        if (source != null) {
            idField.setText(String.valueOf(source.getId()));
            nameField.setText(source.getName());
            cityField.setText(source.getCity());
            yearField.setText(String.valueOf(source.getBirthYear()));
            idField.setDisable(true);
            if (source instanceof Community community) {
                descriptionField.setText(community.getDescription());
                administratorField.setText(String.valueOf(community.getAdministratorId()));
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        Label descriptionLabel = new Label("Описание:");
        Label administratorLabel = new Label("ID администратора:");
        grid.addRow(0, new Label("Тип:"), typeField);
        grid.addRow(1, new Label("ID:"), idField);
        grid.addRow(2, new Label("Имя / название:"), nameField);
        grid.addRow(3, new Label("Город:"), cityField);
        grid.addRow(4, new Label("Год:"), yearField);
        grid.addRow(5, descriptionLabel, descriptionField);
        grid.addRow(6, administratorLabel, administratorField);
        List<Node> communityFields = List.of(descriptionLabel, descriptionField,
                administratorLabel, administratorField);
        Runnable updateFields = () -> communityFields.forEach(node -> {
            boolean shown = typeField.getValue() == ProfileType.COMMUNITY;
            node.setVisible(shown);
            node.setManaged(shown);
        });
        typeField.valueProperty().addListener((obs, oldValue, newValue) -> updateFields.run());
        updateFields.run();
        getDialogPane().setContent(grid);
        setResizable(true);

        Button save = (Button) getDialogPane().lookupButton(saveType);
        save.addEventFilter(ActionEvent.ACTION, event -> {
            candidate = null;
            try {
                Profile parsed = readFields();
                List<String> errors = parsed.validate();
                if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("\n", errors));
                contextValidator.accept(parsed);
                candidate = parsed;
            } catch (IllegalArgumentException e) {
                event.consume(); // Не позволяет JavaFX закрыть окно при ошибке.
                Window dialogWindow = getDialogPane().getScene() == null ? owner
                        : getDialogPane().getScene().getWindow();
                GuiAlerts.show(dialogWindow, Alert.AlertType.ERROR, "Некорректные данные", e.getMessage());
            }
        });
        setResultConverter(button -> button == saveType ? candidate : null);
    }

    private Profile readFields() {
        ProfileType type = typeField.getValue();
        if (type == null) throw new IllegalArgumentException("Выберите тип профиля");
        int id = number(idField, "ID");
        int year = number(yearField, "Год");
        String name = nameField.getText().trim();
        String city = cityField.getText().trim();
        if (type == ProfileType.COMMUNITY) {
            return new Community(id, name, city, year, descriptionField.getText().trim(),
                    number(administratorField, "ID администратора"));
        }
        return new EditableProfile(id, name, city, year);
    }

    private static int number(TextField field, String label) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Поле «" + label + "» должно быть целым числом", e);
        }
    }
}
