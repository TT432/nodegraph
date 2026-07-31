package io.github.tt432.nodegraph.api;

import io.github.tt432.nodegraph.api.def.MultiInputNodeDefinition;
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

class TestMultiInputNode {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
    }

    private final Types types = new Types();

    private static PortSpec port(String key, Type t) {
        return new PortSpec(key, new TypedValue(Component.literal(key), t, Component.literal("desc-" + key)));
    }

    private static NodeDefinition def(String id, List<PortSpec> in, List<PortSpec> out) {
        return new NodeDefinition(
                rl(id),
                Component.literal(id),
                List.of(),
                in,
                out,
                (inputs, widgets) -> Map.of()
        );
    }

    private static MultiInputNodeDefinition multiDef(String id, List<PortSpec> in, List<PortSpec> out) {
        return new MultiInputNodeDefinition(
                rl(id),
                Component.literal(id),
                List.of(),
                in,
                out,
                (inputs, widgets) -> Map.of()
        );
    }

    private NodeDefinition intOutNode() {
        return def("src", List.of(), List.of(port("out", types.inte)));
    }

    private MultiInputNodeDefinition multiIntInNode() {
        return multiDef("multisink", List.of(port("in", types.inte)), List.of());
    }

    @Test
    void multiDefinitionReportsMultiInputAndBaseDoesNot() {
        assertFalse(intOutNode().isMultiInput());
        assertTrue(multiIntInNode().isMultiInput());
    }

    @Test
    void multiInputAccumulatesWiresWithoutReplacement() {
        NodeGraph g = new NodeGraph(types.reg);
        List<ConnectionEvent> events = new ArrayList<>();
        g.addConnectionListener(events::add);
        Node a = g.addNode(intOutNode(), 0, 0);
        Node b = g.addNode(intOutNode(), 0, 100);
        Node sink = g.addNode(multiIntInNode(), 100, 50);

        Connection c1 = g.connect(a.id(), 0, sink.id(), 0);
        Connection c2 = g.connect(b.id(), 0, sink.id(), 0);

        assertEquals(2, g.connections().size(), "wires accumulate, no replacement");
        assertEquals(List.of(c1, c2), g.inputConnections(sink.id(), 0));
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e.kind() == ConnectionEvent.Kind.CREATED),
                "no REMOVED event: nothing is replaced");
    }

    @Test
    void duplicateConnectIsIdempotent() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(multiIntInNode(), 100, 50);

        Connection first = g.connect(a.id(), 0, sink.id(), 0);

        List<ConnectionEvent> events = new ArrayList<>();
        g.addConnectionListener(events::add);
        Connection second = g.connect(a.id(), 0, sink.id(), 0);

        assertSame(first, second, "exact quadruple returns the existing connection");
        assertEquals(1, g.connections().size(), "no duplicate wire");
        assertTrue(events.isEmpty(), "no event fired for idempotent re-connect");

        // a *different* wire to the same port still accumulates
        Node b = g.addNode(intOutNode(), 0, 100);
        g.connect(b.id(), 0, sink.id(), 0);
        assertEquals(2, g.connections().size());
        assertEquals(1, events.size());
    }

    @Test
    void singleInputStillReplacesExisting() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(intOutNode(), 0, 0);
        Node b = g.addNode(intOutNode(), 0, 100);
        Node sink = g.addNode(def("sink", List.of(port("in", types.inte)), List.of()), 100, 50);

        g.connect(a.id(), 0, sink.id(), 0);
        g.connect(b.id(), 0, sink.id(), 0);

        assertEquals(1, g.connections().size(), "single-input keeps single-source semantics");
        assertEquals(b.id(), g.inputConnection(sink.id(), 0).orElseThrow().fromNode());
    }

    @Test
    void inputConnectionReturnsFirstInInsertionOrder() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(intOutNode(), 0, 0);
        Node b = g.addNode(intOutNode(), 0, 100);
        Node sink = g.addNode(multiIntInNode(), 100, 50);

        Connection c1 = g.connect(a.id(), 0, sink.id(), 0);
        g.connect(b.id(), 0, sink.id(), 0);

        assertEquals(c1, g.inputConnection(sink.id(), 0).orElseThrow(),
                "inputConnection returns the first wire; inputConnections lists all");
    }

    @Test
    void addConnectionSkipsSingleSourceCheckForMultiInputTarget() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(intOutNode(), 0, 0);
        Node b = g.addNode(intOutNode(), 0, 100);
        Node sink = g.addNode(multiIntInNode(), 100, 50);

        g.addConnection(new Connection(a.id(), 0, sink.id(), 0, false, null));
        assertDoesNotThrow(() ->
                g.addConnection(new Connection(b.id(), 0, sink.id(), 0, false, null)));
        assertEquals(2, g.inputConnections(sink.id(), 0).size());
    }

    @Test
    void addConnectionStillEnforcesSingleSourceForSingleInputTarget() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(intOutNode(), 0, 0);
        Node b = g.addNode(intOutNode(), 0, 100);
        Node sink = g.addNode(def("sink", List.of(port("in", types.inte)), List.of()), 100, 50);

        g.addConnection(new Connection(a.id(), 0, sink.id(), 0, false, null));
        assertThrows(IllegalStateException.class, () ->
                g.addConnection(new Connection(b.id(), 0, sink.id(), 0, false, null)));
    }
}
