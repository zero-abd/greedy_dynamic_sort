import app.CampusNavigator;
import graph.Edge;
import graph.Graph;
import graph.GraphFormatException;
import graph.GraphGenerator;
import graph.GraphReader;
import shortestpath.BellmanFord;
import shortestpath.Dijkstra;
import shortestpath.NegativeCycleException;
import shortestpath.ShortestPathAlgorithm;
import shortestpath.ShortestPaths;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Test suite that needs no testing library. Run it from the repository root
 * with {@code java -cp out ShortestPathTests}; the exit code is 1 if a test fails.
 *
 * <p>Results are checked against Floyd-Warshall, a third algorithm that shares
 * no code with the two under test.
 */
public final class ShortestPathTests {

    private static final String CAMPUS_FILE = "data/campus.txt";
    private static final String SOURCE = "Computer Science";

    private static final ShortestPathAlgorithm DIJKSTRA = new Dijkstra();
    private static final ShortestPathAlgorithm BELLMAN_FORD = new BellmanFord();
    private static final List<ShortestPathAlgorithm> BOTH = Arrays.asList(DIJKSTRA, BELLMAN_FORD);

    /** Shortest distances from Computer Science, worked out by hand from the map. */
    private static final Object[][] CAMPUS_DISTANCES = {
        {"Computer Science", 0L},
        {"Burdick", 30L},
        {"Torreyson Library", 40L},
        {"Prince Center", 70L},
        {"Old Main", 70L},
        {"Speech Language Hearing", 130L},
        {"Lewis Science Center", 150L},
        {"Fine Art", 160L},
        {"Police Dept.", 170L},
        {"McAlister Hall", 170L},
        {"Wingo", 220L},
        {"Student Center", 240L},
        {"Maintenance College", 250L},
        {"Student Health Center", 270L},
        {"New Business Building", 270L},
        {"Brewer-Hegeman", 290L},
        {"Oak Tree Apt.", 300L},
        {"College Square", 350L},
        {"Bear Village Apt.", 640L},
    };

    private static int passed;
    private static int failed;

    private ShortestPathTests() {
    }

    public static void main(String[] args) {
        run("undirected edges can be used in both directions", ShortestPathTests::undirectedEdges);
        run("directed edges can be used in one direction only", ShortestPathTests::directedEdges);
        run("vertex names are normalized", ShortestPathTests::vertexNames);
        run("reader loads the campus map", ShortestPathTests::readerLoadsCampusMap);
        run("reader understands comments, directives and a byte-order mark", ShortestPathTests::readerSyntax);
        run("reader rejects malformed files with a clear message", ShortestPathTests::readerRejectsMalformedFiles);

        run("campus distances match the hand-computed values", ShortestPathTests::campusDistances);
        run("campus routes are the expected ones", ShortestPathTests::campusRoutes);
        run("campus results are correct from every start building", ShortestPathTests::campusAllSources);

        run("random undirected graphs and a grid match Floyd-Warshall", ShortestPathTests::randomUndirectedGraphs);
        run("random directed graphs match Floyd-Warshall", ShortestPathTests::randomDirectedGraphs);
        run("random graphs with negative weights match Floyd-Warshall", ShortestPathTests::randomNegativeWeights);
        run("random graphs: a negative cycle is reported exactly when one is reachable",
                ShortestPathTests::randomNegativeCycles);
        run("unreachable vertices are reported as unreachable", ShortestPathTests::unreachableVertices);
        run("single vertex, self-loops, parallel and zero-weight edges", ShortestPathTests::edgeCases);
        run("an invalid source is rejected", ShortestPathTests::invalidSource);

        run("Dijkstra refuses negative weights", ShortestPathTests::dijkstraRefusesNegativeWeights);
        run("Bellman-Ford solves the negative-weight example", ShortestPathTests::negativeWeightExample);
        run("Bellman-Ford reports the negative cycle of the example", ShortestPathTests::negativeCycleExample);
        run("a negative cycle the source cannot reach is harmless", ShortestPathTests::unreachableNegativeCycle);
        run("negative cycles of different shapes are recovered", ShortestPathTests::negativeCycleShapes);

        run("Bellman-Ford stops early, and needs every pass in the worst case", ShortestPathTests::passCounts);
        run("work counters are exact on the campus map", ShortestPathTests::workCounters);

        run("command line output matches the README", ShortestPathTests::commandLineMatchesReadme);
        run("command line options", ShortestPathTests::commandLineOptions);
        run("command line errors", ShortestPathTests::commandLineErrors);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---- Graph and reader ----

    private static void undirectedEdges() {
        Graph graph = Graph.undirected();
        graph.addEdge("A", "B", 7);
        int a = graph.indexOf("A");
        int b = graph.indexOf("B");

        check(!graph.isDirected(), "graph should be undirected");
        checkEquals(2, graph.vertexCount(), "vertex count");
        checkEquals(1, graph.edgeCount(), "an undirected edge counts once");
        checkEquals(2, graph.arcs().size(), "an undirected edge is two arcs");
        checkEquals(b, graph.edgesFrom(a).get(0).to(), "A leads to B");
        checkEquals(a, graph.edgesFrom(b).get(0).to(), "B leads to A");
        checkEquals(7, graph.edgesFrom(b).get(0).weight(), "weight of the reverse arc");

        graph.addEdge("A", "A", 3);
        checkEquals(3, graph.arcs().size(), "a self-loop is a single arc");
    }

    private static void directedEdges() {
        Graph graph = Graph.directed();
        graph.addEdge("A", "B", 7);

        check(graph.isDirected(), "graph should be directed");
        checkEquals(1, graph.arcs().size(), "a directed edge is one arc");
        checkEquals(1, graph.edgesFrom(graph.indexOf("A")).size(), "A has one outgoing arc");
        checkEquals(0, graph.edgesFrom(graph.indexOf("B")).size(), "B has no outgoing arc");
    }

    private static void vertexNames() {
        Graph graph = Graph.undirected();
        int first = graph.addVertex("Old Main");

        checkEquals(first, graph.addVertex("  Old   Main "), "same vertex despite extra spaces");
        checkEquals(1, graph.vertexCount(), "no duplicate vertex");
        checkEquals("Old Main", graph.nameOf(first), "stored name");
        checkEquals(-1, graph.indexOf("old main"), "indexOf is case-sensitive");
        checkEquals(first, graph.indexOfIgnoreCase("old  MAIN"), "indexOfIgnoreCase");
        checkEquals(-1, graph.indexOfIgnoreCase("Nowhere"), "unknown vertex, ignoring case");
        checkEquals(-1, graph.indexOf("Nowhere"), "unknown vertex");
        checkEquals(-1, graph.indexOf(null), "null name");
        checkThrows(IllegalArgumentException.class, () -> graph.addVertex("   "), "blank name");
    }

    private static void readerLoadsCampusMap() throws IOException {
        Graph campus = GraphReader.read(Paths.get(CAMPUS_FILE));

        check(!campus.isDirected(), "the campus map is undirected");
        checkEquals(19, campus.vertexCount(), "buildings");
        checkEquals(37, campus.edgeCount(), "walkways");
        check(!campus.hasNegativeWeight(), "no negative distances");
        check(campus.indexOf(SOURCE) >= 0, "Computer Science is on the map");
    }

    private static void readerSyntax() throws IOException {
        String text = "\uFEFF# a comment\n"
                + "\n"
                + "DIRECTED\n"
                + "  A ,  B , 5   # trailing comment\n"
                + "B,C,-2\n";
        Graph graph = GraphReader.read(new StringReader(text), "test");

        check(graph.isDirected(), "the directive is case-insensitive");
        checkEquals(3, graph.vertexCount(), "vertex count");
        checkEquals(2, graph.edgeCount(), "edge count");
        check(graph.hasNegativeWeight(), "negative weight is noticed");
        check(!GraphReader.read(new StringReader("A, B, 1\n"), "test").isDirected(),
                "graphs are undirected unless the file says otherwise");
    }

    private static void readerRejectsMalformedFiles() throws IOException {
        String[][] cases = {
            {"", "does not contain any edges"},
            {"# only a comment\n", "does not contain any edges"},
            {"directed\n", "does not contain any edges"},
            {"A, B\n", "expected \"from, to, weight\""},
            {"A, B, 1, 2\n", "expected \"from, to, weight\""},
            {"A, B, far\n", "not a whole number"},
            {"A, B, 1.5\n", "not a whole number"},
            {", B, 3\n", "a vertex name is missing"},
            {"A, B, 1\ndirected\nC, D, 1\n", "only allowed as the first entry"},
            {"directed\nundirected\nA, B, 1\n", "only allowed as the first entry"},
            {"A, B, 1\nA, B, far\n", "map.txt, line 2"},
        };
        for (String[] malformed : cases) {
            String input = malformed[0].replace("\n", "\\n");
            try {
                GraphReader.read(new StringReader(malformed[0]), "map.txt");
            } catch (GraphFormatException e) {
                check(e.getMessage().contains(malformed[1]),
                        "message for \"" + input + "\" should contain <" + malformed[1] + ">: " + e.getMessage());
                continue;
            }
            throw new AssertionError("malformed input was accepted: \"" + input + "\"");
        }
    }

    // ---- The campus map ----

    private static void campusDistances() throws IOException {
        Graph campus = GraphReader.read(Paths.get(CAMPUS_FILE));
        checkEquals(CAMPUS_DISTANCES.length, campus.vertexCount(), "every building has an expected distance");

        for (ShortestPathAlgorithm algorithm : BOTH) {
            ShortestPaths paths = algorithm.run(campus, campus.indexOf(SOURCE));
            for (Object[] expected : CAMPUS_DISTANCES) {
                String building = (String) expected[0];
                checkEquals(expected[1], paths.distanceTo(building),
                        algorithm.name() + " distance to " + building);
            }
            checkPathsAreConsistent(campus, paths);
        }
    }

    private static void campusRoutes() throws IOException {
        Graph campus = GraphReader.read(Paths.get(CAMPUS_FILE));
        for (ShortestPathAlgorithm algorithm : BOTH) {
            ShortestPaths paths = algorithm.run(campus, campus.indexOf(SOURCE));

            checkEquals(Arrays.asList(SOURCE), paths.pathTo(SOURCE), "route to the source itself");
            checkEquals(Arrays.asList(SOURCE, "Torreyson Library", "Prince Center"),
                    paths.pathTo("Prince Center"),
                    "the detour through the library (70) beats the direct walkway (80)");
            checkEquals(Arrays.asList(SOURCE, "Burdick", "Speech Language Hearing", "Maintenance College"),
                    paths.pathTo("Maintenance College"), "route to Maintenance College");
            checkEquals(Arrays.asList(SOURCE, "Torreyson Library", "Old Main", "McAlister Hall", "Wingo",
                    "New Business Building", "Brewer-Hegeman", "Bear Village Apt."),
                    paths.pathTo("Bear Village Apt."), "route to the farthest building");
        }
    }

    private static void campusAllSources() throws IOException {
        Graph campus = GraphReader.read(Paths.get(CAMPUS_FILE));
        checkAllSources(campus, BOTH);
        check(!DIJKSTRA.run(campus, 0).sameDistancesAs(DIJKSTRA.run(campus, 1)),
                "results from different buildings differ");
    }

    // ---- Random graphs ----

    private static void randomUndirectedGraphs() {
        Random random = new Random(3364);
        for (int trial = 0; trial < 300; trial++) {
            int vertices = 1 + random.nextInt(40);
            int edges = random.nextInt(4 * vertices);
            Graph graph = GraphGenerator.randomConnected(vertices, edges, 50, trial);
            long[] expected = floydWarshall(graph)[0];

            ShortestPaths dijkstra = DIJKSTRA.run(graph, 0);
            ShortestPaths bellmanFord = BELLMAN_FORD.run(graph, 0);
            checkMatches(expected, dijkstra);
            checkMatches(expected, bellmanFord);
            check(dijkstra.sameDistancesAs(bellmanFord), "the two algorithms agree");
        }

        Graph grid = GraphGenerator.grid(5, 7, 50, 1);
        checkEquals(35, grid.vertexCount(), "junctions of a 5 x 7 grid");
        checkEquals(5 * 6 + 4 * 7, grid.edgeCount(), "streets of a 5 x 7 grid");
        checkAllSources(grid, BOTH);
    }

    private static void randomDirectedGraphs() {
        Random random = new Random(1959);
        for (int trial = 0; trial < 300; trial++) {
            Graph graph = randomDirected(random, 0);
            check(!graph.hasNegativeWeight(), "weights are non-negative");
            int source = random.nextInt(graph.vertexCount());
            long[] expected = floydWarshall(graph)[source];
            checkMatches(expected, DIJKSTRA.run(graph, source));
            checkMatches(expected, BELLMAN_FORD.run(graph, source));
        }
    }

    private static void randomNegativeWeights() {
        Random random = new Random(2024);
        int graphsWithNegativeWeights = 0;
        for (int trial = 0; trial < 300; trial++) {
            Graph graph = randomDirected(random, 60);
            if (graph.hasNegativeWeight()) {
                graphsWithNegativeWeights++;
            }
            int source = random.nextInt(graph.vertexCount());
            checkMatches(floydWarshall(graph)[source], BELLMAN_FORD.run(graph, source));
        }
        check(graphsWithNegativeWeights > 250, "the test really exercises negative weights");
    }

    private static void randomNegativeCycles() {
        Random random = new Random(7);
        int cyclesReported = 0;
        for (int trial = 0; trial < 500; trial++) {
            int vertices = 2 + random.nextInt(9);
            Graph graph = Graph.directed();
            for (int v = 0; v < vertices; v++) {
                graph.addVertex(GraphGenerator.vertexName(v));
            }
            int edges = random.nextInt(3 * vertices);
            for (int i = 0; i < edges; i++) {
                graph.addEdge(GraphGenerator.vertexName(random.nextInt(vertices)),
                        GraphGenerator.vertexName(random.nextInt(vertices)), random.nextInt(31) - 10);
            }

            int source = random.nextInt(vertices);
            long[][] reference = floydWarshall(graph);
            boolean cycleReachable = false;
            for (int v = 0; v < vertices; v++) {
                if (reference[source][v] != ShortestPaths.UNREACHABLE && reference[v][v] < 0) {
                    cycleReachable = true;
                }
            }

            try {
                ShortestPaths paths = BELLMAN_FORD.run(graph, source);
                check(!cycleReachable, "a reachable negative cycle was missed");
                checkMatches(reference[source], paths);
            } catch (NegativeCycleException e) {
                check(cycleReachable, "a negative cycle was reported but none is reachable: " + e.cycle());
                checkNegativeCycle(graph, e.cycle());
                cyclesReported++;
            }
        }
        check(cyclesReported > 100 && cyclesReported < 400,
                "the test covers graphs with and without a cycle: " + cyclesReported);
    }

    /**
     * A random directed graph. An edge (u, v) with cost c >= 0 gets the weight
     * c + height(u) - height(v): the heights cancel around every cycle, so
     * single edges can be negative but cycles cannot. A maximum height of 0
     * gives a graph without negative weights.
     */
    private static Graph randomDirected(Random random, int maxHeight) {
        int vertices = 2 + random.nextInt(25);
        int[] height = new int[vertices];
        Graph graph = Graph.directed();
        for (int v = 0; v < vertices; v++) {
            height[v] = random.nextInt(maxHeight + 1);
            graph.addVertex(GraphGenerator.vertexName(v));
        }
        int edges = random.nextInt(5 * vertices);
        for (int i = 0; i < edges; i++) {
            int u = random.nextInt(vertices);
            int v = random.nextInt(vertices);
            graph.addEdge(GraphGenerator.vertexName(u), GraphGenerator.vertexName(v),
                    random.nextInt(20) + height[u] - height[v]);
        }
        return graph;
    }

    // ---- Special cases ----

    private static void unreachableVertices() {
        Graph graph = Graph.directed();
        graph.addEdge("A", "B", 1);
        graph.addEdge("C", "A", 1);
        graph.addVertex("D");

        for (ShortestPathAlgorithm algorithm : BOTH) {
            ShortestPaths paths = algorithm.run(graph, graph.indexOf("A"));
            checkEquals(1L, paths.distanceTo("B"), "reachable vertex");
            for (String name : Arrays.asList("C", "D")) {
                int vertex = graph.indexOf(name);
                check(!paths.isReachable(vertex), name + " is unreachable");
                checkEquals(ShortestPaths.UNREACHABLE, paths.distanceTo(vertex), "distance of " + name);
                check(paths.pathTo(vertex).isEmpty(), "no route to " + name);
            }
        }
    }

    private static void edgeCases() {
        Graph single = Graph.undirected();
        single.addVertex("Only");
        Graph tricky = Graph.undirected();
        tricky.addEdge("A", "A", 5);
        tricky.addEdge("A", "B", 9);
        tricky.addEdge("A", "B", 4);
        tricky.addEdge("B", "C", 0);
        tricky.addEdge("C", "D", 0);

        for (ShortestPathAlgorithm algorithm : BOTH) {
            ShortestPaths alone = algorithm.run(single, 0);
            checkEquals(0L, alone.distanceTo("Only"), "distance from a vertex to itself");
            checkEquals(Arrays.asList("Only"), alone.pathTo("Only"), "route from a vertex to itself");

            ShortestPaths paths = algorithm.run(tricky, tricky.indexOf("A"));
            checkEquals(0L, paths.distanceTo("A"), "a self-loop does not change anything");
            checkEquals(4L, paths.distanceTo("B"), "the cheaper parallel edge is used");
            checkEquals(4L, paths.distanceTo("D"), "zero-weight edges are followed");
            checkEquals(Arrays.asList("A", "B", "C", "D"), paths.pathTo("D"), "route over zero-weight edges");
            checkThrows(IllegalArgumentException.class, () -> paths.distanceTo("Nowhere"), "unknown vertex");
        }
    }

    private static void invalidSource() {
        Graph graph = Graph.undirected();
        graph.addEdge("A", "B", 1);
        for (ShortestPathAlgorithm algorithm : BOTH) {
            checkThrows(IllegalArgumentException.class, () -> algorithm.run(graph, -1), "negative index");
            checkThrows(IllegalArgumentException.class, () -> algorithm.run(graph, 2), "index too large");
        }
    }

    // ---- Negative weights ----

    private static void dijkstraRefusesNegativeWeights() throws IOException {
        Graph graph = GraphReader.read(Paths.get("data/negative-weights.txt"));
        checkThrows(IllegalArgumentException.class, () -> DIJKSTRA.run(graph, 0), "negative weight");
    }

    private static void negativeWeightExample() throws IOException {
        Graph graph = GraphReader.read(Paths.get("data/negative-weights.txt"));
        ShortestPaths paths = BELLMAN_FORD.run(graph, graph.indexOf("A"));

        checkEquals(2L, paths.distanceTo("B"), "A -> C -> B costs 5 - 3 = 2, less than the direct edge");
        checkEquals(Arrays.asList("A", "C", "B"), paths.pathTo("B"), "route to B");
        checkEquals(5L, paths.distanceTo("C"), "distance to C");
        checkEquals(4L, paths.distanceTo("D"), "distance to D");
        checkPathsAreConsistent(graph, paths);
    }

    private static void negativeCycleExample() throws IOException {
        Graph graph = GraphReader.read(Paths.get("data/negative-cycle.txt"));
        List<String> cycle = expectNegativeCycle(graph, "A");
        checkEquals(3, cycle.size(), "the cycle is B -> C -> B or C -> B -> C");
        check(cycle.contains("B") && cycle.contains("C"), "cycle vertices: " + cycle);
    }

    private static void unreachableNegativeCycle() {
        Graph graph = Graph.directed();
        graph.addEdge("A", "B", 3);
        graph.addEdge("X", "Y", -5);
        graph.addEdge("Y", "X", 1);
        graph.addEdge("X", "A", 1);

        ShortestPaths paths = BELLMAN_FORD.run(graph, graph.indexOf("A"));
        checkEquals(3L, paths.distanceTo("B"), "distance to B");
        check(!paths.isReachable(graph.indexOf("X")), "X is unreachable from A");
        expectNegativeCycle(graph, "X");
    }

    private static void negativeCycleShapes() {
        Graph undirected = Graph.undirected();
        undirected.addEdge("A", "B", 2);
        undirected.addEdge("B", "C", -1);
        expectNegativeCycle(undirected, "A");

        // A long tail leads into the cycle v20 -> ... -> v30 -> v20.
        Graph tail = Graph.directed();
        for (int v = 0; v < 30; v++) {
            tail.addEdge(GraphGenerator.vertexName(v), GraphGenerator.vertexName(v + 1), 1);
        }
        tail.addEdge(GraphGenerator.vertexName(30), GraphGenerator.vertexName(20), -15);
        checkEquals(12, expectNegativeCycle(tail, "v0").size(), "11 vertices plus the repeated start");

        // A long chain hangs off the cycle A -> B -> C -> A. Its edges come first and
        // far end first, so the improvement that reveals the cycle is seen at the far
        // end of the chain, and the cycle has to be found by walking back from there.
        Graph chain = Graph.directed();
        for (int v = 19; v >= 1; v--) {
            chain.addEdge(GraphGenerator.vertexName(v), GraphGenerator.vertexName(v + 1), 1);
        }
        chain.addEdge("C", GraphGenerator.vertexName(1), 1);
        chain.addEdge("S", "A", 1);
        chain.addEdge("A", "B", 1);
        chain.addEdge("B", "C", 1);
        chain.addEdge("C", "A", -5);
        List<String> cycle = expectNegativeCycle(chain, "S");
        checkEquals(4, cycle.size(), "A, B and C plus the repeated start: " + cycle);
        check(cycle.containsAll(Arrays.asList("A", "B", "C")), "cycle vertices: " + cycle);
    }

    /** Runs Bellman-Ford, which must report a real negative cycle, and returns that cycle. */
    private static List<String> expectNegativeCycle(Graph graph, String source) {
        try {
            BELLMAN_FORD.run(graph, graph.indexOf(source));
        } catch (NegativeCycleException e) {
            checkNegativeCycle(graph, e.cycle());
            return e.cycle();
        }
        throw new AssertionError("expected a NegativeCycleException");
    }

    // ---- Amount of work ----

    private static void passCounts() throws IOException {
        Graph campus = GraphReader.read(Paths.get(CAMPUS_FILE));
        ShortestPaths paths = BELLMAN_FORD.run(campus, campus.indexOf(SOURCE));
        checkEquals(3, paths.rounds(), "passes on the campus map, far fewer than V - 1 = 18");

        int vertices = 50;
        Graph chain = GraphGenerator.reversedChain(vertices);
        ShortestPaths worst = BELLMAN_FORD.run(chain, 0);
        checkEquals(vertices, worst.rounds(), "V - 1 improving passes plus the final check");
        checkEquals((long) (vertices - 1), worst.distanceTo(GraphGenerator.vertexName(vertices - 1)),
                "distance to the far end of the chain");
    }

    private static void workCounters() throws IOException {
        Graph campus = GraphReader.read(Paths.get(CAMPUS_FILE));
        int source = campus.indexOf(SOURCE);
        long arcs = campus.arcs().size();

        ShortestPaths dijkstra = DIJKSTRA.run(campus, source);
        checkEquals(campus.vertexCount(), dijkstra.rounds(), "Dijkstra settles every building once");
        checkEquals(arcs, dijkstra.edgesExamined(), "Dijkstra examines every arc once");
        checkEquals(23L, dijkstra.distanceUpdates(), "Dijkstra distance updates");

        ShortestPaths bellmanFord = BELLMAN_FORD.run(campus, source);
        checkEquals(3 * arcs, bellmanFord.edgesExamined(), "Bellman-Ford examines every arc in every pass");
        checkEquals(23L, bellmanFord.distanceUpdates(), "Bellman-Ford distance updates");
    }

    // ---- The command-line program ----

    private static void commandLineMatchesReadme() throws IOException {
        String[] output = runCommandLine(0);
        String readme = new String(Files.readAllBytes(Paths.get("README.md")), StandardCharsets.UTF_8);

        check(output[0].contains("Bear Village Apt."), "the routes are printed");
        check(unixLines(readme).contains(unixLines(output[0])),
                "README.md shows the output of the program exactly");
        checkEquals("", output[1], "nothing is written to the error stream");
    }

    private static void commandLineOptions() {
        String single = runCommandLine(0, "--source", "wingo", "--to", "old  main", "--algorithm", "DIJKSTRA")[0];
        check(single.contains("Shortest paths from Wingo"), "building names ignore case");
        check(single.contains("Destination: Old Main"), "the destination is named");
        check(single.contains("Distance:    150"), "Wingo to Old Main is 50 + 100");
        check(single.contains("Route:       Wingo -> McAlister Hall -> Old Main"), "route from Wingo to Old Main");
        check(single.matches("(?s).*\\nDijkstra\\s+19\\s.*"), "the requested algorithm is in the work table");
        check(!single.matches("(?s).*\\nBellman-Ford\\s+\\d.*"), "the other algorithm is not run");
        check(!single.contains("agree"), "nothing to compare with a single algorithm");

        String negative = runCommandLine(0, "--graph", "data/negative-weights.txt", "--source", "A")[0];
        check(negative.contains("(directed, 4 vertices, 4 edges)"), "the graph is described");
        check(negative.contains("only Bellman-Ford is run"), "Dijkstra is skipped for negative weights");
        check(negative.contains("A -> C -> B -> D"), "route through the negative edge");

        check(runCommandLine(0, "--help")[0].contains("Usage:"), "--help prints the usage");
    }

    private static void commandLineErrors() {
        checkError("no building named", "--source", "Hogwarts");
        checkError("no building named", "--to", "Hogwarts");
        checkError("Unknown algorithm", "--algorithm", "a-star");
        checkError("needs a value", "--source");
        checkError("Option --source needs a value", "--source", "--help");
        checkError("Unknown option --speed", "--speed", "fast");
        checkError("Unknown option campus.txt", "campus.txt");
        checkError("missing.txt does not exist", "--graph", "missing.txt");
        checkError("requires non-negative edge weights",
                "--graph", "data/negative-weights.txt", "--source", "A", "--algorithm", "dijkstra");

        String cycle = runCommandLine(1, "--graph", "data/negative-cycle.txt", "--source", "A")[1];
        check(cycle.contains("negative cycle: B -> C -> B") || cycle.contains("negative cycle: C -> B -> C"),
                "the negative cycle is named: " + cycle);
    }

    private static void checkError(String expectedMessage, String... args) {
        String error = runCommandLine(1, args)[1];
        check(error.startsWith("Error: ") && error.contains(expectedMessage),
                "error for " + Arrays.toString(args) + " should contain <" + expectedMessage + ">: " + error);
    }

    /** Runs the program and returns its standard output and its error output. */
    private static String[] runCommandLine(int expectedExitCode, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = CampusNavigator.run(args, new PrintStream(out), new PrintStream(err));
        checkEquals(expectedExitCode, exitCode, "exit code of " + Arrays.toString(args) + " " + err);
        return new String[] {out.toString(), err.toString()};
    }

    private static String unixLines(String text) {
        return text.replace("\r\n", "\n");
    }

    // ---- Reference implementation and shared checks ----

    /** All-pairs shortest distances; unreachable pairs hold {@link ShortestPaths#UNREACHABLE}. */
    private static long[][] floydWarshall(Graph graph) {
        int n = graph.vertexCount();
        long[][] distance = new long[n][n];
        for (long[] row : distance) {
            Arrays.fill(row, ShortestPaths.UNREACHABLE);
        }
        for (int v = 0; v < n; v++) {
            distance[v][v] = 0;
        }
        for (Edge edge : graph.arcs()) {
            distance[edge.from()][edge.to()] = Math.min(distance[edge.from()][edge.to()], edge.weight());
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (distance[i][k] != ShortestPaths.UNREACHABLE
                            && distance[k][j] != ShortestPaths.UNREACHABLE
                            && distance[i][k] + distance[k][j] < distance[i][j]) {
                        distance[i][j] = distance[i][k] + distance[k][j];
                    }
                }
            }
        }
        return distance;
    }

    private static void checkAllSources(Graph graph, List<ShortestPathAlgorithm> algorithms) {
        long[][] expected = floydWarshall(graph);
        for (int source = 0; source < graph.vertexCount(); source++) {
            for (ShortestPathAlgorithm algorithm : algorithms) {
                checkMatches(expected[source], algorithm.run(graph, source));
            }
        }
    }

    private static void checkMatches(long[] expected, ShortestPaths actual) {
        Graph graph = actual.graph();
        for (int v = 0; v < graph.vertexCount(); v++) {
            checkEquals(expected[v], actual.distanceTo(v),
                    actual.algorithm() + " distance from " + graph.nameOf(actual.source())
                            + " to " + graph.nameOf(v) + " (" + graph.vertexCount() + " vertices)");
        }
        checkPathsAreConsistent(graph, actual);
    }

    /** Every route must start at the source, use existing edges and add up to its distance. */
    private static void checkPathsAreConsistent(Graph graph, ShortestPaths paths) {
        String source = graph.nameOf(paths.source());
        for (int target = 0; target < graph.vertexCount(); target++) {
            List<String> route = paths.pathTo(target);
            if (!paths.isReachable(target)) {
                check(route.isEmpty(), "an unreachable vertex has no route");
                continue;
            }
            checkEquals(source, route.get(0), "a route starts at the source");
            checkEquals(graph.nameOf(target), route.get(route.size() - 1), "a route ends at its destination");
            checkEquals(paths.distanceTo(target), walkLength(graph, route),
                    "length of the route to " + graph.nameOf(target));
        }
    }

    private static void checkNegativeCycle(Graph graph, List<String> cycle) {
        check(cycle.size() >= 2, "a cycle has at least one edge: " + cycle);
        checkEquals(cycle.get(0), cycle.get(cycle.size() - 1), "a cycle ends where it starts");
        check(walkLength(graph, cycle) < 0, "the cycle has negative weight: " + cycle);
    }

    /** Total weight of a walk, using the cheapest arc between consecutive vertices. */
    private static long walkLength(Graph graph, List<String> walk) {
        long total = 0;
        for (int i = 0; i + 1 < walk.size(); i++) {
            int from = graph.indexOf(walk.get(i));
            int to = graph.indexOf(walk.get(i + 1));
            long cheapest = Long.MAX_VALUE;
            for (Edge edge : graph.edgesFrom(from)) {
                if (edge.to() == to) {
                    cheapest = Math.min(cheapest, edge.weight());
                }
            }
            check(cheapest != Long.MAX_VALUE, "there is no edge " + walk.get(i) + " -> " + walk.get(i + 1));
            total += cheapest;
        }
        return total;
    }

    // ---- A minimal test harness ----

    private interface Test {
        void run() throws Exception;
    }

    private static void run(String name, Test test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable e) {
            failed++;
            System.out.println("FAIL  " + name);
            System.out.println("      " + e);
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void checkEquals(Object expected, Object actual, String description) {
        if (!expected.equals(actual)) {
            throw new AssertionError(description + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void checkThrows(Class<? extends Throwable> expected, Test action, String description) {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return;
            }
            throw new AssertionError(description + ": expected " + expected.getSimpleName()
                    + " but got " + thrown);
        }
        throw new AssertionError(description + ": expected " + expected.getSimpleName()
                + " but nothing was thrown");
    }
}
