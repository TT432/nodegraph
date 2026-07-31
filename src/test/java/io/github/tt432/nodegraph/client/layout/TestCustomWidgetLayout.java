package io.github.tt432.nodegraph.client.layout;

import io.github.tt432.nodegraph.api.def.InputWidgetSpec;
import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.def.PortSpec;
import io.github.tt432.nodegraph.api.model.InputWidgetKind;
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

/**
 * 可变行高（CUSTOM widget）下的布局：widgetsHeight 逐行累计、inputWidget 矩形偏移、
 * portRowsTop 与总 height 跟随、pickInputWidget 命中可变高行。
 */
class TestCustomWidgetLayout {

    private static final Type T = new Type("t", 0xFF112233);

    private NodeDefinition def(List<InputWidgetSpec> widgets, int inputs, int outputs) {
        java.util.List<PortSpec> in = new java.util.ArrayList<>();
        for (int i = 0; i < inputs; i++) {
            in.add(new PortSpec("in" + i, new TypedValue(Component.literal("i" + i), T, Component.literal(""))));
        }
        java.util.List<PortSpec> out = new java.util.ArrayList<>();
        for (int i = 0; i < outputs; i++) {
            out.add(new PortSpec("out" + i, new TypedValue(Component.literal("o" + i), T, Component.literal(""))));
        }
        return new NodeDefinition(rl("test/custom"), Component.literal("n"), widgets, in, out,
                (inp, w) -> Map.of());
    }

    private InputWidgetSpec widget(String key, InputWidgetKind kind, double height) {
        return new InputWidgetSpec(key, new TypedValue(Component.literal(key), T, Component.literal("")),
                kind, "", height);
    }

    @Test
    void widgetsHeightAccumulatesPerRowHeights() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(List.of(
                widget("a", InputWidgetKind.TEXT, 14),
                widget("preview", InputWidgetKind.CUSTOM, 64),
                widget("b", InputWidgetKind.TEXT, 14)), 0, 0), 0, 0);
        NodeLayout layout = new NodeLayout(n);
        assertEquals(14 + 64 + 14, layout.widgetsHeight(), 1e-9);
        assertEquals(NodeLayout.HEADER_HEIGHT + 92, layout.height(), 1e-9);
    }

    @Test
    void inputWidgetRectOffsetsByPriorHeights() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(List.of(
                widget("a", InputWidgetKind.TEXT, 14),
                widget("preview", InputWidgetKind.CUSTOM, 64),
                widget("b", InputWidgetKind.TEXT, 14)), 0, 0), 10, 20);
        NodeLayout layout = new NodeLayout(n);
        NodeLayout.Rect row0 = layout.inputWidget(0);
        NodeLayout.Rect row1 = layout.inputWidget(1);
        NodeLayout.Rect row2 = layout.inputWidget(2);
        assertEquals(20 + NodeLayout.HEADER_HEIGHT, row0.y(), 1e-9);
        assertEquals(14, row0.h(), 1e-9);
        assertEquals(row0.y() + 14, row1.y(), 1e-9);
        assertEquals(64, row1.h(), 1e-9);
        assertEquals(row1.y() + 64, row2.y(), 1e-9);
        assertEquals(14, row2.h(), 1e-9);
        assertEquals(10, row1.x(), 1e-9);
        assertEquals(NodeLayout.NODE_WIDTH, row1.w(), 1e-9);
    }

    @Test
    void portRowsTopFollowsWidgetsHeight() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(List.of(widget("preview", InputWidgetKind.CUSTOM, 64)), 1, 1), 0, 0);
        NodeLayout layout = new NodeLayout(n);
        assertEquals(NodeLayout.HEADER_HEIGHT + 64, layout.portRowsTop(), 1e-9);
        assertEquals(layout.portRowsTop() + NodeLayout.ROW_HEIGHT / 2.0, layout.inputPort(0).y(), 1e-9);
    }

    @Test
    void pickInputWidgetHitsVariableHeightRow() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(List.of(
                widget("a", InputWidgetKind.TEXT, 14),
                widget("preview", InputWidgetKind.CUSTOM, 64)), 0, 0), 0, 0);
        NodeLayout layout = new NodeLayout(n);
        double midCustom = NodeLayout.HEADER_HEIGHT + 14 + 32;
        assertEquals(1, layout.pickInputWidget(NodeLayout.PADDING + 1, midCustom).orElse(-1));
        assertEquals(0, layout.pickInputWidget(NodeLayout.PADDING + 1, NodeLayout.HEADER_HEIGHT + 7).orElse(-1));
        assertTrue(layout.pickInputWidget(NodeLayout.PADDING + 1, NodeLayout.HEADER_HEIGHT + 14 + 64 + 1).isEmpty());
    }

    @Test
    void defaultHeightMatchesStandardRow() {
        InputWidgetSpec spec = new InputWidgetSpec("k",
                new TypedValue(Component.literal("k"), T, Component.literal("")), InputWidgetKind.TEXT, "");
        assertEquals(InputWidgetSpec.DEFAULT_HEIGHT, spec.height(), 1e-9);
        assertEquals(NodeLayout.ROW_HEIGHT, InputWidgetSpec.DEFAULT_HEIGHT, 1e-9);
    }

    @Test
    void addNodeCopiesSpecHeightToInstance() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(List.of(widget("preview", InputWidgetKind.CUSTOM, 48)), 0, 0), 0, 0);
        assertEquals(48, n.widgets().get(0).height(), 1e-9);
    }

    @Test
    void negativeHeightRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> widget("x", InputWidgetKind.CUSTOM, 0));
    }
}
