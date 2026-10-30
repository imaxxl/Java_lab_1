package ru.university.socialnetwork.gui;

import javafx.stage.Window;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.store.ProfileRepository;

interface Workspace {
    ProfileRepository repository();
    TaskRunner runner();
    Window window();

    void show(Highlight highlight);

    void refresh(Highlight highlight);

    void select(Profile profile);
    void status(String text);
    void markDirty();
    void markFriendsDirty();
    void friendsSaved();
    void resetDirty();

    boolean confirmDiscard();
}
