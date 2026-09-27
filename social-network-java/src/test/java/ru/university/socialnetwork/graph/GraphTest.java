package ru.university.socialnetwork.graph;

import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    @Test
    void findsShortestFriendshipChain() {
        Graph graph = new Graph();

        graph.addProfile(new Profile(1, "А", "Москва", 2000));
        graph.addProfile(new Profile(2, "Б", "Москва", 2000));
        graph.addProfile(new Profile(3, "В", "Москва", 2000));
        graph.addProfile(new Profile(4, "Г", "Москва", 2000));

        graph.addFriendship(new Friendship(1, 2, 5));
        graph.addFriendship(new Friendship(2, 4, 5));
        graph.addFriendship(new Friendship(4, 3, 5));
        graph.addFriendship(new Friendship(1, 3, 5));

        List<Integer> path = graph.shortestPath(1, 3);

        assertEquals(List.of(1, 3), path);
    }

    @Test
    void returnsEmptyPathWhenProfilesAreDisconnected() {
        Graph graph = new Graph();

        graph.addProfile(new Profile(1, "А", "Москва", 2000));
        graph.addProfile(new Profile(2, "Б", "Москва", 2000));

        assertTrue(graph.shortestPath(1, 2).isEmpty());
    }

    @Test
    void returnsEmptyPathWhenProfileDoesNotExist() {
        Graph graph = new Graph();

        graph.addProfile(new Profile(1, "А", "Москва", 2000));

        assertTrue(graph.shortestPath(1, 99).isEmpty());
        assertTrue(graph.shortestPath(99, 1).isEmpty());
    }

    @Test
    void returnsPathFromProfileToItself() {
        Graph graph = new Graph();

        graph.addProfile(new Profile(1, "А", "Москва", 2000));

        assertEquals(List.of(1), graph.shortestPath(1, 1));
    }

    @Test
    void addsProfilesAndReturnsThem() {
        Graph graph = new Graph();

        Profile first = new Profile(1, "А", "Москва", 2000);
        Profile second = new Profile(2, "Б", "Омск", 2001);

        graph.addProfile(first);
        graph.addProfile(second);

        assertEquals(2, graph.size());
        assertSame(first, graph.getProfile(1));
        assertSame(second, graph.getProfile(2));
    }

    @Test
    void doesNotAllowFriendshipWithUnknownProfile() {
        Graph graph = new Graph();

        graph.addProfile(new Profile(1, "А", "Москва", 2000));

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addFriendship(new Friendship(1, 99, 5))
        );
    }
}