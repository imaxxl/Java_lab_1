package ru.university.socialnetwork.gui;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import ru.university.socialnetwork.model.ProfileType;
import ru.university.socialnetwork.store.StructureSnapshot;

final class GraphPainter {
    private static final double MARGIN = 54;
    private static final double BIG_RADIUS = 17;
    private static final double SMALL_RADIUS = 11;
    private static final int SMALL_AFTER = 24;
    private static final int LABELS_UP_TO = 30;
    private static final double TEXT_X = 12;
    private static final double LEGEND_Y = 16;
    private static final double LEGEND_GAP = 20;
    private static final double LEGEND_TEXT_SHIFT = 4;
    private static final double LABEL_HALO = 3;
    private static final double LEGEND_DOT = 6;
    private static final double FOCUS_GAP = 5;
    private static final double EDGE_WIDTH = 1.4;
    private static final double PATH_WIDTH = 4;
    private static final double LABEL_SIZE = 11;
    private static final double SMALL_LABEL_SIZE = 9;
    private static final double ORDER_SIZE = 9;

    private static final Color BACKGROUND = Color.web("#fbfbfb");
    private static final Color TEXT = Color.web("#202124");
    private static final Color EDGE = Color.web("#9aa0a6");
    private static final Color PATH = Color.web("#d93025");
    private static final Color VISITED = Color.web("#fde047");
    private static final Color FOCUS = Color.web("#1e8e3e");
    private static final Color OUTLINE = Color.web("#5f6368");

    private GraphPainter() { }

    static Color colorOf(ProfileType type) {
        return switch (type) {
            case PROFILE -> Color.web("#c8e6c9");
            case COMMUNITY -> Color.web("#ffe0b2");
            case DELETED -> Color.web("#cfd8dc");
        };
    }

    static void paint(GraphicsContext g, double width, double height, StructureSnapshot snapshot, Highlight highlight) {
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, width, height);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);
        drawLegend(g);
        int count = snapshot.shownVertices();
        if (count == 0) {
            g.setFill(TEXT);
            g.setFont(Font.font(14));
            g.fillText("Граф пуст: загрузите файл, добавьте профиль или сгенерируйте данные.", TEXT_X, height / 2);
            return;
        }
        double[] xs = new double[count];
        double[] ys = new double[count];
        layout(xs, ys, width, height);
        drawEdges(g, snapshot, highlight, xs, ys);
        drawVertices(g, snapshot, highlight, xs, ys);
        drawFooter(g, snapshot, highlight, height);
    }

    private static void layout(double[] xs, double[] ys, double width, double height) {
        double radius = Math.max(10, Math.min(width, height) / 2 - MARGIN);
        for (int i = 0; i < xs.length; i++) {
            double angle = 2 * Math.PI * i / xs.length - Math.PI / 2;
            xs[i] = width / 2 + radius * Math.cos(angle);
            ys[i] = height / 2 + radius * Math.sin(angle);
        }
    }

    private static void drawLegend(GraphicsContext g) {
        Font font = Font.font(LABEL_SIZE);
        g.setFont(font);
        double x = TEXT_X;
        for (ProfileType type : ProfileType.values()) {
            x = legendItem(g, x, colorOf(type), type.toString().toLowerCase(), font);
        }
        x = legendItem(g, x, VISITED, "посещён", font);
        g.setStroke(PATH);
        g.setLineWidth(PATH_WIDTH);
        g.strokeLine(x, LEGEND_Y, x + 2 * LEGEND_DOT, LEGEND_Y);
        g.setLineWidth(1);
        g.setFill(TEXT);
        g.fillText("путь", x + 2 * LEGEND_DOT + LEGEND_TEXT_SHIFT, LEGEND_Y);
    }

    private static double legendItem(GraphicsContext g, double x, Color color, String label, Font font) {
        g.setFill(color);
        g.fillOval(x, LEGEND_Y - LEGEND_DOT, 2 * LEGEND_DOT, 2 * LEGEND_DOT);
        g.setStroke(OUTLINE);
        g.strokeOval(x, LEGEND_Y - LEGEND_DOT, 2 * LEGEND_DOT, 2 * LEGEND_DOT);
        g.setFill(TEXT);
        double textX = x + 2 * LEGEND_DOT + LEGEND_TEXT_SHIFT;
        g.fillText(label, textX, LEGEND_Y);
        Text measure = new Text(label);
        measure.setFont(font);
        return textX + measure.getLayoutBounds().getWidth() + LEGEND_GAP;
    }

    private static void drawEdges(GraphicsContext g, StructureSnapshot snapshot, Highlight highlight,
                                  double[] xs, double[] ys) {
        boolean labels = snapshot.shownEdges() <= LABELS_UP_TO;
        g.setFont(Font.font(SMALL_LABEL_SIZE));
        g.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < snapshot.shownEdges(); i++) {
            int from = snapshot.indexOf(snapshot.edgeFrom(i));
            int to = snapshot.indexOf(snapshot.edgeTo(i));
            boolean onPath = highlight.pathUses(snapshot.edgeFrom(i), snapshot.edgeTo(i));
            g.setStroke(onPath ? PATH : EDGE);
            g.setLineWidth(onPath ? PATH_WIDTH : EDGE_WIDTH);
            g.strokeLine(xs[from], ys[from], xs[to], ys[to]);
            if (labels) drawStrength(g, String.valueOf(snapshot.edgeStrength(i)),
                    (xs[from] + xs[to]) / 2, (ys[from] + ys[to]) / 2);
        }
        g.setLineWidth(1);
    }

    private static void drawStrength(GraphicsContext g, String text, double x, double y) {
        g.setStroke(BACKGROUND);
        g.setLineWidth(LABEL_HALO);
        g.strokeText(text, x, y);
        g.setFill(TEXT);
        g.fillText(text, x, y);
        g.setLineWidth(1);
    }

    private static void drawVertices(GraphicsContext g, StructureSnapshot snapshot, Highlight highlight,
                                     double[] xs, double[] ys) {
        double radius = snapshot.shownVertices() > SMALL_AFTER ? SMALL_RADIUS : BIG_RADIUS;
        g.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < snapshot.shownVertices(); i++) {
            int id = snapshot.id(i);
            boolean visited = highlight.visitOrder(id) > 0;
            g.setFill(visited ? VISITED : colorOf(snapshot.type(i)));
            g.fillOval(xs[i] - radius, ys[i] - radius, 2 * radius, 2 * radius);
            boolean onPath = highlight.onPath(id);
            g.setStroke(onPath ? PATH : OUTLINE);
            g.setLineWidth(onPath ? 3 : 1);
            g.strokeOval(xs[i] - radius, ys[i] - radius, 2 * radius, 2 * radius);
            if (id == highlight.focus()) {
                g.setStroke(FOCUS);
                g.setLineWidth(3);
                double ring = radius + FOCUS_GAP;
                g.strokeOval(xs[i] - ring, ys[i] - ring, 2 * ring, 2 * ring);
            }
            g.setLineWidth(1);
            g.setFill(TEXT);
            g.setFont(Font.font(radius == BIG_RADIUS ? LABEL_SIZE : SMALL_LABEL_SIZE));
            g.fillText(String.valueOf(id), xs[i], ys[i]);
            if (visited && snapshot.shownVertices() <= LABELS_UP_TO) {
                g.setFont(Font.font(ORDER_SIZE));
                g.fillText("#" + highlight.visitOrder(id), xs[i] + radius, ys[i] - radius);
            }
        }
    }

    private static void drawFooter(GraphicsContext g, StructureSnapshot snapshot, Highlight highlight, double height) {
        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(TEXT);
        g.setFont(Font.font(LABEL_SIZE + 1));
        String shown = snapshot.vertexTotal() > snapshot.shownVertices()
                ? "Показаны первые " + snapshot.shownVertices() + " из " + snapshot.vertexTotal() + " вершин. " : "";
        g.fillText(shown + "Вершин: " + snapshot.vertexTotal() + ", связей: " + snapshot.edgeTotal(),
                TEXT_X, height - 34);
        g.fillText(highlight.caption(), TEXT_X, height - 14);
    }
}
