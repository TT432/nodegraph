package io.github.tt432.nodegraph.api.model;

import io.github.tt432.nodegraph.api.type.Type;
import net.minecraft.network.chat.Component;

/**
 * A node-body input component: an inline value editor (text / slider / button
 * group / dropdown) holding a node-local parameter value. The {@code currentValue}
 * is what {@link io.github.tt432.nodegraph.api.def.NodeFunction} reads for the
 * corresponding parameter (when no external connection drives it).
 */
public final class InputWidget {
    private final String key;
    private final TypedValue value;
    private final InputWidgetKind kind;
    private Object currentValue;

    public InputWidget(String key, TypedValue value, InputWidgetKind kind, Object currentValue) {
        this.key = key;
        this.value = value;
        this.kind = kind;
        this.currentValue = currentValue;
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

    public Type type() {
        return value.type();
    }

    public Component name() {
        return value.name();
    }

    public Component description() {
        return value.description();
    }

    public Object currentValue() {
        return currentValue;
    }

    public void setCurrentValue(Object currentValue) {
        this.currentValue = currentValue;
    }
}
