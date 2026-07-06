package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeId;

import java.util.Objects;
import java.util.Optional;

/**
 * Connects {@code (fromNode, fromOutput) -> (toNode, toInput)}.
 *
 * <p>On every {@link #execute()} the current single-source connection driving
 * {@code toInput} (if any) is captured, then {@link NodeGraph#connect} is
 * called — which will replace the existing connection under TaskA's
 * single-source semantics. {@link #undo()} removes the connection we created
 * (located by the endpoint quadruple) and restores the previously captured
 * connection via {@link NodeGraph#addConnection}.
 *
 * <p>Re-capturing on every {@code execute} is robust against the input state
 * being changed by other commands earlier in the undo stack; the linear undo
 * ordering of {@link UndoManager} guarantees the captured connection is the
 * one this command last left behind.
 */
public final class ConnectCommand implements Command {
    private final NodeGraph graph;
    private final NodeId fromNode;
    private final int fromOutput;
    private final NodeId toNode;
    private final int toInput;
    private Connection oldConnection;

    public ConnectCommand(NodeGraph graph,
                          NodeId fromNode, int fromOutput,
                          NodeId toNode, int toInput) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.fromNode = Objects.requireNonNull(fromNode, "fromNode");
        this.toNode = Objects.requireNonNull(toNode, "toNode");
        this.fromOutput = fromOutput;
        this.toInput = toInput;
    }

    @Override
    public void execute() {
        Optional<Connection> cur = graph.inputConnection(toNode, toInput);
        oldConnection = cur.orElse(null);
        graph.connect(fromNode, fromOutput, toNode, toInput);
    }

    @Override
    public void undo() {
        // Remove the connection this command produced (match by quadruple).
        for (Connection c : graph.connections()) {
            if (c.fromNode().equals(fromNode) && c.fromOutput() == fromOutput
                    && c.toNode().equals(toNode) && c.toInput() == toInput) {
                graph.disconnect(c);
                break;
            }
        }
        if (oldConnection != null) {
            graph.addConnection(oldConnection);
        }
    }

    @Override
    public String description() {
        return "Connect";
    }
}
