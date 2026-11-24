package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A weighted graph stored as adjacency lists. Vertices are created by name and
 * numbered from 0 in the order they first appear; the algorithms use the numbers.
 */
public final class Graph {

    private final boolean directed;
    private final List<String> names = new ArrayList<>();
    private final Map<String, Integer> indexByName = new HashMap<>();
    private final List<List<Edge>> outgoing = new ArrayList<>();
    private final List<Edge> arcs = new ArrayList<>();
    private int edgeCount;
    private boolean negativeWeight;

    private Graph(boolean directed) {
        this.directed = directed;
    }

    public static Graph undirected() {
        return new Graph(false);
    }

    public static Graph directed() {
        return new Graph(true);
    }

    /** Adds the vertex if it is new and returns its index. */
    public int addVertex(String name) {
        String key = normalize(name);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Vertex name must not be empty");
        }
        Integer existing = indexByName.get(key);
        if (existing != null) {
            return existing;
        }
        int index = names.size();
        names.add(key);
        indexByName.put(key, index);
        outgoing.add(new ArrayList<>());
        return index;
    }

    /** Adds an edge, creating its vertices if necessary. */
    public void addEdge(String from, String to, int weight) {
        int u = addVertex(from);
        int v = addVertex(to);
        addArc(u, v, weight);
        if (!directed && u != v) {
            addArc(v, u, weight);
        }
        edgeCount++;
        if (weight < 0) {
            negativeWeight = true;
        }
    }

    private void addArc(int from, int to, int weight) {
        Edge arc = new Edge(from, to, weight);
        outgoing.get(from).add(arc);
        arcs.add(arc);
    }

    public boolean isDirected() {
        return directed;
    }

    public int vertexCount() {
        return names.size();
    }

    /** An undirected edge counts once. */
    public int edgeCount() {
        return edgeCount;
    }

    public boolean hasNegativeWeight() {
        return negativeWeight;
    }

    public String nameOf(int vertex) {
        return names.get(vertex);
    }

    /** Returns -1 if there is no vertex with this name. */
    public int indexOf(String name) {
        Integer index = indexByName.get(normalize(name));
        return index == null ? -1 : index;
    }

    /** Like {@link #indexOf(String)}, but ignores upper and lower case. */
    public int indexOfIgnoreCase(String name) {
        String key = normalize(name);
        for (int vertex = 0; vertex < names.size(); vertex++) {
            if (names.get(vertex).equalsIgnoreCase(key)) {
                return vertex;
            }
        }
        return -1;
    }

    public List<String> vertexNames() {
        return Collections.unmodifiableList(names);
    }

    public List<Edge> edgesFrom(int vertex) {
        return Collections.unmodifiableList(outgoing.get(vertex));
    }

    /** Every arc in the order the edges were added; an undirected edge contributes two, a self-loop one. */
    public List<Edge> arcs() {
        return Collections.unmodifiableList(arcs);
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }
}
