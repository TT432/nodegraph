package io.github.tt432.nodegraph.api.command;

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

class TestMultiInputConnectCommand {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
    }

    private final Types types = new Types();

    private static PortSpec port(String key, Type t) {
        return new PortSpec(key, new TypedValue(Component.literal(key), t, Component.literal("desc")));
    }

    private NodeDefinition srcNode(String id) {
        return new NodeDefinition(
                rl(id),
                Component.literal(id),
                List.of(),
                List.of(),
                List.of(port("out", types.inte)),
                (inputs, widgets) -> Map.of()
        );
    }

    private MultiInputNodeDefinition multiSink(String id) {
        return new MultiInputNodeDefinition(
                rl(id),
                Component.literal(id),
                List.of(),
                List.of(port("in", types.inte)),
                List.of(),
                (inputs, widgets) -> Map.of()
        );
    }

    @Test
    void multiInputUndoRemovesOnlyOwnConnection() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a"), 0, 0);
        Node b = g.addNode(srcNode("b"), 0, 100);
        Node sink = g.addNode(multiSink("sink"), 100, 50);

        ConnectCommand first = new ConnectCommand(g, a.id(), 0, sink.id(), 0);
        ConnectCommand second = new ConnectCommand(g, b.id(), 0, sink.id(), 0);
        first.execute();
        second.execute();

        assertEquals(2, g.connections().size(), "both wires accumulate");

        second.undo();
        assertEquals(1, g.connections().size(), "undo removes only its own wire");
        assertEquals(a.id(), g.inputConnection(sink.id(), 0).orElseThrow().fromNode(),
                "the other wire is untouched");

        second.redo();
        assertEquals(2, g.connections().size());
        assertEquals(b.id(), g.inputConnections(sink.id(), 0).get(1).fromNode(),
                "redo appends its wire after the surviving one");

        first.undo();
        assertEquals(1, g.connections().size());
        assertEquals(b.id(), g.inputConnection(sink.id(), 0).orElseThrow().fromNode());
    }

    @Test
    void multiInputUndoAfterPreexistingWires() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a"), 0, 0);
        Node b = g.addNode(srcNode("b"), 0, 100);
        Node sink = g.addNode(multiSink("sink"), 100, 50);

        g.connect(a.id(), 0, sink.id(), 0); // pre-existing wire (not via command)

        ConnectCommand cmd = new ConnectCommand(g, b.id(), 0, sink.id(), 0);
        cmd.execute();
        assertEquals(2, g.connections().size());

        cmd.undo();
        assertEquals(1, g.connections().size(), "only the command's wire is removed");
        assertEquals(a.id(), g.inputConnection(sink.id(), 0).orElseThrow().fromNode(),
                "pre-existing wire survives; nothing spuriously restored");
    }

    @Test
    void singleInputReplaceRestoreNotRegressed() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a"), 0, 0);
        Node b = g.addNode(srcNode("b"), 0, 100);
        Node sink = g.addNode(new NodeDefinition(
                rl("sink"),
                Component.literal("sink"),
                List.of(),
                List.of(port("in", types.inte)),
                List.of(),
                (inputs, widgets) -> Map.of()), 100, 50);

        g.connect(a.id(), 0, sink.id(), 0);
        Connection before = g.inputConnection(sink.id(), 0).orElseThrow();

        ConnectCommand cmd = new ConnectCommand(g, b.id(), 0, sink.id(), 0);
        cmd.execute();
        assertEquals(1, g.connections().size());
        assertEquals(b.id(), g.inputConnection(sink.id(), 0).orElseThrow().fromNode());

        cmd.undo();
        Connection restored = g.inputConnection(sink.id(), 0).orElseThrow();
        assertEquals(before, restored, "replaced connection is restored");

        cmd.redo();
        assertEquals(b.id(), g.inputConnection(sink.id(), 0).orElseThrow().fromNode());
    }
}
