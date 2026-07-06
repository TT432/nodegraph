package io.github.tt432.nodegraph.api.model;

/** Unique identifier of a node instance within a graph. */
public final class NodeId {
    private final long value;

    public NodeId(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof NodeId n) && value == n.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return "Node#" + value;
    }
}
