package ru.university.socialnetwork.store;

import ru.university.socialnetwork.model.Community;
import ru.university.socialnetwork.model.DeletedProfile;
import ru.university.socialnetwork.model.Editable;
import ru.university.socialnetwork.model.Friendship;
import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.model.ProfileType;
import ru.university.socialnetwork.structure.AdjacencyGraph;
import ru.university.socialnetwork.structure.Chain;
import ru.university.socialnetwork.structure.GraphStore;
import ru.university.socialnetwork.structure.SynchronizedGraph;
import ru.university.socialnetwork.structure.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

public final class ProfileRepository {
    private static final int NO_LIMIT = Integer.MAX_VALUE;

    private GraphStore<Integer, Profile> graph = newGraph();

    private static GraphStore<Integer, Profile> newGraph() {
        return new SynchronizedGraph<>(new AdjacencyGraph<>(Profile::getId));
    }

    public synchronized int size() { return graph.size(); }
    public synchronized int friendshipCount() { return graph.edgeCount(); }
    public synchronized Profile findById(int id) { return graph.findByKey(id); }
    public synchronized Profile findById(int id, Trace trace) { return graph.findByKey(id, trace); }

    public synchronized List<Profile> snapshot() {
        List<Profile> copy = new ArrayList<>(graph.size());
        graph.forEachVertex(NO_LIMIT, copy::add);
        return List.copyOf(copy);
    }

    public synchronized void validateForAdd(Profile profile) {
        requireEditable(profile);
        requireValid(profile);
        if (graph.findByKey(profile.getId()) != null) {
            throw new IllegalArgumentException("ID " + profile.getId() + " уже занят");
        }
        requireAdministrator(profile, graph::findByKey);
    }

    public synchronized void add(Profile profile) { add(profile, null); }

    public synchronized void add(Profile profile, Trace trace) {
        validateForAdd(profile);
        graph.add(profile, trace);
    }

    public synchronized void validateForReplace(int originalId, Profile replacement) {
        Profile original = graph.findByKey(originalId);
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
        requireAdministrator(replacement, graph::findByKey);
    }

    public synchronized void replace(int originalId, Profile replacement) {
        validateForReplace(originalId, replacement);
        graph.replaceByKey(originalId, replacement);
    }

    public synchronized void remove(int id, Trace trace) {
        Profile profile = graph.findByKey(id);
        if (profile == null) throw new IllegalArgumentException("Профиль " + id + " не найден");
        requireEditable(profile);
        int[] community = {0};
        graph.forEachVertex(NO_LIMIT, other -> {
            if (other instanceof Community c && c.getAdministratorId() == id) community[0] = c.getId();
        });
        if (community[0] != 0) {
            throw new IllegalArgumentException("Нельзя удалить профиль " + id
                    + ": он администрирует сообщество " + community[0]);
        }
        graph.removeByKey(id, trace);
    }

    synchronized void forceAdd(Profile profile) {
        requireValid(profile);
        if (graph.findByKey(profile.getId()) != null) {
            throw new IllegalArgumentException("ID " + profile.getId() + " уже занят");
        }
        requireAdministrator(profile, graph::findByKey);
        graph.add(profile);
    }

    synchronized void forceRemove(int id) { graph.removeByKey(id); }

    public synchronized void replaceAll(List<Profile> loaded) {
        Objects.requireNonNull(loaded, "Список профилей не задан");
        GraphStore<Integer, Profile> checked = new AdjacencyGraph<>(Profile::getId);
        for (Profile profile : loaded) {
            requireValid(profile);
            if (checked.findByKey(profile.getId()) != null) {
                throw new IllegalArgumentException("Дублирующийся ID: " + profile.getId());
            }
            checked.add(profile);
        }
        checked.forEachVertex(NO_LIMIT, profile -> requireAdministrator(profile, checked::findByKey));
        graph = new SynchronizedGraph<>(checked);
    }

    public synchronized void clear() { graph.clear(); }

    public synchronized void addFriendship(Friendship friendship) {
        Objects.requireNonNull(friendship, "Дружба не задана");
        graph.addEdge(friendship.getFirstId(), friendship.getSecondId(), friendship.getStrength());
    }

    public synchronized boolean removeFriendship(int firstId, int secondId) {
        return graph.removeEdge(firstId, secondId);
    }

    public synchronized int friendsCount(int id) { return graph.degree(id); }

    public synchronized int friendshipStrength(int firstId, int secondId) {
        return graph.edgeStrength(firstId, secondId);
    }

    public synchronized List<Friendship> friendships() {
        List<Friendship> result = new ArrayList<>(graph.edgeCount());
        graph.forEachEdge((from, to, strength) -> result.add(new Friendship(from, to, strength)));
        return List.copyOf(result);
    }

    public synchronized Chain<Integer> shortestChain(int from, int to, Trace trace) {
        return graph.shortestChain(from, to, trace);
    }

    public synchronized int maxId() {
        int[] max = {0};
        graph.forEachVertex(NO_LIMIT, profile -> max[0] = Math.max(max[0], profile.getId()));
        return max[0];
    }

    public synchronized StructureSnapshot structure(int maxVertices, int maxBuckets, int maxChain) {
        int shown = Math.min(graph.size(), maxVertices);
        int[] ids = new int[shown];
        ProfileType[] types = new ProfileType[shown];
        int[] position = {0};
        graph.forEachVertex(shown, profile -> {
            ids[position[0]] = profile.getId();
            types[position[0]++] = ProfileType.of(profile);
        });
        List<int[]> edges = new ArrayList<>();
        for (int id : ids) {
            graph.forEachNeighbor(id, (from, to, strength) -> {
                if (from < to && contains(ids, to)) edges.add(new int[]{from, to, strength});
            });
        }
        int[] edgeFrom = new int[edges.size()];
        int[] edgeTo = new int[edges.size()];
        int[] edgeStrength = new int[edges.size()];
        for (int i = 0; i < edges.size(); i++) {
            edgeFrom[i] = edges.get(i)[0];
            edgeTo[i] = edges.get(i)[1];
            edgeStrength[i] = edges.get(i)[2];
        }
        int shownBuckets = Math.min(graph.bucketCount(), maxBuckets);
        int[][] buckets = new int[shownBuckets][];
        for (int bucket = 0; bucket < shownBuckets; bucket++) {
            Object[] keys = graph.bucketKeys(bucket, maxChain);
            buckets[bucket] = new int[keys.length];
            for (int i = 0; i < keys.length; i++) buckets[bucket][i] = (Integer) keys[i];
        }
        return new StructureSnapshot(graph.size(), graph.edgeCount(), ids, types,
                edgeFrom, edgeTo, edgeStrength, graph.bucketCount(), buckets);
    }

    public static boolean canBeAdministrator(Profile profile) {
        return profile != null && !(profile instanceof DeletedProfile)
                && !(profile instanceof Community);
    }

    private static boolean contains(int[] ids, int id) {
        for (int candidate : ids) {
            if (candidate == id) return true;
        }
        return false;
    }

    private static void requireAdministrator(Profile profile, IntFunction<Profile> lookup) {
        if (profile instanceof Community community
                && !canBeAdministrator(lookup.apply(community.getAdministratorId()))) {
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
