package ru.university.socialnetwork.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.university.socialnetwork.csv.CsvLoadException;
import ru.university.socialnetwork.csv.CsvLoader;
import ru.university.socialnetwork.csv.CsvSaver;
import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.Profile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends Application {
    private final ObservableList<Profile> profiles = FXCollections.observableArrayList();
    private final TableView<Profile> table = new TableView<>(profiles);
    private final Button editButton = new Button("Изменить");
    private final CsvLoader loader = new CsvLoader();
    private final CsvSaver saver = new CsvSaver();

    @Override
    public void start(Stage stage) {
        TableColumn<Profile, String> typeColumn = new TableColumn<>("Тип");
        typeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(typeOf(data.getValue())));

        TableColumn<Profile, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));

        TableColumn<Profile, String> nameColumn = new TableColumn<>("Имя");
        nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));

        TableColumn<Profile, String> cityColumn = new TableColumn<>("Город");
        cityColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCity()));

        TableColumn<Profile, String> yearColumn = new TableColumn<>("Год");
        yearColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getBirthYear())));

        TableColumn<Profile, String> extraColumn = new TableColumn<>("Дополнительно");
        extraColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(extraOf(data.getValue())));

        table.getColumns().addAll(typeColumn, idColumn, nameColumn, cityColumn, yearColumn, extraColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button loadButton = new Button("Загрузить из CSV");
        Button saveButton = new Button("Сохранить в CSV");
        Button addButton = new Button("Добавить");

        editButton.setDisable(true);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            editButton.setDisable(!(newValue instanceof ru.university.socialnetwork.model.Editable));
        });

        loadButton.setOnAction(event -> load(stage));
        saveButton.setOnAction(event -> save(stage));
        addButton.setOnAction(event -> add());
        editButton.setOnAction(event -> edit());

        HBox buttons = new HBox(10, loadButton, saveButton, addButton, editButton);
        buttons.setPadding(new Insets(10));

        VBox root = new VBox(10, table, buttons);
        root.setPadding(new Insets(10));

        stage.setTitle("Социальная сеть");
        stage.setScene(new Scene(root, 950, 550));
        stage.show();
    }

    private String typeOf(Profile profile) {
        if (profile instanceof Community) {
            return "Сообщество";
        }
        if (profile instanceof DeletedProfile) {
            return "Удалённый профиль";
        }
        return "Профиль";
    }

    private String extraOf(Profile profile) {
        if (profile instanceof Community community) {
            return community.getDescription() + ", администратор: " + community.getAdministratorId();
        }
        if (profile instanceof DeletedProfile deletedProfile) {
            return "Причина: " + deletedProfile.getReason();
        }
        return "";
    }

    private void load(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите CSV-файл");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));

        File file = chooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        try {
            CsvLoader.LoadResult result = loader.load(file.toPath());
            profiles.setAll(result.profiles());

            if (!result.errors().isEmpty()) {
                StringBuilder message = new StringBuilder("Некоторые строки пропущены:\n\n");

                for (CsvLoadException error : result.errors()) {
                    message.append("Строка ")
                            .append(error.getLineNumber())
                            .append(": ")
                            .append(error.getCode())
                            .append(" — ")
                            .append(error.getMessage())
                            .append("\n");
                }

                showAlert(Alert.AlertType.WARNING, "Ошибки загрузки", message.toString());
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", e.getMessage());
        }
    }

    private void save(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить CSV-файл");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));

        File file = chooser.showSaveDialog(stage);

        if (file == null) {
            return;
        }

        try {
            saver.save(file.toPath(), new ArrayList<>(profiles));
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка сохранения", e.getMessage());
        }
    }

    private void add() {
        Dialog<Community> dialog = communityDialog(null);
        dialog.setTitle("Добавить сообщество");

        dialog.showAndWait().ifPresent(community -> {
            profiles.add(community);
            table.getSelectionModel().select(community);
        });
    }

    private void edit() {
        Profile selected = table.getSelectionModel().getSelectedItem();

        if (!(selected instanceof Community community)) {
            return;
        }

        Dialog<Community> dialog = communityDialog(community);
        dialog.setTitle("Изменить сообщество");

        dialog.showAndWait().ifPresent(updated -> {
            community.setName(updated.getName());
            community.setCity(updated.getCity());
            community.setBirthYear(updated.getBirthYear());
            community.setDescription(updated.getDescription());
            community.setAdministratorId(updated.getAdministratorId());
            table.refresh();
        });
    }

    private Dialog<Community> communityDialog(Community source) {
        Dialog<Community> dialog = new Dialog<>();
        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField idField = new TextField(source == null ? "" : String.valueOf(source.getId()));
        TextField nameField = new TextField(source == null ? "" : source.getName());
        TextField cityField = new TextField(source == null ? "" : source.getCity());
        TextField yearField = new TextField(source == null ? "" : String.valueOf(source.getBirthYear()));
        TextField descriptionField = new TextField(source == null ? "" : source.getDescription());
        TextField administratorField = new TextField(source == null ? "" : String.valueOf(source.getAdministratorId()));

        if (source != null) {
            idField.setDisable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.addRow(0, new Label("ID:"), idField);
        grid.addRow(1, new Label("Название:"), nameField);
        grid.addRow(2, new Label("Город:"), cityField);
        grid.addRow(3, new Label("Год:"), yearField);
        grid.addRow(4, new Label("Описание:"), descriptionField);
        grid.addRow(5, new Label("ID администратора:"), administratorField);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }

            try {
                Community community = new Community(
                        Integer.parseInt(idField.getText().trim()),
                        nameField.getText().trim(),
                        cityField.getText().trim(),
                        Integer.parseInt(yearField.getText().trim()),
                        descriptionField.getText().trim(),
                        Integer.parseInt(administratorField.getText().trim())
                );

                List<String> errors = community.validate();

                if (!errors.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка валидации", String.join("\n", errors));
                    return null;
                }

                return community;
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "ID, год и ID администратора должны быть числами");
                return null;
            }
        });

        return dialog;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void launch(Class<? extends Application> appClass, String... args) {
        Application.launch(appClass, args);
    }
}
