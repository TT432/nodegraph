package com.nodegraph.nodegraph.api.model;

import com.nodegraph.nodegraph.api.def.NodeDefinition;
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
}
