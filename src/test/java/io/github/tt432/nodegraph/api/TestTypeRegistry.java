package io.github.tt432.nodegraph.api;

import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeConversionRule;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestTypeRegistry {

    @Test
    void registerAndGet() {
        TypeRegistry r = new TypeRegistry();
        Type t = r.register("int", 0xFFAA0000);
        assertEquals("int", t.id());
        assertEquals(0xFFAA0000, t.color());
        assertSame(t, r.get("int"));
    }

    @Test
    void getUnknownThrows() {
        TypeRegistry r = new TypeRegistry();
        assertThrows(IllegalArgumentException.class, () -> r.get("nope"));
    }

    @Test
    void registerTwiceReturnsSame() {
        TypeRegistry r = new TypeRegistry();
        Type a = r.register("int", 1);
        Type b = r.register("int", 2);
        assertSame(a, b);
        assertEquals(1, b.color(), "color should not be overwritten on re-register");
    }

    @Test
    void identityConversion() {
        TypeRegistry r = new TypeRegistry();
        Type t = r.register("int", 0);
        assertTrue(r.canConvert(t, t));
    }

    @Test
    void registeredConversionOneWay() {
        TypeRegistry r = new TypeRegistry();
        Type inte = r.register("int", 0);
        Type bite = r.register("byte", 0);
        r.registerConversion(inte, bite, v -> ((Number) v).byteValue());

        assertTrue(r.canConvert(inte, bite), "int -> byte should convert");
        assertFalse(r.canConvert(bite, inte), "byte -> int has no rule");

        Optional<TypeConversionRule> rule = r.conversion(inte, bite);
        assertTrue(rule.isPresent());
        assertEquals((byte) 5, rule.get().apply(5));
    }

    @Test
    void distinctTypesNotEqual() {
        TypeRegistry r = new TypeRegistry();
        Type a = r.register("int", 0);
        Type b = r.register("byte", 0);
        assertNotEquals(a, b);
        assertEquals(a, new Type("int", 999));
    }
}
