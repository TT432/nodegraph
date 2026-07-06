package io.github.tt432.nodegraph.api.command;

import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeGroup;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Creates a node group. On first {@link #execute()} the group is allocated by
 * the graph (id assigned); the resulting {@link NodeGroup} is stashed so that
 * redos re-insert the <b>same</b> group via {@link NodeGraph#insertGroup},
 * keeping its id stable across undo/redo cycles.
 *
 * <p>Undoing only removes the group frame — it does <b>not</b> restore
 * membership of nodes that joined the group via {@link SetNodeGroupCommand},
 * because those are independent commands elsewhere on the undo stack. This
 * relies on {@link UndoManager}'s linear undo sequence (the join commands
 * will be undone before this create command is reached).
 */
public final class CreateGroupCommand implements Command {
    private final NodeGraph graph;
    private final Component header;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private NodeGroup savedGroup;

    public CreateGroupCommand(NodeGraph graph, Component header,
                              double x, double y, double width, double height) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.header = Objects.requireNonNull(header, "header");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void execute() {
        if (savedGroup == null) {
            savedGroup = graph.createGroup(header, x, y, width, height);
        } else {
            graph.insertGroup(savedGroup);
        }
    }

    @Override
    public void undo() {
        graph.removeGroup(savedGroup.id());
    }

    public NodeGroup group() {
        return savedGroup;
    }

    @Override
    public String description() {
        return "Create group";
    }
}
