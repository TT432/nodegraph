package io.github.tt432.nodegraph.api.command;

import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.model.Connection;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adds a node to a {@link NodeGraph}. On first {@link #execute()} the node is
 * allocated by the graph (id assigned); the resulting {@link Node} reference
 * is stashed so that subsequent executions (redos) re-insert the <b>same</b>
 * node via {@link NodeGraph#insertNode}, keeping its id stable across
 * undo/redo cycles.
 *
 * <p>When the node is removed on undo, any connections that referenced it
 * (cascaded deletions from {@link NodeGraph#removeNode}) are captured and
 * restored together on redo.
 */
public final class AddNodeCommand implements Command {
    private final NodeGraph graph;
    private final NodeDefinition definition;
    private final double x;
    private final double y;
    private Node savedNode;
    private List<Connection> cascaded = List.of();

    public AddNodeCommand(NodeGraph graph, NodeDefinition definition, double x, double y) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.x = x;
        this.y = y;
    }

    @Override
    public void execute() {
        if (savedNode == null) {
            savedNode = graph.addNode(definition, x, y);
        } else {
            graph.insertNode(savedNode);
            for (Connection c : cascaded) {
                graph.addConnection(c);
            }
            cascaded = List.of();
        }
    }

    @Override
    public void undo() {
        NodeId id = savedNode.id();
        List<Connection> deps = new ArrayList<>();
        for (Connection c : graph.connections()) {
            if (c.fromNode().equals(id) || c.toNode().equals(id)) {
                deps.add(c);
            }
        }
        this.cascaded = deps;
        graph.removeNode(id);
    }

    public Node node() {
        return savedNode;
    }

    @Override
    public String description() {
        return "Add node";
    }
}
