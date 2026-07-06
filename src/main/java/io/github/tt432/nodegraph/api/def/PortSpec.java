package io.github.tt432.nodegraph.api.def;

import io.github.tt432.nodegraph.api.model.TypedValue;

/**
 * Schema for a single port (input or output) on a {@link NodeDefinition}.
 * The {@code key} is the stable programmatic id used by {@link NodeFunction}
 * to read inputs / write outputs; the {@code value} carries the user-facing
 * name/type/description.
 */
public final class PortSpec {
    private final String key;
    private final TypedValue value;

    public PortSpec(String key, TypedValue value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public TypedValue value() {
        return value;
    }
}
