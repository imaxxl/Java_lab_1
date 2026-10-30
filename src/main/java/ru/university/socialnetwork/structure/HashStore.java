package ru.university.socialnetwork.structure;

public class HashStore<K, T> implements EntityStore<K, T> {
    static final int DEFAULT_CAPACITY = 16;
    private static final int MAX_CAPACITY = 1 << 30;
    private static final int LOAD_NUMERATOR = 3;
    private static final int LOAD_DENOMINATOR = 4;
    private static final int HASH_SHIFT = 16;

    private static final class Node<K, T> {
        final K key;
        final int hash;
        T value;
        Node<K, T> next;
        Node<K, T> before;
        Node<K, T> after;

        Node(K key, int hash, T value) {
            this.key = key;
            this.hash = hash;
            this.value = value;
        }
    }

    private final KeyExtractor<K, T> keys;
    private final boolean autoResize;
    private Node<K, T>[] buckets;
    private Node<K, T> first;
    private Node<K, T> last;
    private int size;

    public HashStore(KeyExtractor<K, T> keys) {
        this(keys, DEFAULT_CAPACITY, true);
    }

    public HashStore(KeyExtractor<K, T> keys, int capacity, boolean autoResize) {
        if (keys == null) throw new IllegalArgumentException("Не задана функция ключа");
        this.keys = keys;
        this.autoResize = autoResize;
        this.buckets = newTable(tableSize(capacity));
    }

    @Override
    public void add(T entity) { add(entity, null); }

    public void add(T entity, Trace trace) {
        if (entity == null) throw new IllegalArgumentException("Пустую запись добавить нельзя");
        K key = keys.keyOf(entity);
        int hash = hash(key);
        int index = indexFor(hash, buckets.length);
        for (Node<K, T> node = buckets[index]; node != null; node = node.next) {
            if (trace != null) trace.visit(node.key);
            if (node.hash == hash && node.key.equals(key)) {
                throw new IllegalArgumentException("Ключ уже существует: " + key);
            }
        }
        Node<K, T> created = new Node<>(key, hash, entity);
        created.next = buckets[index];
        buckets[index] = created;
        linkLast(created);
        size++;
        if (trace != null) trace.markResult(key);
        if (autoResize && size > threshold()) resize();
    }

    @Override
    public T findByKey(K key) { return findByKey(key, null); }

    public T findByKey(K key, Trace trace) {
        Node<K, T> node = findNode(key, trace);
        return node == null ? null : node.value;
    }

    @Override
    public boolean removeByKey(K key) { return removeByKey(key, null); }

    public boolean removeByKey(K key, Trace trace) {
        int hash = hash(key);
        int index = indexFor(hash, buckets.length);
        Node<K, T> previous = null;
        for (Node<K, T> node = buckets[index]; node != null; node = node.next) {
            if (trace != null) trace.visit(node.key);
            if (node.hash == hash && node.key.equals(key)) {
                if (previous == null) buckets[index] = node.next;
                else previous.next = node.next;
                unlink(node);
                size--;
                if (trace != null) trace.markResult(node.key);
                return true;
            }
            previous = node;
        }
        return false;
    }

    public boolean replaceByKey(K key, T entity) {
        if (entity == null || !key.equals(keys.keyOf(entity))) {
            throw new IllegalArgumentException("Ключ новой записи должен совпадать с прежним");
        }
        Node<K, T> node = findNode(key, null);
        if (node == null) return false;
        node.value = entity;
        return true;
    }

    @Override
    public int size() { return size; }

    public void forEach(int limit, Visitor<T> visitor) {
        int shown = 0;
        for (Node<K, T> node = first; node != null && shown < limit; node = node.after) {
            visitor.visit(node.value);
            shown++;
        }
    }

    public void clear() {
        buckets = newTable(DEFAULT_CAPACITY);
        first = null;
        last = null;
        size = 0;
    }

    public int capacity() { return buckets.length; }

    public Object[] bucketKeys(int bucket, int limit) {
        if (bucket < 0 || bucket >= buckets.length) throw new IndexOutOfBoundsException("Нет корзины " + bucket);
        int length = 0;
        for (Node<K, T> node = buckets[bucket]; node != null && length < limit; node = node.next) length++;
        Object[] result = new Object[length];
        Node<K, T> node = buckets[bucket];
        for (int i = 0; i < length; i++, node = node.next) result[i] = node.key;
        return result;
    }

    public int bucketOf(K key) { return indexFor(hash(key), buckets.length); }

    public int reachableCount() {
        int count = 0;
        for (Node<K, T> head : buckets) {
            for (Node<K, T> node = head; node != null; node = node.next) count++;
        }
        return count;
    }

    private Node<K, T> findNode(K key, Trace trace) {
        int hash = hash(key);
        for (Node<K, T> node = buckets[indexFor(hash, buckets.length)]; node != null; node = node.next) {
            if (trace != null) trace.visit(node.key);
            if (node.hash == hash && node.key.equals(key)) {
                if (trace != null) trace.markResult(node.key);
                return node;
            }
        }
        return null;
    }

    private int threshold() {
        return (int) ((long) buckets.length * LOAD_NUMERATOR / LOAD_DENOMINATOR);
    }

    private void resize() {
        if (buckets.length >= MAX_CAPACITY) return;
        Node<K, T>[] bigger = newTable(buckets.length * 2);
        for (Node<K, T> node = first; node != null; node = node.after) {
            int index = indexFor(node.hash, bigger.length);
            node.next = bigger[index];
            bigger[index] = node;
        }
        buckets = bigger;
    }

    private void linkLast(Node<K, T> node) {
        node.before = last;
        if (last == null) first = node;
        else last.after = node;
        last = node;
    }

    private void unlink(Node<K, T> node) {
        if (node.before == null) first = node.after;
        else node.before.after = node.after;
        if (node.after == null) last = node.before;
        else node.after.before = node.before;
        node.before = null;
        node.after = null;
    }

    private static int hash(Object key) {
        if (key == null) throw new IllegalArgumentException("Ключ не задан");
        int h = key.hashCode();
        return h ^ (h >>> HASH_SHIFT);
    }

    private static int indexFor(int hash, int length) { return hash & (length - 1); }

    private static int tableSize(int requested) {
        int size = 2;
        while (size < requested && size < MAX_CAPACITY) size <<= 1;
        return size;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <K, T> Node<K, T>[] newTable(int length) {
        return (Node<K, T>[]) new Node[length];
    }
}
