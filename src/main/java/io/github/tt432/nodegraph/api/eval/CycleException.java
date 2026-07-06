package io.github.tt432.nodegraph.api.eval;

import io.github.tt432.nodegraph.api.model.NodeId;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when the evaluator detects an evaluation cycle (a node whose
 * computation transitively depends on itself along the active recursion
 * stack). The {@link #cycle()} path lists the nodes forming the loop in
 * dependency order, starting and ending at the same node.
 */
public final class CycleException extends RuntimeException {
    private final List<NodeId> cycle;

    public CycleException(List<NodeId> cycle) {
        super("Evaluation cycle detected: " + cycle);
        this.cycle = List.copyOf(Objects.requireNonNull(cycle, "cycle"));
    }

    /** Nodes forming the cycle, in dependency order, closed (first == last). */
    public List<NodeId> cycle() {
        return cycle;
    }
}
