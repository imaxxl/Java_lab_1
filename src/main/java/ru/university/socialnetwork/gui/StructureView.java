package ru.university.socialnetwork.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import ru.university.socialnetwork.store.ProfileRepository;
import ru.university.socialnetwork.store.StructureSnapshot;

/** Панель с внутренним устройством структуры: граф и хеш-индекс. После каждой операции перерисовывается целиком. */
final class StructureView extends TabPane {
    static final int MAX_VERTICES = 40;
    static final int MAX_BUCKETS = 32;
    static final int MAX_CHAIN = 8;

    private final Canvas graphCanvas = new Canvas();
    private final Canvas hashCanvas = new Canvas();
    private StructureSnapshot snapshot;
    private Highlight highlight = Highlight.NONE;

    StructureView() {
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        getTabs().addAll(tab("Граф", graphCanvas), tab("Хеш-таблица", hashCanvas));
    }

    private Tab tab(String title, Canvas canvas) {
        Pane pane = new Pane(canvas);
        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());
        canvas.widthProperty().addListener((observable, before, after) -> redraw());
        canvas.heightProperty().addListener((observable, before, after) -> redraw());
        return new Tab(title, pane);
    }

    void show(ProfileRepository repository, Highlight next) {
        snapshot = repository.structure(MAX_VERTICES, MAX_BUCKETS, MAX_CHAIN);
        highlight = next;
        redraw();
    }

    private void redraw() {
        if (snapshot == null) return;
        GraphPainter.paint(graphCanvas.getGraphicsContext2D(), graphCanvas.getWidth(), graphCanvas.getHeight(),
                snapshot, highlight);
        HashPainter.paint(hashCanvas.getGraphicsContext2D(), hashCanvas.getWidth(), hashCanvas.getHeight(),
                snapshot, highlight);
    }

    Canvas graphCanvas() { return graphCanvas; }

    Canvas hashCanvas() { return hashCanvas; }

    StructureSnapshot snapshot() { return snapshot; }

    Highlight highlight() { return highlight; }
}
