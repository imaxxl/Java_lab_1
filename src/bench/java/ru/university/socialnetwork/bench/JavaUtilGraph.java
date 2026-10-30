package ru.university.socialnetwork.bench;

import ru.university.socialnetwork.model.Profile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

final class JavaUtilGraph {
    private final Map<Integer, Profile> profiles = new HashMap<>();
    private final Map<Integer, List<int[]>> adjacency = new HashMap<>();

    void add(Profile profile) {
        if (profiles.putIfAbsent(profile.getId(), profile) != null) {
            throw new IllegalArgumentException("Ключ уже существует: " + profile.getId());
        }
        adjacency.put(profile.getId(), new ArrayList<>());
    }

    Profile find(int id) { return profiles.get(id); }

    boolean remove(int id) {
        if (profiles.remove(id) == null) return false;
        for (int[] edge : adjacency.remove(id)) adjacency.get(edge[0]).removeIf(other -> other[0] == id);
        return true;
    }

    void addEdge(int first, int second, int strength) {
        List<int[]> a = adjacency.get(first);
        List<int[]> b = adjacency.get(second);
        if (a == null || b == null) throw new IllegalArgumentException("Нет вершины");
        List<int[]> shorter = a.size() <= b.size() ? a : b;
        int target = shorter == a ? second : first;
        for (int[] edge : shorter) {
            if (edge[0] == target) throw new IllegalArgumentException("Такая связь уже есть");
        }
        a.add(new int[]{second, strength});
        b.add(new int[]{first, strength});
    }

    int chainLength(int from, int to) {
        if (!profiles.containsKey(from) || !profiles.containsKey(to)) return 0;
        Map<Integer, Integer> distance = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();
        distance.put(from, 1);
        queue.add(from);
        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (current == to) return distance.get(current);
            for (int[] edge : adjacency.get(current)) {
                if (distance.putIfAbsent(edge[0], distance.get(current) + 1) == null) queue.add(edge[0]);
            }
        }
        return 0;
    }

    int size() { return profiles.size(); }
}
