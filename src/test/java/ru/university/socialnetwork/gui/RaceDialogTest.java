package ru.university.socialnetwork.gui;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.structure.RaceExperiment;

import static org.junit.jupiter.api.Assertions.*;

class RaceDialogTest {
    @BeforeAll static void startFx() throws Exception { FxTestSupport.initialize(); }

    private static String output(RaceDialog dialog) throws Exception {
        return FxTestSupport.run(() -> ((TextArea) dialog.getDialogPane().lookup("#raceOutput")).getText());
    }

    @Test
    void descriptionShowsExpectedFoundAndLostCounts() {
        RaceExperiment.Result result = new RaceExperiment.Result(false, 4, 100, 401, 400, 390, 400, 380, 385, 2, 15);
        String text = RaceDialog.describe(result);
        assertTrue(text.startsWith("Без защиты"));
        assertTrue(text.contains("ожидалось 401, обходом найдено 400, по счётчику 390"));
        assertTrue(text.contains("потеряно вершин 1, рёбер 20, исключений 2"));
        assertTrue(text.contains("данные повреждены"));
        RaceExperiment.Result guarded = new RaceExperiment.Result(true, 4, 100, 401, 401, 401, 400, 400, 400, 0, 9);
        assertTrue(RaceDialog.describe(guarded).contains("данные сошлись"));
    }

    @Test
    void runShowsBothScenarios() throws Exception {
        RaceDialog dialog = FxTestSupport.run(() -> {
            RaceDialog created = new RaceDialog(null);
            created.show();
            ((TextField) created.getDialogPane().lookup("#threadsField")).setText("2");
            ((TextField) created.getDialogPane().lookup("#insertsField")).setText("2000");
            ((Button) created.getDialogPane().lookup("#raceRun")).fire();
            return created;
        });
        try {
            String text = "";
            for (int i = 0; i < 100 && !text.contains("С synchronized"); i++) {
                Thread.sleep(100);
                text = output(dialog);
            }
            assertTrue(text.contains("Без защиты"), text);
            assertTrue(text.contains("С synchronized"), text);
            assertTrue(text.contains("данные сошлись"), text);
        } finally {
            FxTestSupport.run(() -> { dialog.close(); return null; });
        }
    }

    @Test
    void nonsenseNumberIsExplainedInsteadOfStartingTheExperiment() throws Exception {
        RaceDialog dialog = FxTestSupport.run(() -> {
            RaceDialog created = new RaceDialog(null);
            created.show();
            ((TextField) created.getDialogPane().lookup("#threadsField")).setText("0");
            ((Button) created.getDialogPane().lookup("#raceRun")).fire();
            return created;
        });
        try {
            assertTrue(output(dialog).contains("Число потоков"), output(dialog));
        } finally {
            FxTestSupport.run(() -> { dialog.close(); return null; });
        }
    }
}
