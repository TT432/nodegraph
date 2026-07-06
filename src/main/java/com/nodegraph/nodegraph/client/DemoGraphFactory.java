package com.nodegraph.nodegraph.client;

import com.nodegraph.nodegraph.api.def.InputWidgetSpec;
import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.def.NodeDefinitionCatalog;
import com.nodegraph.nodegraph.api.def.PortSpec;
import com.nodegraph.nodegraph.api.model.InputWidgetKind;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.TypedValue;
import com.nodegraph.nodegraph.api.type.Type;
import com.nodegraph.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * 构造演示用 {@link NodeGraph}：两个常量相加后转 byte，含一个 number→byte 自动转换节点、
 * 一个包围常量的节点组。纯客户端工厂（无 MC 渲染依赖，可被 JUnit 调用）。
 */
public final class DemoGraphFactory {
    private DemoGraphFactory() {}

    public static NodeGraph create() {
        TypeRegistry types = new TypeRegistry();
        Type number = types.register("number", 0xFF6ACCD9);
        Type byteType = types.register("byte", 0xFFE6C07B);
        types.registerConversion(number, byteType, v -> Byte.valueOf(((Number) v).byteValue()));

        NodeDefinition constant = new NodeDefinition(
                new ResourceLocation("nodegraph", "constant_number"),
                Component.literal("Constant"),
                List.of(new InputWidgetSpec("value",
                        new TypedValue(Component.literal("Value"), number, Component.literal("Constant number value")),
                        InputWidgetKind.TEXT, "0")),
                List.of(),
                List.of(new PortSpec("out",
                        new TypedValue(Component.literal("Out"), number, Component.literal("The constant value")))),
                (inputs, widgets) -> Map.of("out", toDouble(widgets.get("value"))));

        NodeDefinition add = new NodeDefinition(
                new ResourceLocation("nodegraph", "add"),
                Component.literal("Add"),
                List.of(),
                List.of(
                        new PortSpec("a", new TypedValue(Component.literal("A"), number, Component.literal("First addend"))),
                        new PortSpec("b", new TypedValue(Component.literal("B"), number, Component.literal("Second addend")))),
                List.of(new PortSpec("result",
                        new TypedValue(Component.literal("Result"), number, Component.literal("Sum of A and B")))),
                (inputs, widgets) -> Map.of("result", toDouble(inputs.get("a")) + toDouble(inputs.get("b"))));

        NodeDefinition toByte = new NodeDefinition(
                new ResourceLocation("nodegraph", "to_byte"),
                Component.literal("To Byte"),
                List.of(),
                List.of(new PortSpec("in",
                        new TypedValue(Component.literal("In"), byteType, Component.literal("Input value")))),
                List.of(new PortSpec("out",
                        new TypedValue(Component.literal("Out"), byteType, Component.literal("Clamped byte value")))),
                (inputs, widgets) -> {
                    Object in = inputs.get("in");
                    byte v = (in instanceof Number n) ? n.byteValue() : 0;
                    return Map.of("out", v);
                });

        NodeGraph graph = new NodeGraph(types);
        NodeDefinitionCatalog catalog = new NodeDefinitionCatalog();
        catalog.register(constant);
        catalog.register(add);
        catalog.register(toByte);
        graph.setCatalog(catalog);
        Node a = graph.addNode(constant, 40, 40);
        a.widgets().get(0).setCurrentValue("10");
        Node b = graph.addNode(constant, 40, 160);
        b.widgets().get(0).setCurrentValue("32");
        Node sum = graph.addNode(add, 320, 100);
        Node clamp = graph.addNode(toByte, 620, 100);

        graph.connect(a.id(), 0, sum.id(), 0);
        graph.connect(b.id(), 0, sum.id(), 1);
        graph.connect(sum.id(), 0, clamp.id(), 0);

        NodeGroupId inputsGroup = graph.createGroup(Component.literal("Inputs"), 20, 20, 200, 260).id();
        graph.setNodeGroup(a.id(), inputsGroup);
        graph.setNodeGroup(b.id(), inputsGroup);

        return graph;
    }

    private static double toDouble(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
