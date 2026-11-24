package graph;

import java.util.Random;

/** Builds reproducible synthetic graphs for the tests and the experiments. */
public final class GraphGenerator {

    private GraphGenerator() {
    }

    /** A random spanning tree plus random extra edges, with weights in 1..maxWeight. */
    public static Graph randomConnected(int vertices, int edges, int maxWeight, long seed) {
        Random random = new Random(seed);
        Graph graph = Graph.undirected();
        graph.addVertex(vertexName(0));
        for (int v = 1; v < vertices; v++) {
            int earlier = random.nextInt(v);
            graph.addEdge(vertexName(earlier), vertexName(v), 1 + random.nextInt(maxWeight));
        }
        while (vertices > 1 && graph.edgeCount() < edges) {
            int a = random.nextInt(vertices);
            int b = random.nextInt(vertices);
            if (a != b) {
                graph.addEdge(vertexName(a), vertexName(b), 1 + random.nextInt(maxWeight));
            }
        }
        return graph;
    }

    /** A street grid; the junction in row r and column c has index r * columns + c. */
    public static Graph grid(int rows, int columns, int maxWeight, long seed) {
        Random random = new Random(seed);
        Graph graph = Graph.undirected();
        for (int v = 0; v < rows * columns; v++) {
            graph.addVertex(vertexName(v));
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                String here = vertexName(row * columns + column);
                if (column + 1 < columns) {
                    graph.addEdge(here, vertexName(row * columns + column + 1), 1 + random.nextInt(maxWeight));
                }
                if (row + 1 < rows) {
                    graph.addEdge(here, vertexName((row + 1) * columns + column), 1 + random.nextInt(maxWeight));
                }
            }
        }
        return graph;
    }

    /**
     * A path v0 - v1 - ... whose edges are stored far end first. From v0 this is
     * the worst case for Bellman-Ford: each pass reaches only one more vertex.
     */
    public static Graph reversedChain(int vertices) {
        Graph graph = Graph.undirected();
        for (int v = 0; v < vertices; v++) {
            graph.addVertex(vertexName(v));
        }
        for (int v = vertices - 2; v >= 0; v--) {
            graph.addEdge(vertexName(v), vertexName(v + 1), 1);
        }
        return graph;
    }

    public static String vertexName(int index) {
        return "v" + index;
    }
}
