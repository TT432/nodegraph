package io.github.tt432.nodegraph.api.def;

import io.github.tt432.nodegraph.api.model.InputWidgetKind;
import io.github.tt432.nodegraph.api.model.TypedValue;

/**
 * Schema for a node-body input component (an inline value editor such as a
 * text box, slider, button group or dropdown). Provides a node-local
 * parameter value to {@link NodeFunction}.
 */
public final class InputWidgetSpec {
    /** {@link InputWidgetKind#CUSTOM} 之外的种类默认行高（世界单位，与 NodeLayout.ROW_HEIGHT 一致）。 */
    public static final double DEFAULT_HEIGHT = 14.0;

    private final String key;
    private final TypedValue value;
    private final InputWidgetKind kind;
    private final Object defaultValue;
    /** 行高（世界单位）。CUSTOM 预览区常用更大高度；其余种类保持 {@link #DEFAULT_HEIGHT}。 */
    private final double height;

    public InputWidgetSpec(String key, TypedValue value, InputWidgetKind kind, Object defaultValue) {
        this(key, value, kind, defaultValue, DEFAULT_HEIGHT);
    }

    public InputWidgetSpec(String key, TypedValue value, InputWidgetKind kind, Object defaultValue, double height) {
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0, got " + height);
        }
        this.key = key;
        this.value = value;
        this.kind = kind;
        this.defaultValue = defaultValue;
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

    public Object defaultValue() {
        return defaultValue;
    }

    public double height() {
        return height;
    }
}
