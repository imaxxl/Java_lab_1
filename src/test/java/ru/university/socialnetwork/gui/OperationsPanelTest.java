package ru.university.socialnetwork.gui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.store.ProfileRepository;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OperationsPanelTest {
    @TempDir Path directory;
    @BeforeAll static void startFx() throws Exception { FxTestSupport.initialize(); }

    private static final class StubWorkspace implements Workspace {
        final ProfileRepository repository = new ProfileRepository();
        final BlockingQueue<String> events = new LinkedBlockingQueue<>();
        final TaskRunner runner = new TaskRunner(() -> null, text -> { }, () -> "Профилей: " + repository.size());
        volatile Highlight shown = Highlight.NONE;
        volatile Profile selected;
        volatile String lastStatus = "";
        volatile boolean allowDiscard = true;
        volatile boolean dirty;
        volatile boolean friendsDirty;
        volatile boolean friendsSaved;
        volatile boolean reset;

        @Override public ProfileRepository repository() { return repository; }
        @Override public TaskRunner runner() { return runner; }
        @Override public Window window() { return null; }
        @Override public void show(Highlight highlight) { shown = highlight; events.add("show"); }
        @Override public void refresh(Highlight highlight) { shown = highlight; events.add("refresh"); }
        @Override public void select(Profile profile) { selected = profile; }
        @Override public void status(String text) { lastStatus = text; events.add("status"); }
        @Override public void markDirty() { dirty = true; }
        @Override public void markFriendsDirty() { friendsDirty = true; }
        @Override public void friendsSaved() { friendsSaved = true; }
        @Override public void resetDirty() { reset = true; }
        @Override public boolean confirmDiscard() { return allowDiscard; }

        void await(String event) throws InterruptedException {
            while (true) {
                String next = events.poll(20, TimeUnit.SECONDS);
                assertNotNull(next, "не дождались события " + event);
                if (next.equals(event)) return;
            }
        }
    }

    private static StubWorkspace workspaceWithFriends() {
        StubWorkspace workspace = new StubWorkspace();
        for (int id = 1; id <= 5; id++) workspace.repository.add(new EditableProfile(id, "Имя" + id, "Город", 2000));
        workspace.repository.addFriendship(new Friendship(1, 2, 5));
        workspace.repository.addFriendship(new Friendship(2, 3, 7));
        workspace.repository.addFriendship(new Friendship(3, 4, 2));
        workspace.events.clear();
        return workspace;
    }

    private static OperationsPanel panelFor(StubWorkspace workspace) throws Exception {
        return FxTestSupport.run(() -> new OperationsPanel(workspace));
    }

    private static void type(OperationsPanel panel, String id, String text) {
        ((TextField) panel.lookup("#" + id)).setText(text);
    }

    private static Button button(OperationsPanel panel, String id) { return (Button) panel.lookup("#" + id); }

    @Test
    void findHighlightsFoundProfileAndCountsComparedNodes() throws Exception {
        StubWorkspace workspace = workspaceWithFriends();
        OperationsPanel panel = panelFor(workspace);
        FxTestSupport.run(() -> { type(panel, "findField", "2"); panel.find(); return null; });
        assertEquals(2, workspace.shown.focus());
        assertTrue(workspace.shown.caption().contains("Найден профиль 2"), workspace.shown.caption());
        assertEquals(2, workspace.selected.getId());
        assertEquals(workspace.shown.caption(), workspace.lastStatus);
        assertTrue(workspace.shown.visitOrder(2) > 0);
    }

    @Test
    void findReportsMissingProfileWithoutSelection() throws Exception {
        StubWorkspace workspace = workspaceWithFriends();
        OperationsPanel panel = panelFor(workspace);
        FxTestSupport.run(() -> { type(panel, "findField", "99"); panel.find(); return null; });
        assertEquals(Highlight.NO_FOCUS, workspace.shown.focus());
        assertTrue(workspace.shown.caption().contains("не найден"));
        assertNull(workspace.selected);
    }

    @Test
    void findRejectsTextInsteadOfNumber() throws Exception {
        StubWorkspace workspace = workspaceWithFriends();
        OperationsPanel panel = panelFor(workspace);
        FxTestSupport.run(() -> {
            type(panel, "findField", "abc");
            Platform.runLater(() -> FxTestSupport.dismissDialogs(null));
            panel.find();
            return null;
        });
        assertSame(Highlight.NONE, workspace.shown);
    }

    @Test
    void chainHighlightsPathAndVisitedVertices() throws Exception {
        StubWorkspace workspace = workspaceWithFriends();
        OperationsPanel panel = panelFor(workspace);
        FxTestSupport.run(() -> { type(panel, "fromField", "1"); type(panel, "toField", "4"); panel.chain(); return null; });
        assertArrayEquals(new int[]{1, 2, 3, 4}, workspace.shown.path());
        assertEquals(1, workspace.shown.visited()[0]);
        assertTrue(workspace.shown.caption().contains("1 → 2 → 3 → 4"), workspace.shown.caption());
    }

    @Test
    void chainReportsWhenProfilesAreNotConnected() throws Exception {
        StubWorkspace workspace = workspaceWithFriends();
        OperationsPanel panel = panelFor(workspace);
        FxTestSupport.run(() -> { type(panel, "fromField", "1"); type(panel, "toField", "5"); panel.chain(); return null; });
        assertEquals(0, workspace.shown.path().length);
        assertTrue(workspace.shown.caption().contains("нет"), workspace.shown.caption());
        assertTrue(workspace.shown.visited().length >= 4);
    }

    @Test
    void chainRejectsUnknownProfile() throws Exception {
        StubWorkspace workspace = workspaceWithFriends();
        OperationsPanel panel = panelFor(workspace);
        FxTestSupport.run(() -> {
            type(panel, "fromField", "1");
            type(panel, "toField", "99");
            Platform.runLater(() -> FxTestSupport.dismissDialogs(null));
            panel.chain();
            return null;
        });
        assertSame(Highlight.NONE, workspace.shown);
    }

    @Test
    void generationRunsInBackgroundAndMarksDataChanged() throws Exception {
        StubWorkspace workspace = new StubWorkspace();
        OperationsPanel panel = panelFor(workspace);
        FxTestSupport.run(() -> { panel.generate(300); return null; });
        workspace.await("refresh");
        assertEquals(300, workspace.repository.size());
        assertTrue(workspace.repository.friendshipCount() > 300);
        assertTrue(workspace.dirty && workspace.friendsDirty);
        assertTrue(workspace.shown.caption().contains("Создано профилей: 300"), workspace.shown.caption());
    }

    @Test
    void buttonsAreBlockedWhileGeneratingAndCancelRollsEverythingBack() throws Exception {
        StubWorkspace workspace = new StubWorkspace();
        OperationsPanel panel = panelFor(workspace);
        FxTestSupport.run(() -> {
            panel.generate(500_000);
            assertTrue(button(panel, "findButton").isDisabled());
            assertTrue(button(panel, "generateButton").isDisabled());
            assertThrows(IllegalStateException.class, () -> panel.generate(10));
            workspace.runner.cancel();
            return null;
        });
        workspace.await("refresh");
        assertEquals(0, workspace.repository.size());
        assertEquals(0, workspace.repository.friendshipCount());
        assertTrue(workspace.shown.caption().contains("отменена"), workspace.shown.caption());
        assertFalse(workspace.dirty);
        assertFalse(FxTestSupport.run(() -> button(panel, "findButton").isDisabled()));
    }

    @Test
    void invalidCountIsRejectedWithoutStartingAnything() throws Exception {
        StubWorkspace workspace = new StubWorkspace();
        OperationsPanel panel = panelFor(workspace);
        for (String bad : new String[]{"0", "-5", "abc", "2000000"}) {
            FxTestSupport.run(() -> {
                type(panel, "countField", bad);
                Platform.runLater(() -> FxTestSupport.dismissDialogs(null));
                button(panel, "generateButton").fire();
                return null;
            });
        }
        assertEquals(0, workspace.repository.size());
        assertFalse(workspace.runner.busyProperty().get());
    }

    @Test
    void friendshipsAreSavedAndLoadedBackInBackground() throws Exception {
        StubWorkspace workspace = workspaceWithFriends();
        OperationsPanel panel = panelFor(workspace);
        Path file = directory.resolve("friendships.csv");
        FxTestSupport.run(() -> { panel.saveFriendships(file); return null; });
        workspace.await("status");
        assertTrue(workspace.friendsSaved);
        assertEquals("Сохранено дружб: 3", workspace.lastStatus);
        workspace.repository.removeFriendship(1, 2);
        workspace.repository.removeFriendship(2, 3);
        workspace.repository.removeFriendship(3, 4);
        assertEquals(0, workspace.repository.friendshipCount());
        FxTestSupport.run(() -> { panel.loadFriendships(file); return null; });
        workspace.await("refresh");
        assertEquals(3, workspace.repository.friendshipCount());
        assertEquals(7, workspace.repository.friendshipStrength(3, 2));
        assertTrue(workspace.shown.caption().contains("Добавлено дружб: 3"));
    }

    @Test
    void clearAsksForConfirmationAndKeepsDataWhenRefused() throws Exception {
        StubWorkspace workspace = workspaceWithFriends();
        OperationsPanel panel = panelFor(workspace);
        workspace.allowDiscard = false;
        FxTestSupport.run(() -> { button(panel, "clearButton").fire(); return null; });
        assertEquals(5, workspace.repository.size());
        assertFalse(workspace.reset);
        workspace.allowDiscard = true;
        FxTestSupport.run(() -> { button(panel, "clearButton").fire(); return null; });
        assertEquals(0, workspace.repository.size());
        assertTrue(workspace.reset);
    }
}
