package shortestpath;

import graph.Edge;
import graph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Bellman-Ford algorithm, the dynamic programming approach. With d(k, v)
 * the length of the shortest path to v that uses at most k edges,
 * <pre>
 *   d(k, v) = min(d(k-1, v), min over edges (u, v) of d(k-1, u) + w(u, v))
 * </pre>
 * Pass k relaxes every edge once and so computes row k from row k-1; a single
 * row is kept and updated in place. V - 1 passes are enough, and the algorithm
 * stops earlier once a pass changes nothing. Negative weights are allowed and
 * negative cycles are detected. Runs in O(VE).
 */
public final class BellmanFord implements ShortestPathAlgorithm {

    @Override
    public String name() {
        return "Bellman-Ford";
    }

    @Override
    public ShortestPaths run(Graph graph, int source) {
        if (source < 0 || source >= graph.vertexCount()) {
            throw new IllegalArgumentException("Source vertex " + source + " is not in the graph");
        }

        int vertexCount = graph.vertexCount();
        List<Edge> edges = graph.arcs();
        long[] distance = new long[vertexCount];
        int[] predecessor = new int[vertexCount];
        Arrays.fill(distance, ShortestPaths.UNREACHABLE);
        Arrays.fill(predecessor, ShortestPaths.NO_PREDECESSOR);
        distance[source] = 0;

        int passes = 0;
        long distanceUpdates = 0;

        boolean improved = true;
        while (improved && passes < vertexCount - 1) {
            improved = false;
            passes++;
            for (Edge edge : edges) {
                if (relax(edge, distance, predecessor)) {
                    improved = true;
                    distanceUpdates++;
                }
            }
        }

        // Still improving after V - 1 passes: one more pass tells whether the
        // distances are final or a negative cycle keeps lowering them.
        if (improved) {
            passes++;
            for (Edge edge : edges) {
                if (relax(edge, distance, predecessor)) {
                    throw new NegativeCycleException(findCycle(graph, predecessor, edge.to()));
                }
            }
        }

        long edgesExamined = (long) passes * edges.size();
        return new ShortestPaths(name(), graph, source, distance, predecessor,
                passes, edgesExamined, distanceUpdates);
    }

    /** Lowers the distance of the edge's head if the route through its tail is shorter. */
    private static boolean relax(Edge edge, long[] distance, int[] predecessor) {
        int u = edge.from();
        int v = edge.to();
        if (distance[u] == ShortestPaths.UNREACHABLE) {
            return false;
        }
        long throughU = distance[u] + edge.weight();
        if (throughU < distance[v]) {
            distance[v] = throughU;
            predecessor[v] = u;
            return true;
        }
        return false;
    }

    /** Recovers the negative cycle behind a vertex that was still improving in pass V. */
    private static List<String> findCycle(Graph graph, int[] predecessor, int start) {
        // V steps back along the predecessor links always end on the cycle itself.
        int onCycle = start;
        for (int step = 0; step < graph.vertexCount(); step++) {
            onCycle = predecessor[onCycle];
        }

        List<String> cycle = new ArrayList<>();
        int v = onCycle;
        do {
            cycle.add(graph.nameOf(v));
            v = predecessor[v];
        } while (v != onCycle);
        cycle.add(graph.nameOf(onCycle));

        Collections.reverse(cycle); // predecessor links point backwards
        return cycle;
    }
}
