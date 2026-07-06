package io.github.tt432.nodegraph.api.eval;

import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeId;

import java.util.Map;
import java.util.Objects;

/**
 * Lazy Blender-style evaluator for a {@link NodeGraph}. Stateless and hence
 * thread-safe: each invocation builds a fresh internal context, so results are
 * never shared across runs.
 * <p>
 * Per run, every node is evaluated at most once (single-pass memoization):
 * fan-out and diamond dependencies re-use the cached output map. Cycles along
 * the active recursion path are reported via {@link CycleException}; failures
 * inside a {@link io.github.tt432.nodegraph.api.def.NodeFunction} are wrapped in
 * {@link EvaluationException}.
 */
public final class Evaluator {

    /**
     * Evaluate every node in the graph, returning all outputs. Nodes reachable
     * from another are computed once and reused.
     */
    public EvaluationResult evaluateAll(NodeGraph graph) {
        Objects.requireNonNull(graph, "graph");
        EvaluationContext ctx = new EvaluationContext(graph);
        for (Node n : graph.nodes()) {
            ctx.evaluate(n.id());
        }
        return new EvaluationResult(ctx.done());
    }

    /**
     * Lazily evaluate a single node and its reachable dependencies, returning
     * that node's output map.
     */
    public Map<String, Object> evaluate(NodeGraph graph, NodeId node) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(node, "node");
        EvaluationContext ctx = new EvaluationContext(graph);
        return ctx.evaluate(node);
    }
}
