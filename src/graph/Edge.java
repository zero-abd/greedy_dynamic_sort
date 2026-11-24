package graph;

/** A directed arc between two vertex indices. An undirected edge is stored as one arc per direction. */
public final class Edge {

    private final int from;
    private final int to;
    private final int weight;

    Edge(int from, int to, int weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public int from() {
        return from;
    }

    public int to() {
        return to;
    }

    public int weight() {
        return weight;
    }
}
