package io.github.tt432.nodegraph.api.model;

import io.github.tt432.nodegraph.api.def.NodeDefinition;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A node instance in a graph. Built from a {@link NodeDefinition} which fixes
 * the port/widget schema; instance state is the position, the (mutable) header,
 * the widget current values, and the group membership.
 */
public final class Node {
    private final NodeId id;
    private final NodeDefinition definition;
    private Component header;
    private double x;
    private double y;
    private final List<InputWidget> widgets;
    private final List<Port> inputs;
    private final List<Port> outputs;
    private NodeGroupId groupId;
    /**
     * 可选子图（Blender Group 风格）：非空时双击节点进入该子图（{@code NodeGraphWidget}
     * 导航栈 push）。子图是普通 {@link NodeGraph}，生命周期随本节点（删除节点即抛弃）。
     * 子图内容通常是宿主的派生视图，复制/粘贴会跳过带子图的节点。
     */
    private @org.jetbrains.annotations.Nullable NodeGraph subgraph;
    /**
     * 节点状态描边色（ARGB，0 = 默认描边）。宿主用于表达派生状态（未解析引用红、
     * 初始状态金等）；是视觉属性而非用户数据，不参与撤销/剪贴板。
     */
    private int statusColor;

    public Node(NodeId id, NodeDefinition definition, Component header, double x, double y,
                List<InputWidget> widgets, List<Port> inputs, List<Port> outputs) {
        this.id = Objects.requireNonNull(id, "id");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.header = Objects.requireNonNull(header, "header");
        this.x = x;
        this.y = y;
        this.widgets = List.copyOf(widgets);
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
    }

    public NodeId id() {
        return id;
    }

    public NodeDefinition definition() {
        return definition;
    }

    public Component header() {
        return header;
    }

    public void setHeader(Component header) {
        this.header = Objects.requireNonNull(header, "header");
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public List<InputWidget> widgets() {
        return widgets;
    }

    public List<Port> inputs() {
        return inputs;
    }

    public List<Port> outputs() {
        return outputs;
    }

    public NodeGroupId groupId() {
        return groupId;
    }

    void setGroupId(NodeGroupId groupId) {
        this.groupId = groupId;
    }

    public @org.jetbrains.annotations.Nullable NodeGraph subgraph() {
        return subgraph;
    }

    public void setSubgraph(@org.jetbrains.annotations.Nullable NodeGraph subgraph) {
        this.subgraph = subgraph;
    }

    public boolean hasSubgraph() {
        return subgraph != null;
    }

    /** 状态描边色（ARGB）；0 = 默认描边。 */
    public int statusColor() {
        return statusColor;
    }

    public void setStatusColor(int statusColor) {
        this.statusColor = statusColor;
    }
}
