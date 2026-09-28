package ru.university.socialnetwork.store;

import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.Editable;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.model.ProfileType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Хранилище лабы 1; в фоновые задачи передаётся только неизменяемый snapshot. */
public final class ProfileRepository {
    private final Map<Integer, Profile> profiles = new LinkedHashMap<>();

    public int size() { return profiles.size(); }
    public Profile findById(int id) { return profiles.get(id); }
    public List<Profile> snapshot() { return List.copyOf(profiles.values()); }

    public void validateForAdd(Profile profile) {
        requireEditable(profile);
        requireValid(profile);
        if (profiles.containsKey(profile.getId())) {
            throw new IllegalArgumentException("ID " + profile.getId() + " уже занят");
        }
        requireAdministrator(profile, profiles);
    }

    public void add(Profile profile) {
        validateForAdd(profile);
        profiles.put(profile.getId(), profile);
    }

    public void validateForReplace(int originalId, Profile replacement) {
        Profile original = profiles.get(originalId);
        if (original == null) throw new IllegalArgumentException("Профиль не найден");
        requireEditable(original);
        requireEditable(replacement);
        requireValid(replacement);
        if (replacement.getId() != originalId) {
            throw new IllegalArgumentException("ID существующего профиля менять нельзя");
        }
        if (ProfileType.of(original) != ProfileType.of(replacement)) {
            throw new IllegalArgumentException("Тип существующего профиля менять нельзя");
        }
        requireAdministrator(replacement, profiles);
    }

    public void replace(int originalId, Profile replacement) {
        validateForReplace(originalId, replacement);
        profiles.put(originalId, replacement);
    }

    /** Замена из файла проверяется целиком до изменения текущего состояния. */
    public void replaceAll(List<Profile> loaded) {
        Objects.requireNonNull(loaded, "Список профилей не задан");
        Map<Integer, Profile> checked = new LinkedHashMap<>();
        for (Profile profile : loaded) {
            requireValid(profile);
            if (checked.putIfAbsent(profile.getId(), profile) != null) {
                throw new IllegalArgumentException("Дублирующийся ID: " + profile.getId());
            }
        }
        for (Profile profile : checked.values()) requireAdministrator(profile, checked);
        profiles.clear();
        profiles.putAll(checked);
    }

    public static boolean canBeAdministrator(Profile profile) {
        return profile != null && !(profile instanceof DeletedProfile)
                && !(profile instanceof Community);
    }

    private static void requireAdministrator(Profile profile, Map<Integer, Profile> available) {
        if (profile instanceof Community community
                && !canBeAdministrator(available.get(community.getAdministratorId()))) {
            throw new IllegalArgumentException("Администратор " + community.getAdministratorId()
                    + " должен быть существующим обычным, не удалённым профилем");
        }
    }

    private static void requireEditable(Profile profile) {
        if (!(profile instanceof Editable)) {
            throw new IllegalArgumentException("Этот тип нельзя создавать или редактировать через GUI");
        }
    }

    private static void requireValid(Profile profile) {
        if (profile == null) throw new IllegalArgumentException("Профиль не задан");
        List<String> errors = profile.validate();
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("\n", errors));
    }
}
