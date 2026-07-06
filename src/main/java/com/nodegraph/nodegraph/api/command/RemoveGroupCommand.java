package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeGroup;
import com.nodegraph.nodegraph.api.model.NodeId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Removes a node group, capturing the group frame and the ids of all its
 * member nodes so {@link #undo()} can restore both.
 *
 * <p>On first {@link #execute()} the current members are derived from
 * {@link NodeGraph#members} and stashed; {@link NodeGraph#removeGroup} then
 * clears each member's {@code groupId}. {@link #undo()} re-inserts the group
 * (stable id via {@link NodeGraph#insertGroup}) and re-assigns each stashed
 * member back into it.
 *
 * <p>Redo simply removes the group again (membership cascade repeats).
 */
public final class RemoveGroupCommand implements Command {
    private final NodeGraph graph;
    private final NodeGroupId target;
    private NodeGroup savedGroup;
    private List<NodeId> savedMembers;
    private boolean executed = false;

    public RemoveGroupCommand(NodeGraph graph, NodeGroupId target) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override
    public void execute() {
        if (!executed) {
            savedGroup = graph.group(target);
            List<NodeId> members = new ArrayList<>();
            for (Node n : graph.members(target)) {
                members.add(n.id());
            }
            savedMembers = members;
            executed = true;
        }
        graph.removeGroup(target);
    }

    @Override
    public void undo() {
        graph.insertGroup(savedGroup);
        for (NodeId memberId : savedMembers) {
            graph.setNodeGroup(memberId, savedGroup.id());
        }
    }

    @Override
    public String description() {
        return "Remove group";
    }
}
