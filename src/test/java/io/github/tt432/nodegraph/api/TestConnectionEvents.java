package io.github.tt432.nodegraph.api;

import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.def.PortSpec;
import io.github.tt432.nodegraph.api.model.Connection;
import io.github.tt432.nodegraph.api.model.ConnectionEvent;
import io.github.tt432.nodegraph.api.model.ConnectionListener;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeId;
import io.github.tt432.nodegraph.api.model.TypedValue;
import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestConnectionEvents {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
        final Type bite = reg.register("byte", 0xFF00AA00);
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

    /** Capture every event in order; verify invariants on each. */
    private static final class Recorder implements ConnectionListener {
        final List<ConnectionEvent> events = new ArrayList<>();
        NodeGraph graph;

        @Override
        public void onConnectionEvent(ConnectionEvent e) {
            // Invariant: CREATED => present, REMOVED => absent at fire time.
            boolean present = graph.connections().contains(e.connection());
            if (e.kind() == ConnectionEvent.Kind.CREATED) {
                assertTrue(present, "CREATED must fire after the connection is in the graph");
            } else {
                assertFalse(present, "REMOVED must fire after the connection is gone");
            }
            events.add(e);
        }
    }

    private Recorder attach(NodeGraph g) {
        Recorder r = new Recorder();
        r.graph = g;
        g.addConnectionListener(r);
        return r;
    }

    @Test
    void connectFiresCreated() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);

        Connection c = g.connect(src.id(), 0, sink.id(), 0);

        assertEquals(1, r.events.size());
        assertSame(ConnectionEvent.Kind.CREATED, r.events.get(0).kind());
        assertSame(c, r.events.get(0).connection());
    }

    @Test
    void disconnectFiresRemoved() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);
        Connection c = g.connect(src.id(), 0, sink.id(), 0);
        r.events.clear();

        g.disconnect(c);

        assertEquals(1, r.events.size());
        assertSame(ConnectionEvent.Kind.REMOVED, r.events.get(0).kind());
        assertSame(c, r.events.get(0).connection());
    }

    @Test
    void disconnectAbsentConnectionFiresNothing() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);
        Connection c = g.connect(src.id(), 0, sink.id(), 0);
        g.disconnect(c);
        r.events.clear();

        // Disconnecting an already-removed connection must be a silent no-op.
        g.disconnect(c);
        assertTrue(r.events.isEmpty());
    }

    @Test
    void replacementFiresRemovedOldThenCreatedNew() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src1 = g.addNode(intOutNode(), 0, 0);
        Node src2 = g.addNode(intOutNode(), 0, 50);
        Node sink = g.addNode(intInNode(), 100, 0);

        Connection first = g.connect(src1.id(), 0, sink.id(), 0);
        r.events.clear();
        Connection second = g.connect(src2.id(), 0, sink.id(), 0);

        assertEquals(2, r.events.size(), "replacement = REMOVED(old) + CREATED(new)");
        assertSame(ConnectionEvent.Kind.REMOVED, r.events.get(0).kind());
        assertSame(first, r.events.get(0).connection());
        assertSame(ConnectionEvent.Kind.CREATED, r.events.get(1).kind());
        assertSame(second, r.events.get(1).connection());
    }

    @Test
    void removeNodeCascadesRemovedForEach() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sinkA = g.addNode(intInNode(), 100, 0);
        Node sinkB = g.addNode(intInNode(), 100, 50);
        g.connect(src.id(), 0, sinkA.id(), 0);
        g.connect(src.id(), 0, sinkB.id(), 0);
        r.events.clear();

        g.removeNode(src.id());

        assertEquals(2, r.events.size());
        for (ConnectionEvent e : r.events) {
            assertSame(ConnectionEvent.Kind.REMOVED, e.kind());
        }
        assertTrue(g.connections().isEmpty());
    }

    @Test
    void addConnectionFiresCreated() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);
        Connection c = new Connection(src.id(), 0, sink.id(), 0, false, null);

        g.addConnection(c);

        assertEquals(1, r.events.size());
        assertSame(ConnectionEvent.Kind.CREATED, r.events.get(0).kind());
    }

    @Test
    void addConnectionIdempotentFiresNothing() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);
        Connection c = new Connection(src.id(), 0, sink.id(), 0, false, null);
        g.addConnection(c);
        r.events.clear();

        // Re-adding the exact same connection is a no-op (idempotent).
        g.addConnection(c);

        assertTrue(r.events.isEmpty(), "idempotent addConnection must not re-fire");
    }

    @Test
    void multipleListenersAllNotified() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder a = attach(g);
        Recorder b = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);

        g.connect(src.id(), 0, sink.id(), 0);

        assertEquals(1, a.events.size());
        assertEquals(1, b.events.size());
    }

    @Test
    void removeListenerStopsDelivery() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);

        g.removeConnectionListener(r);
        g.connect(src.id(), 0, sink.id(), 0);

        assertTrue(r.events.isEmpty(), "removed listener must receive nothing");
    }

    @Test
    void reentrantRemovalDoesNotAffectCurrentDispatch() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder surviving = attach(g);
        // This listener removes itself during the callback; the surviving
        // listener registered after it must still be notified this pass.
        ConnectionListener selfRemover = new ConnectionListener() {
            @Override
            public void onConnectionEvent(ConnectionEvent e) {
                g.removeConnectionListener(this);
            }
        };
        g.addConnectionListener(selfRemover);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);

        g.connect(src.id(), 0, sink.id(), 0);

        assertEquals(1, surviving.events.size(), "snapshot iteration must reach later listeners");
    }

    @Test
    void listenerExceptionIsIsolated() {
        NodeGraph g = new NodeGraph(types.reg);
        // Suppress the expected stderr noise from the dispatcher.
        ConnectionListener bomber = e -> {
            throw new IllegalStateException("boom");
        };
        Recorder after = attach(g);
        g.addConnectionListener(bomber);
        // Re-attach 'after' so it sits after the bomber — order is registration order.
        g.removeConnectionListener(after);
        g.addConnectionListener(after);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node sink = g.addNode(intInNode(), 100, 0);

        Connection c = g.connect(src.id(), 0, sink.id(), 0);

        assertEquals(1, after.events.size(), "a throwing listener must not block the others");
        assertSame(c, after.events.get(0).connection());
        assertEquals(1, g.connections().size(), "graph state must remain applied");
    }

    @Test
    void addListenerRejectsNull() {
        NodeGraph g = new NodeGraph(types.reg);
        assertThrows(NullPointerException.class, () -> g.addConnectionListener(null));
    }

    @Test
    void fanOutFiresCreatedPerWire() {
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src = g.addNode(intOutNode(), 0, 0);
        Node a = g.addNode(intInNode(), 100, 0);
        Node b = g.addNode(intInNode(), 100, 50);

        g.connect(src.id(), 0, a.id(), 0);
        g.connect(src.id(), 0, b.id(), 0);

        assertEquals(2, r.events.size());
        assertSame(ConnectionEvent.Kind.CREATED, r.events.get(0).kind());
        assertSame(ConnectionEvent.Kind.CREATED, r.events.get(1).kind());
    }

    @Test
    void undoPathViaAddAndDisconnectFiresConsistently() {
        // Mirrors ConnectCommand.undo(): disconnect(ours) then addConnection(old).
        NodeGraph g = new NodeGraph(types.reg);
        Recorder r = attach(g);
        Node src1 = g.addNode(intOutNode(), 0, 0);
        Node src2 = g.addNode(intOutNode(), 0, 50);
        Node sink = g.addNode(intInNode(), 100, 0);

        Connection first = g.connect(src1.id(), 0, sink.id(), 0);
        Connection second = g.connect(src2.id(), 0, sink.id(), 0); // replaces first
        r.events.clear();

        // Simulate undo of the second connect: remove second, restore first.
        g.disconnect(second);
        g.addConnection(first);

        assertEquals(2, r.events.size());
        assertSame(ConnectionEvent.Kind.REMOVED, r.events.get(0).kind());
        assertSame(second, r.events.get(0).connection());
        assertSame(ConnectionEvent.Kind.CREATED, r.events.get(1).kind());
        assertSame(first, r.events.get(1).connection());
        assertSame(first, g.inputConnection(sink.id(), 0).orElseThrow());
    }
}
