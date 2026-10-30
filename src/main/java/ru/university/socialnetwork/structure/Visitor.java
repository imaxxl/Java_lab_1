package ru.university.socialnetwork.structure;

@FunctionalInterface
public interface Visitor<T> {
    void visit(T value);
}
