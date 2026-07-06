package com.nodegraph.nodegraph.api.eval;

import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.InputWidget;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeId;
import com.nodegraph.nodegraph.api.model.Port;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Per-evaluation-run state: the lazy memoization cache and the DFS recursion
 * stack used for cycle detection. Package-private; obtain via {@link Evaluator}.
 */
final class EvaluationContext {
    private final NodeGraph graph;
    private final Map<NodeId, Map<String, Object>> done = new HashMap<>();
    private final Deque<NodeId> stack = new ArrayDeque<>();
    private final Set<NodeId> onStack = new HashSet<>();

    EvaluationContext(NodeGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph");
    }

    Map<String, Object> evaluate(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        if (done.containsKey(nodeId)) {
            return done.get(nodeId);
        }
        if (onStack.contains(nodeId)) {
            throw new CycleException(cyclePath(nodeId));
        }
        Node node = graph.node(nodeId); // IllegalArgumentException if unknown
        stack.push(nodeId);
        onStack.add(nodeId);
        try {
            Map<String, Object> inputValues = resolveInputs(node);
            Map<String, Object> widgetValues = widgetValuesOf(node);
            Map<String, Object> outputs;
            try {
                outputs = node.definition().function().evaluate(inputValues, widgetValues);
            } catch (Throwable t) {
                throw new EvaluationException(nodeId, t);
            }
            Map<String, Object> safe = (outputs == null)
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(outputs);
            done.put(nodeId, safe);
            return safe;
        } finally {
            stack.pop();
            onStack.remove(nodeId);
        }
    }

    private Map<String, Object> resolveInputs(Node node) {
        Map<String, Object> inputValues = new LinkedHashMap<>();
        List<Port> inputs = node.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            Port port = inputs.get(i);
            Object value = null;
            Optional<Connection> conn = graph.inputConnection(node.id(), i);
            if (conn.isPresent()) {
                Connection c = conn.get();
                Map<String, Object> srcOuts = evaluate(c.fromNode());
                Node src = graph.node(c.fromNode());
                int outIdx = c.fromOutput();
                if (outIdx < 0 || outIdx >= src.outputs().size()) {
                    throw new IllegalStateException("Connection references invalid output index "
                            + outIdx + " on " + src.id());
                }
                String outKey = src.outputs().get(outIdx).key();
                Object raw = srcOuts.get(outKey);
                if (c.isAutoConverted() && c.rule() != null) {
                    raw = c.rule().apply(raw);
                }
                value = raw;
            }
            inputValues.put(port.key(), value);
        }
        return inputValues;
    }

    private Map<String, Object> widgetValuesOf(Node node) {
        Map<String, Object> widgetValues = new LinkedHashMap<>();
        for (InputWidget w : node.widgets()) {
            widgetValues.put(w.key(), w.currentValue());
        }
        return widgetValues;
    }

    private List<NodeId> cyclePath(NodeId repeated) {
        // ArrayDeque iterator is head->tail (top->bottom); reverse to entry order.
        List<NodeId> bottomToTop = new ArrayList<>(stack);
        Collections.reverse(bottomToTop);
        List<NodeId> path = new ArrayList<>();
        boolean found = false;
        for (NodeId n : bottomToTop) {
            if (!found && n.equals(repeated)) {
                found = true;
            }
            if (found) {
                path.add(n);
            }
        }
        path.add(repeated); // close the cycle
        return Collections.unmodifiableList(path);
    }

    Map<NodeId, Map<String, Object>> done() {
        return done;
    }
}
