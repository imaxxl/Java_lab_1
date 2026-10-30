package ru.university.socialnetwork.structure;

@FunctionalInterface
public interface KeyExtractor<K, T> {
    K keyOf(T entity);
}
