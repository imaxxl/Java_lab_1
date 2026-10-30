package ru.university.socialnetwork.store;

import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.*;
import ru.university.socialnetwork.structure.Chain;
import ru.university.socialnetwork.structure.Trace;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileRepositoryFriendshipTest {
    private static EditableProfile human(int id) { return new EditableProfile(id, "Имя" + id, "Город", 2000); }

    private static ProfileRepository withHumans(int... ids) {
        ProfileRepository repository = new ProfileRepository();
        for (int id : ids) repository.add(human(id));
        return repository;
    }

    @Test
    void friendshipsConnectExistingProfilesOnly() {
        ProfileRepository repository = withHumans(1, 2);
        repository.addFriendship(new Friendship(1, 2, 5));
        assertEquals(1, repository.friendshipCount());
        assertEquals(5, repository.friendshipStrength(2, 1));
        assertThrows(IllegalArgumentException.class, () -> repository.addFriendship(new Friendship(1, 99, 5)));
        assertThrows(IllegalArgumentException.class, () -> repository.addFriendship(new Friendship(2, 1, 7)));
        assertEquals(1, repository.friendsCount(1));
    }

    @Test
    void removingFriendshipWorksFromEitherSide() {
        ProfileRepository repository = withHumans(1, 2);
        repository.addFriendship(new Friendship(1, 2, 5));
        assertTrue(repository.removeFriendship(2, 1));
        assertFalse(repository.removeFriendship(2, 1));
        assertEquals(0, repository.friendshipCount());
    }

    @Test
    void friendshipSnapshotListsEveryEdgeOnce() {
        ProfileRepository repository = withHumans(1, 2, 3);
        repository.addFriendship(new Friendship(1, 2, 4));
        repository.addFriendship(new Friendship(3, 1, 6));
        List<Friendship> edges = repository.friendships();
        assertEquals(2, edges.size());
        assertThrows(UnsupportedOperationException.class, edges::clear);
        assertEquals(10, edges.stream().mapToInt(Friendship::getStrength).sum());
    }

    @Test
    void shortestChainUsesFriendships() {
        ProfileRepository repository = withHumans(1, 2, 3, 4);
        repository.addFriendship(new Friendship(1, 2, 1));
        repository.addFriendship(new Friendship(2, 3, 1));
        repository.addFriendship(new Friendship(3, 4, 1));
        Trace trace = new Trace();
        Chain<Integer> chain = repository.shortestChain(1, 4, trace);
        assertEquals(4, chain.length());
        assertEquals(1, chain.get(0));
        assertEquals(4, chain.get(3));
        assertEquals(4, trace.result());
        assertTrue(repository.shortestChain(1, 99, null).isEmpty());
    }

    @Test
    void editingProfileKeepsItsFriendships() {
        ProfileRepository repository = withHumans(1, 2);
        repository.addFriendship(new Friendship(1, 2, 5));
        repository.replace(1, new EditableProfile(1, "Новое имя", "Омск", 1999));
        assertEquals("Новое имя", repository.findById(1).getName());
        assertEquals(5, repository.friendshipStrength(1, 2));
    }

    @Test
    void loadingNewProfilesDropsOldFriendships() {
        ProfileRepository repository = withHumans(1, 2);
        repository.addFriendship(new Friendship(1, 2, 5));
        repository.replaceAll(List.of(human(1), human(2)));
        assertEquals(0, repository.friendshipCount());
    }

    @Test
    void removingProfileTakesItsFriendshipsAway() {
        ProfileRepository repository = withHumans(1, 2, 3);
        repository.addFriendship(new Friendship(1, 2, 1));
        repository.addFriendship(new Friendship(1, 3, 1));
        Trace trace = new Trace();
        repository.remove(1, trace);
        assertNull(repository.findById(1));
        assertEquals(0, repository.friendshipCount());
        assertEquals(0, repository.friendsCount(2));
        assertEquals(1, trace.result());
        assertEquals(2, repository.size());
    }

    @Test
    void archiveCannotBeRemovedThroughRepository() {
        ProfileRepository repository = new ProfileRepository();
        repository.replaceAll(List.of(new DeletedProfile(1, "Архив", "Город", 2000, "")));
        assertThrows(IllegalArgumentException.class, () -> repository.remove(1, null));
        assertEquals(1, repository.size());
    }

    @Test
    void administratorOfCommunityCannotBeRemoved() {
        ProfileRepository repository = withHumans(1, 2);
        repository.add(new Community(3, "Клуб", "Город", 2020, "Java", 1));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> repository.remove(1, null));
        assertTrue(error.getMessage().contains("администрирует"));
        repository.remove(3, null);
        repository.remove(1, null);
        assertEquals(1, repository.size());
    }

    @Test
    void removingUnknownProfileFails() {
        assertThrows(IllegalArgumentException.class, () -> new ProfileRepository().remove(5, null));
    }

    @Test
    void structureSnapshotLimitsVerticesAndKeepsEdgesBetweenShownOnes() {
        ProfileRepository repository = withHumans(1, 2, 3, 4, 5);
        repository.addFriendship(new Friendship(1, 2, 3));
        repository.addFriendship(new Friendship(2, 5, 3));
        StructureSnapshot snapshot = repository.structure(3, 4, 5);
        assertEquals(5, snapshot.vertexTotal());
        assertEquals(2, snapshot.edgeTotal());
        assertEquals(3, snapshot.shownVertices());
        assertEquals(1, snapshot.shownEdges());
        assertEquals(1, snapshot.edgeFrom(0));
        assertEquals(2, snapshot.edgeTo(0));
        assertEquals(ProfileType.PROFILE, snapshot.type(0));
        assertEquals(2, snapshot.indexOf(3));
        assertEquals(-1, snapshot.indexOf(5));
        assertEquals(4, snapshot.shownBuckets());
        int inBuckets = 0;
        for (int bucket = 0; bucket < snapshot.shownBuckets(); bucket++) inBuckets += snapshot.bucketSize(bucket);
        assertTrue(inBuckets <= 5);
    }

    @Test
    void maxIdAndClear() {
        ProfileRepository repository = withHumans(4, 9, 2);
        assertEquals(9, repository.maxId());
        repository.clear();
        assertEquals(0, repository.size());
        assertEquals(0, repository.maxId());
    }
}
