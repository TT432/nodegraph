package com.nodegraph.nodegraph.api.eval;

import com.nodegraph.nodegraph.api.model.NodeId;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Outcome of an {@link Evaluator#evaluateAll(NodeGraph)} run: the output maps
 * of every node that was evaluated in this pass.
 */
public final class EvaluationResult {
    private final Map<NodeId, Map<String, Object>> outputs;

    EvaluationResult(Map<NodeId, Map<String, Object>> outputs) {
        this.outputs = outputs;
    }

    /** All evaluated node outputs, keyed by node id. */
    public Map<NodeId, Map<String, Object>> outputs() {
        return Collections.unmodifiableMap(outputs);
    }

    /** The output map of one node; throws if it was not part of this run. */
    public Map<String, Object> outputsOf(NodeId node) {
        Objects.requireNonNull(node, "node");
        if (!outputs.containsKey(node)) {
            throw new IllegalArgumentException("Node was not evaluated: " + node);
        }
        return Collections.unmodifiableMap(outputs.get(node));
    }

    /** The set of nodes evaluated in this run. */
    public Set<NodeId> evaluatedNodes() {
        return Collections.unmodifiableSet(outputs.keySet());
    }
}
