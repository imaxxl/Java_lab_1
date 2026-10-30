package ru.university.socialnetwork.graph;

import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;
import static org.junit.jupiter.api.Assertions.*;

class GraphRulesTest {
    @Test
    void duplicateProfileDoesNotReplaceOriginalOrLoseEdges() {
        Graph graph = new Graph();
        Profile original = new Profile(1, "А", "Город", 2000);
        graph.addProfile(original);
        graph.addProfile(new Profile(2, "Б", "Город", 2000));
        graph.addFriendship(new Friendship(1, 2, 5));
        assertThrows(IllegalArgumentException.class,
                () -> graph.addProfile(new Profile(1, "Замена", "Город", 2000)));
        assertSame(original, graph.getProfile(1));
        assertEquals(java.util.List.of(1, 2), graph.shortestPath(1, 2));
    }

    @Test
    void reverseFriendshipIsDuplicateOfSameUndirectedEdge() {
        Graph graph = new Graph();
        graph.addProfile(new Profile(1, "А", "Город", 2000));
        graph.addProfile(new Profile(2, "Б", "Город", 2000));
        graph.addFriendship(new Friendship(1, 2, 5));
        assertThrows(IllegalArgumentException.class,
                () -> graph.addFriendship(new Friendship(2, 1, 10)));
    }

    @Test
    void invalidProfileCannotEnterGraph() {
        assertThrows(IllegalArgumentException.class,
                () -> new Graph().addProfile(new Profile(0, "А", "Город", 2000)));
    }
}
