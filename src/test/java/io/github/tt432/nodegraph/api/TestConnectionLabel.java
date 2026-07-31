package io.github.tt432.nodegraph.api;

import io.github.tt432.nodegraph.api.command.ConnectCommand;
import io.github.tt432.nodegraph.api.def.MultiInputNodeDefinition;
import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.def.PortSpec;
import io.github.tt432.nodegraph.api.model.Connection;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.TypedValue;
import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import static io.github.tt432.nodegraph.TestIds.rl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** {@link Connection#label()} 的创建、幂等与 undo 恢复语义。 */
class TestConnectionLabel {

    private final TypeRegistry reg = new TypeRegistry();
    private final Type type = reg.register("ref", 0xFFAA0000);

    private PortSpec port(String key) {
        return new PortSpec(key, new TypedValue(Component.literal(key), type, Component.literal("")));
    }

    private NodeDefinition source() {
        return new NodeDefinition(rl("src"), Component.literal("src"),
                List.of(), List.of(), List.of(port("out")), (inputs, widgets) -> Map.of());
    }

    private NodeDefinition sink() {
        return new NodeDefinition(rl("sink"), Component.literal("sink"),
                List.of(), List.of(port("in")), List.of(), (inputs, widgets) -> Map.of());
    }

    private NodeDefinition multiSink() {
        return new MultiInputNodeDefinition(rl("multi"), Component.literal("multi"),
                List.of(), List.of(port("in")), List.of(), (inputs, widgets) -> Map.of());
    }

    private NodeGraph graphWith(NodeDefinition sinkDefinition) {
        NodeGraph graph = new NodeGraph(reg);
        graph.addNode(source(), 0, 0);
        graph.addNode(sinkDefinition, 100, 0);
        return graph;
    }

    @Test
    void defaultLabelIsEmpty() {
        NodeGraph graph = graphWith(sink());
        Connection c = graph.connect(nodeId(graph, 0), 0, nodeId(graph, 1), 0);
        assertEquals("", c.label());
    }

    @Test
    void connectWithLabelStoresIt() {
        NodeGraph graph = graphWith(sink());
        Connection c = graph.connect(nodeId(graph, 0), 0, nodeId(graph, 1), 0, "!v.flag");
        assertEquals("!v.flag", c.label());
        assertEquals("!v.flag", graph.connections().get(0).label());
    }

    @Test
    void multiInputIdempotentReconnectKeepsOriginalLabel() {
        NodeGraph graph = graphWith(multiSink());
        graph.connect(nodeId(graph, 0), 0, nodeId(graph, 1), 0, "first");
        Connection again = graph.connect(nodeId(graph, 0), 0, nodeId(graph, 1), 0, "second");
        assertEquals("first", again.label());
        assertEquals(1, graph.connections().size());
    }

    @Test
    void singleInputReplacementUsesNewLabel() {
        NodeGraph graph = graphWith(sink());
        graph.addNode(source(), 200, 0);
        graph.connect(nodeId(graph, 0), 0, nodeId(graph, 1), 0, "old");
        graph.connect(nodeId(graph, 2), 0, nodeId(graph, 1), 0, "new");
        assertEquals(1, graph.connections().size());
        assertEquals("new", graph.connections().get(0).label());
    }

    @Test
    void addConnectionPreservesLabel() {
        NodeGraph graph = graphWith(sink());
        Connection labeled = graph.connect(nodeId(graph, 0), 0, nodeId(graph, 1), 0, "cond");
        graph.disconnect(labeled);
        graph.addConnection(labeled);
        assertEquals("cond", graph.connections().get(0).label());
    }

    @Test
    void withLabelCopiesEndpoints() {
        NodeGraph graph = graphWith(sink());
        Connection c = graph.connect(nodeId(graph, 0), 0, nodeId(graph, 1), 0);
        Connection relabeled = c.withLabel("x > 1");
        assertEquals("", c.label());
        assertEquals("x > 1", relabeled.label());
        assertEquals(c, relabeled); // 端点四元组不变 → 同一连接身份
    }

    @Test
    void connectCommandUndoRestoresLabeledConnection() {
        NodeGraph graph = graphWith(sink());
        graph.addNode(source(), 200, 0);
        graph.connect(nodeId(graph, 0), 0, nodeId(graph, 1), 0, "guarded");
        ConnectCommand replace = new ConnectCommand(graph, nodeId(graph, 2), 0, nodeId(graph, 1), 0);
        replace.execute();
        assertEquals("", graph.connections().get(0).label());
        replace.undo();
        assertEquals(1, graph.connections().size());
        assertEquals("guarded", graph.connections().get(0).label());
    }

    private static io.github.tt432.nodegraph.api.model.NodeId nodeId(NodeGraph graph, int index) {
        int i = 0;
        for (Node node : graph.nodes()) {
            if (i++ == index) {
                return node.id();
            }
        }
        throw new IllegalArgumentException("no node at index " + index);
    }
}
