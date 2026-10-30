package ru.university.socialnetwork.gui;

import javafx.stage.Window;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.store.ProfileRepository;

/** Что панель операций может попросить у главного окна. */
interface Workspace {
    ProfileRepository repository();
    TaskRunner runner();
    Window window();

    /** Только перерисовать панель структуры. */
    void show(Highlight highlight);

    /** Данные изменились: обновить таблицу, статус и панель. */
    void refresh(Highlight highlight);

    void select(Profile profile);
    void status(String text);
    void markDirty();
    void markFriendsDirty();
    void friendsSaved();
    void resetDirty();

    /** true — можно продолжать: несохранённых изменений нет или пользователь согласился их потерять. */
    boolean confirmDiscard();
}
