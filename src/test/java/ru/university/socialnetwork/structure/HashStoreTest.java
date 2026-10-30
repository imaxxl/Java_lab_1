package ru.university.socialnetwork.structure;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class HashStoreTest {
    private static HashStore<Integer, Integer> store() { return new HashStore<>(value -> value); }

    private static HashStore<Integer, Integer> crowded() { return new HashStore<>(value -> value, 2, false); }

    private static List<Integer> inOrder(HashStore<Integer, Integer> store) {
        List<Integer> values = new ArrayList<>();
        store.forEach(Integer.MAX_VALUE, values::add);
        return values;
    }

    @Test
    void addsFindsAndRemoves() {
        HashStore<Integer, Integer> store = store();
        store.add(7);
        store.add(8);
        assertEquals(2, store.size());
        assertEquals(7, store.findByKey(7));
        assertNull(store.findByKey(9));
        assertTrue(store.removeByKey(7));
        assertFalse(store.removeByKey(7));
        assertNull(store.findByKey(7));
        assertEquals(1, store.size());
    }

    @Test
    void duplicateKeyIsRejectedAndKeepsOriginal() {
        HashStore<Integer, String> store = new HashStore<>(String::length);
        store.add("abc");
        assertThrows(IllegalArgumentException.class, () -> store.add("xyz"));
        assertEquals("abc", store.findByKey(3));
        assertEquals(1, store.size());
    }

    @Test
    void nullEntityAndNullKeyAreRejected() {
        HashStore<Integer, String> store = new HashStore<>(String::length);
        assertThrows(IllegalArgumentException.class, () -> store.add(null));
        assertThrows(IllegalArgumentException.class, () -> store.findByKey(null));
        assertThrows(IllegalArgumentException.class, () -> new HashStore<Integer, Integer>(null));
    }

    @Test
    void growsAndKeepsEveryEntry() {
        HashStore<Integer, Integer> store = store();
        int initial = store.capacity();
        for (int i = 0; i < 5000; i++) store.add(i);
        assertTrue(store.capacity() > initial);
        assertEquals(5000, store.size());
        assertEquals(5000, store.reachableCount());
        for (int i = 0; i < 5000; i++) assertEquals(i, store.findByKey(i));
    }

    @Test
    void keepsInsertionOrderThroughResizeAndRemoval() {
        HashStore<Integer, Integer> store = store();
        for (int i = 100; i > 0; i--) store.add(i);
        store.removeByKey(50);
        store.removeByKey(100);
        store.removeByKey(1);
        List<Integer> expected = new ArrayList<>();
        for (int i = 99; i > 1; i--) {
            if (i != 50) expected.add(i);
        }
        assertEquals(expected, inOrder(store));
    }

    @Test
    void removesHeadMiddleAndTailOfOneChain() {
        HashStore<Integer, Integer> store = crowded();
        for (int key : new int[]{1, 3, 5, 7, 9}) store.add(key);
        assertTrue(store.removeByKey(9));
        assertTrue(store.removeByKey(5));
        assertTrue(store.removeByKey(1));
        assertEquals(2, store.size());
        assertEquals(2, store.reachableCount());
        assertEquals(3, store.findByKey(3));
        assertEquals(7, store.findByKey(7));
        assertEquals(List.of(3, 7), inOrder(store));
    }

    @Test
    void replaceKeepsPositionAndRejectsOtherKey() {
        HashStore<Integer, String> store = new HashStore<>(String::length);
        store.add("a");
        store.add("bb");
        assertTrue(store.replaceByKey(1, "z"));
        assertEquals("z", store.findByKey(1));
        assertFalse(store.replaceByKey(5, "ддддд"));
        assertThrows(IllegalArgumentException.class, () -> store.replaceByKey(1, "два"));
        List<String> order = new ArrayList<>();
        store.forEach(10, order::add);
        assertEquals(List.of("z", "bb"), order);
    }

    @Test
    void traceShowsComparedNodesOfTheChain() {
        HashStore<Integer, Integer> store = crowded();
        for (int key : new int[]{1, 3, 5}) store.add(key);
        Trace trace = new Trace();
        assertEquals(1, store.findByKey(1, trace));
        assertEquals(3, trace.count());
        assertEquals(5, trace.get(0));
        assertEquals(1, trace.get(2));
        assertEquals(1, trace.result());
        trace.clear();
        assertNull(store.findByKey(7, trace));
        assertEquals(3, trace.count());
        assertNull(trace.result());
    }

    @Test
    void insertTraceEndsWithInsertedKey() {
        HashStore<Integer, Integer> store = crowded();
        store.add(1);
        Trace trace = new Trace();
        store.add(3, trace);
        assertTrue(trace.visited(1));
        assertEquals(3, trace.result());
    }

    @Test
    void bucketKeysShowChainContent() {
        HashStore<Integer, Integer> store = crowded();
        for (int key : new int[]{2, 1, 3}) store.add(key);
        assertArrayEquals(new Object[]{2}, store.bucketKeys(0, 10));
        assertArrayEquals(new Object[]{3, 1}, store.bucketKeys(1, 10));
        assertArrayEquals(new Object[]{3}, store.bucketKeys(1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> store.bucketKeys(2, 1));
        assertEquals(1, store.bucketOf(7));
    }

    @Test
    void clearLeavesEmptyUsableStore() {
        HashStore<Integer, Integer> store = store();
        for (int i = 0; i < 100; i++) store.add(i);
        store.clear();
        assertEquals(0, store.size());
        assertNull(store.findByKey(5));
        store.add(5);
        assertEquals(1, store.size());
    }

    @Test
    void behavesLikeJavaUtilHashMapOnRandomOperations() {
        Random random = new Random(42);
        HashStore<Integer, Integer> store = store();
        Map<Integer, Integer> reference = new HashMap<>();
        for (int step = 0; step < 30_000; step++) {
            int key = random.nextInt(1500);
            switch (random.nextInt(3)) {
                case 0 -> {
                    if (reference.putIfAbsent(key, key) == null) store.add(key);
                    else assertThrows(IllegalArgumentException.class, () -> store.add(key));
                }
                case 1 -> assertEquals(reference.remove(key) != null, store.removeByKey(key));
                default -> assertEquals(reference.get(key), store.findByKey(key));
            }
            assertEquals(reference.size(), store.size());
        }
        assertEquals(reference.size(), store.reachableCount());
        assertEquals(reference.size(), inOrder(store).size());
    }
}
