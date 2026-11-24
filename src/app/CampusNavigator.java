package app;

import graph.Graph;
import graph.GraphFormatException;
import graph.GraphReader;
import shortestpath.BellmanFord;
import shortestpath.Dijkstra;
import shortestpath.NegativeCycleException;
import shortestpath.ShortestPathAlgorithm;
import shortestpath.ShortestPaths;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Command-line program: prints the shortest route from one building (by default
 * Computer Science) to the others, using Dijkstra and Bellman-Ford.
 */
public final class CampusNavigator {

    static final String DEFAULT_GRAPH_FILE = "data/campus.txt";
    static final String DEFAULT_SOURCE = "Computer Science";

    private static final String[] USAGE = {
        "Usage: java -cp out app.CampusNavigator [options]",
        "",
        "  --graph <file>       graph to load (default: " + DEFAULT_GRAPH_FILE + ")",
        "  --source <name>      start building (default: " + DEFAULT_SOURCE + ")",
        "  --to <name>          show only the route to this building",
        "  --algorithm <name>   dijkstra, bellman-ford or both (default: both)",
        "  --help               show this message",
    };

    private CampusNavigator() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    /** Runs the program and returns its exit code: 0 on success, 1 on error. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Options options = Options.parse(args);
            if (options.help) {
                for (String line : USAGE) {
                    out.println(line);
                }
            } else {
                printRoutes(options, out);
            }
            return 0;
        } catch (IllegalArgumentException | IOException | NegativeCycleException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private static void printRoutes(Options options, PrintStream out) throws IOException {
        Graph graph = loadGraph(options.graphFile);
        int source = findVertex(graph, options.source);

        out.println("Graph: " + options.graphFile + " ("
                + (graph.isDirected() ? "directed" : "undirected") + ", "
                + graph.vertexCount() + " vertices, " + graph.edgeCount() + " edges)");

        List<ShortestPaths> results = new ArrayList<>();
        for (ShortestPathAlgorithm algorithm : chooseAlgorithms(options.algorithm, graph, out)) {
            results.add(algorithm.run(graph, source));
        }
        ShortestPaths answer = results.get(0);

        out.println("Shortest paths from " + graph.nameOf(source));
        out.println();
        if (options.target == null) {
            printAllRoutes(answer, out);
        } else {
            printRoute(answer, findVertex(graph, options.target), out);
        }
        out.println();
        if (results.size() == 2) {
            printAgreement(results.get(0), results.get(1), out);
            out.println();
        }
        printWork(results, out);
    }

    private static Graph loadGraph(String file) throws IOException {
        try {
            return GraphReader.read(Paths.get(file));
        } catch (GraphFormatException e) {
            throw e;
        } catch (NoSuchFileException e) {
            throw new IOException("The graph file " + file + " does not exist");
        } catch (IOException e) {
            throw new IOException("The graph file " + file + " could not be read: " + e);
        }
    }

    private static List<ShortestPathAlgorithm> chooseAlgorithms(String choice, Graph graph, PrintStream out) {
        List<ShortestPathAlgorithm> algorithms = new ArrayList<>();
        switch (choice.toLowerCase(Locale.ROOT)) {
            case "dijkstra":
                algorithms.add(new Dijkstra());
                break;
            case "bellman-ford":
                algorithms.add(new BellmanFord());
                break;
            case "both":
                if (graph.hasNegativeWeight()) {
                    out.println("Note: the graph has negative weights, so only Bellman-Ford is run.");
                } else {
                    algorithms.add(new Dijkstra());
                }
                algorithms.add(new BellmanFord());
                break;
            default:
                throw new IllegalArgumentException(
                        "Unknown algorithm \"" + choice + "\"; expected dijkstra, bellman-ford or both");
        }
        return algorithms;
    }

    /** An exact match wins; otherwise upper and lower case are ignored. */
    private static int findVertex(Graph graph, String name) {
        int vertex = graph.indexOf(name);
        if (vertex < 0) {
            vertex = graph.indexOfIgnoreCase(name);
        }
        if (vertex < 0) {
            throw new IllegalArgumentException("There is no building named \"" + name
                    + "\". Known buildings: " + String.join(", ", graph.vertexNames()));
        }
        return vertex;
    }

    /** Prints one row per destination, nearest first. */
    private static void printAllRoutes(ShortestPaths paths, PrintStream out) {
        Graph graph = paths.graph();
        List<Integer> destinations = new ArrayList<>();
        int nameWidth = "Destination".length();
        for (int v = 0; v < graph.vertexCount(); v++) {
            if (v != paths.source()) {
                destinations.add(v);
                nameWidth = Math.max(nameWidth, graph.nameOf(v).length());
            }
        }
        destinations.sort((a, b) -> Long.compare(paths.distanceTo(a), paths.distanceTo(b)));

        String rowFormat = "%-" + nameWidth + "s  %11s  %s%n";
        out.printf(rowFormat, "Destination", "Distance", "Route");
        out.printf(rowFormat, dashes(nameWidth), dashes(11), dashes(5));
        for (int v : destinations) {
            out.printf(rowFormat, graph.nameOf(v), describeDistance(paths, v), describeRoute(paths, v));
        }
    }

    private static void printRoute(ShortestPaths paths, int target, PrintStream out) {
        out.println("Destination: " + paths.graph().nameOf(target));
        out.println("Distance:    " + describeDistance(paths, target));
        out.println("Route:       " + describeRoute(paths, target));
    }

    private static String describeDistance(ShortestPaths paths, int vertex) {
        return paths.isReachable(vertex) ? Long.toString(paths.distanceTo(vertex)) : "unreachable";
    }

    private static String describeRoute(ShortestPaths paths, int vertex) {
        return paths.isReachable(vertex) ? String.join(" -> ", paths.pathTo(vertex)) : "-";
    }

    private static void printAgreement(ShortestPaths first, ShortestPaths second, PrintStream out) {
        if (first.sameDistancesAs(second)) {
            out.println(first.algorithm() + " and " + second.algorithm()
                    + " agree on the distance to every building.");
        } else {
            out.println("WARNING: " + first.algorithm() + " and " + second.algorithm()
                    + " disagree on at least one distance.");
        }
    }

    private static void printWork(List<ShortestPaths> results, PrintStream out) {
        String rowFormat = "%-12s  %6s  %14s  %16s%n";
        out.printf(rowFormat, "Algorithm", "Rounds", "Edges examined", "Distance updates");
        out.printf(rowFormat, dashes(12), dashes(6), dashes(14), dashes(16));
        for (ShortestPaths result : results) {
            out.printf(rowFormat, result.algorithm(), result.rounds(),
                    result.edgesExamined(), result.distanceUpdates());
        }
        out.println("(Rounds: vertices settled by Dijkstra, passes over all edges by Bellman-Ford.)");
    }

    private static String dashes(int count) {
        char[] line = new char[count];
        Arrays.fill(line, '-');
        return new String(line);
    }

    private static final class Options {

        private static final List<String> WITH_VALUE = Arrays.asList("--graph", "--source", "--to", "--algorithm");

        private String graphFile = DEFAULT_GRAPH_FILE;
        private String source = DEFAULT_SOURCE;
        private String target;
        private String algorithm = "both";
        private boolean help;

        static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                String option = args[i];
                if (option.equals("--help")) {
                    options.help = true;
                    continue;
                }
                if (!WITH_VALUE.contains(option)) {
                    throw new IllegalArgumentException(
                            "Unknown option " + option + ". Use --help for the list of options.");
                }
                if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                    throw new IllegalArgumentException(
                            "Option " + option + " needs a value. Use --help for the list of options.");
                }
                String value = args[++i];
                switch (option) {
                    case "--graph":
                        options.graphFile = value;
                        break;
                    case "--source":
                        options.source = value;
                        break;
                    case "--to":
                        options.target = value;
                        break;
                    default: // --algorithm
                        options.algorithm = value;
                        break;
                }
            }
            return options;
        }
    }
}
