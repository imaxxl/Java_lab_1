package ru.university.socialnetwork.structure;

/**
 * Граф со списками смежности. Вершины лежат в собственной хеш-таблице, список соседей каждой вершины —
 * цепочка собственных узлов Edge, готовых коллекций нет. Класс не потокобезопасен: при обходе в ширину
 * вершины получают метки, поэтому даже чтение из двух потоков портит данные друг друга.
 */
public class AdjacencyGraph<K, T> implements GraphStore<K, T> {
    private static final class Vertex<K, T> {
        final K key;
        final long sequence;
        T value;
        Edge<K, T> edges;
        int degree;
        int mark;
        Vertex<K, T> parent;

        Vertex(K key, T value, long sequence) {
            this.key = key;
            this.value = value;
            this.sequence = sequence;
        }
    }

    private static final class Edge<K, T> {
        final Vertex<K, T> to;
        final int strength;
        Edge<K, T> next;

        Edge(Vertex<K, T> to, int strength, Edge<K, T> next) {
            this.to = to;
            this.strength = strength;
            this.next = next;
        }
    }

    private final KeyExtractor<K, T> keys;
    private final HashStore<K, Vertex<K, T>> vertices;
    private long nextSequence;
    private int edgeCount;
    private int stamp;

    public AdjacencyGraph(KeyExtractor<K, T> keys) {
        this(keys, 16, true);
    }

    public AdjacencyGraph(KeyExtractor<K, T> keys, int capacity, boolean autoResize) {
        if (keys == null) throw new IllegalArgumentException("Не задана функция ключа");
        this.keys = keys;
        this.vertices = new HashStore<>(vertex -> vertex.key, capacity, autoResize);
    }

    @Override
    public void add(T entity) { add(entity, null); }

    @Override
    public void add(T entity, Trace trace) {
        if (entity == null) throw new IllegalArgumentException("Пустую запись добавить нельзя");
        vertices.add(new Vertex<>(keys.keyOf(entity), entity, nextSequence), trace);
        nextSequence++;
    }

    @Override
    public T findByKey(K key) { return findByKey(key, null); }

    @Override
    public T findByKey(K key, Trace trace) {
        Vertex<K, T> vertex = vertices.findByKey(key, trace);
        return vertex == null ? null : vertex.value;
    }

    @Override
    public boolean removeByKey(K key) { return removeByKey(key, null); }

    @Override
    public boolean removeByKey(K key, Trace trace) {
        Vertex<K, T> vertex = vertices.findByKey(key, null);
        if (vertex == null) return false;
        for (Edge<K, T> edge = vertex.edges; edge != null; edge = edge.next) {
            unlink(edge.to, vertex);
            edgeCount--;
        }
        vertex.edges = null;
        vertex.degree = 0;
        return vertices.removeByKey(key, trace);
    }

    @Override
    public boolean replaceByKey(K key, T entity) {
        if (entity == null || !key.equals(keys.keyOf(entity))) {
            throw new IllegalArgumentException("Ключ новой записи должен совпадать с прежним");
        }
        Vertex<K, T> vertex = vertices.findByKey(key, null);
        if (vertex == null) return false;
        vertex.value = entity;
        return true;
    }

    @Override
    public int size() { return vertices.size(); }

    @Override
    public void addEdge(K first, K second, int strength) {
        if (strength < 1) throw new IllegalArgumentException("Сила связи должна быть положительной");
        if (first.equals(second)) throw new IllegalArgumentException("Нельзя добавить дружбу с самим собой");
        Vertex<K, T> a = require(first);
        Vertex<K, T> b = require(second);
        // Повтор ищем в списке с меньшим числом соседей: связь с «хабом» остаётся дешёвой.
        Vertex<K, T> shorter = a.degree <= b.degree ? a : b;
        Vertex<K, T> other = shorter == a ? b : a;
        if (findEdge(shorter, other) != null) throw new IllegalArgumentException("Такая связь уже есть");
        a.edges = new Edge<>(b, strength, a.edges);
        a.degree++;
        b.edges = new Edge<>(a, strength, b.edges);
        b.degree++;
        edgeCount++;
    }

    @Override
    public boolean removeEdge(K first, K second) {
        Vertex<K, T> a = vertices.findByKey(first, null);
        Vertex<K, T> b = vertices.findByKey(second, null);
        if (a == null || b == null || !unlink(a, b)) return false;
        unlink(b, a);
        edgeCount--;
        return true;
    }

    @Override
    public int edgeStrength(K first, K second) {
        Vertex<K, T> a = vertices.findByKey(first, null);
        Vertex<K, T> b = vertices.findByKey(second, null);
        if (a == null || b == null) return 0;
        Edge<K, T> edge = a.degree <= b.degree ? findEdge(a, b) : findEdge(b, a);
        return edge == null ? 0 : edge.strength;
    }

    @Override
    public int edgeCount() { return edgeCount; }

    @Override
    public int degree(K key) { return require(key).degree; }

    /** Поиск в ширину: очередь — массив, длина пути считается по числу связей. O(V + E). */
    @Override
    @SuppressWarnings("unchecked")
    public Chain<K> shortestChain(K from, K to, Trace trace) {
        Vertex<K, T> start = vertices.findByKey(from, null);
        Vertex<K, T> goal = vertices.findByKey(to, null);
        if (start == null || goal == null) return Chain.none();
        nextStamp();
        Object[] queue = new Object[vertices.size()];
        int head = 0;
        int tail = 0;
        start.mark = stamp;
        start.parent = null;
        queue[tail++] = start;
        while (head < tail) {
            Vertex<K, T> current = (Vertex<K, T>) queue[head++];
            if (trace != null) trace.visit(current.key);
            if (current == goal) break;
            for (Edge<K, T> edge = current.edges; edge != null; edge = edge.next) {
                Vertex<K, T> next = edge.to;
                if (next.mark != stamp) {
                    next.mark = stamp;
                    next.parent = current;
                    queue[tail++] = next;
                }
            }
        }
        if (goal.mark != stamp) return Chain.none();
        if (trace != null) trace.markResult(goal.key);
        return restorePath(start, goal);
    }

    @Override
    public void forEachVertex(int limit, Visitor<T> visitor) {
        vertices.forEach(limit, vertex -> visitor.visit(vertex.value));
    }

    @Override
    public void forEachEdge(EdgeVisitor<K> visitor) {
        vertices.forEach(Integer.MAX_VALUE, vertex -> {
            for (Edge<K, T> edge = vertex.edges; edge != null; edge = edge.next) {
                if (vertex.sequence < edge.to.sequence) visitor.visit(vertex.key, edge.to.key, edge.strength);
            }
        });
    }

    @Override
    public void forEachNeighbor(K key, EdgeVisitor<K> visitor) {
        Vertex<K, T> vertex = vertices.findByKey(key, null);
        if (vertex == null) return;
        for (Edge<K, T> edge = vertex.edges; edge != null; edge = edge.next) {
            visitor.visit(key, edge.to.key, edge.strength);
        }
    }

    @Override
    public void clear() {
        vertices.clear();
        nextSequence = 0;
        edgeCount = 0;
        stamp = 0;
    }

    @Override
    public int bucketCount() { return vertices.capacity(); }

    @Override
    public Object[] bucketKeys(int bucket, int limit) { return vertices.bucketKeys(bucket, limit); }

    @Override
    public int countVertices() { return vertices.reachableCount(); }

    @Override
    public int countNeighbors(K key) {
        int count = 0;
        for (Edge<K, T> edge = require(key).edges; edge != null; edge = edge.next) count++;
        return count;
    }

    private Vertex<K, T> require(K key) {
        Vertex<K, T> vertex = vertices.findByKey(key, null);
        if (vertex == null) throw new IllegalArgumentException("Нет вершины с ключом " + key);
        return vertex;
    }

    private static <K, T> Edge<K, T> findEdge(Vertex<K, T> owner, Vertex<K, T> target) {
        for (Edge<K, T> edge = owner.edges; edge != null; edge = edge.next) {
            if (edge.to == target) return edge;
        }
        return null;
    }

    /** Убирает из списка owner узел, ведущий к target. */
    private static <K, T> boolean unlink(Vertex<K, T> owner, Vertex<K, T> target) {
        Edge<K, T> previous = null;
        for (Edge<K, T> edge = owner.edges; edge != null; previous = edge, edge = edge.next) {
            if (edge.to == target) {
                if (previous == null) owner.edges = edge.next;
                else previous.next = edge.next;
                owner.degree--;
                return true;
            }
        }
        return false;
    }

    private void nextStamp() {
        if (stamp == Integer.MAX_VALUE) {
            vertices.forEach(Integer.MAX_VALUE, vertex -> vertex.mark = 0);
            stamp = 0;
        }
        stamp++;
    }

    private Chain<K> restorePath(Vertex<K, T> start, Vertex<K, T> goal) {
        int length = 1;
        for (Vertex<K, T> vertex = goal; vertex != start; vertex = vertex.parent) length++;
        Object[] path = new Object[length];
        int index = length - 1;
        for (Vertex<K, T> vertex = goal; ; vertex = vertex.parent) {
            path[index--] = vertex.key;
            if (vertex == start) break;
        }
        return new Chain<>(path);
    }
}
