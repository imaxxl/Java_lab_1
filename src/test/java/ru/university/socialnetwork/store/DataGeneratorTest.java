package ru.university.socialnetwork.store;

import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DataGeneratorTest {
    @Test
    void createsRequestedNumberOfValidUniqueProfiles() {
        ProfileRepository repository = new ProfileRepository();
        DataGenerator.Result result = new DataGenerator(1).generate(repository, 500, null, () -> false);
        assertEquals(500, result.profiles());
        assertFalse(result.cancelled());
        assertEquals(500, repository.size());
        Set<Integer> ids = new HashSet<>();
        for (Profile profile : repository.snapshot()) {
            assertTrue(profile.validate().isEmpty(), profile.toString());
            assertTrue(ids.add(profile.getId()));
        }
    }

    @Test
    void generatesAllThreeTypesAndValidAdministrators() {
        ProfileRepository repository = new ProfileRepository();
        new DataGenerator(2).generate(repository, 1000, null, () -> false);
        Set<ProfileType> types = new HashSet<>();
        for (Profile profile : repository.snapshot()) {
            types.add(ProfileType.of(profile));
            if (profile instanceof Community community) {
                assertTrue(ProfileRepository.canBeAdministrator(repository.findById(community.getAdministratorId())));
            }
        }
        assertEquals(3, types.size());
    }

    @Test
    void createsFriendships() {
        ProfileRepository repository = new ProfileRepository();
        DataGenerator.Result result = new DataGenerator(3).generate(repository, 200, null, () -> false);
        assertTrue(result.friendships() > 200);
        assertEquals(result.friendships(), repository.friendshipCount());
    }

    @Test
    void sameSeedGivesSameData() {
        ProfileRepository first = new ProfileRepository();
        ProfileRepository second = new ProfileRepository();
        new DataGenerator(42).generate(first, 300, null, () -> false);
        new DataGenerator(42).generate(second, 300, null, () -> false);
        assertEquals(first.friendshipCount(), second.friendshipCount());
        List<Profile> a = first.snapshot();
        List<Profile> b = second.snapshot();
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).toString(), b.get(i).toString());
    }

    @Test
    void continuesAfterExistingIdsAndKeepsOldData() {
        ProfileRepository repository = new ProfileRepository();
        repository.add(new EditableProfile(50, "Анна", "Город", 2000));
        new DataGenerator(5).generate(repository, 20, null, () -> false);
        assertEquals(21, repository.size());
        assertNotNull(repository.findById(50));
        assertNotNull(repository.findById(51));
        assertNotNull(repository.findById(70));
    }

    @Test
    void reportsGrowingProgressUpToTotal() {
        List<Integer> done = new ArrayList<>();
        new DataGenerator(6).generate(new ProfileRepository(), 50, (current, total) -> {
            assertEquals(50, total);
            done.add(current);
        }, () -> false);
        assertEquals(50, done.size());
        assertEquals(50, done.get(done.size() - 1));
        for (int i = 1; i < done.size(); i++) assertTrue(done.get(i) > done.get(i - 1));
    }

    @Test
    void cancellationRollsEverythingBack() {
        ProfileRepository repository = new ProfileRepository();
        repository.add(new EditableProfile(1, "Анна", "Город", 2000));
        int[] checks = {0};
        DataGenerator.Result result = new DataGenerator(7).generate(repository, 400, null, () -> ++checks[0] > 150);
        assertTrue(result.cancelled());
        assertEquals(1, repository.size());
        assertEquals(0, repository.friendshipCount());
        assertEquals(0, repository.friendsCount(1));
    }

    @Test
    void rejectsEmptyRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> new DataGenerator(1).generate(new ProfileRepository(), 0, null, () -> false));
    }
}
