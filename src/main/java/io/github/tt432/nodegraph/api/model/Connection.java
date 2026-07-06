package io.github.tt432.nodegraph.api.model;

import io.github.tt432.nodegraph.api.type.TypeConversionRule;

import java.util.Objects;

/**
 * An immutable directed wire from an output port to an input port.
 * Ports are addressed by {@code (nodeId, index)} rather than object reference
 * so that the relationship serializes trivially and avoids reference cycles.
 */
public final class Connection {
    private final NodeId fromNode;
    private final int fromOutput;
    private final NodeId toNode;
    private final int toInput;
    private final boolean autoConverted;
    private final TypeConversionRule rule;

    public Connection(NodeId fromNode, int fromOutput, NodeId toNode, int toInput,
                      boolean autoConverted, TypeConversionRule rule) {
        this.fromNode = Objects.requireNonNull(fromNode, "fromNode");
        this.fromOutput = fromOutput;
        this.toNode = Objects.requireNonNull(toNode, "toNode");
        this.toInput = toInput;
        this.autoConverted = autoConverted;
        this.rule = rule;
    }

    public NodeId fromNode() {
        return fromNode;
    }

    public int fromOutput() {
        return fromOutput;
    }

    public NodeId toNode() {
        return toNode;
    }

    public int toInput() {
        return toInput;
    }

    public boolean isAutoConverted() {
        return autoConverted;
    }

    /** Non-null iff {@link #isAutoConverted()}. */
    public TypeConversionRule rule() {
        return rule;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Connection c)
                && fromNode.equals(c.fromNode) && fromOutput == c.fromOutput
                && toNode.equals(c.toNode) && toInput == c.toInput;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromNode, fromOutput, toNode, toInput);
    }

    @Override
    public String toString() {
        return "Connection[" + fromNode + ".out" + fromOutput + " -> " + toNode + ".in" + toInput
                + (autoConverted ? " auto" : "") + "]";
    }
}
