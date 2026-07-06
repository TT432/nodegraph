package io.github.tt432.nodegraph.api.eval;

import io.github.tt432.nodegraph.api.model.NodeId;

import java.util.Objects;

/**
 * Thrown when a node's {@link io.github.tt432.nodegraph.api.def.NodeFunction}
 * raises an exception during evaluation. Wraps the original cause and records
 * which node failed.
 */
public final class EvaluationException extends RuntimeException {
    private final NodeId node;

    public EvaluationException(NodeId node, Throwable cause) {
        super("Evaluation failed at " + node, Objects.requireNonNull(cause, "cause"));
        this.node = Objects.requireNonNull(node, "node");
    }

    public NodeId node() {
        return node;
    }
}
