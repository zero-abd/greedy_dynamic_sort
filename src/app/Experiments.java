package app;

import graph.Graph;
import graph.GraphGenerator;
import graph.GraphReader;
import shortestpath.BellmanFord;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathAlgorithm;
import shortestpath.ShortestPaths;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Prints the tables of docs/REPORT.md: both algorithms on the same graphs,
 * compared by edges examined and by running time in microseconds.
 */
public final class Experiments {

    /** The fastest of these runs is reported: the one least disturbed by other activity. */
    private static final int TIMED_RUNS = 25;
    /** Untimed runs come first so that the JIT compiler has done its work. */
    private static final long WARM_UP_NANOS = 200_000_000L;

    private static final ShortestPathAlgorithm DIJKSTRA = new Dijkstra();
    private static final ShortestPathAlgorithm BELLMAN_FORD = new BellmanFord();

    private static final String ROW_FORMAT = "%7s %7s | %9s %9s | %6s %9s %9s | %7s %7s%n";

    private Experiments() {
    }

    public static void main(String[] args) throws IOException {
        campusMap();
        randomSparseGraphs();
        streetGrids();
        worstCaseForBellmanFord();
    }

    private static void campusMap() throws IOException {
        Graph campus = GraphReader.read(Paths.get(CampusNavigator.DEFAULT_GRAPH_FILE));
        printTitle("Experiment 1: campus map, source " + CampusNavigator.DEFAULT_SOURCE);
        printRow(campus, campus.indexOf(CampusNavigator.DEFAULT_SOURCE));
    }

    private static void randomSparseGraphs() {
        printTitle("Experiment 2: random connected graphs, E = 4V, weights 1..1000");
        for (int vertices = 500; vertices <= 32000; vertices *= 2) {
            Graph graph = GraphGenerator.randomConnected(vertices, 4 * vertices, 1000, vertices);
            printRow(graph, 0);
        }
    }

    private static void streetGrids() {
        printTitle("Experiment 3: n x n street grids, weights 1..1000, source in the middle");
        for (int side = 25; side <= 200; side *= 2) {
            Graph graph = GraphGenerator.grid(side, side, 1000, side);
            int middle = (side / 2) * side + side / 2;
            printRow(graph, middle);
        }
    }

    private static void worstCaseForBellmanFord() {
        printTitle("Experiment 4: path graph, edges stored far end first (Bellman-Ford worst case)");
        for (int vertices = 500; vertices <= 4000; vertices *= 2) {
            printRow(GraphGenerator.reversedChain(vertices), 0);
        }
    }

    private static void printTitle(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println();
        System.out.printf(ROW_FORMAT, "", "", "Dijkstra", "Dijkstra", "B-F", "B-F", "B-F", "ratio", "ratio");
        System.out.printf(ROW_FORMAT, "V", "E", "edges", "time us", "passes", "edges", "time us", "edges", "time");
    }

    private static void printRow(Graph graph, int source) {
        ShortestPaths dijkstra = DIJKSTRA.run(graph, source);
        ShortestPaths bellmanFord = BELLMAN_FORD.run(graph, source);
        if (!dijkstra.sameDistancesAs(bellmanFord)) {
            throw new IllegalStateException("The two algorithms disagree on a graph with "
                    + graph.vertexCount() + " vertices");
        }
        double dijkstraMicros = fastestMicros(DIJKSTRA, graph, source);
        double bellmanFordMicros = fastestMicros(BELLMAN_FORD, graph, source);

        System.out.printf(ROW_FORMAT,
                graph.vertexCount(), graph.edgeCount(),
                dijkstra.edgesExamined(), oneDecimal(dijkstraMicros),
                bellmanFord.rounds(), bellmanFord.edgesExamined(), oneDecimal(bellmanFordMicros),
                oneDecimal((double) bellmanFord.edgesExamined() / dijkstra.edgesExamined()) + "x",
                oneDecimal(bellmanFordMicros / dijkstraMicros) + "x");
    }

    private static double fastestMicros(ShortestPathAlgorithm algorithm, Graph graph, int source) {
        long warmUpEnd = System.nanoTime() + WARM_UP_NANOS;
        do {
            algorithm.run(graph, source);
        } while (System.nanoTime() < warmUpEnd);

        long fastest = Long.MAX_VALUE;
        for (int run = 0; run < TIMED_RUNS; run++) {
            long start = System.nanoTime();
            algorithm.run(graph, source);
            fastest = Math.min(fastest, System.nanoTime() - start);
        }
        return fastest / 1_000.0;
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
