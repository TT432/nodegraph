package io.github.tt432.nodegraph.api.command;

import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeGroup;
import io.github.tt432.nodegraph.api.model.NodeGroupId;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Sets a node group's header {@link Component}. Captures the previous header
 * on first {@link #execute()} for {@link #undo()}.
 */
public final class SetGroupHeaderCommand implements Command {
    private final NodeGraph graph;
    private final NodeGroupId target;
    private final Component newHeader;
    private Component oldHeader;
    private boolean captured = false;

    public SetGroupHeaderCommand(NodeGraph graph, NodeGroupId target, Component newHeader) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.target = Objects.requireNonNull(target, "target");
        this.newHeader = Objects.requireNonNull(newHeader, "newHeader");
    }

    @Override
    public void execute() {
        NodeGroup g = graph.group(target);
        if (!captured) {
            oldHeader = g.header();
            captured = true;
        }
        g.setHeader(newHeader);
    }

    @Override
    public void undo() {
        graph.group(target).setHeader(oldHeader);
    }

    @Override
    public String description() {
        return "Rename group";
    }
}
