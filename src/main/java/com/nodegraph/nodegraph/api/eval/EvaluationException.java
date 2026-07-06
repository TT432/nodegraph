package com.nodegraph.nodegraph.api.eval;

import com.nodegraph.nodegraph.api.model.NodeId;

import java.util.Objects;

/**
 * Thrown when a node's {@link com.nodegraph.nodegraph.api.def.NodeFunction}
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
