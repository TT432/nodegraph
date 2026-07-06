package io.github.tt432.nodegraph.api.command;

import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeId;

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

    /**
     * Full constructor with the previous position supplied up front (used when
     * the caller already knows the origin — e.g. a live drag that mutated the
     * node during interaction and now commits a single command on release).
     * {@code captured} is set {@code true} so {@link #execute()} skips its
     * lazy capture and uses the supplied {@code oldX/oldY} for {@link #undo()}.
     */
    public MoveNodeCommand(NodeGraph graph, NodeId target,
                           double oldX, double oldY, double newX, double newY) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.target = Objects.requireNonNull(target, "target");
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
        this.captured = true;
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
