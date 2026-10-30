package ru.university.socialnetwork.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentAccessTest {
    @Test
    void guardedGraphKeepsEveryVertexAndEdge() throws InterruptedException {
        RaceExperiment.Result result = RaceExperiment.run(true, 4, 4000);
        assertTrue(result.consistent(), result.toString());
        assertEquals(16_001, result.countedVertices());
        assertEquals(16_000, result.countedEdges());
    }

    @Test
    void unguardedGraphNeverReportsMoreThanWasAdded() throws InterruptedException {
        RaceExperiment.Result result = RaceExperiment.run(false, 4, 4000);
        assertTrue(result.countedVertices() <= result.expectedVertices());
        assertTrue(result.countedEdges() <= result.expectedEdges());
        assertTrue(result.lostVertices() >= 0 && result.lostEdges() >= 0);
    }

    @Test
    void experimentRejectsNonsenseParameters() {
        assertThrows(IllegalArgumentException.class, () -> RaceExperiment.run(true, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> RaceExperiment.run(true, 2, 0));
    }

    @Test
    void synchronizedWrapperDelegatesEveryOperation() {
        SynchronizedGraph<Integer, Integer> graph = new SynchronizedGraph<>(new AdjacencyGraph<>(value -> value));
        graph.add(1);
        graph.add(2);
        graph.add(3);
        graph.addEdge(1, 2, 4);
        graph.addEdge(2, 3, 5);
        assertEquals(3, graph.size());
        assertEquals(2, graph.edgeCount());
        assertEquals(4, graph.edgeStrength(2, 1));
        assertEquals(2, graph.degree(2));
        assertEquals(3, graph.shortestChain(1, 3, null).length());
        assertTrue(graph.removeEdge(1, 2));
        assertTrue(graph.removeByKey(3));
        assertEquals(2, graph.countVertices());
        assertEquals(0, graph.countNeighbors(2));
        assertThrows(IllegalArgumentException.class, () -> new SynchronizedGraph<Integer, Integer>(null));
    }

    @Test
    void structurePackageDoesNotUseJavaUtil() throws IOException {
        Path folder = Path.of("src/main/java/ru/university/socialnetwork/structure");
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file);
                assertFalse(text.contains("java.util"), file.getFileName() + " использует java.util");
            }
        }
    }
}
