package ru.university.socialnetwork.gui;

import javafx.stage.FileChooser;

final class CsvFiles {
    private CsvFiles() { }

    static FileChooser chooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV UTF-8", "*.csv"));
        return chooser;
    }
}
