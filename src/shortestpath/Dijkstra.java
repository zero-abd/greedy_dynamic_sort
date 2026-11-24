package shortestpath;

import graph.Edge;
import graph.Graph;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Dijkstra's algorithm, the greedy approach: repeatedly settle the closest
 * vertex that is not settled yet, then relax the edges leaving it. The choice
 * is only safe when no weight is negative, so other graphs are rejected.
 * Runs in O((V + E) log V) with a binary heap.
 */
public final class Dijkstra implements ShortestPathAlgorithm {

    @Override
    public String name() {
        return "Dijkstra";
    }

    @Override
    public ShortestPaths run(Graph graph, int source) {
        if (source < 0 || source >= graph.vertexCount()) {
            throw new IllegalArgumentException("Source vertex " + source + " is not in the graph");
        }
        if (graph.hasNegativeWeight()) {
            throw new IllegalArgumentException(
                    "Dijkstra's algorithm requires non-negative edge weights; use Bellman-Ford instead");
        }

        int vertexCount = graph.vertexCount();
        long[] distance = new long[vertexCount];
        int[] predecessor = new int[vertexCount];
        boolean[] settled = new boolean[vertexCount];
        Arrays.fill(distance, ShortestPaths.UNREACHABLE);
        Arrays.fill(predecessor, ShortestPaths.NO_PREDECESSOR);

        int settledCount = 0;
        long edgesExamined = 0;
        long distanceUpdates = 0;

        PriorityQueue<QueueEntry> frontier = new PriorityQueue<>();
        distance[source] = 0;
        frontier.add(new QueueEntry(source, 0));

        while (!frontier.isEmpty()) {
            // Greedy choice: the closest vertex that is not settled yet.
            int u = frontier.poll().vertex;
            if (settled[u]) {
                continue; // outdated entry, u was already settled with a smaller distance
            }
            settled[u] = true;
            settledCount++;

            for (Edge edge : graph.edgesFrom(u)) {
                edgesExamined++;
                int v = edge.to();
                long throughU = distance[u] + edge.weight();
                if (throughU < distance[v]) {
                    distance[v] = throughU;
                    predecessor[v] = u;
                    frontier.add(new QueueEntry(v, throughU));
                    distanceUpdates++;
                }
            }
        }

        return new ShortestPaths(name(), graph, source, distance, predecessor,
                settledCount, edgesExamined, distanceUpdates);
    }

    /**
     * A vertex with the distance it had when queued. PriorityQueue cannot lower
     * a key, so each improvement adds a new entry and old ones are skipped.
     */
    private static final class QueueEntry implements Comparable<QueueEntry> {

        private final int vertex;
        private final long distance;

        QueueEntry(int vertex, long distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        @Override
        public int compareTo(QueueEntry other) {
            return Long.compare(distance, other.distance);
        }
    }
}
