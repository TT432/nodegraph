package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeId;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Sets a node's header {@link Component}. Captures the previous header on
 * first {@link #execute()} for {@link #undo()}.
 */
public final class SetNodeHeaderCommand implements Command {
    private final NodeGraph graph;
    private final NodeId target;
    private final Component newHeader;
    private Component oldHeader;
    private boolean captured = false;

    public SetNodeHeaderCommand(NodeGraph graph, NodeId target, Component newHeader) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.target = Objects.requireNonNull(target, "target");
        this.newHeader = Objects.requireNonNull(newHeader, "newHeader");
    }

    @Override
    public void execute() {
        Node node = graph.node(target);
        if (!captured) {
            oldHeader = node.header();
            captured = true;
        }
        node.setHeader(newHeader);
    }

    @Override
    public void undo() {
        graph.node(target).setHeader(oldHeader);
    }

    @Override
    public String description() {
        return "Rename node";
    }
}
