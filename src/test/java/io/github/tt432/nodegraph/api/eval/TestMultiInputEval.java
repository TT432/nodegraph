package io.github.tt432.nodegraph.api.eval;

import io.github.tt432.nodegraph.api.def.InputWidgetSpec;
import io.github.tt432.nodegraph.api.def.MultiInputNodeDefinition;
import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.def.PortSpec;
import io.github.tt432.nodegraph.api.model.InputWidgetKind;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.TypedValue;
import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import static io.github.tt432.nodegraph.TestIds.rl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TestMultiInputEval {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
        final Type bite = reg.register("byte", 0xFF00AA00);
    }

    private final Types types = new Types();
    private final Evaluator evaluator = new Evaluator();


    private static PortSpec port(String key, Type t) {
        return new PortSpec(key, new TypedValue(Component.literal(key), t, Component.literal("desc")));
    }

    private static InputWidgetSpec widget(String key, Type t, Object defVal) {
        return new InputWidgetSpec(key,
                new TypedValue(Component.literal(key), t, Component.literal("desc")),
                InputWidgetKind.TEXT, defVal);
    }

    /** Constant node: no inputs, one output, value comes from its body widget. */
    private NodeDefinition constNode(String id, Type t, Object value) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(widget("v", t, value)),
                List.of(),
                List.of(port("out", t)),
                (inputs, widgets) -> Map.of("out", widgets.get("v")));
    }

    /** Multi-input node: out = sum of every wire value on "in". */
    private MultiInputNodeDefinition sumAll(String id, Type t) {
        return new MultiInputNodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in", t)),
                List.of(port("out", t)),
                (inputs, widgets) -> {
                    int sum = 0;
                    for (Object v : inputs.get("in")) {
                        sum += ((Number) v).intValue();
                    }
                    return Map.of("out", sum);
                });
    }

    /** Multi-input passthrough used to build cycles: out = first wire value (or 0). */
    private MultiInputNodeDefinition multiPassthrough(String id, Type t) {
        return new MultiInputNodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in", t)),
                List.of(port("out", t)),
                (inputs, widgets) -> {
                    List<Object> vs = inputs.get("in");
                    return Map.of("out", vs.isEmpty() ? 0 : vs.get(0));
                });
    }

    /** Single-input passthrough (in -> out). */
    private NodeDefinition passthrough(String id, Type t) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in", t)),
                List.of(port("out", t)),
                (inputs, widgets) -> Map.of("out", inputs.get("in")));
    }

    @Test
    void receivesListInConnectionCreationOrder() {
        NodeGraph g = new NodeGraph(types.reg);
        Node c10 = g.addNode(constNode("c10", types.inte, 10), 0, 0);
        Node c20 = g.addNode(constNode("c20", types.inte, 20), 0, 50);
        Node c30 = g.addNode(constNode("c30", types.inte, 30), 0, 100);

        AtomicReference<List<Object>> seen = new AtomicReference<>();
        MultiInputNodeDefinition probe = new MultiInputNodeDefinition(rl("probe"), Component.literal("probe"),
                List.of(),
                List.of(port("in", types.inte)),
                List.of(port("out", types.inte)),
                (inputs, widgets) -> {
                    seen.set(List.copyOf(inputs.get("in")));
                    return Map.of("out", 0);
                });
        Node sink = g.addNode(probe, 100, 50);

        // connect in a non-numeric order to prove list order follows creation order
        g.connect(c30.id(), 0, sink.id(), 0);
        g.connect(c10.id(), 0, sink.id(), 0);
        g.connect(c20.id(), 0, sink.id(), 0);

        evaluator.evaluate(g, sink.id());
        assertEquals(List.of(30, 10, 20), seen.get(),
                "list order must equal connection-creation order");
    }

    @Test
    void sumsAccumulatedWires() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(constNode("a", types.inte, 1), 0, 0);
        Node b = g.addNode(constNode("b", types.inte, 2), 0, 50);
        Node c = g.addNode(constNode("c", types.inte, 3), 0, 100);
        Node sum = g.addNode(sumAll("sum", types.inte), 100, 50);

        g.connect(a.id(), 0, sum.id(), 0);
        g.connect(b.id(), 0, sum.id(), 0);
        g.connect(c.id(), 0, sum.id(), 0);

        Map<String, Object> out = evaluator.evaluate(g, sum.id());
        assertEquals(6, out.get("out"));
    }

    @Test
    void unwiredPortMapsToEmptyList() {
        NodeGraph g = new NodeGraph(types.reg);
        Node sum = g.addNode(sumAll("sum", types.inte), 100, 50);
        Map<String, Object> out = evaluator.evaluate(g, sum.id());
        assertEquals(0, out.get("out"), "no wires -> empty list -> sum 0 (not null)");
    }

    @Test
    void autoConversionAppliesPerWire() {
        // byte -> int conversion that is observable: value + 100
        types.reg.registerConversion(types.bite, types.inte, v -> ((Number) v).intValue() + 100);

        NodeGraph g = new NodeGraph(types.reg);
        Node byteSrc = g.addNode(constNode("bytesrc", types.bite, 1), 0, 0);
        Node intSrc = g.addNode(constNode("intsrc", types.inte, 2), 0, 50);

        AtomicReference<List<Object>> seen = new AtomicReference<>();
        MultiInputNodeDefinition probe = new MultiInputNodeDefinition(rl("probe"), Component.literal("probe"),
                List.of(),
                List.of(port("in", types.inte)),
                List.of(port("out", types.inte)),
                (inputs, widgets) -> {
                    seen.set(List.copyOf(inputs.get("in")));
                    return Map.of("out", 0);
                });
        Node sink = g.addNode(probe, 100, 50);

        g.connect(byteSrc.id(), 0, sink.id(), 0); // auto-converted wire
        g.connect(intSrc.id(), 0, sink.id(), 0);  // direct wire

        evaluator.evaluate(g, sink.id());
        assertEquals(List.of(101, 2), seen.get(),
                "conversion applied per wire, order preserved");
    }

    @Test
    void cycleThroughMultiInputNodeStillThrows() {
        NodeGraph g = new NodeGraph(types.reg);
        Node m = g.addNode(multiPassthrough("m", types.inte), 0, 0);
        Node p = g.addNode(passthrough("p", types.inte), 100, 0);

        g.connect(m.id(), 0, p.id(), 0);
        g.connect(p.id(), 0, m.id(), 0);

        assertThrows(CycleException.class, () -> evaluator.evaluateAll(g));
    }

    @Test
    void multiNodeInAcyclicChainEvaluates() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(constNode("a", types.inte, 5), 0, 0);
        Node b = g.addNode(constNode("b", types.inte, 7), 0, 50);
        Node sum = g.addNode(sumAll("sum", types.inte), 100, 0);
        Node p = g.addNode(passthrough("p", types.inte), 200, 0);

        g.connect(a.id(), 0, sum.id(), 0);
        g.connect(b.id(), 0, sum.id(), 0);
        g.connect(sum.id(), 0, p.id(), 0);

        Map<String, Object> out = evaluator.evaluate(g, p.id());
        assertEquals(12, out.get("out"),
                "multi node feeds downstream single-input node normally");
    }
}
