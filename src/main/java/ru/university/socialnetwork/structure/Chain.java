package ru.university.socialnetwork.structure;

/** Результат поиска цепочки знакомств: ключи вершин от начала до конца пути. */
public final class Chain<K> {
    private static final Chain<?> NONE = new Chain<>(new Object[0]);

    private final Object[] keys;

    Chain(Object[] keys) { this.keys = keys; }

    @SuppressWarnings("unchecked")
    public static <K> Chain<K> none() { return (Chain<K>) NONE; }

    public int length() { return keys.length; }

    public boolean isEmpty() { return keys.length == 0; }

    @SuppressWarnings("unchecked")
    public K get(int index) {
        if (index < 0 || index >= keys.length) throw new IndexOutOfBoundsException("Нет вершины " + index);
        return (K) keys[index];
    }
}
