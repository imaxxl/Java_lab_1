package ru.university.socialnetwork.structure;

@FunctionalInterface
public interface EdgeVisitor<K> {
    void visit(K from, K to, int strength);
}
