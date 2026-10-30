package ru.university.socialnetwork.structure;

public interface GraphStore<K, T> extends EntityStore<K, T> {
    void add(T entity, Trace trace);
    T findByKey(K key, Trace trace);
    boolean removeByKey(K key, Trace trace);
    boolean replaceByKey(K key, T entity);

    void addEdge(K first, K second, int strength);
    boolean removeEdge(K first, K second);
    int edgeStrength(K first, K second);
    int edgeCount();
    int degree(K key);

    Chain<K> shortestChain(K from, K to, Trace trace);

    void forEachVertex(int limit, Visitor<T> visitor);
    void forEachEdge(EdgeVisitor<K> visitor);
    void forEachNeighbor(K key, EdgeVisitor<K> visitor);
    void clear();

    int bucketCount();
    Object[] bucketKeys(int bucket, int limit);

    int countVertices();
    int countNeighbors(K key);
}
