package ru.university.socialnetwork.store;

import ru.university.socialnetwork.model.ProfileType;

public final class StructureSnapshot {
    private final int vertexTotal;
    private final int edgeTotal;
    private final int[] ids;
    private final ProfileType[] types;
    private final int[] edgeFrom;
    private final int[] edgeTo;
    private final int[] edgeStrength;
    private final int bucketTotal;
    private final int[][] buckets;

    StructureSnapshot(int vertexTotal, int edgeTotal, int[] ids, ProfileType[] types,
                      int[] edgeFrom, int[] edgeTo, int[] edgeStrength, int bucketTotal, int[][] buckets) {
        this.vertexTotal = vertexTotal;
        this.edgeTotal = edgeTotal;
        this.ids = ids;
        this.types = types;
        this.edgeFrom = edgeFrom;
        this.edgeTo = edgeTo;
        this.edgeStrength = edgeStrength;
        this.bucketTotal = bucketTotal;
        this.buckets = buckets;
    }

    public int vertexTotal() { return vertexTotal; }
    public int edgeTotal() { return edgeTotal; }
    public int shownVertices() { return ids.length; }
    public int id(int index) { return ids[index]; }
    public ProfileType type(int index) { return types[index]; }
    public int shownEdges() { return edgeFrom.length; }
    public int edgeFrom(int index) { return edgeFrom[index]; }
    public int edgeTo(int index) { return edgeTo[index]; }
    public int edgeStrength(int index) { return edgeStrength[index]; }
    public int bucketTotal() { return bucketTotal; }
    public int shownBuckets() { return buckets.length; }
    public int bucketSize(int bucket) { return buckets[bucket].length; }
    public int bucketKey(int bucket, int position) { return buckets[bucket][position]; }

    public int indexOf(int id) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == id) return i;
        }
        return -1;
    }
}
