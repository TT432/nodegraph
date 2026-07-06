package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroup;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeId;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Atomically create a node group and join a fixed list of member nodes into it.
 * Single undo unit — avoids the two-step apply limitation noted in
 * {@code CreateGroupCommand} (gid only allocated on first execute, so a
 * CompositeCommand of {@code SetNodeGroupCommand}s cannot be wired at
 * construction time).
 *
 * <p>Lazy-capture idiom (same as {@code PasteCommand}): first {@link #execute()}
 * captures each member's previous {@code groupId} (so {@link #undo()} can
 * restore cross-group membership), allocates the group, and joins the members.
 * Subsequent calls (redo) re-insert the stashed group object (stable id via
 * {@link NodeGraph#insertGroup}) and re-join the members.
 *
 * <p>Undo removes the group (which clears members' groupId) then restores each
 * member to its captured previous group (null = was ungrouped).
 */
public final class GroupNodesCommand implements Command {
    private final NodeGraph graph;
    private final Component header;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final List<NodeId> members;

    private NodeGroup savedGroup;
    private boolean executed = false;
    private final Map<NodeId, NodeGroupId> oldGroups = new LinkedHashMap<>();

    public GroupNodesCommand(NodeGraph graph, Component header,
                             double x, double y, double width, double height,
                             List<NodeId> members) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.header = Objects.requireNonNull(header, "header");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.members = List.copyOf(Objects.requireNonNull(members, "members"));
    }

    @Override
    public void execute() {
        if (!executed) {
            oldGroups.clear();
            for (NodeId m : members) {
                oldGroups.put(m, graph.node(m).groupId());
            }
            savedGroup = graph.createGroup(header, x, y, width, height);
            NodeGroupId gid = savedGroup.id();
            for (NodeId m : members) {
                graph.setNodeGroup(m, gid);
            }
            executed = true;
        } else {
            graph.insertGroup(savedGroup);
            NodeGroupId gid = savedGroup.id();
            for (NodeId m : members) {
                graph.setNodeGroup(m, gid);
            }
        }
    }

    @Override
    public void undo() {
        graph.removeGroup(savedGroup.id());
        for (Map.Entry<NodeId, NodeGroupId> e : oldGroups.entrySet()) {
            graph.setNodeGroup(e.getKey(), e.getValue());
        }
    }

    @Override
    public String description() {
        return "Group";
    }

    /** The created group; null until first {@link #execute()}. */
    public NodeGroup group() {
        return savedGroup;
    }

    /** Immutable copy of the member ids this command joins. */
    public List<NodeId> members() {
        return members;
    }
}
