package io.github.tt432.nodegraph.api.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Registry of {@link Type}s and {@link TypeConversionRule}s.
 * Identity of a type is its string id; color is assigned at registration time.
 */
public final class TypeRegistry {
    private final Map<String, Type> types = new HashMap<>();
    private final Map<Key, TypeConversionRule> conversions = new HashMap<>();

    public Type register(String id, int color) {
        Objects.requireNonNull(id, "id");
        Type existing = types.get(id);
        if (existing != null) {
            return existing;
        }
        Type t = new Type(id, color);
        types.put(id, t);
        return t;
    }

    public Type get(String id) {
        Type t = types.get(Objects.requireNonNull(id, "id"));
        if (t == null) {
            throw new IllegalArgumentException("Unknown type: " + id);
        }
        return t;
    }

    public boolean contains(String id) {
        return types.containsKey(id);
    }

    public Collection<Type> all() {
        return types.values();
    }

    public TypeConversionRule registerConversion(Type from, Type to, Function<Object, Object> converter) {
        TypeConversionRule rule = new TypeConversionRule(from, to, converter);
        conversions.put(new Key(from, to), rule);
        return rule;
    }

    public Optional<TypeConversionRule> conversion(Type from, Type to) {
        return Optional.ofNullable(conversions.get(new Key(from, to)));
    }

    /** Identity (same type) always converts; otherwise a registered rule must exist. */
    public boolean canConvert(Type from, Type to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return from.equals(to) || conversions.containsKey(new Key(from, to));
    }

    private record Key(Type from, Type to) {
    }
}
