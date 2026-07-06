package io.github.tt432.nodegraph.api.def;

import io.github.tt432.nodegraph.api.model.InputWidgetKind;
import io.github.tt432.nodegraph.api.model.TypedValue;

/**
 * Schema for a node-body input component (an inline value editor such as a
 * text box, slider, button group or dropdown). Provides a node-local
 * parameter value to {@link NodeFunction}.
 */
public final class InputWidgetSpec {
    private final String key;
    private final TypedValue value;
    private final InputWidgetKind kind;
    private final Object defaultValue;

    public InputWidgetSpec(String key, TypedValue value, InputWidgetKind kind, Object defaultValue) {
        this.key = key;
        this.value = value;
        this.kind = kind;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public TypedValue value() {
        return value;
    }

    public InputWidgetKind kind() {
        return kind;
    }

    public Object defaultValue() {
        return defaultValue;
    }
}
