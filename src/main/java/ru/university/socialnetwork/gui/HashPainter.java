package ru.university.socialnetwork.gui;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import ru.university.socialnetwork.store.StructureSnapshot;

/** Рисует внутреннее устройство индекса вершин: массив корзин и цепочки узлов в каждой корзине. */
final class HashPainter {
    private static final int ROWS_PER_COLUMN = 16;
    private static final double MARGIN = 14;
    private static final double TOP = 46;
    private static final double ROW_HEIGHT = 25;
    private static final double LABEL_WIDTH = 34;
    private static final double BOX_WIDTH = 38;
    private static final double BOX_HEIGHT = 19;
    private static final double GAP = 12;
    private static final double FONT_SIZE = 11;

    private static final Color BACKGROUND = Color.web("#fbfbfb");
    private static final Color TEXT = Color.web("#202124");
    private static final Color BOX = Color.web("#ffffff");
    private static final Color OUTLINE = Color.web("#5f6368");
    private static final Color VISITED = Color.web("#fde047");
    private static final Color FOCUS = Color.web("#86efac");

    private HashPainter() { }

    static void paint(GraphicsContext g, double width, double height, StructureSnapshot snapshot, Highlight highlight) {
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, width, height);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font(FONT_SIZE + 1));
        g.setFill(TEXT);
        g.setTextAlign(TextAlignment.LEFT);
        String shown = snapshot.bucketTotal() > snapshot.shownBuckets()
                ? " (показаны первые " + snapshot.shownBuckets() + ")" : "";
        g.fillText("Индекс вершин — хеш-таблица: корзин " + snapshot.bucketTotal() + shown
                + ", записей " + snapshot.vertexTotal(), MARGIN, 18);
        g.fillText(highlight.caption(), MARGIN, height - 14);
        double columnWidth = (width - 2 * MARGIN) / 2;
        for (int bucket = 0; bucket < snapshot.shownBuckets(); bucket++) {
            double x = MARGIN + (bucket / ROWS_PER_COLUMN) * columnWidth;
            double y = TOP + (bucket % ROWS_PER_COLUMN) * ROW_HEIGHT;
            drawBucket(g, snapshot, highlight, bucket, x, y);
        }
    }

    private static void drawBucket(GraphicsContext g, StructureSnapshot snapshot, Highlight highlight,
                                   int bucket, double x, double y) {
        g.setFont(Font.font(FONT_SIZE));
        g.setFill(TEXT);
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText("[" + bucket + "]", x, y);
        g.setTextAlign(TextAlignment.CENTER);
        for (int position = 0; position < snapshot.bucketSize(bucket); position++) {
            double boxX = x + LABEL_WIDTH + position * (BOX_WIDTH + GAP);
            int id = snapshot.bucketKey(bucket, position);
            if (position > 0) {
                g.setStroke(OUTLINE);
                g.strokeLine(boxX - GAP, y, boxX, y);
            }
            boolean focus = id == highlight.focus();
            g.setFill(focus ? FOCUS : highlight.visitOrder(id) > 0 ? VISITED : BOX);
            g.fillRect(boxX, y - BOX_HEIGHT / 2, BOX_WIDTH, BOX_HEIGHT);
            g.setStroke(OUTLINE);
            g.strokeRect(boxX, y - BOX_HEIGHT / 2, BOX_WIDTH, BOX_HEIGHT);
            g.setFill(TEXT);
            g.fillText(String.valueOf(id), boxX + BOX_WIDTH / 2, y);
        }
    }
}
