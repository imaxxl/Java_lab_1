package ru.university.socialnetwork.bench;

import ru.university.socialnetwork.model.Profile;
import ru.university.socialnetwork.structure.AdjacencyGraph;
import ru.university.socialnetwork.structure.GraphStore;
import ru.university.socialnetwork.structure.SynchronizedGraph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class Benchmark {
    private static final int[] SIZES = {10_000, 100_000};
    private static final int WARM_UP_SIZE = 20_000;
    private static final int EDGES_PER_VERTEX = 3;
    private static final int CHAIN_QUERIES = 100;
    private static final int REPEATS = 5;
    private static final long SEED = 2026;
    private static final int STRENGTH_RANGE = 9;
    private static final String[] OPERATIONS = {"Вставка вершин", "Вставка рёбер (3 на вершину)", "Поиск вершины",
            "Цепочка знакомств, BFS (100 запросов)", "Удаление вершин"};
    private static final int INSERT = 0;
    private static final int EDGES = 1;
    private static final int SEARCH = 2;
    private static final int CHAIN = 3;
    private static final int REMOVE = 4;

    private interface Subject {
        void add(Profile profile);
        Profile find(int id);
        void remove(int id);
        void addEdge(int first, int second, int strength);
        int chain(int from, int to);
    }

    private record Data(Profile[] profiles, int[] searchOrder, int[] removeOrder, int[][] edges, int[][] queries) { }

    private record Outcome(long[] nanos, int[] chainLengths) { }

    private Benchmark() { }

    public static void main(String[] args) {
        System.out.println("Среда: " + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + ", ядер " + Runtime.getRuntime().availableProcessors() + ", "
                + System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        System.out.println("Медиана из " + REPEATS + " прогонов, время в миллисекундах.\n");
        run(WARM_UP_SIZE, 1, false);
        StringBuilder csv = new StringBuilder("n;операция;своя;своя_synchronized;java_util;своя_мкс_на_операцию\n");
        for (int size : SIZES) {
            long[][] table = run(size, REPEATS, true);
            System.out.println("n = " + size);
            System.out.println("| Операция | Своя структура | Своя + synchronized | java.util | Своя, мкс на операцию |");
            System.out.println("| --- | ---: | ---: | ---: | ---: |");
            for (int op = 0; op < OPERATIONS.length; op++) {
                int count = op == CHAIN ? CHAIN_QUERIES : op == EDGES ? size * EDGES_PER_VERTEX : size;
                double micros = table[0][op] / 1000.0 / count;
                System.out.printf("| %s | %s | %s | %s | %.2f |%n", OPERATIONS[op], ms(table[0][op]),
                        ms(table[1][op]), ms(table[2][op]), micros);
                csv.append(size).append(';').append(OPERATIONS[op]).append(';').append(ms(table[0][op]))
                        .append(';').append(ms(table[1][op])).append(';').append(ms(table[2][op]))
                        .append(';').append(String.format("%.2f", micros)).append('\n');
            }
            System.out.println();
        }
        System.out.println("CSV:\n" + csv);
    }

    private static String ms(long nanos) { return String.format("%.1f", nanos / 1_000_000.0); }

    private static long[][] run(int size, int repeats, boolean verify) {
        Data data = prepare(size);
        long[][][] samples = new long[3][repeats][];
        int[][] lengths = new int[3][];
        for (int repeat = 0; repeat < repeats; repeat++) {
            for (int kind = 0; kind < 3; kind++) {
                System.gc();
                Outcome outcome = measure(subject(kind), data);
                samples[kind][repeat] = outcome.nanos();
                lengths[kind] = outcome.chainLengths();
            }
        }
        if (verify) {
            if (!Arrays.equals(lengths[0], lengths[2]) || !Arrays.equals(lengths[0], lengths[1])) {
                throw new IllegalStateException("Реализации нашли цепочки разной длины");
            }
        }
        long[][] median = new long[3][OPERATIONS.length];
        for (int kind = 0; kind < 3; kind++) {
            for (int op = 0; op < OPERATIONS.length; op++) {
                long[] values = new long[repeats];
                for (int repeat = 0; repeat < repeats; repeat++) values[repeat] = samples[kind][repeat][op];
                Arrays.sort(values);
                median[kind][op] = values[repeats / 2];
            }
        }
        return median;
    }

    private static Subject subject(int kind) {
        return switch (kind) {
            case 0 -> own(new AdjacencyGraph<>(Profile::getId));
            case 1 -> own(new SynchronizedGraph<>(new AdjacencyGraph<>(Profile::getId)));
            default -> analog(new JavaUtilGraph());
        };
    }

    private static Subject own(GraphStore<Integer, Profile> graph) {
        return new Subject() {
            @Override public void add(Profile profile) { graph.add(profile); }
            @Override public Profile find(int id) { return graph.findByKey(id); }
            @Override public void remove(int id) { graph.removeByKey(id); }
            @Override public void addEdge(int first, int second, int strength) { graph.addEdge(first, second, strength); }
            @Override public int chain(int from, int to) { return graph.shortestChain(from, to, null).length(); }
        };
    }

    private static Subject analog(JavaUtilGraph graph) {
        return new Subject() {
            @Override public void add(Profile profile) { graph.add(profile); }
            @Override public Profile find(int id) { return graph.find(id); }
            @Override public void remove(int id) { graph.remove(id); }
            @Override public void addEdge(int first, int second, int strength) { graph.addEdge(first, second, strength); }
            @Override public int chain(int from, int to) { return graph.chainLength(from, to); }
        };
    }

    private static Outcome measure(Subject subject, Data data) {
        long[] nanos = new long[OPERATIONS.length];
        long started = System.nanoTime();
        for (Profile profile : data.profiles()) subject.add(profile);
        nanos[INSERT] = System.nanoTime() - started;

        started = System.nanoTime();
        for (int[] edge : data.edges()) subject.addEdge(edge[0], edge[1], edge[2]);
        nanos[EDGES] = System.nanoTime() - started;

        int found = 0;
        started = System.nanoTime();
        for (int id : data.searchOrder()) {
            if (subject.find(id) != null) found++;
        }
        nanos[SEARCH] = System.nanoTime() - started;
        if (found != data.profiles().length) throw new IllegalStateException("Поиск потерял записи");

        int[] lengths = new int[data.queries().length];
        started = System.nanoTime();
        for (int i = 0; i < lengths.length; i++) lengths[i] = subject.chain(data.queries()[i][0], data.queries()[i][1]);
        nanos[CHAIN] = System.nanoTime() - started;

        started = System.nanoTime();
        for (int id : data.removeOrder()) subject.remove(id);
        nanos[REMOVE] = System.nanoTime() - started;
        if (subject.find(data.removeOrder()[0]) != null) throw new IllegalStateException("Удаление не сработало");
        return new Outcome(nanos, lengths);
    }

    private static Data prepare(int size) {
        Random random = new Random(SEED + size);
        Profile[] profiles = new Profile[size];
        for (int i = 0; i < size; i++) profiles[i] = new Profile(i + 1, "Имя" + i, "Город", 2000);
        int[] search = shuffled(size, random);
        int[] remove = shuffled(size, random);
        int[][] edges = new int[size * EDGES_PER_VERTEX][];
        Set<Long> used = new HashSet<>();
        for (int i = 0; i < edges.length; ) {
            int a = 1 + random.nextInt(size);
            int b = 1 + random.nextInt(size);
            long pair = ((long) Math.min(a, b) << 32) | Math.max(a, b);
            if (a != b && used.add(pair)) edges[i++] = new int[]{a, b, 1 + random.nextInt(STRENGTH_RANGE)};
        }
        int[][] queries = new int[CHAIN_QUERIES][];
        for (int i = 0; i < queries.length; i++) queries[i] = new int[]{1 + random.nextInt(size), 1 + random.nextInt(size)};
        return new Data(profiles, search, remove, edges, queries);
    }

    private static int[] shuffled(int size, Random random) {
        int[] ids = new int[size];
        for (int i = 0; i < size; i++) ids[i] = i + 1;
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = ids[i];
            ids[i] = ids[j];
            ids[j] = swap;
        }
        return ids;
    }
}
