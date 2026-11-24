package shortestpath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Thrown when a negative cycle can be reached from the source, so no shortest path exists. */
public final class NegativeCycleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String[] cycle;

    /** @param cycle the vertices in travel order, with the first one repeated at the end */
    public NegativeCycleException(List<String> cycle) {
        super("The graph contains a negative cycle: " + String.join(" -> ", cycle));
        this.cycle = cycle.toArray(new String[0]);
    }

    public List<String> cycle() {
        return Collections.unmodifiableList(Arrays.asList(cycle));
    }
}
