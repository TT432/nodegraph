package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Removes a node from a {@link NodeGraph}, capturing the node and all
 * connections that referenced it (which {@link NodeGraph#removeNode} cascades
 * away) so that {@link #undo()} can restore both the node and its wires.
 *
 * <p>On redo, the node is removed again; undo re-inserts the same node (stable
 * id) and its captured connections via the low-level recovery primitives.
 */
public final class RemoveNodeCommand implements Command {
    private final NodeGraph graph;
    private final NodeId target;
    private Node savedNode;
    private List<Connection> savedConnections;
    private boolean executed = false;

    public RemoveNodeCommand(NodeGraph graph, NodeId target) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override
    public void execute() {
        if (!executed) {
            savedNode = graph.node(target);
            List<Connection> deps = new ArrayList<>();
            for (Connection c : graph.connections()) {
                if (c.fromNode().equals(target) || c.toNode().equals(target)) {
                    deps.add(c);
                }
            }
            savedConnections = deps;
            executed = true;
        }
        graph.removeNode(target);
    }

    @Override
    public void undo() {
        graph.insertNode(savedNode);
        for (Connection c : savedConnections) {
            graph.addConnection(c);
        }
    }

    @Override
    public String description() {
        return "Remove node";
    }
}
