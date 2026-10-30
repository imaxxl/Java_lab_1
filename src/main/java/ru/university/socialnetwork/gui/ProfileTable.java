package ru.university.socialnetwork.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.model.ProfileType;

import java.util.List;

/** Таблица профилей: колонки ID и года числовые, поэтому сортировка идёт по значению, а не по строке. */
final class ProfileTable {
    private static final double TYPE_WIDTH = 160;
    private static final double ID_WIDTH = 48;
    private static final double NAME_WIDTH = 120;
    private static final double CITY_WIDTH = 95;
    private static final double YEAR_WIDTH = 52;
    private static final double EXTRA_WIDTH = 200;

    private ProfileTable() { }

    static TableView<Profile> create(ObservableList<Profile> items) {
        TableView<Profile> table = new TableView<>(items);
        TableColumn<Profile, String> typeColumn = new TableColumn<>("Тип");
        typeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(ProfileType.of(data.getValue()).toString()));
        TableColumn<Profile, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        TableColumn<Profile, String> nameColumn = new TableColumn<>("Имя");
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
        TableColumn<Profile, String> cityColumn = new TableColumn<>("Город");
        cityColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCity()));
        TableColumn<Profile, Integer> yearColumn = new TableColumn<>("Год");
        yearColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getBirthYear()));
        TableColumn<Profile, String> extraColumn = new TableColumn<>("Дополнительно");
        extraColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(extraOf(data.getValue())));
        typeColumn.setPrefWidth(TYPE_WIDTH);
        idColumn.setPrefWidth(ID_WIDTH);
        nameColumn.setPrefWidth(NAME_WIDTH);
        cityColumn.setPrefWidth(CITY_WIDTH);
        yearColumn.setPrefWidth(YEAR_WIDTH);
        extraColumn.setPrefWidth(EXTRA_WIDTH);
        table.getColumns().addAll(List.of(typeColumn, idColumn, nameColumn, cityColumn, yearColumn, extraColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    static String extraOf(Profile profile) {
        if (profile instanceof Community community) {
            return community.getDescription() + ", администратор: " + community.getAdministratorId();
        }
        if (profile instanceof DeletedProfile deleted) return "Причина: " + deleted.getReason();
        return "";
    }
}
