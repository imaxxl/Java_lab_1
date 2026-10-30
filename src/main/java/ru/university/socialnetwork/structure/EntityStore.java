package ru.university.socialnetwork.structure;

public interface EntityStore<K, T> {
    void add(T entity);
    T findByKey(K key);
    boolean removeByKey(K key);
    int size();
}
