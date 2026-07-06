package com.nodegraph.nodegraph.api.clipboard;

import com.nodegraph.nodegraph.api.command.UndoManager;
import com.nodegraph.nodegraph.api.def.InputWidgetSpec;
import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.def.PortSpec;
import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.InputWidgetKind;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroup;
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

class TestClipboard {

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

    private static Component header(String s) {
        return Component.literal(s);
    }

    // ---- copy / clear / hasContent ----------------------------------------

    @Test
    void copySetsContentAndClearDropsIt() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);

        Clipboard cb = new Clipboard();
        assertFalse(cb.hasContent());
        cb.copy(g, List.of(a.id()), List.of());
        assertTrue(cb.hasContent());
        assertNotNull(cb.snapshot());

        cb.clear();
        assertFalse(cb.hasContent());
        assertNull(cb.snapshot());
    }

    // ---- cut ---------------------------------------------------------------

    @Test
    void cutCopiesAndRemovesSelectionAsSingleCommand() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(srcNode("b", types.inte, 2), 50, 0);
        NodeGroup grp = g.createGroup(header("g"), 0, 0, 200, 100);
        g.setNodeGroup(a.id(), grp.id());

        UndoManager undo = new UndoManager();
        Clipboard cb = new Clipboard();
        int before = undo.undoDepth();

        cb.cut(g, List.of(a.id(), b.id()), List.of(grp.id()), undo);

        assertEquals(before + 1, undo.undoDepth(), "cut must be a single composite command");
        assertTrue(g.nodes().isEmpty());
        assertTrue(g.groups().isEmpty());
        assertTrue(cb.hasContent(), "cut must populate the clipboard");

        // undo restores everything
        undo.undo();
        assertEquals(2, g.nodes().size());
        assertEquals(1, g.groups().size());
        assertEquals(grp.id(), g.node(a.id()).groupId(), "membership restored");

        // redo removes again
        undo.redo();
        assertTrue(g.nodes().isEmpty());
        assertTrue(g.groups().isEmpty());
    }

    @Test
    void cutInternalConnectionRemovedAndUndoRestored() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(passthrough("b", types.inte), 100, 0);
        g.connect(a.id(), 0, b.id(), 0);
        assertEquals(1, g.connections().size());

        UndoManager undo = new UndoManager();
        Clipboard cb = new Clipboard();
        cb.cut(g, List.of(a.id(), b.id()), List.of(), undo);

        assertEquals(0, g.connections().size());

        undo.undo();
        assertEquals(1, g.connections().size(), "internal wire restored on undo");
        Connection c = g.connections().get(0);
        assertEquals(a.id(), c.fromNode());
        assertEquals(b.id(), c.toNode());
    }

    @Test
    void cutEmptySelectionDoesNotPushCommand() {
        NodeGraph g = new NodeGraph(types.reg);
        UndoManager undo = new UndoManager();
        Clipboard cb = new Clipboard();
        int before = undo.undoDepth();

        cb.cut(g, List.of(), List.of(), undo);
        assertEquals(before, undo.undoDepth(), "empty cut must not push");
    }

    // ---- paste -------------------------------------------------------------

    @Test
    void pasteEmptyClipboardReturnsNullAndDoesNotPush() {
        NodeGraph g = new NodeGraph(types.reg);
        UndoManager undo = new UndoManager();
        Clipboard cb = new Clipboard();
        int before = undo.undoDepth();

        PasteCommand cmd = cb.paste(g, undo, 0, 0);
        assertNull(cmd);
        assertEquals(before, undo.undoDepth());
    }

    @Test
    void pasteViaUndoManagerIsUndoableAndFiresListener() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        UndoManager undo = new UndoManager();
        Clipboard cb = new Clipboard();

        cb.copy(g, List.of(a.id()), List.of());

        int[] fires = {0};
        undo.addListener(() -> fires[0]++);

        PasteCommand cmd = cb.paste(g, undo, 10, 10);
        assertNotNull(cmd);
        assertEquals(1, cmd.pastedNodeIds().size());
        assertEquals(2, g.nodes().size());
        assertTrue(fires[0] > 0, "listener should fire on apply");

        int firesAfterApply = fires[0];
        assertTrue(undo.undo());
        assertEquals(1, g.nodes().size());
        assertTrue(fires[0] > firesAfterApply, "listener should fire on undo");

        int firesAfterUndo = fires[0];
        assertTrue(undo.redo());
        assertEquals(2, g.nodes().size());
        assertTrue(fires[0] > firesAfterUndo, "listener should fire on redo");
    }

    @Test
    void cutThenPasteProducesEquivalentContent() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 7), 0, 0);
        a.setHeader(header("orig"));
        a.widgets().get(0).setCurrentValue(99);

        UndoManager undo = new UndoManager();
        Clipboard cb = new Clipboard();

        cb.cut(g, List.of(a.id()), List.of(), undo);
        assertTrue(g.nodes().isEmpty());

        PasteCommand pasted = cb.paste(g, undo, 0, 0);
        assertNotNull(pasted);
        assertEquals(1, g.nodes().size());
        Node n = g.node(pasted.pastedNodeIds().get(0));
        assertEquals("orig", n.header().getString());
        assertEquals(99, n.widgets().get(0).currentValue());
        assertEquals(0, n.x());
        assertEquals(0, n.y());
    }
}
