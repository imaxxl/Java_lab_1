package ru.university.socialnetwork.store;

import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProfileRepositoryTest {
    private static EditableProfile human(int id) { return new EditableProfile(id, "Анна", "Город", 2000); }

    @Test
    void duplicateAddDoesNotReplaceOriginal() {
        ProfileRepository repository = new ProfileRepository();
        Profile original = human(1);
        repository.add(original);
        assertThrows(IllegalArgumentException.class, () -> repository.add(human(1)));
        assertSame(original, repository.findById(1));
        assertEquals(1, repository.size());
    }

    @Test
    void duplicateAcrossDifferentTypesIsRejected() {
        ProfileRepository repository = new ProfileRepository();
        repository.add(human(1));
        repository.add(human(2));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> repository.add(new Community(1, "Клуб", "Город", 2020, "Java", 2)));
        assertTrue(error.getMessage().contains("уже занят"));
    }

    @Test
    void archiveCanOnlyEnterThroughBatchLoad() {
        ProfileRepository repository = new ProfileRepository();
        Profile deleted = new DeletedProfile(1, "Архив", "Город", 2000, "Причина");
        assertThrows(IllegalArgumentException.class, () -> repository.add(deleted));
        repository.replaceAll(List.of(deleted));
        assertSame(deleted, repository.findById(1));
    }

    @Test
    void archiveCannotBeReplacedByEditing() {
        ProfileRepository repository = new ProfileRepository();
        repository.replaceAll(List.of(new DeletedProfile(1, "Архив", "Город", 2000, "Причина")));
        assertThrows(IllegalArgumentException.class, () -> repository.replace(1, human(1)));
    }

    @Test
    void ordinaryProfileCanBeReplacedWithoutMutatingOldObject() {
        ProfileRepository repository = new ProfileRepository();
        Profile original = human(1);
        repository.add(original);
        repository.replace(1, new EditableProfile(1, "Новое имя", "Город", 2000));
        assertEquals("Новое имя", repository.findById(1).getName());
        assertEquals("Анна", original.getName());
    }

    @Test
    void editingCannotChangeIdOrType() {
        ProfileRepository repository = new ProfileRepository();
        repository.add(human(1));
        assertThrows(IllegalArgumentException.class, () -> repository.replace(1, human(2)));
        assertThrows(IllegalArgumentException.class,
                () -> repository.replace(1, new Community(1, "Клуб", "Город", 2020, "Java", 1)));
    }

    @Test
    void administratorMustExistAndBeActiveHuman() {
        ProfileRepository repository = new ProfileRepository();
        repository.replaceAll(List.of(new DeletedProfile(1, "Архив", "Город", 2000, "")));
        assertThrows(IllegalArgumentException.class,
                () -> repository.add(new Community(2, "Клуб", "Город", 2020, "Java", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> repository.add(new Community(2, "Клуб", "Город", 2020, "Java", 99)));
    }

    @Test
    void invalidBatchDoesNotEraseExistingData() {
        ProfileRepository repository = new ProfileRepository();
        Profile original = human(1);
        repository.add(original);
        assertThrows(IllegalArgumentException.class,
                () -> repository.replaceAll(List.of(human(2), human(2))));
        assertEquals(List.of(original), repository.snapshot());
    }

    @Test
    void communityCannotBeAdministrator() {
        ProfileRepository repository = new ProfileRepository();
        repository.add(human(1));
        repository.add(new Community(2, "Клуб", "Город", 2020, "Java", 1));
        assertThrows(IllegalArgumentException.class,
                () -> repository.add(new Community(3, "Клуб 2", "Город", 2020, "Java", 2)));
    }

    @Test
    void snapshotIsUnmodifiable() {
        ProfileRepository repository = new ProfileRepository();
        repository.add(human(1));
        assertThrows(UnsupportedOperationException.class, () -> repository.snapshot().clear());
    }
}
