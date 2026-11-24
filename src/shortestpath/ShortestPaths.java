package shortestpath;

import graph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The result of a shortest path algorithm: the distance from the source to
 * every vertex, the shortest-path tree (as predecessor links), and counters
 * that describe how much work the algorithm did.
 */
public final class ShortestPaths {

    public static final long UNREACHABLE = Long.MAX_VALUE;

    static final int NO_PREDECESSOR = -1;

    private final String algorithm;
    private final Graph graph;
    private final int source;
    private final long[] distance;
    private final int[] predecessor;
    private final int rounds;
    private final long edgesExamined;
    private final long distanceUpdates;

    ShortestPaths(String algorithm, Graph graph, int source, long[] distance, int[] predecessor,
                  int rounds, long edgesExamined, long distanceUpdates) {
        this.algorithm = algorithm;
        this.graph = graph;
        this.source = source;
        this.distance = distance;
        this.predecessor = predecessor;
        this.rounds = rounds;
        this.edgesExamined = edgesExamined;
        this.distanceUpdates = distanceUpdates;
    }

    public String algorithm() {
        return algorithm;
    }

    public Graph graph() {
        return graph;
    }

    public int source() {
        return source;
    }

    public boolean isReachable(int vertex) {
        return distance[vertex] != UNREACHABLE;
    }

    /** Returns {@link #UNREACHABLE} if there is no path to the vertex. */
    public long distanceTo(int vertex) {
        return distance[vertex];
    }

    public long distanceTo(String name) {
        return distanceTo(requireVertex(name));
    }

    /** The vertices from the source to {@code vertex}, or an empty list if it is unreachable. */
    public List<String> pathTo(int vertex) {
        List<String> path = new ArrayList<>();
        if (!isReachable(vertex)) {
            return path;
        }
        for (int v = vertex; v != NO_PREDECESSOR; v = predecessor[v]) {
            path.add(graph.nameOf(v));
        }
        Collections.reverse(path);
        return path;
    }

    public List<String> pathTo(String name) {
        return pathTo(requireVertex(name));
    }

    public boolean sameDistancesAs(ShortestPaths other) {
        return Arrays.equals(distance, other.distance);
    }

    /** Vertices settled (Dijkstra) or passes over the edge list (Bellman-Ford). */
    public int rounds() {
        return rounds;
    }

    public long edgesExamined() {
        return edgesExamined;
    }

    public long distanceUpdates() {
        return distanceUpdates;
    }

    private int requireVertex(String name) {
        int vertex = graph.indexOf(name);
        if (vertex < 0) {
            throw new IllegalArgumentException("Unknown vertex: " + name);
        }
        return vertex;
    }
}
