package ru.university.socialnetwork.structure;

public final class RaceExperiment {
    private static final int HUB = 0;
    private static final int TABLE_CAPACITY = 1024;

    public record Result(boolean guarded, int threads, int perThread,
                         int expectedVertices, int countedVertices, int vertexCounter,
                         int expectedEdges, int countedEdges, int edgeCounter,
                         int failures, long millis) {
        public int lostVertices() { return expectedVertices - countedVertices; }
        public int lostEdges() { return expectedEdges - countedEdges; }

        public boolean consistent() {
            return countedVertices == expectedVertices && vertexCounter == expectedVertices
                    && countedEdges == expectedEdges && edgeCounter == expectedEdges && failures == 0;
        }
    }

    private RaceExperiment() { }

    public static Result run(boolean guarded, int threads, int perThread) throws InterruptedException {
        if (threads < 1 || perThread < 1) throw new IllegalArgumentException("Нужен хотя бы один поток и одна вставка");
        GraphStore<Integer, Integer> graph = new AdjacencyGraph<>(value -> value, TABLE_CAPACITY, false);
        if (guarded) graph = new SynchronizedGraph<>(graph);
        graph.add(HUB);
        int[] failures = new int[threads];
        long started = System.nanoTime();
        runPhase(graph, threads, perThread, false, failures);
        runPhase(graph, threads, perThread, true, failures);
        long millis = (System.nanoTime() - started) / 1_000_000;
        int failed = 0;
        for (int count : failures) failed += count;
        int expectedVertices = threads * perThread + 1;
        int expectedEdges = threads * perThread;
        return new Result(guarded, threads, perThread, expectedVertices, graph.countVertices(), graph.size(),
                expectedEdges, graph.countNeighbors(HUB), graph.edgeCount(), failed, millis);
    }

    private static void runPhase(GraphStore<Integer, Integer> graph, int threads, int perThread,
                                 boolean edges, int[] failures) throws InterruptedException {
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = worker(graph, t, perThread, edges, failures);
            workers[t].start();
        }
        for (Thread worker : workers) worker.join();
    }

    private static Thread worker(GraphStore<Integer, Integer> graph, int thread, int perThread,
                                 boolean edges, int[] failures) {
        return new Thread(() -> {
            for (int i = 0; i < perThread; i++) {
                int key = 1 + thread * perThread + i;
                try {
                    if (edges) graph.addEdge(HUB, key, 1);
                    else graph.add(key);
                } catch (RuntimeException e) {
                    failures[thread]++;
                }
            }
        }, "race-" + thread);
    }
}
