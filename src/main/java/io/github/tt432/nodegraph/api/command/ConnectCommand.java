package io.github.tt432.nodegraph.api.command;

import io.github.tt432.nodegraph.api.model.Connection;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeId;

import java.util.Objects;

/**
 * Connects {@code (fromNode, fromOutput) -> (toNode, toInput)}.
 *
 * <p>On every {@link #execute()} against a <b>single-input</b> target node the
 * current single-source connection driving {@code toInput} (if any) is
 * captured, then {@link NodeGraph#connect} is called — which replaces the
 * existing connection under single-source semantics. Against a
 * <b>multi-input</b> target node nothing is captured: the new wire accumulates
 * next to the existing ones. {@link #undo()} removes the connection we created
 * (located by the endpoint quadruple) and, only when a replaced connection was
 * captured, restores it via {@link NodeGraph#addConnection}.
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
        // Multi-input targets accumulate wires; there is no replaced
        // connection to capture. Single-input targets keep capture semantics.
        oldConnection = graph.node(toNode).definition().isMultiInput()
                ? null
                : graph.inputConnection(toNode, toInput).orElse(null);
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
