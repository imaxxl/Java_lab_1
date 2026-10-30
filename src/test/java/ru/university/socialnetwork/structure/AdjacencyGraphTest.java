package ru.university.socialnetwork.structure;

import org.junit.jupiter.api.Test;
import ru.university.socialnetwork.model.Profile;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AdjacencyGraphTest {
    private static AdjacencyGraph<Integer, Profile> profiles(int... ids) {
        AdjacencyGraph<Integer, Profile> graph = new AdjacencyGraph<>(Profile::getId);
        for (int id : ids) graph.add(new Profile(id, "Имя" + id, "Москва", 2000));
        return graph;
    }

    private static List<Integer> keys(Chain<Integer> chain) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < chain.length(); i++) result.add(chain.get(i));
        return result;
    }

    @Test
    void findsShortestFriendshipChain() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2, 3, 4);
        graph.addEdge(1, 2, 5);
        graph.addEdge(2, 4, 5);
        graph.addEdge(4, 3, 5);
        graph.addEdge(1, 3, 5);
        assertEquals(List.of(1, 3), keys(graph.shortestChain(1, 3, null)));
    }

    @Test
    void chainIsShortestByLinksNotByStrength() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2, 3, 4);
        graph.addEdge(1, 4, 1);
        graph.addEdge(1, 2, 100);
        graph.addEdge(2, 3, 100);
        graph.addEdge(3, 4, 100);
        assertEquals(List.of(1, 4), keys(graph.shortestChain(1, 4, null)));
    }

    @Test
    void emptyChainWhenDisconnectedOrUnknown() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2);
        assertTrue(graph.shortestChain(1, 2, null).isEmpty());
        assertTrue(graph.shortestChain(1, 99, null).isEmpty());
        assertTrue(graph.shortestChain(99, 1, null).isEmpty());
    }

    @Test
    void chainFromVertexToItself() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1);
        assertEquals(List.of(1), keys(graph.shortestChain(1, 1, null)));
    }

    @Test
    void addsFindsAndReplacesProfiles() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2);
        Profile first = graph.findByKey(1);
        graph.addEdge(1, 2, 3);
        Profile updated = new Profile(1, "Новое", "Омск", 2001);
        assertTrue(graph.replaceByKey(1, updated));
        assertSame(updated, graph.findByKey(1));
        assertEquals(3, graph.edgeStrength(1, 2));
        assertNotSame(first, graph.findByKey(1));
        assertThrows(IllegalArgumentException.class,
                () -> graph.replaceByKey(1, new Profile(2, "Другой", "Омск", 2001)));
        assertFalse(graph.replaceByKey(7, new Profile(7, "Нет", "Омск", 2001)));
    }

    @Test
    void duplicateVertexDoesNotReplaceOriginalOrLoseEdges() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2);
        Profile original = graph.findByKey(1);
        graph.addEdge(1, 2, 5);
        assertThrows(IllegalArgumentException.class, () -> graph.add(new Profile(1, "Замена", "Город", 2000)));
        assertSame(original, graph.findByKey(1));
        assertEquals(List.of(1, 2), keys(graph.shortestChain(1, 2, null)));
    }

    @Test
    void edgeRulesAreChecked() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2);
        graph.addEdge(1, 2, 5);
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(2, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(1, 99, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(99, 1, 1));
        profiles(1, 2).addEdge(1, 2, 1);
        assertThrows(IllegalArgumentException.class, () -> profiles(1, 2).addEdge(1, 2, 0));
        assertEquals(1, graph.edgeCount());
    }

    @Test
    void removingVertexRemovesItsEdgesFromNeighbours() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2, 3, 4);
        graph.addEdge(1, 2, 1);
        graph.addEdge(1, 3, 1);
        graph.addEdge(1, 4, 1);
        graph.addEdge(3, 4, 1);
        assertTrue(graph.removeByKey(1));
        assertEquals(1, graph.edgeCount());
        assertEquals(0, graph.degree(2));
        assertEquals(1, graph.degree(3));
        assertEquals(1, graph.countNeighbors(4));
        assertTrue(graph.shortestChain(2, 4, null).isEmpty());
        assertFalse(graph.removeByKey(1));
    }

    @Test
    void removesEdgeFromBothEnds() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2, 3);
        graph.addEdge(1, 2, 4);
        graph.addEdge(1, 3, 4);
        assertTrue(graph.removeEdge(2, 1));
        assertFalse(graph.removeEdge(1, 2));
        assertFalse(graph.removeEdge(1, 99));
        assertEquals(0, graph.edgeStrength(1, 2));
        assertEquals(1, graph.degree(1));
        assertEquals(0, graph.degree(2));
        assertEquals(1, graph.edgeCount());
    }

    @Test
    void edgesAreVisitedOnceAndNeighboursListed() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2, 3);
        graph.addEdge(1, 2, 7);
        graph.addEdge(3, 1, 9);
        List<String> edges = new ArrayList<>();
        graph.forEachEdge((from, to, strength) -> edges.add(from + "-" + to + ":" + strength));
        assertEquals(List.of("1-2:7", "1-3:9").size(), edges.size());
        List<Integer> neighbours = new ArrayList<>();
        graph.forEachNeighbor(1, (from, to, strength) -> neighbours.add(to));
        assertEquals(2, neighbours.size());
        assertTrue(neighbours.containsAll(List.of(2, 3)));
    }

    @Test
    void bfsTraceStartsAtSourceAndEndsAtTarget() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2, 3, 4, 5);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(1, 5, 1);
        Trace trace = new Trace();
        graph.shortestChain(1, 3, trace);
        assertEquals(1, trace.get(0));
        assertEquals(3, trace.get(trace.count() - 1));
        assertEquals(3, trace.result());
        assertFalse(trace.visited(4));
    }

    @Test
    void repeatedSearchesDoNotInterfereAndSurviveStampOverflow() throws Exception {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2, 3);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        Field stamp = AdjacencyGraph.class.getDeclaredField("stamp");
        stamp.setAccessible(true);
        stamp.setInt(graph, Integer.MAX_VALUE - 1);
        for (int i = 0; i < 4; i++) assertEquals(List.of(1, 2, 3), keys(graph.shortestChain(1, 3, null)));
        assertTrue(stamp.getInt(graph) < 10);
    }

    @Test
    void clearRemovesEverything() {
        AdjacencyGraph<Integer, Profile> graph = profiles(1, 2);
        graph.addEdge(1, 2, 1);
        graph.clear();
        assertEquals(0, graph.size());
        assertEquals(0, graph.edgeCount());
        graph.add(new Profile(1, "Снова", "Город", 2000));
        assertEquals(1, graph.size());
    }

    @Test
    void agreesWithJavaUtilBreadthFirstSearchOnRandomGraphs() {
        Random random = new Random(7);
        int vertices = 80;
        AdjacencyGraph<Integer, Integer> graph = new AdjacencyGraph<>(value -> value);
        Map<Integer, List<Integer>> reference = new HashMap<>();
        for (int i = 0; i < vertices; i++) {
            graph.add(i);
            reference.put(i, new ArrayList<>());
        }
        for (int i = 0; i < 110; i++) {
            int a = random.nextInt(vertices);
            int b = random.nextInt(vertices);
            if (a == b || reference.get(a).contains(b)) continue;
            graph.addEdge(a, b, 1 + random.nextInt(9));
            reference.get(a).add(b);
            reference.get(b).add(a);
        }
        for (int round = 0; round < 300; round++) {
            int from = random.nextInt(vertices);
            int to = random.nextInt(vertices);
            int expected = distance(reference, from, to);
            Chain<Integer> chain = graph.shortestChain(from, to, null);
            assertEquals(expected < 0 ? 0 : expected + 1, chain.length());
            for (int i = 0; i + 1 < chain.length(); i++) {
                assertTrue(graph.edgeStrength(chain.get(i), chain.get(i + 1)) > 0);
            }
        }
    }

    private static int distance(Map<Integer, List<Integer>> graph, int from, int to) {
        Map<Integer, Integer> distance = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();
        distance.put(from, 0);
        queue.add(from);
        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (current == to) return distance.get(current);
            for (int next : graph.get(current)) {
                if (distance.putIfAbsent(next, distance.get(current) + 1) == null) queue.add(next);
            }
        }
        return -1;
    }
}
