package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroup;
import com.nodegraph.nodegraph.api.model.NodeGroupId;

import java.util.Objects;

/**
 * Sets a group's geometric transform (position, size, scale) atomically.
 * Captures the previous transform on first {@link #execute()} for
 * {@link #undo()}.
 */
public final class SetGroupTransformCommand implements Command {
    private final NodeGraph graph;
    private final NodeGroupId target;
    private final double newX, newY, newW, newH, newScale;
    private double oldX, oldY, oldW, oldH, oldScale;
    private boolean captured = false;

    public SetGroupTransformCommand(NodeGraph graph, NodeGroupId target,
                                    double newX, double newY,
                                    double newW, double newH,
                                    double newScale) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.target = Objects.requireNonNull(target, "target");
        this.newX = newX;
        this.newY = newY;
        this.newW = newW;
        this.newH = newH;
        this.newScale = newScale;
    }

    @Override
    public void execute() {
        NodeGroup g = graph.group(target);
        if (!captured) {
            oldX = g.x();
            oldY = g.y();
            oldW = g.width();
            oldH = g.height();
            oldScale = g.scale();
            captured = true;
        }
        g.setPosition(newX, newY);
        g.setSize(newW, newH);
        g.setScale(newScale);
    }

    @Override
    public void undo() {
        NodeGroup g = graph.group(target);
        g.setPosition(oldX, oldY);
        g.setSize(oldW, oldH);
        g.setScale(oldScale);
    }

    @Override
    public String description() {
        return "Transform group";
    }
}
