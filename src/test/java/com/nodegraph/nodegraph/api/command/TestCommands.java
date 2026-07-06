package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.def.InputWidgetSpec;
import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.def.PortSpec;
import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.InputWidgetKind;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroup;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeId;
import com.nodegraph.nodegraph.api.model.TypedValue;
import com.nodegraph.nodegraph.api.type.Type;
import com.nodegraph.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestCommands {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
    }

    private final Types types = new Types();

    // ---- builders ----------------------------------------------------------

    private static ResourceLocation rl(String id) {
        return new ResourceLocation("nodegraph", id);
    }

    private static PortSpec port(String key, Type t) {
        return new PortSpec(key, new TypedValue(Component.literal(key), t, Component.literal("d")));
    }

    private static InputWidgetSpec widget(String key, Type t, Object defVal) {
        return new InputWidgetSpec(key,
                new TypedValue(Component.literal(key), t, Component.literal("d")),
                InputWidgetKind.TEXT, defVal);
    }

    /** one widget "v", one input "in", one output "out" = widget v (or input if connected). */
    private NodeDefinition srcNode(String id, Type t, Object value) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(widget("v", t, value)),
                List.of(),
                List.of(port("out", t)),
                (inputs, widgets) -> Map.of("out", widgets.get("v")));
    }

    private NodeDefinition passthrough(String id, Type t) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in", t)),
                List.of(port("out", t)),
                (inputs, widgets) -> Map.of("out", inputs.get("in")));
    }

    // ---- AddNodeCommand ----------------------------------------------------

    @Test
    void addNodeUndoRedoKeepsIdStable() {
        NodeGraph g = new NodeGraph(types.reg);
        AddNodeCommand cmd = new AddNodeCommand(g, srcNode("a", types.inte, 1), 10, 20);
        cmd.execute();
        NodeId id = cmd.node().id();
        assertEquals(1, g.nodes().size());
        assertEquals(10, g.node(id).x());
        cmd.undo();
        assertTrue(g.nodes().isEmpty());
        // redo — same id must come back
        cmd.redo();
        assertEquals(1, g.nodes().size());
        assertEquals(id, g.nodes().iterator().next().id());
        // repeat several cycles
        for (int i = 0; i < 5; i++) {
            cmd.undo();
            cmd.redo();
        }
        assertEquals(id, g.nodes().iterator().next().id());
    }

    @Test
    void addNodeUndoRedoRestoresCascadedConnections() {
        NodeGraph g = new NodeGraph(types.reg);
        AddNodeCommand addA = new AddNodeCommand(g, srcNode("a", types.inte, 7), 0, 0);
        AddNodeCommand addB = new AddNodeCommand(g, passthrough("b", types.inte), 100, 0);
        addA.execute();
        addB.execute();
        g.connect(addA.node().id(), 0, addB.node().id(), 0);
        assertEquals(1, g.connections().size());

        // undo addA -> connection (which references a) is cascaded away
        addA.undo();
        assertEquals(0, g.connections().size());
        assertEquals(1, g.nodes().size(), "only b should remain");

        // redo addA -> connection must reappear, referencing the same a id
        addA.redo();
        assertEquals(1, g.connections().size());
        Connection c = g.connections().get(0);
        assertEquals(addA.node().id(), c.fromNode());
        assertEquals(addB.node().id(), c.toNode());
    }

    // ---- RemoveNodeCommand -------------------------------------------------

    @Test
    void removeNodeUndoRedoRestoresNodeAndConnections() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 3), 0, 0);
        Node b = g.addNode(passthrough("b", types.inte), 100, 0);
        g.connect(a.id(), 0, b.id(), 0);

        RemoveNodeCommand cmd = new RemoveNodeCommand(g, a.id());
        cmd.execute();
        assertEquals(1, g.nodes().size());
        assertEquals(0, g.connections().size(), "connection referencing a should be cascaded");

        cmd.undo();
        assertEquals(2, g.nodes().size());
        assertEquals(1, g.connections().size(), "connection must be restored");
        // a's id should be the same
        assertNotNull(g.node(a.id()));

        cmd.redo();
        assertEquals(1, g.nodes().size());
        assertEquals(0, g.connections().size());
    }

    // ---- MoveNodeCommand ---------------------------------------------------

    @Test
    void moveNodeUndoRedo() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        MoveNodeCommand cmd = new MoveNodeCommand(g, a.id(), 50, 70);
        cmd.execute();
        assertEquals(50, a.x());
        assertEquals(70, a.y());
        cmd.undo();
        assertEquals(0, a.x());
        assertEquals(0, a.y());
        cmd.redo();
        assertEquals(50, a.x());
        assertEquals(70, a.y());
        cmd.undo();
        cmd.redo();
        assertEquals(50, a.x());
    }

    // ---- SetNodeHeaderCommand ---------------------------------------------

    @Test
    void setNodeHeaderUndoRedo() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Component original = a.header();
        Component renamed = Component.literal("renamed");
        SetNodeHeaderCommand cmd = new SetNodeHeaderCommand(g, a.id(), renamed);
        cmd.execute();
        assertSame(renamed, a.header());
        cmd.undo();
        assertSame(original, a.header());
        cmd.redo();
        assertSame(renamed, a.header());
    }

    // ---- SetWidgetValueCommand --------------------------------------------

    @Test
    void setWidgetValueUndoRedo() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        SetWidgetValueCommand cmd = new SetWidgetValueCommand(g, a.id(), "v", 42);
        cmd.execute();
        assertEquals(42, a.widgets().get(0).currentValue());
        cmd.undo();
        assertEquals(1, a.widgets().get(0).currentValue());
        cmd.redo();
        assertEquals(42, a.widgets().get(0).currentValue());
    }

    @Test
    void setWidgetValueUnknownKeyThrows() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        SetWidgetValueCommand cmd = new SetWidgetValueCommand(g, a.id(), "nope", 1);
        assertThrows(IllegalArgumentException.class, cmd::execute);
    }

    // ---- ConnectCommand ----------------------------------------------------

    @Test
    void connectReplacesExistingAndUndoRestores() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(srcNode("b", types.inte, 2), 0, 100);
        Node c = g.addNode(passthrough("c", types.inte), 100, 50);

        // initial: a -> c[0]
        g.connect(a.id(), 0, c.id(), 0);
        assertEquals(1, g.connections().size());
        Connection before = g.inputConnection(c.id(), 0).orElseThrow();

        // now connect b -> c[0], replacing a
        ConnectCommand cmd = new ConnectCommand(g, b.id(), 0, c.id(), 0);
        cmd.execute();
        Connection after = g.inputConnection(c.id(), 0).orElseThrow();
        assertEquals(b.id(), after.fromNode());
        assertEquals(1, g.connections().size(), "single-source: still one");

        cmd.undo();
        Connection restored = g.inputConnection(c.id(), 0).orElseThrow();
        assertEquals(a.id(), restored.fromNode());
        assertEquals(before, restored, "exact prior connection object restored");

        cmd.redo();
        Connection redo = g.inputConnection(c.id(), 0).orElseThrow();
        assertEquals(b.id(), redo.fromNode());
    }

    // ---- DisconnectCommand -------------------------------------------------

    @Test
    void disconnectUndoRestoresConnection() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(passthrough("b", types.inte), 100, 0);
        g.connect(a.id(), 0, b.id(), 0);
        Connection c = g.connections().get(0);

        DisconnectCommand cmd = new DisconnectCommand(g, c);
        cmd.execute();
        assertTrue(g.connections().isEmpty());

        cmd.undo();
        assertEquals(1, g.connections().size());
        assertSame(c, g.connections().get(0));

        cmd.redo();
        assertTrue(g.connections().isEmpty());
    }

    // ---- CreateGroupCommand ------------------------------------------------

    @Test
    void createGroupUndoRedoIdStable() {
        NodeGraph g = new NodeGraph(types.reg);
        CreateGroupCommand cmd = new CreateGroupCommand(g, Component.literal("g"), 0, 0, 200, 100);
        cmd.execute();
        NodeGroupId id = cmd.group().id();
        assertEquals(1, g.groups().size());
        cmd.undo();
        assertTrue(g.groups().isEmpty());
        cmd.redo();
        assertEquals(id, g.groups().iterator().next().id());
        for (int i = 0; i < 3; i++) {
            cmd.undo();
            cmd.redo();
        }
        assertEquals(id, g.groups().iterator().next().id());
    }

    // ---- RemoveGroupCommand ------------------------------------------------

    @Test
    void removeGroupUndoRestoresGroupAndMembers() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(srcNode("b", types.inte, 2), 50, 0);
        NodeGroup grp = g.createGroup(Component.literal("g"), 0, 0, 300, 200);
        g.setNodeGroup(a.id(), grp.id());
        g.setNodeGroup(b.id(), grp.id());
        assertEquals(2, g.members(grp.id()).size());

        RemoveGroupCommand cmd = new RemoveGroupCommand(g, grp.id());
        cmd.execute();
        assertTrue(g.groups().isEmpty());
        assertNull(g.node(a.id()).groupId());
        assertNull(g.node(b.id()).groupId());

        cmd.undo();
        assertEquals(1, g.groups().size());
        assertEquals(grp.id(), g.groups().iterator().next().id());
        assertEquals(2, g.members(grp.id()).size());
        assertEquals(grp.id(), g.node(a.id()).groupId());
        assertEquals(grp.id(), g.node(b.id()).groupId());

        cmd.redo();
        assertTrue(g.groups().isEmpty());
        assertNull(g.node(a.id()).groupId());
    }

    // ---- SetGroupHeaderCommand --------------------------------------------

    @Test
    void setGroupHeaderUndoRedo() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeGroup grp = g.createGroup(Component.literal("g"), 0, 0, 10, 10);
        Component renamed = Component.literal("G2");
        SetGroupHeaderCommand cmd = new SetGroupHeaderCommand(g, grp.id(), renamed);
        cmd.execute();
        assertSame(renamed, grp.header());
        cmd.undo();
        assertEquals(Component.literal("g").getString(), grp.header().getString());
        cmd.redo();
        assertSame(renamed, grp.header());
    }

    // ---- SetGroupTransformCommand -----------------------------------------

    @Test
    void setGroupTransformUndoRedo() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeGroup grp = g.createGroup(Component.literal("g"), 1, 2, 30, 40);
        // default scale 1.0 from createGroup
        assertEquals(1.0, grp.scale());
        SetGroupTransformCommand cmd =
                new SetGroupTransformCommand(g, grp.id(), 100, 200, 300, 400, 2.5);
        cmd.execute();
        assertEquals(100, grp.x());
        assertEquals(200, grp.y());
        assertEquals(300, grp.width());
        assertEquals(400, grp.height());
        assertEquals(2.5, grp.scale());

        cmd.undo();
        assertEquals(1, grp.x());
        assertEquals(2, grp.y());
        assertEquals(30, grp.width());
        assertEquals(40, grp.height());
        assertEquals(1.0, grp.scale());

        cmd.redo();
        assertEquals(100, grp.x());
        assertEquals(2.5, grp.scale());
    }

    // ---- SetNodeGroupCommand ----------------------------------------------

    @Test
    void setNodeGroupUndoRedo() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        NodeGroup grp = g.createGroup(Component.literal("g"), 0, 0, 100, 100);

        SetNodeGroupCommand join = new SetNodeGroupCommand(g, a.id(), grp.id());
        join.execute();
        assertEquals(grp.id(), a.groupId());

        join.undo();
        assertNull(a.groupId());

        join.redo();
        assertEquals(grp.id(), a.groupId());

        // remove via null
        SetNodeGroupCommand leave = new SetNodeGroupCommand(g, a.id(), null);
        leave.execute();
        assertNull(a.groupId());
        leave.undo();
        assertEquals(grp.id(), a.groupId());
    }

    // ---- CompositeCommand --------------------------------------------------

    /**
     * Composite runs children forward on execute, backward on undo.
     * Here we use MoveNodeCommand + SetNodeHeaderCommand on the same node:
     * no inter-child id dependency, isolating composite ordering semantics.
     */
    @Test
    void compositeExecutesForwardAndUndoesReverse() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Component oldHeader = a.header();
        Component newHeader = Component.literal("renamed");

        CompositeCommand cmd = new CompositeCommand("Move + rename",
                new MoveNodeCommand(g, a.id(), 50, 60),
                new SetNodeHeaderCommand(g, a.id(), newHeader));

        cmd.execute();
        assertEquals(50, a.x());
        assertEquals(60, a.y());
        assertSame(newHeader, a.header());

        cmd.undo();
        assertEquals(0, a.x());
        assertEquals(0, a.y());
        assertSame(oldHeader, a.header());

        cmd.redo();
        assertEquals(50, a.x());
        assertEquals(60, a.y());
        assertSame(newHeader, a.header());
    }

    /**
     * Realistic "combine selection into a group" use-case: the UI applies the
     * CreateGroup first (id is allocated), then builds a CompositeCommand for
     * the membership joins using the now-known id, and applies that composite.
     * This yields two undo steps but each is internally atomic; we test the
     * membership composite's atomicity here.
     */
    @Test
    void compositeJoinMembersAtomically() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(srcNode("b", types.inte, 2), 50, 0);
        NodeGroup grp = g.createGroup(Component.literal("g"), 0, 0, 300, 200);
        NodeGroupId gid = grp.id();

        CompositeCommand join = new CompositeCommand("Join both",
                new SetNodeGroupCommand(g, a.id(), gid),
                new SetNodeGroupCommand(g, b.id(), gid));

        join.execute();
        assertEquals(gid, a.groupId());
        assertEquals(gid, b.groupId());
        assertEquals(2, g.members(gid).size());

        join.undo();
        assertNull(a.groupId());
        assertNull(b.groupId());
        assertEquals(0, g.members(gid).size());

        join.redo();
        assertEquals(gid, a.groupId());
        assertEquals(gid, b.groupId());
    }
}
