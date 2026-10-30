package ru.university.socialnetwork.gui;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.university.socialnetwork.csv.CsvLoader;
import ru.university.socialnetwork.csv.CsvSaver;
import ru.university.socialnetwork.model.Editable;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.store.ProfileRepository;
import ru.university.socialnetwork.structure.Trace;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MainWindow extends Application {
    private static final double SCENE_WIDTH = 1320;
    private static final double SCENE_HEIGHT = 790;
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 560;
    private static final double SPLIT_POSITION = 0.5;
    private static final double GAP = 10;
    private static final double PROGRESS_WIDTH = 190;

    private ProfileRepository repository = new ProfileRepository();
    private final ObservableList<Profile> profiles = FXCollections.observableArrayList();
    private final TableView<Profile> table = ProfileTable.create(profiles);
    private final Button editButton = new Button("Изменить");
    private final Button removeButton = new Button("Удалить");
    private final Label status = new Label("Профилей: 0");
    private final CsvLoader loader = new CsvLoader();
    private final CsvSaver saver = new CsvSaver();
    private final StructureView structureView = new StructureView();
    private Stage stage;
    private final TaskRunner runner = new TaskRunner(() -> stage, status::setText,
            () -> "Профилей: " + repository.size());
    private final BooleanProperty busy = runner.busyProperty();
    private boolean dirty;
    private boolean friendsDirty;

    private final Workspace workspace = new Workspace() {
        @Override public ProfileRepository repository() { return repository; }
        @Override public TaskRunner runner() { return runner; }
        @Override public Window window() { return stage; }
        @Override public void show(Highlight highlight) { structureView.show(repository, highlight); }
        @Override public void refresh(Highlight highlight) { updateView(highlight); }
        @Override public void status(String text) { status.setText(text); }
        @Override public void markDirty() { MainWindow.this.markDirty(); }
        @Override public void markFriendsDirty() { friendsDirty = true; updateTitle(); }
        @Override public void friendsSaved() { friendsDirty = false; updateTitle(); }
        @Override public void resetDirty() { dirty = false; friendsDirty = false; updateTitle(); }
        @Override public boolean confirmDiscard() { return !unsaved() || GuiAlerts.confirmDiscard(stage); }
        @Override public void select(Profile profile) {
            table.getSelectionModel().select(profile);
            table.scrollTo(profile);
        }
    };

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Button loadButton = new Button("Загрузить из CSV");
        Button saveButton = new Button("Сохранить в CSV");
        Button addButton = new Button("Добавить");
        loadButton.disableProperty().bind(busy);
        saveButton.disableProperty().bind(busy);
        addButton.disableProperty().bind(busy);
        var notEditable = Bindings.createBooleanBinding(
                () -> !(table.getSelectionModel().getSelectedItem() instanceof Editable),
                table.getSelectionModel().selectedItemProperty());
        editButton.disableProperty().bind(busy.or(notEditable));
        removeButton.disableProperty().bind(busy.or(notEditable));
        loadButton.setOnAction(event -> chooseLoad());
        saveButton.setOnAction(event -> chooseSave());
        addButton.setOnAction(event -> add());
        editButton.setOnAction(event -> edit());
        removeButton.setOnAction(event -> remove());

        ProgressBar progress = new ProgressBar();
        progress.setPrefWidth(PROGRESS_WIDTH);
        progress.progressProperty().bind(runner.progressProperty());
        Button cancelButton = new Button("Отмена");
        cancelButton.setOnAction(event -> runner.cancel());
        for (var node : List.of(progress, cancelButton)) {
            node.visibleProperty().bind(busy);
            node.managedProperty().bind(busy);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(GAP, loadButton, saveButton, addButton, editButton, removeButton,
                spacer, progress, cancelButton);
        SplitPane split = new SplitPane(table, structureView);
        split.setDividerPositions(SPLIT_POSITION);
        VBox root = new VBox(GAP, buttons, split, new OperationsPanel(workspace), status);
        root.setPadding(new Insets(GAP));
        VBox.setVgrow(split, Priority.ALWAYS);
        stage.setScene(new Scene(root, SCENE_WIDTH, SCENE_HEIGHT));
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        updateTitle();
        stage.setOnCloseRequest(event -> {
            if (busy.get()) {
                event.consume();
                GuiAlerts.show(stage, Alert.AlertType.INFORMATION, "Операция выполняется",
                        "Дождитесь завершения операции или нажмите «Отмена».");
            } else if (unsaved() && !GuiAlerts.confirmDiscard(stage)) {
                event.consume();
            }
        });
        updateView();
        stage.show();
    }

    private void chooseLoad() {
        File file = CsvFiles.chooser("Выберите CSV-файл").showOpenDialog(stage);
        if (file == null || !workspace.confirmDiscard()) return;
        loadFile(file.toPath());
    }

    private void chooseSave() {
        FileChooser chooser = CsvFiles.chooser("Сохранить CSV-файл");
        chooser.setInitialFileName("profiles.csv");
        File file = chooser.showSaveDialog(stage);
        if (file != null) saveFile(file.toPath());
    }

    // Отделены от FileChooser, чтобы I/O можно было проверять без нативного диалога.
    void loadFile(Path file) {
        Task<LoadedCsv> task = new Task<>() {
            @Override protected LoadedCsv call() throws Exception {
                CsvLoader.LoadResult result = loader.load(file, (done, total) -> updateProgress(done, total));
                ProfileRepository replacement = new ProfileRepository();
                replacement.replaceAll(result.profiles()); // Валидация большого набора тоже в фоне.
                return new LoadedCsv(result, replacement);
            }
        };
        runner.run(task, "Загрузка CSV…", "Ошибка работы с CSV", loaded -> {
            CsvLoader.LoadResult result = loaded.result();
            boolean retained = result.profiles().isEmpty() && !profiles.isEmpty();
            if (!retained) {
                repository = loaded.repository();
                dirty = false;
                friendsDirty = false;
                updateView();
                updateTitle();
            }
            if (!result.errors().isEmpty()) {
                GuiAlerts.showCsvErrors(stage, result, retained);
            } else if (retained) {
                GuiAlerts.show(stage, Alert.AlertType.INFORMATION, "Пустой CSV",
                        "В файле нет профилей. Текущие данные сохранены.");
            }
        }, () -> { });
    }

    void saveFile(Path file) {
        List<Profile> snapshot = repository.snapshot();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                saver.save(file, snapshot);
                return null;
            }
        };
        runner.run(task, "Сохранение CSV…", "Ошибка работы с CSV", ignored -> {
            dirty = false;
            updateTitle();
            status.setText("Сохранено профилей: " + snapshot.size());
        }, () -> { });
    }

    private void add() {
        ProfileEditorDialog dialog = new ProfileEditorDialog(stage, null, repository::validateForAdd);
        dialog.showAndWait().ifPresent(profile -> {
            Trace trace = new Trace();
            repository.add(profile, trace);
            updateView(Highlight.of(trace, null, profile.getId(),
                    "Добавлен профиль " + profile.getId() + ", просмотрено узлов цепочки: " + trace.count()));
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
            updateView(new Highlight(new int[0], new int[0], source.getId(), "Изменён профиль " + source.getId()));
            table.getSelectionModel().select(replacement);
            markDirty();
        });
    }

    private void remove() {
        Profile source = table.getSelectionModel().getSelectedItem();
        if (!(source instanceof Editable)) return;
        Trace trace = new Trace();
        try {
            repository.remove(source.getId(), trace);
        } catch (IllegalArgumentException e) {
            GuiAlerts.show(stage, Alert.AlertType.ERROR, "Удаление невозможно", e.getMessage());
            return;
        }
        updateView(Highlight.of(trace, null, Highlight.NO_FOCUS,
                "Удалён профиль " + source.getId() + " вместе со связями"));
        markDirty();
        friendsDirty = true;
        updateTitle();
    }

    private void updateView() { updateView(Highlight.NONE); }

    private void updateView(Highlight highlight) {
        profiles.setAll(repository.snapshot());
        table.sort();
        status.setText("Профилей: " + repository.size());
        structureView.show(repository, highlight);
    }

    private record LoadedCsv(CsvLoader.LoadResult result, ProfileRepository repository) { }

    private boolean unsaved() { return dirty || friendsDirty; }
    private void markDirty() { dirty = true; updateTitle(); }
    private void updateTitle() {
        if (stage != null) stage.setTitle("Социальная сеть — лабораторная 2" + (unsaved() ? " *" : ""));
    }

    @Override
    public void stop() { runner.shutdown(); }
}
