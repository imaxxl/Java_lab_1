package ru.university.socialnetwork.graph;

import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Graph {
    private final Map<Integer, Profile> profiles = new HashMap<>();
    private final Map<Integer, EdgeNode> adjacency = new HashMap<>();

    public void addProfile(Profile profile) {
        profiles.put(profile.getId(), profile);
        adjacency.putIfAbsent(profile.getId(), null);
    }

    public void addFriendship(Friendship friendship) {
        if (!profiles.containsKey(friendship.getFirstId()) || !profiles.containsKey(friendship.getSecondId())) {
            throw new IllegalArgumentException("Профиль для дружбы не найден");
        }

        addEdge(friendship.getFirstId(), friendship);
        addEdge(friendship.getSecondId(), friendship);
    }

    private void addEdge(int id, Friendship friendship) {
        EdgeNode newNode = new EdgeNode(friendship);
        newNode.next = adjacency.get(id);
        adjacency.put(id, newNode);
    }

    public List<Integer> shortestPath(int startId, int finishId) {
        if (!profiles.containsKey(startId) || !profiles.containsKey(finishId)) {
            return List.of();
        }

        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> previous = new HashMap<>();

        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            int current = queue.remove();

            if (current == finishId) {
                break;
            }

            EdgeNode node = adjacency.get(current);
            while (node != null) {
                int nextId = node.friendship.getOtherId(current);

                if (!visited.contains(nextId)) {
                    visited.add(nextId);
                    previous.put(nextId, current);
                    queue.add(nextId);
                }

                node = node.next;
            }
        }

        if (!visited.contains(finishId)) {
            return List.of();
        }

        List<Integer> path = new ArrayList<>();
        int current = finishId;

        while (current != startId) {
            path.add(current);
            current = previous.get(current);
        }

        path.add(startId);
        java.util.Collections.reverse(path);
        return path;
    }

    public Profile getProfile(int id) {
        return profiles.get(id);
    }

    public int size() {
        return profiles.size();
    }

    private static class EdgeNode {
        private final Friendship friendship;
        private EdgeNode next;

        private EdgeNode(Friendship friendship) {
            this.friendship = friendship;
        }
    }
}
