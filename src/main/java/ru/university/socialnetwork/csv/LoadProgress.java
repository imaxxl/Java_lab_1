package ru.university.socialnetwork.csv;

@FunctionalInterface
public interface LoadProgress {
    void update(long done, long total);
}
