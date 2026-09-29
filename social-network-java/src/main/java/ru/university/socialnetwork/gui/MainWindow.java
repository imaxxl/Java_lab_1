package ru.university.socialnetwork.gui;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.university.socialnetwork.csv.CsvLoader;
import ru.university.socialnetwork.csv.CsvSaver;
import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.Editable;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.model.ProfileType;
import ru.university.socialnetwork.store.ProfileRepository;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MainWindow extends Application {
    private ProfileRepository repository = new ProfileRepository();
    private final ObservableList<Profile> profiles = FXCollections.observableArrayList();
    private final TableView<Profile> table = new TableView<>(profiles);
    private final Button editButton = new Button("Изменить");
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final Label status = new Label("Профилей: 0");
    private final CsvLoader loader = new CsvLoader();
    private final CsvSaver saver = new CsvSaver();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "csv-io");
        thread.setDaemon(true);
        return thread;
    });
    private Stage stage;
    private boolean dirty;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        TableColumn<Profile, String> typeColumn = new TableColumn<>("Тип");
        typeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(ProfileType.of(data.getValue()).toString()));
        TableColumn<Profile, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        TableColumn<Profile, String> nameColumn = new TableColumn<>("Имя");
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        TableColumn<Profile, String> cityColumn = new TableColumn<>("Город");
        cityColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCity()));
        TableColumn<Profile, Integer> yearColumn = new TableColumn<>("Год");
        yearColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getBirthYear()));
        TableColumn<Profile, String> extraColumn = new TableColumn<>("Дополнительно");
        extraColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(extraOf(data.getValue())));
        typeColumn.setPrefWidth(180);
        idColumn.setPrefWidth(65);
        nameColumn.setPrefWidth(160);
        cityColumn.setPrefWidth(140);
        yearColumn.setPrefWidth(80);
        extraColumn.setPrefWidth(370);
        table.getColumns().addAll(List.of(typeColumn, idColumn, nameColumn, cityColumn, yearColumn, extraColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Button loadButton = new Button("Загрузить из CSV");
        Button saveButton = new Button("Сохранить в CSV");
        Button addButton = new Button("Добавить");
        loadButton.disableProperty().bind(busy);
        saveButton.disableProperty().bind(busy);
        addButton.disableProperty().bind(busy);
        editButton.disableProperty().bind(busy.or(Bindings.createBooleanBinding(
                () -> !(table.getSelectionModel().getSelectedItem() instanceof Editable),
                table.getSelectionModel().selectedItemProperty())));
        loadButton.setOnAction(event -> chooseLoad());
        saveButton.setOnAction(event -> chooseSave());
        addButton.setOnAction(event -> add());
        editButton.setOnAction(event -> edit());

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(22, 22);
        progress.visibleProperty().bind(busy);
        progress.managedProperty().bind(busy);
        HBox buttons = new HBox(10, loadButton, saveButton, addButton, editButton, progress);
        HBox footer = new HBox(status);
        VBox root = new VBox(12, table, buttons, footer);
        root.setPadding(new Insets(12));
        VBox.setVgrow(table, Priority.ALWAYS);
        stage.setScene(new Scene(root, 1050, 580));
        stage.setMinWidth(740);
        stage.setMinHeight(400);
        updateTitle();
        stage.setOnCloseRequest(event -> {
            if (busy.get()) {
                event.consume();
                GuiAlerts.show(stage, Alert.AlertType.INFORMATION, "Операция выполняется",
                        "Дождитесь завершения загрузки или сохранения.");
            } else if (dirty && !GuiAlerts.confirmDiscard(stage)) {
                event.consume();
            }
        });
        stage.show();
    }

    private static String extraOf(Profile profile) {
        if (profile instanceof Community community) {
            return community.getDescription() + ", администратор: " + community.getAdministratorId();
        }
        if (profile instanceof DeletedProfile deleted) return "Причина: " + deleted.getReason();
        return "";
    }

    private void chooseLoad() {
        File file = csvChooser("Выберите CSV-файл").showOpenDialog(stage);
        if (file == null || (dirty && !GuiAlerts.confirmDiscard(stage))) return;
        loadFile(file.toPath());
    }

    private void chooseSave() {
        FileChooser chooser = csvChooser("Сохранить CSV-файл");
        chooser.setInitialFileName("profiles.csv");
        File file = chooser.showSaveDialog(stage);
        if (file != null) saveFile(file.toPath());
    }

    private static FileChooser csvChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV UTF-8", "*.csv"));
        return chooser;
    }

    // Отделены от FileChooser, чтобы I/O можно было проверять без нативного диалога.
    void loadFile(Path file) {
        Task<LoadedCsv> task = new Task<>() {
            @Override protected LoadedCsv call() throws Exception {
                CsvLoader.LoadResult result = loader.load(file);
                ProfileRepository replacement = new ProfileRepository();
                replacement.replaceAll(result.profiles()); // Валидация большого набора тоже в фоне.
                return new LoadedCsv(result, replacement);
            }
        };
        runTask(task, "Загрузка CSV…", loaded -> {
            CsvLoader.LoadResult result = loaded.result();
            boolean retained = result.profiles().isEmpty() && !profiles.isEmpty();
            if (!retained) {
                repository = loaded.repository();
                updateView();
                dirty = false;
                updateTitle();
            }
            if (!result.errors().isEmpty()) {
                GuiAlerts.showCsvErrors(stage, result, retained);
            } else if (retained) {
                GuiAlerts.show(stage, Alert.AlertType.INFORMATION, "Пустой CSV",
                        "В файле нет профилей. Текущие данные сохранены.");
            }
        });
    }

    void saveFile(Path file) {
        List<Profile> snapshot = repository.snapshot();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                saver.save(file, snapshot);
                return null;
            }
        };
        runTask(task, "Сохранение CSV…", ignored -> {
            dirty = false;
            updateTitle();
            status.setText("Сохранено профилей: " + snapshot.size());
        });
    }

    private <T> void runTask(Task<T> task, String message, Consumer<T> onSuccess) {
        if (busy.get()) throw new IllegalStateException("Другая операция ещё выполняется");
        busy.set(true);
        status.setText(message);
        task.setOnSucceeded(event -> {
            busy.set(false);
            status.setText("Профилей: " + repository.size());
            try {
                onSuccess.accept(task.getValue());
            } catch (IllegalArgumentException e) {
                GuiAlerts.show(stage, Alert.AlertType.ERROR, "Некорректные данные", e.getMessage());
            }
        });
        task.setOnFailed(event -> {
            busy.set(false);
            status.setText("Профилей: " + repository.size());
            Throwable failure = task.getException();
            GuiAlerts.show(stage, Alert.AlertType.ERROR, "Ошибка работы с CSV",
                    failure == null ? "Неизвестная ошибка" : failure.getMessage());
        });
        task.setOnCancelled(event -> {
            busy.set(false);
            status.setText("Операция отменена. Профилей: " + repository.size());
        });
        ioExecutor.execute(task);
    }

    private void add() {
        ProfileEditorDialog dialog = new ProfileEditorDialog(stage, null, repository::validateForAdd);
        dialog.showAndWait().ifPresent(profile -> {
            repository.add(profile);
            updateView();
            table.getSelectionModel().select(profile);
            markDirty();
        });
    }

    private void edit() {
        Profile source = table.getSelectionModel().getSelectedItem();
        if (!(source instanceof Editable)) return;
        ProfileEditorDialog dialog = new ProfileEditorDialog(stage, source,
                replacement -> repository.validateForReplace(source.getId(), replacement));
        dialog.showAndWait().ifPresent(replacement -> {
            repository.replace(source.getId(), replacement);
            updateView();
            table.getSelectionModel().select(replacement);
            markDirty();
        });
    }

    private void updateView() {
        profiles.setAll(repository.snapshot());
        table.sort();
        status.setText("Профилей: " + repository.size());
    }

    private record LoadedCsv(CsvLoader.LoadResult result, ProfileRepository repository) { }

    private void markDirty() { dirty = true; updateTitle(); }
    private void updateTitle() { if (stage != null) stage.setTitle("Социальная сеть — лабораторная 1" + (dirty ? " *" : "")); }

    @Override
    public void stop() { ioExecutor.shutdownNow(); }
}
