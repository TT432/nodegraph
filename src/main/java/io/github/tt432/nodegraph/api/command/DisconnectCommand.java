package io.github.tt432.nodegraph.api.command;

import io.github.tt432.nodegraph.api.model.Connection;
import io.github.tt432.nodegraph.api.model.NodeGraph;

import java.util.Objects;

/**
 * Removes a specific {@link Connection}. The connection object is captured so
 * {@link #undo()} can restore it via {@link NodeGraph#addConnection}.
 */
public final class DisconnectCommand implements Command {
    private final NodeGraph graph;
    private final Connection connection;

    public DisconnectCommand(NodeGraph graph, Connection connection) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public void execute() {
        graph.disconnect(connection);
    }

    @Override
    public void undo() {
        graph.addConnection(connection);
    }

    @Override
    public String description() {
        return "Disconnect";
    }
}
