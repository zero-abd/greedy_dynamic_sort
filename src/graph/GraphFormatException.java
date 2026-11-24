package graph;

import java.io.IOException;

/** Thrown when a graph file does not follow the expected format. */
public final class GraphFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    public GraphFormatException(String message) {
        super(message);
    }
}
