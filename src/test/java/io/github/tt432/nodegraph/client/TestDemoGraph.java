package io.github.tt432.nodegraph.client;

import io.github.tt432.nodegraph.api.eval.EvaluationResult;
import io.github.tt432.nodegraph.api.eval.Evaluator;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.TestIds;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestDemoGraph {
    @Test
    void createReturnsPopulatedGraph() {
        NodeGraph graph = DemoGraphFactory.create();
        assertNotNull(graph);
        assertEquals(6, graph.nodes().size());
        assertEquals(1, graph.groups().size());
        assertEquals(6, graph.connections().size());
    }

    @Test
    void evaluatorProducesByte42AtToByte() {
        NodeGraph graph = DemoGraphFactory.create();
        EvaluationResult result = new Evaluator().evaluateAll(graph);
        Node clamp = findNode(graph, "nodegraph", "to_byte");
        Map<String, Object> outs = result.outputsOf(clamp.id());
        assertEquals(Byte.valueOf((byte) 42), outs.get("out"));
    }

    @Test
    void addResultIs42() {
        NodeGraph graph = DemoGraphFactory.create();
        EvaluationResult result = new Evaluator().evaluateAll(graph);
        Node sum = findNode(graph, "nodegraph", "add");
        Object res = result.outputsOf(sum.id()).get("result");
        assertEquals(42.0, ((Number) res).doubleValue(), 1e-9);
    }

    @Test
    void multiSumReceivesBothConstantsAsList() {
        NodeGraph graph = DemoGraphFactory.create();
        EvaluationResult result = new Evaluator().evaluateAll(graph);
        Node multiSum = findNode(graph, "nodegraph", "multi_sum");
        assertEquals(2, graph.inputConnections(multiSum.id(), 0).size());
        Object res = result.outputsOf(multiSum.id()).get("sum");
        assertEquals(42.0, ((Number) res).doubleValue(), 1e-9);
    }

    private static Node findNode(NodeGraph graph, String namespace, String path) {
        var rl = TestIds.rl(namespace, path);
        for (Node n : graph.nodes()) {
            if (n.definition().id().equals(rl)) {
                return n;
            }
        }
        throw new AssertionError("node " + namespace + ":" + path + " not found");
    }
}
