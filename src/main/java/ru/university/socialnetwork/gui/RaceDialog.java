package ru.university.socialnetwork.gui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.university.socialnetwork.structure.RaceExperiment;

final class RaceDialog extends Dialog<Void> {
    private static final int DEFAULT_THREADS = 4;
    private static final int DEFAULT_INSERTS = 20_000;
    private static final int MAX_THREADS = 16;
    private static final int MAX_INSERTS = 200_000;
    private static final double GAP = 10;
    private static final int OUTPUT_ROWS = 14;
    private static final int OUTPUT_COLUMNS = 72;

    private final TextField threadsField = new TextField(String.valueOf(DEFAULT_THREADS));
    private final TextField insertsField = new TextField(String.valueOf(DEFAULT_INSERTS));
    private final TextArea output = new TextArea();
    private final Button run = new Button("Запустить");

    RaceDialog(Window owner) {
        initOwner(owner);
        setTitle("Гонка потоков");
        threadsField.setId("threadsField");
        insertsField.setId("insertsField");
        output.setId("raceOutput");
        output.setEditable(false);
        output.setPrefRowCount(OUTPUT_ROWS);
        output.setPrefColumnCount(OUTPUT_COLUMNS);
        output.setStyle("-fx-font-family: 'monospace';");
        output.setText("Потоки одновременно добавляют вершины, а потом рёбра к общей вершине.\n"
                + "Без защиты часть записей и счётчиков теряется, под synchronized всё сходится.");
        run.setId("raceRun");
        run.setOnAction(event -> start());
        HBox controls = new HBox(GAP, new Label("Потоков:"), threadsField, new Label("вставок на поток:"),
                insertsField, run);
        VBox content = new VBox(GAP, controls, output);
        content.setPadding(new Insets(GAP));
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE));
        setResizable(true);
    }

    private void start() {
        int threads;
        int inserts;
        try {
            threads = bounded(threadsField, "потоков", MAX_THREADS);
            inserts = bounded(insertsField, "вставок", MAX_INSERTS);
        } catch (IllegalArgumentException e) {
            output.setText(e.getMessage());
            return;
        }
        run.setDisable(true);
        output.setText("Выполняется…");
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return describe(RaceExperiment.run(false, threads, inserts)) + "\n"
                        + describe(RaceExperiment.run(true, threads, inserts));
            }
        };
        task.setOnSucceeded(event -> finish(task.getValue()));
        task.setOnFailed(event -> finish("Опыт не удался: " + task.getException().getMessage()));
        Thread worker = new Thread(task, "race-experiment");
        worker.setDaemon(true);
        worker.start();
    }

    private void finish(String text) {
        output.setText(text);
        run.setDisable(false);
    }

    private static int bounded(TextField field, String what, int max) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value >= 1 && value <= max) return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Число " + what + " должно быть целым от 1 до " + max, e);
        }
        throw new IllegalArgumentException("Число " + what + " должно быть от 1 до " + max);
    }

    static String describe(RaceExperiment.Result r) {
        return (r.guarded() ? "С synchronized" : "Без защиты") + " (потоков " + r.threads()
                + ", вставок на поток " + r.perThread() + ")\n"
                + "  вершин: ожидалось " + r.expectedVertices() + ", обходом найдено " + r.countedVertices()
                + ", по счётчику " + r.vertexCounter() + "\n"
                + "  рёбер у общей вершины: ожидалось " + r.expectedEdges() + ", найдено " + r.countedEdges()
                + ", по счётчику " + r.edgeCounter() + "\n"
                + "  потеряно вершин " + r.lostVertices() + ", рёбер " + r.lostEdges()
                + ", исключений " + r.failures() + ", время " + r.millis() + " мс\n"
                + "  " + (r.consistent() ? "данные сошлись" : "данные повреждены") + "\n";
    }
}
