package io.github.tt432.nodegraph.api.model;

import io.github.tt432.nodegraph.api.type.Type;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * A named, typed, described value. The building block for ports and input
 * components. {@code name} and {@code description} are {@link Component}s
 * (i18n); {@code type} drives coloring and connection rules.
 */
public final class TypedValue {
    private final Component name;
    private final Type type;
    private final Component description;

    public TypedValue(Component name, Type type, Component description) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.description = Objects.requireNonNull(description, "description");
    }

    public Component name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public Component description() {
        return description;
    }
}
