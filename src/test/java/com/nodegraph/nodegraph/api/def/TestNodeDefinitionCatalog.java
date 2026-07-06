package com.nodegraph.nodegraph.api.def;

import com.nodegraph.nodegraph.api.model.InputWidgetKind;
import com.nodegraph.nodegraph.api.model.TypedValue;
import com.nodegraph.nodegraph.api.type.Type;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestNodeDefinitionCatalog {

    private static NodeDefinition def(String id, String header, List<Type> inTypes, List<Type> outTypes) {
        List<PortSpec> ins = new java.util.ArrayList<>();
        for (int i = 0; i < inTypes.size(); i++) {
            ins.add(new PortSpec("in" + i, new TypedValue(Component.literal("In" + i), inTypes.get(i), Component.empty())));
        }
        List<PortSpec> outs = new java.util.ArrayList<>();
        for (int i = 0; i < outTypes.size(); i++) {
            outs.add(new PortSpec("out" + i, new TypedValue(Component.literal("Out" + i), outTypes.get(i), Component.empty())));
        }
        return new NodeDefinition(
                new ResourceLocation("nodegraph", id),
                Component.literal(header),
                List.of(),
                ins,
                outs,
                (inputs, widgets) -> Map.of());
    }

    @Test
    void registerGetContains() {
        NodeDefinitionCatalog c = new NodeDefinitionCatalog();
        Type num = new Type("number", 0xFFFFFFFF);
        NodeDefinition a = def("add", "Add", List.of(num, num), List.of(num));
        c.register(a);
        assertSame(a, c.get(new ResourceLocation("nodegraph", "add")));
        assertTrue(c.contains(new ResourceLocation("nodegraph", "add")));
        assertFalse(c.contains(new ResourceLocation("nodegraph", "nope")));
        assertEquals(1, c.size());
    }

    @Test
    void duplicateThrows() {
        NodeDefinitionCatalog c = new NodeDefinitionCatalog();
        c.register(def("add", "Add", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> c.register(def("add", "Add2", List.of(), List.of())));
    }

    @Test
    void getUnknownThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new NodeDefinitionCatalog().get(new ResourceLocation("nodegraph", "x")));
    }

    @Test
    void matchingCaseInsensitiveAndEmptyQuery() {
        NodeDefinitionCatalog c = new NodeDefinitionCatalog();
        c.register(def("add", "Add", List.of(), List.of()));
        c.register(def("mul", "Multiply", List.of(), List.of()));
        assertEquals(2, c.matching("").size());
        assertEquals(2, c.matching(null).size());
        assertEquals(1, c.matching("add").size());
        assertEquals(1, c.matching("MUL").size());
        assertEquals(1, c.matching("ulti").size());
        assertTrue(c.matching("zzz").isEmpty());
    }

    @Test
    void withInputTypeAndOutputType() {
        NodeDefinitionCatalog c = new NodeDefinitionCatalog();
        Type num = new Type("number", 0xFFFFFFFF);
        Type str = new Type("string", 0xFFFFFFFF);
        c.register(def("add", "Add", List.of(num, num), List.of(num)));
        c.register(def("tostr", "To String", List.of(num), List.of(str)));
        c.register(def("cat", "Concat", List.of(str, str), List.of(str)));
        assertEquals(2, c.withInputType(num).size()); // add, tostr
        assertEquals(1, c.withInputType(str).size()); // cat
        assertEquals(2, c.withOutputType(str).size()); // tostr, cat
        assertEquals(1, c.withOutputType(num).size()); // add
    }

    @Test
    void preservesInsertionOrder() {
        NodeDefinitionCatalog c = new NodeDefinitionCatalog();
        c.register(def("b", "Bee", List.of(), List.of()));
        c.register(def("a", "Aye", List.of(), List.of()));
        List<NodeDefinition> all = c.matching("");
        assertEquals("nodegraph:b", all.get(0).id().toString());
        assertEquals("nodegraph:a", all.get(1).id().toString());
    }

    @Test
    void registerInputWidgetKindCoverage() {
        // ensure InputWidgetSpec/NodeDefinition wiring compiles with all kinds
        Type num = new Type("number", 0xFFFFFFFF);
        NodeDefinition d = new NodeDefinition(
                new ResourceLocation("nodegraph", "k"),
                Component.literal("K"),
                List.of(
                        new InputWidgetSpec("t", new TypedValue(Component.literal("T"), num, Component.empty()), InputWidgetKind.TEXT, "0"),
                        new InputWidgetSpec("s", new TypedValue(Component.literal("S"), num, Component.empty()), InputWidgetKind.SLIDER, 0),
                        new InputWidgetSpec("b", new TypedValue(Component.literal("B"), num, Component.empty()), InputWidgetKind.BUTTON_GROUP, "x"),
                        new InputWidgetSpec("d", new TypedValue(Component.literal("D"), num, Component.empty()), InputWidgetKind.DROPDOWN, "y")),
                List.of(),
                List.of(),
                (inputs, widgets) -> Map.of());
        NodeDefinitionCatalog c = new NodeDefinitionCatalog();
        c.register(d);
        assertEquals(4, d.widgets().size());
    }
}
