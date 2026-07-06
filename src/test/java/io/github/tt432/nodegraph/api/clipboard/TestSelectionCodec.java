package io.github.tt432.nodegraph.api.clipboard;

import io.github.tt432.nodegraph.api.def.InputWidgetSpec;
import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.def.PortSpec;
import io.github.tt432.nodegraph.api.model.Connection;
import io.github.tt432.nodegraph.api.model.InputWidgetKind;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeGroup;
import io.github.tt432.nodegraph.api.model.NodeGroupId;
import io.github.tt432.nodegraph.api.model.NodeId;
import io.github.tt432.nodegraph.api.model.TypedValue;
import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeConversionRule;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestSelectionCodec {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
        final Type bite = reg.register("byte", 0xFF00AA00);
        final TypeConversionRule intToByte = reg.registerConversion(inte, bite, v -> ((Number) v).byteValue());
    }

    private final Types types = new Types();

    // ---- builders ----------------------------------------------------------

    private static ResourceLocation rl(String id) {
        return new ResourceLocation("nodegraph", id);
    }

    private static PortSpec port(String key, Type t) {
        return new PortSpec(key, new TypedValue(Component.literal(key), t, Component.literal("desc")));
    }

    private static InputWidgetSpec widget(String key, Type t, Object defVal) {
        return new InputWidgetSpec(key,
                new TypedValue(Component.literal(key), t, Component.literal("desc")),
                InputWidgetKind.TEXT, defVal);
    }

    /** Constant-ish node: one widget "v", one output "out" = widget v. */
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

    /** A node with two inputs (for testing partial connection capture). */
    private NodeDefinition twoInputNode(String id, Type t) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in0", t), port("in1", t)),
                List.of(),
                (inputs, widgets) -> Map.of());
    }

    private static Component header(String s) {
        return Component.literal(s);
    }

    // ---- encode ------------------------------------------------------------

    @Test
    void encodeCapturesNodePositionHeaderAndWidgetValues() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 12.5, 70);
        a.setHeader(header("renamed"));
        a.widgets().get(0).setCurrentValue(42);

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id()), List.of());
        assertEquals(1, snap.nodes().size());
        NodeSnapshot ns = snap.nodes().get(0);
        assertEquals(12.5, ns.x());
        assertEquals(70, ns.y());
        assertEquals("renamed", ns.header().getString());
        assertEquals(rl("a"), ns.defId());
        assertEquals(42, ns.widgetValues().get("v"));
        assertNull(ns.localGroupId());
        assertTrue(snap.groups().isEmpty());
        assertTrue(snap.connections().isEmpty());
    }

    @Test
    void encodeCapturesGroupTransform() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeGroup grp = g.createGroup(header("g"), 10, 20, 300, 200);
        grp.setScale(2.5);

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(), List.of(grp.id()));
        assertEquals(1, snap.groups().size());
        GroupSnapshot gs = snap.groups().get(0);
        assertEquals(0L, gs.localId());
        assertEquals("g", gs.header().getString());
        assertEquals(10, gs.x());
        assertEquals(20, gs.y());
        assertEquals(300, gs.w());
        assertEquals(200, gs.h());
        assertEquals(2.5, gs.scale());
    }

    @Test
    void encodeKeepsOnlyInternalConnections() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(passthrough("b", types.inte), 100, 0);
        Node c = g.addNode(passthrough("c", types.inte), 200, 0);  // not selected
        g.connect(a.id(), 0, b.id(), 0);   // internal
        g.connect(b.id(), 0, c.id(), 0);   // external (c not selected)

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id(), b.id()), List.of());
        assertEquals(1, snap.connections().size());
        ConnectionSnapshot cs = snap.connections().get(0);
        assertEquals(0L, cs.fromLocalId()); // a is local 0
        assertEquals(0, cs.fromOutput());
        assertEquals(1L, cs.toLocalId());   // b is local 1
        assertEquals(0, cs.toInput());
    }

    @Test
    void encodeMapsGroupIdOnlyWhenGroupIsAlsoSelected() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        NodeGroup grpIn = g.createGroup(header("g1"), 0, 0, 100, 100);
        NodeGroup grpOut = g.createGroup(header("g2"), 0, 0, 100, 100);
        g.setNodeGroup(a.id(), grpIn.id());

        // a's group is grpIn; we select grpOut (not grpIn) — localGroupId should be null
        SelectionSnapshot snapWrong = SelectionCodec.encode(g, List.of(a.id()), List.of(grpOut.id()));
        assertNull(snapWrong.nodes().get(0).localGroupId());

        // now select grpIn too — localGroupId should resolve to grpIn's local id (0)
        SelectionSnapshot snapRight = SelectionCodec.encode(g, List.of(a.id()), List.of(grpIn.id()));
        assertEquals(0L, snapRight.nodes().get(0).localGroupId());
    }

    @Test
    void encodeDefinitionsDeduplicated() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeDefinition def = srcNode("a", types.inte, 1);
        Node a = g.addNode(def, 0, 0);
        Node b = g.addNode(def, 100, 0); // same def

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id(), b.id()), List.of());
        assertEquals(2, snap.nodes().size());
        assertEquals(1, snap.definitions().size(), "definition must be deduplicated");
        assertTrue(snap.definitions().containsKey(rl("a")));
    }

    @Test
    void encodeEmptySelectionProducesEmptySnapshot() {
        NodeGraph g = new NodeGraph(types.reg);
        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(), List.of());
        assertTrue(snap.nodes().isEmpty());
        assertTrue(snap.groups().isEmpty());
        assertTrue(snap.connections().isEmpty());
        assertTrue(snap.definitions().isEmpty());
    }

    @Test
    void encodeUnknownNodeThrows() {
        NodeGraph g = new NodeGraph(types.reg);
        assertThrows(IllegalArgumentException.class,
                () -> SelectionCodec.encode(g, List.of(new NodeId(999)), List.of()));
    }

    @Test
    void encodeUnknownGroupThrows() {
        NodeGraph g = new NodeGraph(types.reg);
        assertThrows(IllegalArgumentException.class,
                () -> SelectionCodec.encode(g, List.of(), List.of(new NodeGroupId(999))));
    }

    // ---- paste -------------------------------------------------------------

    @Test
    void pasteCreatesFreshNodesWithOffset() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 10, 20);

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id()), List.of());
        PasteCommand cmd = SelectionCodec.decode(snap, g, 100, 200);
        cmd.execute();

        assertEquals(2, g.nodes().size(), "original + pasted");
        assertEquals(1, cmd.pastedNodeIds().size());
        NodeId pastedId = cmd.pastedNodeIds().get(0);
        assertNotEquals(a.id(), pastedId);
        assertEquals(110, g.node(pastedId).x());
        assertEquals(220, g.node(pastedId).y());
    }

    @Test
    void pasteRestoresHeaderAndWidgetValues() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        a.setHeader(header("renamed"));
        a.widgets().get(0).setCurrentValue(77);

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id()), List.of());
        SelectionCodec.decode(snap, g, 0, 0).execute();

        NodeId pastedId = g.nodes().stream()
                .map(Node::id).filter(id -> !id.equals(a.id())).findFirst().orElseThrow();
        Node pasted = g.node(pastedId);
        assertEquals("renamed", pasted.header().getString());
        assertEquals(77, pasted.widgets().get(0).currentValue());
    }

    @Test
    void pasteRemapsConnectionIdsAndKeepsAutoConvertedAndRule() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 5), 0, 0);  // outputs int
        Node b = g.addNode(passthrough("b", types.bite), 0, 0); // input byte, requires int->byte
        // int -> byte auto-converted wire (rule = intToByte)
        g.connect(a.id(), 0, b.id(), 0);

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id(), b.id()), List.of());
        SelectionCodec.decode(snap, g, 0, 0).execute();

        // After paste: 4 nodes, 2 connections total (original + pasted). Past wire must be auto.
        List<Connection> all = g.connections();
        assertEquals(2, all.size());
        Connection original = all.stream().filter(c -> c.fromNode().equals(a.id())).findFirst().orElseThrow();
        Connection pasted = all.stream().filter(c -> !c.fromNode().equals(a.id())).findFirst().orElseThrow();

        assertTrue(original.isAutoConverted());
        assertTrue(pasted.isAutoConverted(), "pasted connection must preserve autoConverted flag");
        assertSame(types.intToByte, pasted.rule(), "pasted connection must reuse the same rule object");
        assertNotEquals(original.fromNode(), pasted.fromNode());
        assertNotEquals(original.toNode(), pasted.toNode());
    }

    @Test
    void pasteRestoresGroupAndSubsetMembership() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(srcNode("b", types.inte, 2), 50, 0);
        NodeGroup grp = g.createGroup(header("g"), 0, 0, 300, 200);
        grp.setScale(1.5);
        g.setNodeGroup(a.id(), grp.id());
        g.setNodeGroup(b.id(), grp.id());

        // copy only a + grp (subset) — pasted group should contain only the pasted a
        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id()), List.of(grp.id()));
        SelectionCodec.decode(snap, g, 0, 0).execute();

        assertEquals(2, g.groups().size());
        NodeGroup pastedGrp = g.groups().stream()
                .filter(gr -> !gr.id().equals(grp.id())).findFirst().orElseThrow();
        assertEquals(1.5, pastedGrp.scale());
        assertEquals(1, g.members(pastedGrp.id()).size(), "pasted group contains only the pasted node");
        NodeId pastedA = g.members(pastedGrp.id()).get(0).id();
        assertNotEquals(a.id(), pastedA);
    }

    @Test
    void pasteDoesNotMutateOriginalObjects() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 10, 20);
        a.setHeader(header("orig"));
        NodeGroup grp = g.createGroup(header("g"), 1, 2, 3, 4);
        grp.setScale(0.5);
        g.setNodeGroup(a.id(), grp.id());

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id()), List.of(grp.id()));
        SelectionCodec.decode(snap, g, 100, 100).execute();

        // originals untouched
        assertEquals(10, a.x());
        assertEquals(20, a.y());
        assertEquals("orig", a.header().getString());
        assertEquals(grp.id(), a.groupId());
        assertEquals(0.5, grp.scale());
        assertEquals(1, grp.x());
    }

    @Test
    void pasteTwiceProducesIndependentIds() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id()), List.of());

        PasteCommand c1 = SelectionCodec.decode(snap, g, 0, 0);
        c1.execute();
        PasteCommand c2 = SelectionCodec.decode(snap, g, 0, 0);
        c2.execute();

        NodeId p1 = c1.pastedNodeIds().get(0);
        NodeId p2 = c2.pastedNodeIds().get(0);
        assertNotEquals(p1, p2);
        assertEquals(3, g.nodes().size());
    }

    @Test
    void pasteCommandUndoRedoRepeatableWithStableIds() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id()), List.of());
        PasteCommand cmd = SelectionCodec.decode(snap, g, 0, 0);

        cmd.execute();
        assertEquals(2, g.nodes().size());
        NodeId pasted = cmd.pastedNodeIds().get(0);

        cmd.undo();
        assertEquals(1, g.nodes().size(), "undo removes pasted node");
        assertFalse(g.nodes().stream().anyMatch(n -> n.id().equals(pasted)));

        cmd.redo();
        assertEquals(2, g.nodes().size());
        assertEquals(pasted, cmd.pastedNodeIds().get(0), "redo reuses same id");

        // several cycles
        for (int i = 0; i < 4; i++) {
            cmd.undo();
            cmd.redo();
        }
        assertEquals(pasted, cmd.pastedNodeIds().get(0));
        assertEquals(2, g.nodes().size());
    }

    @Test
    void pasteUndoRemovesGroupAndCascadesConnections() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(srcNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(passthrough("b", types.inte), 100, 0);
        g.connect(a.id(), 0, b.id(), 0);

        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(a.id(), b.id()), List.of());
        PasteCommand cmd = SelectionCodec.decode(snap, g, 0, 0);
        cmd.execute();

        assertEquals(4, g.nodes().size());
        assertEquals(2, g.connections().size());

        cmd.undo();
        assertEquals(2, g.nodes().size());
        assertEquals(1, g.connections().size(), "pasted connection cascade-removed; original remains");
    }

    @Test
    void pasteAcrossGraphs() {
        NodeGraph src = new NodeGraph(types.reg);
        Node a = src.addNode(srcNode("a", types.inte, 9), 0, 0);
        Node b = src.addNode(passthrough("b", types.inte), 50, 0);
        src.connect(a.id(), 0, b.id(), 0);

        SelectionSnapshot snap = SelectionCodec.encode(src, List.of(a.id(), b.id()), List.of());

        NodeGraph dst = new NodeGraph(types.reg);
        assertTrue(dst.nodes().isEmpty());
        SelectionCodec.decode(snap, dst, 0, 0).execute();

        assertEquals(2, dst.nodes().size());
        assertEquals(1, dst.connections().size());
        Connection c = dst.connections().get(0);
        assertEquals(0, c.fromOutput());
        assertEquals(0, c.toInput());
    }

    @Test
    void pasteGroupPositionWithOffset() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeGroup grp = g.createGroup(header("g"), 10, 20, 100, 50);
        SelectionSnapshot snap = SelectionCodec.encode(g, List.of(), List.of(grp.id()));

        SelectionCodec.decode(snap, g, 5, 7).execute();

        NodeGroup pasted = g.groups().stream()
                .filter(gr -> !gr.id().equals(grp.id())).findFirst().orElseThrow();
        assertEquals(15, pasted.x());
        assertEquals(27, pasted.y());
        assertEquals(100, pasted.width());
        assertEquals(50, pasted.height());
    }
}
