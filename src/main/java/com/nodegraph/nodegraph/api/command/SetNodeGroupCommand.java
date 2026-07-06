package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeId;

import java.util.Objects;

/**
 * Assigns (or removes, when {@code newGroup == null}) a node's group
 * membership. Captures the previous group id on first {@link #execute()} for
 * {@link #undo()}.
 */
public final class SetNodeGroupCommand implements Command {
    private final NodeGraph graph;
    private final NodeId node;
    private final NodeGroupId newGroup;
    private NodeGroupId oldGroup;
    private boolean captured = false;

    public SetNodeGroupCommand(NodeGraph graph, NodeId node, NodeGroupId newGroup) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.node = Objects.requireNonNull(node, "node");
        this.newGroup = newGroup; // null means "remove from any group"
    }

    @Override
    public void execute() {
        if (!captured) {
            oldGroup = graph.node(node).groupId();
            captured = true;
        }
        graph.setNodeGroup(node, newGroup);
    }

    @Override
    public void undo() {
        graph.setNodeGroup(node, oldGroup);
    }

    @Override
    public String description() {
        return "Set node group";
    }
}
