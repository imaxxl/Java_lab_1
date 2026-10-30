package ru.university.socialnetwork.structure;

public class SynchronizedGraph<K, T> implements GraphStore<K, T> {
    private final GraphStore<K, T> graph;

    public SynchronizedGraph(GraphStore<K, T> graph) {
        if (graph == null) throw new IllegalArgumentException("Не задан граф для защиты");
        this.graph = graph;
    }

    @Override public synchronized void add(T entity) { graph.add(entity); }
    @Override public synchronized void add(T entity, Trace trace) { graph.add(entity, trace); }
    @Override public synchronized T findByKey(K key) { return graph.findByKey(key); }
    @Override public synchronized T findByKey(K key, Trace trace) { return graph.findByKey(key, trace); }
    @Override public synchronized boolean removeByKey(K key) { return graph.removeByKey(key); }
    @Override public synchronized boolean removeByKey(K key, Trace trace) { return graph.removeByKey(key, trace); }
    @Override public synchronized boolean replaceByKey(K key, T entity) { return graph.replaceByKey(key, entity); }
    @Override public synchronized int size() { return graph.size(); }

    @Override public synchronized void addEdge(K first, K second, int strength) { graph.addEdge(first, second, strength); }
    @Override public synchronized boolean removeEdge(K first, K second) { return graph.removeEdge(first, second); }
    @Override public synchronized int edgeStrength(K first, K second) { return graph.edgeStrength(first, second); }
    @Override public synchronized int edgeCount() { return graph.edgeCount(); }
    @Override public synchronized int degree(K key) { return graph.degree(key); }

    @Override public synchronized Chain<K> shortestChain(K from, K to, Trace trace) {
        return graph.shortestChain(from, to, trace);
    }

    @Override public synchronized void forEachVertex(int limit, Visitor<T> visitor) { graph.forEachVertex(limit, visitor); }
    @Override public synchronized void forEachEdge(EdgeVisitor<K> visitor) { graph.forEachEdge(visitor); }
    @Override public synchronized void forEachNeighbor(K key, EdgeVisitor<K> visitor) { graph.forEachNeighbor(key, visitor); }
    @Override public synchronized void clear() { graph.clear(); }

    @Override public synchronized int bucketCount() { return graph.bucketCount(); }
    @Override public synchronized Object[] bucketKeys(int bucket, int limit) { return graph.bucketKeys(bucket, limit); }

    @Override public synchronized int countVertices() { return graph.countVertices(); }
    @Override public synchronized int countNeighbors(K key) { return graph.countNeighbors(key); }
}
