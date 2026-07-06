package io.github.tt432.nodegraph.api.type;

import java.util.Objects;

/**
 * A node-graph value type. The internal identity is the string {@code id};
 * the {@code color} is the ARGB color used for visual coding of ports.
 */
public final class Type {
    private final String id;
    private final int color;

    public Type(String id, int color) {
        this.id = Objects.requireNonNull(id, "id");
        this.color = color;
    }

    public String id() {
        return id;
    }

    /** ARGB color used to tint ports / wires of this type. */
    public int color() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Type t) && id.equals(t.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Type[" + id + "]";
    }
}
