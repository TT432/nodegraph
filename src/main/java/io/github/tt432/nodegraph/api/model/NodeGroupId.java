package io.github.tt432.nodegraph.api.model;

/** Unique identifier of a node group within a graph. Distinct from {@link NodeId}. */
public final class NodeGroupId {
    private final long value;

    public NodeGroupId(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof NodeGroupId g) && value == g.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public String toString() {
        return "Group#" + value;
    }
}
