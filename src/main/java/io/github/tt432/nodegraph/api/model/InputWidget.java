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
    private InputWidgetKind kind;
    private Object currentValue;
    /** 行高（世界单位），由 {@code InputWidgetSpec.height} 拷贝；布局与渲染共用。 */
    private final double height;

    public InputWidget(String key, TypedValue value, InputWidgetKind kind, Object currentValue) {
        this(key, value, kind, currentValue, io.github.tt432.nodegraph.api.def.InputWidgetSpec.DEFAULT_HEIGHT);
    }

    public InputWidget(String key, TypedValue value, InputWidgetKind kind, Object currentValue, double height) {
        this.key = key;
        this.value = value;
        this.kind = kind;
        this.currentValue = currentValue;
        this.height = height;
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

    /**
     * per 实例覆盖 kind（如将 TEXT 实例只读化为 DISPLAY）。定义层 schema 不变；
     * 仅影响本实例的渲染/编辑行为。
     */
    public void setKind(InputWidgetKind kind) {
        this.kind = java.util.Objects.requireNonNull(kind, "kind");
    }

    public double height() {
        return height;
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
