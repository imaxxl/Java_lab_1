package ru.university.socialnetwork.store;

import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.EditableProfile;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;

import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

public final class DataGenerator {
    private static final String[] NAMES = {"Анна", "Иван", "Мария", "Пётр", "Елена", "Сергей", "Ольга", "Дмитрий",
            "Наталья", "Алексей", "Татьяна", "Андрей", "Юлия", "Максим", "Ирина", "Артём", "Светлана", "Николай",
            "Екатерина", "Владимир", "Алиса", "Егор", "Полина", "Кирилл", "Дарья", "Михаил", "Ксения", "Роман"};
    private static final String[] SURNAMES = {"Иванов", "Петров", "Сидоров", "Смирнов", "Кузнецов", "Попов",
            "Васильев", "Соколов", "Михайлов", "Новиков", "Фёдоров", "Морозов", "Волков", "Алексеев", "Лебедев",
            "Семёнов", "Егоров", "Павлов", "Козлов", "Степанов"};
    private static final String[] CITIES = {"Москва", "Новосибирск", "Омск", "Томск", "Казань", "Самара", "Пермь",
            "Уфа", "Челябинск", "Красноярск", "Иркутск", "Барнаул", "Сочи", "Тюмень", "Владивосток"};
    private static final String[] TOPICS = {"шахматы", "фотография", "Java", "путешествия", "кино", "музыка",
            "велоспорт", "настольные игры", "кулинария", "астрономия"};
    private static final String[] REASONS = {"Нарушение правил", "Заблокирован по запросу", "Дубликат аккаунта"};

    private static final int FRIENDS_PER_PROFILE = 3;
    private static final int MAX_STRENGTH = 10;
    private static final int COMMUNITY_PERCENT = 8;
    private static final int DELETED_PERCENT = 4;
    private static final int PERCENT = 100;
    private static final int FIRST_BIRTH_YEAR = 1950;
    private static final int BIRTH_YEAR_SPAN = 61;
    private static final int FIRST_COMMUNITY_YEAR = 2005;
    private static final int COMMUNITY_YEAR_SPAN = 20;

    public record Result(int profiles, int friendships, boolean cancelled) { }

    @FunctionalInterface
    public interface Listener {
        void progress(int done, int total);
    }

    private final Random random;

    public DataGenerator(long seed) { this.random = new Random(seed); }

    public Result generate(ProfileRepository repository, int count, Listener listener, BooleanSupplier cancelled) {
        if (count < 1) throw new IllegalArgumentException("Нужно сгенерировать хотя бы одну запись");
        List<Profile> existing = repository.snapshot();
        int[] pool = new int[existing.size() + count];
        int[] administrators = new int[existing.size() + count];
        int poolSize = 0;
        int administratorCount = 0;
        for (Profile profile : existing) {
            pool[poolSize++] = profile.getId();
            if (ProfileRepository.canBeAdministrator(profile)) administrators[administratorCount++] = profile.getId();
        }
        int firstId = repository.maxId() + 1;
        int friendships = 0;
        for (int i = 0; i < count; i++) {
            if (cancelled.getAsBoolean()) {
                rollback(repository, firstId, i);
                return new Result(0, 0, true);
            }
            int id = firstId + i;
            Profile profile = newProfile(id, administrators, administratorCount);
            repository.forceAdd(profile);
            if (!(profile instanceof Community) && ProfileRepository.canBeAdministrator(profile)) {
                administrators[administratorCount++] = id;
            }
            friendships += befriend(repository, id, pool, poolSize);
            pool[poolSize++] = id;
            if (listener != null) listener.progress(i + 1, count);
        }
        return new Result(count, friendships, false);
    }

    private Profile newProfile(int id, int[] administrators, int administratorCount) {
        int roll = random.nextInt(PERCENT);
        String city = pick(CITIES);
        if (roll < COMMUNITY_PERCENT && administratorCount > 0) {
            String topic = pick(TOPICS);
            int administrator = administrators[random.nextInt(administratorCount)];
            return new Community(id, "Клуб «" + topic + "» " + id, city,
                    FIRST_COMMUNITY_YEAR + random.nextInt(COMMUNITY_YEAR_SPAN), "Сообщество: " + topic, administrator);
        }
        String name = pick(NAMES) + " " + pick(SURNAMES);
        int year = FIRST_BIRTH_YEAR + random.nextInt(BIRTH_YEAR_SPAN);
        if (roll < COMMUNITY_PERCENT + DELETED_PERCENT) return new DeletedProfile(id, name, city, year, pick(REASONS));
        return new EditableProfile(id, name, city, year);
    }

    private int befriend(ProfileRepository repository, int id, int[] pool, int poolSize) {
        int added = 0;
        for (int i = 0; poolSize > 0 && i < FRIENDS_PER_PROFILE; i++) {
            int other = pool[random.nextInt(poolSize)];
            if (repository.friendshipStrength(id, other) > 0) continue;
            repository.addFriendship(new Friendship(id, other, 1 + random.nextInt(MAX_STRENGTH)));
            added++;
        }
        return added;
    }

    // новые записи удаляются с конца, чтобы сообщество исчезало раньше своего администратора
    private static void rollback(ProfileRepository repository, int firstId, int created) {
        for (int i = created - 1; i >= 0; i--) repository.forceRemove(firstId + i);
    }

    private String pick(String[] values) { return values[random.nextInt(values.length)]; }
}
