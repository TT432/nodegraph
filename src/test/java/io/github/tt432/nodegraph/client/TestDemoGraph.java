package io.github.tt432.nodegraph.client;

import io.github.tt432.nodegraph.api.eval.EvaluationResult;
import io.github.tt432.nodegraph.api.eval.Evaluator;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestDemoGraph {
    @Test
    void createReturnsPopulatedGraph() {
        NodeGraph graph = DemoGraphFactory.create();
        assertNotNull(graph);
        assertEquals(4, graph.nodes().size());
        assertEquals(1, graph.groups().size());
        assertEquals(3, graph.connections().size());
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

    private static Node findNode(NodeGraph graph, String namespace, String path) {
        ResourceLocation rl = new ResourceLocation(namespace, path);
        for (Node n : graph.nodes()) {
            if (n.definition().id().equals(rl)) {
                return n;
            }
        }
        throw new AssertionError("node " + namespace + ":" + path + " not found");
    }
}
