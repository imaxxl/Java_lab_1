package ru.university.socialnetwork.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.store.DataGenerator;
import ru.university.socialnetwork.store.ProfileRepository;
import ru.university.socialnetwork.store.StructureSnapshot;
import ru.university.socialnetwork.structure.Trace;

import static org.junit.jupiter.api.Assertions.*;

class StructurePaintingTest {
    private static final double WIDTH = 700;
    private static final double HEIGHT = 480;
    private static final Color YELLOW = Color.web("#fde047");
    private static final Color RED = Color.web("#d93025");
    private static final Color GREEN_RING = Color.web("#1e8e3e");
    private static final Color GREEN_BOX = Color.web("#86efac");
    private static final int LEGEND_PIXELS = 150;

    @BeforeAll static void startFx() throws Exception { FxTestSupport.initialize(); }

    private static ProfileRepository ring(int size) {
        ProfileRepository repository = new ProfileRepository();
        for (int id = 1; id <= size; id++) repository.add(new EditableProfile(id, "Имя" + id, "Город", 2000));
        for (int id = 1; id < size; id++) repository.addFriendship(new Friendship(id, id + 1, id));
        return repository;
    }

    private static int count(Image image, Color target) {
        PixelReader reader = image.getPixelReader();
        int hits = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = reader.getColor(x, y);
                if (Math.abs(color.getRed() - target.getRed()) < 0.03
                        && Math.abs(color.getGreen() - target.getGreen()) < 0.03
                        && Math.abs(color.getBlue() - target.getBlue()) < 0.03) hits++;
            }
        }
        return hits;
    }

    private interface Painter {
        void paint(Canvas canvas, StructureSnapshot snapshot, Highlight highlight);
    }

    private static Image draw(ProfileRepository repository, Highlight highlight, Painter painter) throws Exception {
        return FxTestSupport.run(() -> {
            Canvas canvas = new Canvas(WIDTH, HEIGHT);
            painter.paint(canvas, repository.structure(StructureView.MAX_VERTICES, StructureView.MAX_BUCKETS,
                    StructureView.MAX_CHAIN), highlight);
            return canvas.snapshot(null, null);
        });
    }

    private static final Painter GRAPH = (canvas, snapshot, highlight) ->
            GraphPainter.paint(canvas.getGraphicsContext2D(), WIDTH, HEIGHT, snapshot, highlight);
    private static final Painter HASH = (canvas, snapshot, highlight) ->
            HashPainter.paint(canvas.getGraphicsContext2D(), WIDTH, HEIGHT, snapshot, highlight);

    @Test
    void graphWithoutHighlightHasNeitherVisitedNorPathColours() throws Exception {
        Image image = draw(ring(6), Highlight.NONE, GRAPH);
        assertTrue(count(image, RED) < LEGEND_PIXELS, "красным может быть только значок «путь» в легенде");
        assertTrue(count(image, YELLOW) < LEGEND_PIXELS, "жёлтым может быть только значок «посещён» в легенде");
    }

    @Test
    void visitedVerticesAreFilledYellow() throws Exception {
        Trace trace = new Trace();
        for (int id = 1; id <= 4; id++) trace.visit(id);
        Image plain = draw(ring(6), Highlight.NONE, GRAPH);
        Image marked = draw(ring(6), Highlight.of(trace, null, Highlight.NO_FOCUS, ""), GRAPH);
        assertTrue(count(marked, YELLOW) > count(plain, YELLOW) + 800);
    }

    @Test
    void pathEdgesAreDrawnRed() throws Exception {
        ProfileRepository repository = ring(6);
        Image image = draw(repository,
                Highlight.of(null, repository.shortestChain(1, 4, null), Highlight.NO_FOCUS, ""), GRAPH);
        assertTrue(count(image, RED) > 300);
    }

    @Test
    void focusedVertexGetsGreenRing() throws Exception {
        Image plain = draw(ring(6), Highlight.NONE, GRAPH);
        Image marked = draw(ring(6), new Highlight(new int[0], new int[0], 3, ""), GRAPH);
        assertTrue(count(marked, GREEN_RING) > count(plain, GREEN_RING) + 100);
    }

    @Test
    void emptyGraphIsDrawnWithoutVerticesOrFailures() throws Exception {
        Image image = draw(new ProfileRepository(), Highlight.NONE, GRAPH);
        assertEquals(WIDTH, image.getWidth());
        assertTrue(count(image, RED) < LEGEND_PIXELS);
    }

    @Test
    void hashTableMarksFocusAndVisitedBoxes() throws Exception {
        ProfileRepository repository = ring(8);
        Trace trace = new Trace();
        repository.findById(3, trace);
        Image image = draw(repository, Highlight.of(trace, null, 3, ""), HASH);
        assertTrue(count(image, GREEN_BOX) > 200);
    }

    @Test
    void hashTableShowsMoreBucketsAfterGrowth() throws Exception {
        ProfileRepository repository = ring(40);
        StructureSnapshot snapshot = repository.structure(StructureView.MAX_VERTICES, StructureView.MAX_BUCKETS,
                StructureView.MAX_CHAIN);
        assertTrue(snapshot.bucketTotal() >= 64);
        assertEquals(StructureView.MAX_BUCKETS, snapshot.shownBuckets());
        assertNotNull(draw(repository, Highlight.NONE, HASH));
    }

    @Test
    void largeGraphShowsOnlyFirstVerticesButCountsAllOfThem() throws Exception {
        ProfileRepository repository = new ProfileRepository();
        new DataGenerator(1).generate(repository, 300, null, () -> false);
        StructureSnapshot snapshot = repository.structure(StructureView.MAX_VERTICES, StructureView.MAX_BUCKETS,
                StructureView.MAX_CHAIN);
        assertEquals(StructureView.MAX_VERTICES, snapshot.shownVertices());
        assertEquals(300, snapshot.vertexTotal());
        assertNotNull(draw(repository, Highlight.NONE, GRAPH));
    }

    @Test
    void viewRemembersLastSnapshotAndHighlight() throws Exception {
        FxTestSupport.run(() -> {
            StructureView view = new StructureView();
            Highlight highlight = Highlight.caption("после операции");
            view.show(ring(5), highlight);
            assertEquals(5, view.snapshot().vertexTotal());
            assertSame(highlight, view.highlight());
            view.show(ring(2), Highlight.NONE);
            assertEquals(2, view.snapshot().vertexTotal());
            return null;
        });
    }

    @Test
    void highlightAnswersQuestionsAboutVisitedPathAndEdges() {
        Trace trace = new Trace();
        trace.visit(5);
        trace.visit(7);
        Highlight highlight = Highlight.of(trace, ring(8).shortestChain(5, 7, null), 7, "подпись");
        assertEquals(1, highlight.visitOrder(5));
        assertEquals(2, highlight.visitOrder(7));
        assertEquals(0, highlight.visitOrder(1));
        assertTrue(highlight.onPath(6));
        assertTrue(highlight.pathUses(6, 5));
        assertFalse(highlight.pathUses(5, 7));
        assertFalse(Highlight.NONE.onPath(1));
    }
}
