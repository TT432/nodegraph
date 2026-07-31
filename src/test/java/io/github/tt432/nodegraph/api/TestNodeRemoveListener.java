package io.github.tt432.nodegraph.api;

import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.def.PortSpec;
import io.github.tt432.nodegraph.api.model.Connection;
import io.github.tt432.nodegraph.api.model.ConnectionEvent;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.TypedValue;
import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import static io.github.tt432.nodegraph.TestIds.rl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestNodeRemoveListener {

    private final TypeRegistry reg = new TypeRegistry();
    private final Type type = reg.register("t", 0xFFAA0000);

    private PortSpec port(String key) {
        return new PortSpec(key, new TypedValue(Component.literal(key), type, Component.literal("")));
    }

    private NodeDefinition def(String id, List<PortSpec> in, List<PortSpec> out) {
        return new NodeDefinition(rl(id), Component.literal(id), List.of(), in, out,
                (inputs, widgets) -> Map.of());
    }

    @Test
    void removeNodeFiresAfterConnectionCascades() {
        NodeGraph graph = new NodeGraph(reg);
        Node src = graph.addNode(def("src", List.of(), List.of(port("out"))), 0, 0);
        Node sink = graph.addNode(def("sink", List.of(port("in")), List.of()), 0, 0);
        graph.connect(src.id(), 0, sink.id(), 0);

        List<String> order = new ArrayList<>();
        graph.addConnectionListener(event -> {
            if (event.kind() == ConnectionEvent.Kind.REMOVED) {
                order.add("connection");
            }
        });
        graph.addNodeRemoveListener((g, node) -> {
            order.add("node:" + node.definition().header().getString());
            // 触发时节点已脱离图，其连接也已清空
            assertSame(graph, g);
            assertTrue(g.findNode(node.id()).isEmpty());
            assertTrue(g.connections().isEmpty());
        });

        graph.removeNode(src.id());
        assertEquals(List.of("connection", "node:src"), order);
    }

    @Test
    void listenerExceptionsAreIsolated() {
        NodeGraph graph = new NodeGraph(reg);
        Node a = graph.addNode(def("a", List.of(), List.of()), 0, 0);
        List<Node> seen = new ArrayList<>();
        graph.addNodeRemoveListener((g, node) -> {
            throw new RuntimeException("boom");
        });
        graph.addNodeRemoveListener((g, node) -> seen.add(node));

        graph.removeNode(a.id());
        assertEquals(List.of(a), seen);
    }

    @Test
    void removeListenerStopsDelivery() {
        NodeGraph graph = new NodeGraph(reg);
        Node a = graph.addNode(def("a", List.of(), List.of()), 0, 0);
        Node b = graph.addNode(def("b", List.of(), List.of()), 0, 0);
        List<Node> seen = new ArrayList<>();
        var listener = (io.github.tt432.nodegraph.api.model.NodeRemoveListener) (g, node) -> seen.add(node);
        graph.addNodeRemoveListener(listener);
        graph.removeNode(a.id());
        graph.removeNodeRemoveListener(listener);
        graph.removeNode(b.id());
        assertEquals(List.of(a), seen);
    }
}
