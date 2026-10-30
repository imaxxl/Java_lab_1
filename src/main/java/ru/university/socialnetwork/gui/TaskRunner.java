package ru.university.socialnetwork.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Window;

import java.io.InterruptedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class TaskRunner {
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final Supplier<Window> owner;
    private final Consumer<String> status;
    private final Supplier<String> idleText;
    private volatile Thread worker;
    private volatile boolean cancelRequested;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "csv-io");
        thread.setDaemon(true);
        worker = thread;
        return thread;
    });
    private Task<?> current;

    TaskRunner(Supplier<Window> owner, Consumer<String> status, Supplier<String> idleText) {
        this.owner = owner;
        this.status = status;
        this.idleText = idleText;
    }

    BooleanProperty busyProperty() { return busy; }

    ReadOnlyDoubleProperty progressProperty() { return progress; }

    boolean cancelRequested() { return cancelRequested; }

    <T> void run(Task<T> task, String message, String failureTitle, Consumer<T> onSuccess, Runnable onCancelled) {
        if (busy.get()) throw new IllegalStateException("Другая операция ещё выполняется");
        busy.set(true);
        cancelRequested = false;
        current = task;
        progress.bind(task.progressProperty());
        status.accept(message);
        task.setOnSucceeded(event -> {
            finish();
            accept(onSuccess, task.getValue());
        });
        task.setOnFailed(event -> {
            finish();
            Throwable failure = task.getException();
            if (cancelRequested && failure instanceof InterruptedIOException) {
                cancelled(onCancelled);
            } else {
                GuiAlerts.show(owner.get(), Alert.AlertType.ERROR, failureTitle,
                        failure == null ? "Неизвестная ошибка" : failure.getMessage());
            }
        });
        // Исполнитель сбрасывает прерывание перед началом задачи, поэтому отмену, пришедшую раньше, применяем заново.
        executor.execute(() -> {
            if (cancelRequested) Thread.currentThread().interrupt();
            task.run();
        });
    }

    void cancel() {
        Thread thread = worker;
        if (current == null || thread == null) return;
        cancelRequested = true;
        thread.interrupt();
    }

    void shutdown() { executor.shutdownNow(); }

    private void finish() {
        progress.unbind();
        progress.set(0);
        busy.set(false);
        current = null;
        status.accept(idleText.get());
    }

    private void cancelled(Runnable onCancelled) {
        status.accept("Операция отменена. " + idleText.get());
        onCancelled.run();
    }

    private <T> void accept(Consumer<T> onSuccess, T value) {
        try {
            onSuccess.accept(value);
        } catch (IllegalArgumentException e) {
            GuiAlerts.show(owner.get(), Alert.AlertType.ERROR, "Некорректные данные", e.getMessage());
        }
    }
}
