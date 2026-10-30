package ru.university.socialnetwork.gui;

import ru.university.socialnetwork.structure.Chain;
import ru.university.socialnetwork.structure.Trace;

record Highlight(int[] visited, int[] path, int focus, String caption) {
    static final int NO_FOCUS = -1;
    static final Highlight NONE = new Highlight(new int[0], new int[0], NO_FOCUS, "");

    static Highlight of(Trace trace, Chain<Integer> chain, int focus, String caption) {
        int[] visited = new int[trace == null ? 0 : trace.count()];
        for (int i = 0; i < visited.length; i++) visited[i] = (Integer) trace.get(i);
        int[] path = new int[chain == null ? 0 : chain.length()];
        for (int i = 0; i < path.length; i++) path[i] = chain.get(i);
        return new Highlight(visited, path, focus, caption);
    }

    static Highlight caption(String text) { return new Highlight(new int[0], new int[0], NO_FOCUS, text); }

    int visitOrder(int id) {
        for (int i = 0; i < visited.length; i++) {
            if (visited[i] == id) return i + 1;
        }
        return 0;
    }

    boolean onPath(int id) {
        for (int step : path) {
            if (step == id) return true;
        }
        return false;
    }

    boolean pathUses(int first, int second) {
        for (int i = 0; i + 1 < path.length; i++) {
            if ((path[i] == first && path[i + 1] == second) || (path[i] == second && path[i + 1] == first)) return true;
        }
        return false;
    }
}
