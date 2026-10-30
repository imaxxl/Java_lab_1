package ru.university.socialnetwork.structure;

public final class Trace {
    private static final int START_CAPACITY = 16;

    private Object[] keys = new Object[START_CAPACITY];
    private int count;
    private Object result;

    public void clear() {
        for (int i = 0; i < count; i++) keys[i] = null;
        count = 0;
        result = null;
    }

    public void visit(Object key) {
        if (count == keys.length) {
            Object[] bigger = new Object[keys.length * 2];
            System.arraycopy(keys, 0, bigger, 0, count);
            keys = bigger;
        }
        keys[count++] = key;
    }

    public void markResult(Object key) { result = key; }

    public int count() { return count; }

    public Object get(int index) {
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException("Нет шага " + index);
        return keys[index];
    }

    public boolean visited(Object key) {
        for (int i = 0; i < count; i++) {
            if (keys[i].equals(key)) return true;
        }
        return false;
    }

    public Object result() { return result; }
}
