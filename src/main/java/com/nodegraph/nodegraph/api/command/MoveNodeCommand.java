package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeId;

import java.util.Objects;

/**
 * Moves a node to a new (x, y) position. Captures the previous position on
 * first {@link #execute()} so {@link #undo()} restores it.
 */
public final class MoveNodeCommand implements Command {
    private final NodeGraph graph;
    private final NodeId target;
    private final double newX;
    private final double newY;
    private double oldX;
    private double oldY;
    private boolean captured = false;

    public MoveNodeCommand(NodeGraph graph, NodeId target, double newX, double newY) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.target = Objects.requireNonNull(target, "target");
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public void execute() {
        Node node = graph.node(target);
        if (!captured) {
            oldX = node.x();
            oldY = node.y();
            captured = true;
        }
        node.setPosition(newX, newY);
    }

    @Override
    public void undo() {
        graph.node(target).setPosition(oldX, oldY);
    }

    @Override
    public String description() {
        return "Move node";
    }
}
