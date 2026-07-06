package io.github.tt432.nodegraph.api;

import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.def.PortSpec;
import io.github.tt432.nodegraph.api.model.ConnectResult;
import io.github.tt432.nodegraph.api.model.Connection;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeGroupId;
import io.github.tt432.nodegraph.api.model.NodeId;
import io.github.tt432.nodegraph.api.model.TypedValue;
import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestNodeGraphConstraints {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
        final Type bite = reg.register("byte", 0xFF00AA00);
        final Type str = reg.register("string", 0xFF0000AA);
    }

    private final Types types = new Types();

    private static PortSpec port(String key, Type t) {
        return new PortSpec(key, new TypedValue(Component.literal(key), t, Component.literal("desc-" + key)));
    }

    private static NodeDefinition def(String id, List<PortSpec> in, List<PortSpec> out) {
        return new NodeDefinition(
                new ResourceLocation("nodegraph", id),
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

    private NodeDefinition intInNode() {
        return def("sink", List.of(port("in", types.inte)), List.of());
    }

    @Test
    void fanOutOneOutputToManyInputs() {
        NodeGraph g = new NodeGraph(types.reg);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node a = g.addNode(intInNode(), 100, 0);
        Node b = g.addNode(intInNode(), 100, 50);

        Connection c1 = g.connect(src.id(), 0, a.id(), 0);
        Connection c2 = g.connect(src.id(), 0, b.id(), 0);

        assertEquals(2, g.outputsOf(src.id(), 0).size(), "output should fan out");
        assertTrue(g.outputsOf(src.id(), 0).contains(c1));
        assertTrue(g.outputsOf(src.id(), 0).contains(c2));
    }

    @Test
    void inputSingleSourceReplacesExisting() {
        NodeGraph g = new NodeGraph(types.reg);
        Node src1 = g.addNode(intOutNode(), 0, 0);
        Node src2 = g.addNode(intOutNode(), 0, 50);
        Node sink = g.addNode(intInNode(), 100, 0);

        Connection first = g.connect(src1.id(), 0, sink.id(), 0);
        assertEquals(first, g.inputConnection(sink.id(), 0).orElseThrow());
        assertEquals(1, g.outputsOf(src1.id(), 0).size());

        Connection second = g.connect(src2.id(), 0, sink.id(), 0);

        assertEquals(second, g.inputConnection(sink.id(), 0).orElseThrow(), "input should now hold the new source");
        assertTrue(g.outputsOf(src1.id(), 0).isEmpty(), "old source should have lost the wire");
        assertEquals(1, g.outputsOf(src2.id(), 0).size());
        assertEquals(1, g.connections().size(), "replacement must not duplicate the input wire");
    }

    @Test
    void canConnectOkAutoIncompatible() {
        types.reg.registerConversion(types.inte, types.bite, v -> ((Number) v).byteValue());
        NodeGraph g = new NodeGraph(types.reg);

        Node intSrc = g.addNode(intOutNode(), 0, 0);
        Node intSink = g.addNode(intInNode(), 100, 0);
        Node byteSink = g.addNode(def("bsink", List.of(port("in", types.bite)), List.of()), 100, 50);
        Node strSink = g.addNode(def("ssink", List.of(port("in", types.str)), List.of()), 100, 100);

        assertEquals(ConnectResult.OK, g.canConnect(intSrc.id(), 0, intSink.id(), 0));
        assertEquals(ConnectResult.AUTO_CONVERTED, g.canConnect(intSrc.id(), 0, byteSink.id(), 0));
        assertEquals(ConnectResult.INCOMPATIBLE, g.canConnect(intSrc.id(), 0, strSink.id(), 0));
    }

    @Test
    void connectIncompatibleThrows() {
        NodeGraph g = new NodeGraph(types.reg);
        Node intSrc = g.addNode(intOutNode(), 0, 0);
        Node strSink = g.addNode(def("ssink", List.of(port("in", types.str)), List.of()), 100, 0);

        assertThrows(IllegalArgumentException.class,
                () -> g.connect(intSrc.id(), 0, strSink.id(), 0));
        assertTrue(g.connections().isEmpty());
    }

    @Test
    void autoConvertedConnectionCarriesRule() {
        types.reg.registerConversion(types.inte, types.bite, v -> ((Number) v).byteValue());
        NodeGraph g = new NodeGraph(types.reg);
        Node intSrc = g.addNode(intOutNode(), 0, 0);
        Node byteSink = g.addNode(def("bsink", List.of(port("in", types.bite)), List.of()), 100, 0);

        Connection c = g.connect(intSrc.id(), 0, byteSink.id(), 0);
        assertTrue(c.isAutoConverted());
        assertNotNull(c.rule());
        assertEquals((byte) 7, c.rule().apply(7));
    }

    @Test
    void removeNodeDropsConnections() {
        NodeGraph g = new NodeGraph(types.reg);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);
        g.connect(src.id(), 0, sink.id(), 0);
        assertEquals(1, g.connections().size());

        g.removeNode(src.id());
        assertTrue(g.connections().isEmpty(), "removing source must drop its wires");

        Node src2 = g.addNode(intOutNode(), 0, 0);
        g.connect(src2.id(), 0, sink.id(), 0);
        assertEquals(1, g.connections().size());
        g.removeNode(sink.id());
        assertTrue(g.connections().isEmpty());
    }

    @Test
    void groupMembershipDerivedFromNode() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(intOutNode(), 0, 0);
        Node b = g.addNode(intInNode(), 0, 100);
        var group = g.createGroup(Component.literal("G"), 0, 0, 500, 500);

        assertTrue(g.members(group.id()).isEmpty());
        g.setNodeGroup(a.id(), group.id());
        g.setNodeGroup(b.id(), group.id());
        assertEquals(2, g.members(group.id()).size());
        assertEquals(group.id(), a.groupId());

        g.removeGroup(group.id());
        assertNull(a.groupId(), "removing the group should clear membership");
        assertTrue(g.members(group.id()).isEmpty());
    }

    @Test
    void unknownNodeAndGroupThrow() {
        NodeGraph g = new NodeGraph(types.reg);
        assertThrows(IllegalArgumentException.class, () -> g.node(new NodeId(999)));
        assertThrows(IllegalArgumentException.class,
                () -> g.setNodeGroup(new NodeId(1), new NodeGroupId(1)));
    }

    @Test
    void outOfRangePortThrows() {
        NodeGraph g = new NodeGraph(types.reg);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);
        assertThrows(IllegalArgumentException.class, () -> g.canConnect(src.id(), 5, sink.id(), 0));
        assertThrows(IllegalArgumentException.class, () -> g.canConnect(src.id(), 0, sink.id(), 5));
    }

    @Test
    void inputConnectionEmptyWhenNone() {
        NodeGraph g = new NodeGraph(types.reg);
        Node sink = g.addNode(intInNode(), 0, 0);
        Optional<Connection> none = g.inputConnection(sink.id(), 0);
        assertTrue(none.isEmpty());
    }
}
