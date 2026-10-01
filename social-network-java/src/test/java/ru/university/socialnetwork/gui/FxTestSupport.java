package ru.university.socialnetwork.gui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

final class FxTestSupport {
    private static boolean started;
    private FxTestSupport() { }

    static synchronized void initialize() throws Exception {
        if (started) return;
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        if (!ready.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("JavaFX startup timed out");
        started = true;
    }

    static <T> T run(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    static void dismissDialogs(DialogPane except) {
        for (Window window : List.copyOf(Window.getWindows())) {
            if (window.isShowing() && window.getScene().getRoot() instanceof DialogPane pane && pane != except) {
                var type = pane.getButtonTypes().stream()
                        .filter(t -> t.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE
                                || t.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst().orElse(pane.getButtonTypes().getFirst());
                ((Button) pane.lookupButton(type)).fire();
            }
        }
    }
}
