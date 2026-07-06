package com.nodegraph.nodegraph.client.layout;

import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.Port;

import java.util.Objects;
import java.util.Optional;

/**
 * 节点的视觉几何（世界坐标系）。固定宽度策略：不依赖 Font 即可定出全部矩形与端口锚点，
 * 因此可被渲染（TaskF）、连线（TaskG）、命中检测（TaskH）共用同一几何源。
 *
 * <p>所有坐标为世界单位（= scale=1 时的像素）。布局：头部条在顶部，其下为先堆叠的
 * {@code InputWidget} 行，再是端口区（每行左=输入端口，右=输出端口；行数=max(inputs,outputs)）。
 *
 * <p>不变量见 {@docRoot docs/task/nodegraph/TaskF/规格.md}。
 */
public final class NodeLayout {
    public static final double NODE_WIDTH = 120.0;
    public static final double HEADER_HEIGHT = 14.0;
    public static final double ROW_HEIGHT = 14.0;
    public static final double PORT_RADIUS = 5.0;
    public static final double PORT_HIT_RADIUS = 9.0;
    public static final double PADDING = 4.0;

    public static final int HEADER_COLOR = 0xFF3A3A3A;
    public static final int BODY_COLOR = 0xFF2A2A2A;
    public static final int OUTLINE_COLOR = 0xFF6A6A6A;
    public static final int OUTLINE_HOVER = 0xFFE0E0E0;
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int WIDGET_VALUE_COLOR = 0xFFBBBBBB;

    public record Rect(double x, double y, double w, double h) {
        public boolean contains(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    public record PortAnchor(double x, double y, int index) {}

    private final Node node;

    public NodeLayout(Node node) {
        this.node = Objects.requireNonNull(node, "node");
    }

    public Node node() {
        return node;
    }

    public double width() {
        return NODE_WIDTH;
    }

    public int widgetRowCount() {
        return node.widgets().size();
    }

    public int portRowCount() {
        return Math.max(node.inputs().size(), node.outputs().size());
    }

    public double height() {
        return HEADER_HEIGHT + widgetRowCount() * ROW_HEIGHT + portRowCount() * ROW_HEIGHT;
    }

    public Rect bounds() {
        return new Rect(node.x(), node.y(), NODE_WIDTH, height());
    }

    public Rect header() {
        return new Rect(node.x(), node.y(), NODE_WIDTH, HEADER_HEIGHT);
    }

    public Rect body() {
        return new Rect(node.x(), node.y() + HEADER_HEIGHT, NODE_WIDTH, height() - HEADER_HEIGHT);
    }

    public Rect inputWidget(int i) {
        if (i < 0 || i >= widgetRowCount()) {
            throw new IndexOutOfBoundsException("inputWidget index " + i + " out of [0," + widgetRowCount() + ")");
        }
        return new Rect(node.x(), node.y() + HEADER_HEIGHT + i * ROW_HEIGHT, NODE_WIDTH, ROW_HEIGHT);
    }

    /** 端口区起始 y（世界）。 */
    public double portRowsTop() {
        return node.y() + HEADER_HEIGHT + widgetRowCount() * ROW_HEIGHT;
    }

    public PortAnchor inputPort(int i) {
        if (i < 0 || i >= node.inputs().size()) {
            throw new IndexOutOfBoundsException("inputPort index " + i + " out of [0," + node.inputs().size() + ")");
        }
        return new PortAnchor(node.x(), portRowsTop() + i * ROW_HEIGHT + ROW_HEIGHT / 2.0, i);
    }

    public PortAnchor outputPort(int i) {
        if (i < 0 || i >= node.outputs().size()) {
            throw new IndexOutOfBoundsException("outputPort index " + i + " out of [0," + node.outputs().size() + ")");
        }
        return new PortAnchor(node.x() + NODE_WIDTH, portRowsTop() + i * ROW_HEIGHT + ROW_HEIGHT / 2.0, i);
    }

    /** 输入端口标签矩形：端口点右侧到节点中部。 */
    public Rect inputPortLabel(int i) {
        PortAnchor a = inputPort(i);
        double lx = a.x() + PORT_RADIUS + PADDING;
        return new Rect(lx, a.y() - ROW_HEIGHT / 2.0, NODE_WIDTH / 2.0 - lx + node.x(), ROW_HEIGHT);
    }

    /** 输出端口标签矩形：节点中部到端口点左侧。 */
    public Rect outputPortLabel(int i) {
        PortAnchor a = outputPort(i);
        double rx = a.x() - PORT_RADIUS - PADDING;
        double left = node.x() + NODE_WIDTH / 2.0;
        return new Rect(left, a.y() - ROW_HEIGHT / 2.0, rx - left, ROW_HEIGHT);
    }

    public boolean headerContains(double wx, double wy) {
        return header().contains(wx, wy);
    }

    public Optional<Integer> pickInputPort(double wx, double wy) {
        for (int i = 0; i < node.inputs().size(); i++) {
            PortAnchor a = inputPort(i);
            double dx = wx - a.x();
            double dy = wy - a.y();
            if (dx * dx + dy * dy <= PORT_HIT_RADIUS * PORT_HIT_RADIUS) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public Optional<Integer> pickOutputPort(double wx, double wy) {
        for (int i = 0; i < node.outputs().size(); i++) {
            PortAnchor a = outputPort(i);
            double dx = wx - a.x();
            double dy = wy - a.y();
            if (dx * dx + dy * dy <= PORT_HIT_RADIUS * PORT_HIT_RADIUS) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public Optional<Integer> pickInputWidget(double wx, double wy) {
        for (int i = 0; i < widgetRowCount(); i++) {
            if (inputWidget(i).contains(wx, wy)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    /** 便于上层端口访问；不越界（调用方已用 pick*）。 */
    public Port inputPortData(int i) {
        return node.inputs().get(i);
    }

    public Port outputPortData(int i) {
        return node.outputs().get(i);
    }
}
