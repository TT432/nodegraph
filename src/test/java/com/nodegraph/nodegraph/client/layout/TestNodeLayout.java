package com.nodegraph.nodegraph.client.layout;

import com.nodegraph.nodegraph.api.def.InputWidgetSpec;
import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.def.NodeFunction;
import com.nodegraph.nodegraph.api.def.PortSpec;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.TypedValue;
import com.nodegraph.nodegraph.api.type.Type;
import com.nodegraph.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestNodeLayout {

    private static final Type INT = new Type("int", 0xFFAA0000);
    private static final Type STR = new Type("string", 0xFF00AA00);

    private Component c(String s) {
        return Component.literal(s);
    }

    private NodeDefinition def(String id, int inputs, int outputs, int widgets) {
        List<PortSpec> ins = java.util.stream.IntStream.range(0, inputs)
                .mapToObj(i -> new PortSpec("in" + i, new TypedValue(c("in" + i), INT, c("din" + i))))
                .toList();
        List<PortSpec> outs = java.util.stream.IntStream.range(0, outputs)
                .mapToObj(i -> new PortSpec("out" + i, new TypedValue(c("out" + i), STR, c("dout" + i))))
                .toList();
        List<InputWidgetSpec> ws = java.util.stream.IntStream.range(0, widgets)
                .mapToObj(i -> new InputWidgetSpec("w" + i, new TypedValue(c("w" + i), INT, c("dw" + i)),
                        com.nodegraph.nodegraph.api.model.InputWidgetKind.TEXT, 0))
                .toList();
        return new NodeDefinition(new ResourceLocation("nodegraph", id), c("N"), ws, ins, outs,
                (NodeFunction) (Map<String, Object> in, Map<String, Object> w) -> Map.of());
    }

    private Node node(NodeGraph g, NodeDefinition d, double x, double y) {
        return g.addNode(d, x, y);
    }

    @Test
    void emptyNodeHeightIsHeaderOnly() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 0, 0, 0), 0, 0);
        NodeLayout l = new NodeLayout(n);
        assertEquals(NodeLayout.HEADER_HEIGHT, l.height());
        assertEquals(0, l.portRowCount());
        assertEquals(0, l.widgetRowCount());
    }

    @Test
    void heightGrowsWithWidgets() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 0, 0, 3), 0, 0);
        NodeLayout l = new NodeLayout(n);
        assertEquals(NodeLayout.HEADER_HEIGHT + 3 * NodeLayout.ROW_HEIGHT, l.height());
    }

    @Test
    void heightGrowsWithMaxPorts() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 2, 5, 0), 0, 0);
        NodeLayout l = new NodeLayout(n);
        assertEquals(5, l.portRowCount());
        assertEquals(NodeLayout.HEADER_HEIGHT + 5 * NodeLayout.ROW_HEIGHT, l.height());
    }

    @Test
    void headerRectAtNodeOrigin() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 0, 0, 0), 10, 20);
        NodeLayout l = new NodeLayout(n);
        NodeLayout.Rect h = l.header();
        assertEquals(10, h.x());
        assertEquals(20, h.y());
        assertEquals(NodeLayout.NODE_WIDTH, h.w());
        assertEquals(NodeLayout.HEADER_HEIGHT, h.h());
    }

    @Test
    void bodyRectBelowHeader() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 1, 1, 1), 0, 0);
        NodeLayout l = new NodeLayout(n);
        NodeLayout.Rect b = l.body();
        assertEquals(NodeLayout.HEADER_HEIGHT, b.y());
        assertEquals(l.height() - NodeLayout.HEADER_HEIGHT, b.h());
    }

    @Test
    void inputPortAnchorOnLeftEdge() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 2, 0, 0), 7, 3);
        NodeLayout l = new NodeLayout(n);
        assertEquals(7, l.inputPort(0).x());
        assertEquals(7, l.inputPort(1).x());
    }

    @Test
    void outputPortAnchorOnRightEdge() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 0, 2, 0), 7, 3);
        NodeLayout l = new NodeLayout(n);
        assertEquals(7 + NodeLayout.NODE_WIDTH, l.outputPort(0).x());
        assertEquals(7 + NodeLayout.NODE_WIDTH, l.outputPort(1).x());
    }

    @Test
    void portAnchorYCenteredInRow() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 3, 2, 1), 0, 0);
        NodeLayout l = new NodeLayout(n);
        double top = l.portRowsTop();
        for (int i = 0; i < 3; i++) {
            double expected = top + i * NodeLayout.ROW_HEIGHT + NodeLayout.ROW_HEIGHT / 2.0;
            assertEquals(expected, l.inputPort(i).y(), 1e-9);
        }
        for (int i = 0; i < 2; i++) {
            double expected = top + i * NodeLayout.ROW_HEIGHT + NodeLayout.ROW_HEIGHT / 2.0;
            assertEquals(expected, l.outputPort(i).y(), 1e-9);
        }
    }

    @Test
    void pickInputPortHitsWithinRadius() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 1, 0, 0), 0, 0);
        NodeLayout l = new NodeLayout(n);
        NodeLayout.PortAnchor a = l.inputPort(0);
        assertEquals(Optional.of(0), l.pickInputPort(a.x(), a.y()));
        assertEquals(Optional.of(0), l.pickInputPort(a.x() + NodeLayout.PORT_HIT_RADIUS - 0.001, a.y()));
        assertTrue(l.pickInputPort(a.x() + NodeLayout.PORT_HIT_RADIUS + 1, a.y()).isEmpty());
    }

    @Test
    void pickOutputPortHitsWithinRadius() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 0, 1, 0), 0, 0);
        NodeLayout l = new NodeLayout(n);
        NodeLayout.PortAnchor a = l.outputPort(0);
        assertEquals(Optional.of(0), l.pickOutputPort(a.x(), a.y()));
        assertTrue(l.pickOutputPort(a.x() - NodeLayout.PORT_HIT_RADIUS - 1, a.y()).isEmpty());
    }

    @Test
    void pickInputWidgetByRow() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 0, 0, 2), 0, 0);
        NodeLayout l = new NodeLayout(n);
        NodeLayout.Rect w0 = l.inputWidget(0);
        NodeLayout.Rect w1 = l.inputWidget(1);
        assertEquals(Optional.of(0), l.pickInputWidget(w0.x() + 1, w0.y() + 1));
        assertEquals(Optional.of(1), l.pickInputWidget(w1.x() + 1, w1.y() + 1));
        assertTrue(l.pickInputWidget(0, l.portRowsTop() + 5).isEmpty());
    }

    @Test
    void headerContains() {
        TypeRegistry reg = new TypeRegistry();
        NodeGraph g = new NodeGraph(reg);
        Node n = node(g, def("a", 0, 0, 0), 0, 0);
        NodeLayout l = new NodeLayout(n);
        assertTrue(l.headerContains(1, 1));
        assertFalse(l.headerContains(1, NodeLayout.HEADER_HEIGHT + 1));
    }
}
