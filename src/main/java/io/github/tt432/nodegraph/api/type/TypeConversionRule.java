package io.github.tt432.nodegraph.api.type;

import java.util.Objects;
import java.util.function.Function;

/**
 * A registered automatic conversion from one {@link Type} to another.
 * When a wire connects ports whose types differ but a matching rule exists,
 * the connection is marked {@code auto-converted} and the rule is applied
 * during evaluation (and the UI shows a warning).
 */
public final class TypeConversionRule {
    private final Type from;
    private final Type to;
    private final Function<Object, Object> converter;

    public TypeConversionRule(Type from, Type to, Function<Object, Object> converter) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    public Type from() {
        return from;
    }

    public Type to() {
        return to;
    }

    public Object apply(Object value) {
        return converter.apply(value);
    }
}
