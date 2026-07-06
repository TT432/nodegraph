package io.github.tt432.nodegraph.api.model;

import io.github.tt432.nodegraph.api.type.Type;
import net.minecraft.network.chat.Component;

/**
 * A single port (input or output) on a node. Immutable; identified within its
 * node by a stable {@code key}. Direction is implied by which list the port
 * lives in on {@link Node}.
 */
public final class Port {
    private final String key;
    private final TypedValue value;

    public Port(String key, TypedValue value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public TypedValue value() {
        return value;
    }

    public Type type() {
        return value.type();
    }

    public Component name() {
        return value.name();
    }

    public Component description() {
        return value.description();
    }
}
