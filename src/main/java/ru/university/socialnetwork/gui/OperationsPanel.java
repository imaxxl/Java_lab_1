package ru.university.socialnetwork.gui;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ru.university.socialnetwork.csv.FriendshipLoader;
import ru.university.socialnetwork.csv.FriendshipSaver;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.store.DataGenerator;
import ru.university.socialnetwork.store.ProfileRepository;
import ru.university.socialnetwork.structure.Chain;
import ru.university.socialnetwork.structure.Trace;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;

final class OperationsPanel extends VBox {
    private static final int DEFAULT_COUNT = 1000;
    private static final int MAX_COUNT = 1_000_000;
    private static final int ID_COLUMNS = 6;
    private static final int COUNT_COLUMNS = 8;
    private static final double GAP = 8;

    private final Workspace workspace;
    private final FriendshipLoader friendshipLoader = new FriendshipLoader();
    private final FriendshipSaver friendshipSaver = new FriendshipSaver();
    private final TextField findField = idField("findField", "ID");
    private final TextField fromField = idField("fromField", "от");
    private final TextField toField = idField("toField", "до");
    private final TextField countField = new TextField(String.valueOf(DEFAULT_COUNT));

    OperationsPanel(Workspace workspace) {
        super(GAP);
        this.workspace = workspace;
        countField.setId("countField");
        countField.setPrefColumnCount(COUNT_COLUMNS);
        FlowPane search = row(new Label("Поиск по ID:"), findField, button("findButton", "Найти", this::find),
                new Label("Цепочка знакомств:"), fromField, toField,
                button("chainButton", "Найти цепочку", this::chain));
        FlowPane friends = row(new Label("Дружба:"), button("addFriendButton", "Добавить…", this::addFriendship),
                button("removeFriendButton", "Удалить…", this::removeFriendship),
                button("loadFriendsButton", "Из CSV…", this::chooseLoadFriendships),
                button("saveFriendsButton", "В CSV…", this::chooseSaveFriendships));
        FlowPane generation = row(new Label("Генерация:"), countField,
                button("generateButton", "Создать", this::generate), button("clearButton", "Очистить", this::clear),
                button("raceButton", "Гонка потоков…", this::race));
        getChildren().addAll(search, friends, generation);
    }

    private static TextField idField(String id, String prompt) {
        TextField field = new TextField();
        field.setId(id);
        field.setPromptText(prompt);
        field.setPrefColumnCount(ID_COLUMNS);
        return field;
    }

    private static FlowPane row(Node... nodes) {
        FlowPane row = new FlowPane(GAP, GAP, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button button(String id, String text, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.disableProperty().bind(workspace.runner().busyProperty());
        button.setOnAction(event -> action.run());
        return button;
    }

    void find() {
        Integer id = readId(findField, "ID для поиска");
        if (id == null) return;
        Trace trace = new Trace();
        Profile profile = workspace.repository().findById(id, trace);
        String text = profile == null
                ? "Профиль " + id + " не найден, просмотрено узлов цепочки: " + trace.count()
                : "Найден профиль " + id + " (" + profile.getName() + "), просмотрено узлов цепочки: " + trace.count();
        workspace.show(Highlight.of(trace, null, profile == null ? Highlight.NO_FOCUS : id, text));
        workspace.status(text);
        if (profile != null) workspace.select(profile);
    }

    void chain() {
        Integer from = readId(fromField, "От");
        Integer to = readId(toField, "До");
        if (from == null || to == null) return;
        ProfileRepository repository = workspace.repository();
        if (repository.findById(from) == null || repository.findById(to) == null) {
            warn("Цепочка знакомств", "Оба профиля должны существовать.");
            return;
        }
        Trace trace = new Trace();
        Chain<Integer> chain = repository.shortestChain(from, to, trace);
        String text = chain.isEmpty()
                ? "Цепочки между " + from + " и " + to + " нет, просмотрено вершин: " + trace.count()
                : "Цепочка из " + chain.length() + " вершин: " + describe(chain) + ", просмотрено вершин: " + trace.count();
        workspace.show(Highlight.of(trace, chain, Highlight.NO_FOCUS, text));
        workspace.status(text);
    }

    private static String describe(Chain<Integer> chain) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < chain.length(); i++) {
            if (i > 0) text.append(" → ");
            text.append(chain.get(i));
        }
        return text.toString();
    }

    private void addFriendship() {
        ProfileRepository repository = workspace.repository();
        FriendshipDialog dialog = new FriendshipDialog(workspace.window(), "Новая дружба", "Добавить", true,
                values -> checkNew(repository, values));
        dialog.showAndWait().ifPresent(values -> {
            repository.addFriendship(new Friendship(values[0], values[1], values[2]));
            workspace.markFriendsDirty();
            int[] pair = {values[0], values[1]};
            workspace.refresh(new Highlight(pair, pair, Highlight.NO_FOCUS,
                    "Добавлена дружба " + values[0] + " — " + values[1] + ", сила " + values[2]));
        });
    }

    private static void checkNew(ProfileRepository repository, int[] values) {
        new Friendship(values[0], values[1], values[2]);
        if (repository.findById(values[0]) == null || repository.findById(values[1]) == null) {
            throw new IllegalArgumentException("Оба профиля должны существовать");
        }
        if (repository.friendshipStrength(values[0], values[1]) > 0) {
            throw new IllegalArgumentException("Такая дружба уже есть");
        }
    }

    private void removeFriendship() {
        ProfileRepository repository = workspace.repository();
        FriendshipDialog dialog = new FriendshipDialog(workspace.window(), "Удаление дружбы", "Удалить", false,
                values -> {
                    if (repository.friendshipStrength(values[0], values[1]) == 0) {
                        throw new IllegalArgumentException("Такой дружбы нет");
                    }
                });
        dialog.showAndWait().ifPresent(values -> {
            repository.removeFriendship(values[0], values[1]);
            workspace.markFriendsDirty();
            int[] pair = {values[0], values[1]};
            workspace.refresh(new Highlight(pair, new int[0], Highlight.NO_FOCUS,
                    "Удалена дружба " + values[0] + " — " + values[1]));
        });
    }

    private void chooseLoadFriendships() {
        File file = CsvFiles.chooser("Выберите CSV с дружбами").showOpenDialog(workspace.window());
        if (file != null) loadFriendships(file.toPath());
    }

    private void chooseSaveFriendships() {
        FileChooser chooser = CsvFiles.chooser("Сохранить дружбы в CSV");
        chooser.setInitialFileName("friendships.csv");
        File file = chooser.showSaveDialog(workspace.window());
        if (file != null) saveFriendships(file.toPath());
    }

    void loadFriendships(Path file) {
        ProfileRepository repository = workspace.repository();
        TaskRunner runner = workspace.runner();
        Task<LoadedFriendships> task = new Task<>() {
            @Override protected LoadedFriendships call() throws Exception {
                FriendshipLoader.LoadResult result = friendshipLoader.load(file, repository,
                        (done, total) -> updateProgress(done, total));
                boolean cancelled = !apply(repository, result.friendships(), runner::cancelRequested);
                return new LoadedFriendships(result, cancelled);
            }
        };
        runner.run(task, "Загрузка дружб…", "Ошибка загрузки дружб", loaded -> {
            if (loaded.cancelled()) {
                workspace.refresh(Highlight.caption("Загрузка дружб отменена, добавленное убрано"));
                return;
            }
            workspace.refresh(Highlight.caption("Добавлено дружб: " + loaded.result().friendships().size()));
            GuiAlerts.showCsvErrors(workspace.window(), "дружб", loaded.result().friendships().size(),
                    loaded.result().errors(), false);
        }, () -> workspace.refresh(Highlight.caption("Загрузка дружб отменена")));
    }

    private static boolean apply(ProfileRepository repository, List<Friendship> friendships, BooleanSupplier cancelled) {
        int added = 0;
        for (Friendship friendship : friendships) {
            if (cancelled.getAsBoolean()) {
                for (int i = 0; i < added; i++) {
                    repository.removeFriendship(friendships.get(i).getFirstId(), friendships.get(i).getSecondId());
                }
                return false;
            }
            repository.addFriendship(friendship);
            added++;
        }
        return true;
    }

    void saveFriendships(Path file) {
        List<Friendship> snapshot = workspace.repository().friendships();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                friendshipSaver.save(file, snapshot);
                return null;
            }
        };
        workspace.runner().run(task, "Сохранение дружб…", "Ошибка сохранения дружб", ignored -> {
            workspace.friendsSaved();
            workspace.status("Сохранено дружб: " + snapshot.size());
        }, () -> { });
    }

    private void generate() {
        Integer count = readCount();
        if (count != null) generate(count);
    }

    private Integer readCount() {
        try {
            int count = Integer.parseInt(countField.getText().trim());
            if (count >= 1 && count <= MAX_COUNT) return count;
        } catch (NumberFormatException e) {
            warn("Генерация", "Число записей должно быть целым.");
            return null;
        }
        warn("Генерация", "Число записей должно быть от 1 до " + MAX_COUNT + ".");
        return null;
    }

    private void warn(String title, String message) {
        GuiAlerts.show(workspace.window(), Alert.AlertType.WARNING, title, message);
    }

    void generate(int count) {
        ProfileRepository repository = workspace.repository();
        TaskRunner runner = workspace.runner();
        DataGenerator generator = new DataGenerator(System.nanoTime());
        Task<DataGenerator.Result> task = new Task<>() {
            @Override protected DataGenerator.Result call() {
                return generator.generate(repository, count, (done, total) -> updateProgress(done, total),
                        runner::cancelRequested);
            }
        };
        runner.run(task, "Генерация данных…", "Ошибка генерации", result -> {
            if (result.cancelled()) {
                workspace.refresh(Highlight.caption("Генерация отменена, созданные записи удалены"));
                return;
            }
            workspace.markDirty();
            workspace.markFriendsDirty();
            workspace.refresh(Highlight.caption("Создано профилей: " + result.profiles()
                    + ", дружб: " + result.friendships()));
        }, () -> workspace.refresh(Highlight.caption("Генерация отменена")));
    }

    private void clear() {
        if (!workspace.confirmDiscard()) return;
        workspace.repository().clear();
        workspace.resetDirty();
        workspace.refresh(Highlight.caption("Данные очищены"));
    }

    private void race() {
        new RaceDialog(workspace.window()).showAndWait();
    }

    private Integer readId(TextField field, String label) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            warn("Некорректный ввод", "Поле «" + label + "» должно быть целым числом.");
            return null;
        }
    }

    private record LoadedFriendships(FriendshipLoader.LoadResult result, boolean cancelled) { }
}
