package shortestpath;

import graph.Graph;

/** A single-source shortest path algorithm. */
public interface ShortestPathAlgorithm {

    String name();

    /**
     * Finds a shortest path from the vertex with index {@code source} to every vertex.
     *
     * @throws IllegalArgumentException if the source or the graph cannot be handled
     * @throws NegativeCycleException if a negative cycle can be reached from the source
     */
    ShortestPaths run(Graph graph, int source);
}
