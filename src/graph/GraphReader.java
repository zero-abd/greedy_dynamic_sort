package graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads a graph from a text file with one edge per line:
 * <pre>
 * # comment
 * undirected                      (optional: "directed" or "undirected", the default)
 * Computer Science, Burdick, 30   (from, to, weight)
 * </pre>
 */
public final class GraphReader {

    private static final String DIRECTED = "directed";
    private static final String UNDIRECTED = "undirected";

    private GraphReader() {
    }

    public static Graph read(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader, file.getFileName().toString());
        }
    }

    /** @param sourceName name used in error messages, for example the file name */
    public static Graph read(Reader source, String sourceName) throws IOException {
        BufferedReader reader = new BufferedReader(source);
        Graph graph = null;
        int lineNumber = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String text = stripComment(line, lineNumber);
            if (text.isEmpty()) {
                continue;
            }
            String where = sourceName + ", line " + lineNumber + ": ";
            if (isDirective(text)) {
                if (graph != null) {
                    throw new GraphFormatException(
                            where + "\"" + text + "\" is only allowed as the first entry of the file");
                }
                graph = text.equalsIgnoreCase(DIRECTED) ? Graph.directed() : Graph.undirected();
                continue;
            }
            if (graph == null) {
                graph = Graph.undirected();
            }
            addEdge(graph, text, where);
        }
        if (graph == null || graph.vertexCount() == 0) {
            throw new GraphFormatException(sourceName + ": the file does not contain any edges");
        }
        return graph;
    }

    private static String stripComment(String line, int lineNumber) {
        String text = line;
        if (lineNumber == 1 && text.startsWith("\uFEFF")) {
            text = text.substring(1); // byte-order mark left by some Windows editors
        }
        int comment = text.indexOf('#');
        if (comment >= 0) {
            text = text.substring(0, comment);
        }
        return text.trim();
    }

    private static boolean isDirective(String text) {
        return text.equalsIgnoreCase(DIRECTED) || text.equalsIgnoreCase(UNDIRECTED);
    }

    private static void addEdge(Graph graph, String text, String where) throws GraphFormatException {
        String[] fields = text.split(",", -1);
        if (fields.length != 3) {
            throw new GraphFormatException(
                    where + "expected \"from, to, weight\" but found \"" + text + "\"");
        }
        String from = fields[0].trim();
        String to = fields[1].trim();
        if (from.isEmpty() || to.isEmpty()) {
            throw new GraphFormatException(where + "a vertex name is missing in \"" + text + "\"");
        }
        int weight;
        try {
            weight = Integer.parseInt(fields[2].trim());
        } catch (NumberFormatException e) {
            throw new GraphFormatException(
                    where + "the weight \"" + fields[2].trim() + "\" is not a whole number");
        }
        graph.addEdge(from, to, weight);
    }
}
