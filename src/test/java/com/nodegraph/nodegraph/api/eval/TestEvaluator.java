package com.nodegraph.nodegraph.api.eval;

import com.nodegraph.nodegraph.api.def.InputWidgetSpec;
import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.def.PortSpec;
import com.nodegraph.nodegraph.api.model.InputWidgetKind;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.TypedValue;
import com.nodegraph.nodegraph.api.type.Type;
import com.nodegraph.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TestEvaluator {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
        final Type bite = reg.register("byte", 0xFF00AA00);
    }

    private final Types types = new Types();
    private final Evaluator evaluator = new Evaluator();

    // ---- builders ----------------------------------------------------------

    private static ResourceLocation rl(String id) {
        return new ResourceLocation("nodegraph", id);
    }

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

    /** Constant node that counts how many times its function was invoked. */
    private NodeDefinition countingConst(String id, Type t, Object value, AtomicInteger counter) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(widget("v", t, value)),
                List.of(),
                List.of(port("out", t)),
                (inputs, widgets) -> {
                    counter.incrementAndGet();
                    return Map.of("out", widgets.get("v"));
                });
    }

    /** out = in + 1 */
    private NodeDefinition plusOne(String id, Type t) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in", t)),
                List.of(port("out", t)),
                (inputs, widgets) -> Map.of("out", ((Number) inputs.get("in")).intValue() + 1));
    }

    /** out = in0 + in1 */
    private NodeDefinition merge(String id, Type t) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in0", t), port("in1", t)),
                List.of(port("out", t)),
                (inputs, widgets) -> Map.of("out",
                        ((Number) inputs.get("in0")).intValue() + ((Number) inputs.get("in1")).intValue()));
    }

    /** out = in (single-in single-out). Used as sink and in cycle tests. */
    private NodeDefinition passthrough(String id, Type inType, Type outType) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in", inType)),
                List.of(port("out", outType)),
                (inputs, widgets) -> Map.of("out", inputs.get("in")));
    }

    /** Reports whether its input port key is present and whether the value is null. */
    private NodeDefinition inputPresenceProbe(String id, Type t) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(port("in", t)),
                List.of(port("hasKey", t), port("isNull", t)),
                (inputs, widgets) -> Map.of(
                        "hasKey", inputs.containsKey("in"),
                        "isNull", inputs.get("in") == null));
    }

    private NodeDefinition bombNode(String id) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(),
                List.of(port("out", types.inte)),
                (inputs, widgets) -> {
                    throw new IllegalStateException("bomb");
                });
    }

    // ---- tests -------------------------------------------------------------

    @Test
    void linearChainPropagatesValues() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(constNode("a", types.inte, 10), 0, 0);
        Node b = g.addNode(plusOne("b", types.inte), 100, 0);
        Node c = g.addNode(plusOne("c", types.inte), 200, 0);
        g.connect(a.id(), 0, b.id(), 0);
        g.connect(b.id(), 0, c.id(), 0);

        Map<String, Object> out = evaluator.evaluate(g, c.id());
        assertEquals(12, ((Number) out.get("out")).intValue());
    }

    @Test
    void diamondEvaluatesSharedDependencyOnce() {
        AtomicInteger aCount = new AtomicInteger();
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(countingConst("a", types.inte, 5, aCount), 0, 0);
        Node b = g.addNode(plusOne("b", types.inte), 100, -50);
        Node c = g.addNode(plusOne("c", types.inte), 100, 50);
        Node d = g.addNode(merge("d", types.inte), 200, 0);
        g.connect(a.id(), 0, b.id(), 0);
        g.connect(a.id(), 0, c.id(), 0);
        g.connect(b.id(), 0, d.id(), 0);
        g.connect(c.id(), 0, d.id(), 1);

        Map<String, Object> out = evaluator.evaluate(g, d.id());
        assertEquals(12, ((Number) out.get("out")).intValue(), "(5+1)+(5+1)");
        assertEquals(1, aCount.get(), "shared dependency must be evaluated exactly once");
    }

    @Test
    void unconnectedInputIsPresentWithNullValue() {
        NodeGraph g = new NodeGraph(types.reg);
        Node probe = g.addNode(inputPresenceProbe("p", types.inte), 0, 0);

        Map<String, Object> out = evaluator.evaluate(g, probe.id());
        assertEquals(Boolean.TRUE, out.get("hasKey"), "input port key must always be present");
        assertEquals(Boolean.TRUE, out.get("isNull"), "unconnected input value must be null");
    }

    @Test
    void connectedInputIsPresentWithNonNullValue() {
        NodeGraph g = new NodeGraph(types.reg);
        Node src = g.addNode(constNode("src", types.inte, 42), 0, 0);
        Node probe = g.addNode(inputPresenceProbe("p", types.inte), 100, 0);
        g.connect(src.id(), 0, probe.id(), 0);

        Map<String, Object> out = evaluator.evaluate(g, probe.id());
        assertEquals(Boolean.TRUE, out.get("hasKey"));
        assertEquals(Boolean.FALSE, out.get("isNull"));
    }

    @Test
    void widgetValuesArePassedToFunction() {
        NodeGraph g = new NodeGraph(types.reg);
        Node n = g.addNode(constNode("n", types.inte, 7), 0, 0);
        Map<String, Object> out = evaluator.evaluate(g, n.id());
        assertEquals(7, ((Number) out.get("out")).intValue());

        // mutate current widget value and re-evaluate
        n.widgets().get(0).setCurrentValue(999);
        Map<String, Object> out2 = evaluator.evaluate(g, n.id());
        assertEquals(999, ((Number) out2.get("out")).intValue());
    }

    @Test
    void autoConvertedConnectionAppliesRuleDuringEvaluation() {
        types.reg.registerConversion(types.inte, types.bite, v -> ((Number) v).byteValue());
        NodeGraph g = new NodeGraph(types.reg);
        Node src = g.addNode(constNode("src", types.inte, 300), 0, 0);
        Node sink = g.addNode(passthrough("sink", types.bite, types.bite), 100, 0);

        // int -> byte : auto-converted at connect time, rule applied at eval time
        var conn = g.connect(src.id(), 0, sink.id(), 0);
        assertTrue(conn.isAutoConverted());

        Map<String, Object> out = evaluator.evaluate(g, sink.id());
        assertEquals((byte) 300, out.get("out"));
    }

    @Test
    void selfLoopIsDetectedAsCycle() {
        NodeGraph g = new NodeGraph(types.reg);
        Node n = g.addNode(passthrough("loop", types.inte, types.inte), 0, 0);
        g.connect(n.id(), 0, n.id(), 0);

        CycleException ex = assertThrows(CycleException.class, () -> evaluator.evaluateAll(g));
        assertNotNull(ex.cycle());
        assertFalse(ex.cycle().isEmpty());
        assertEquals(n.id(), ex.cycle().get(0));
        assertEquals(n.id(), ex.cycle().get(ex.cycle().size() - 1), "cycle path should close on the same node");
    }

    @Test
    void twoNodeCycleIsDetected() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(passthrough("a", types.inte, types.inte), 0, 0);
        Node b = g.addNode(passthrough("b", types.inte, types.inte), 100, 0);
        g.connect(a.id(), 0, b.id(), 0);
        g.connect(b.id(), 0, a.id(), 0);

        assertThrows(CycleException.class, () -> evaluator.evaluate(g, a.id()));
    }

    @Test
    void singleNodeEvaluateIsLazyAndDoesNotTouchUnreachableNodes() {
        AtomicInteger aCount = new AtomicInteger();
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(countingConst("a", types.inte, 1, aCount), 0, 0);
        Node b = g.addNode(plusOne("b", types.inte), 100, 0);
        Node c = g.addNode(plusOne("c", types.inte), 200, 0);
        Node isolated = g.addNode(bombNode("bomb"), 0, 200);
        g.connect(a.id(), 0, b.id(), 0);
        g.connect(b.id(), 0, c.id(), 0);

        Map<String, Object> out = evaluator.evaluate(g, c.id());
        assertEquals(3, ((Number) out.get("out")).intValue());
        assertEquals(1, aCount.get(), "dependency evaluated once");
        // isolated bomb node must NOT have been evaluated (no exception thrown)
        assertNotNull(g.node(isolated.id()));
    }

    @Test
    void functionExceptionIsWrapped() {
        NodeGraph g = new NodeGraph(types.reg);
        Node bomb = g.addNode(bombNode("bomb"), 0, 0);

        EvaluationException ex = assertThrows(EvaluationException.class, () -> evaluator.evaluateAll(g));
        assertEquals(bomb.id(), ex.node());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("bomb", ex.getCause().getMessage());
    }

    @Test
    void evaluateAllCoversEveryNode() {
        NodeGraph g = new NodeGraph(types.reg);
        Node a = g.addNode(constNode("a", types.inte, 10), 0, 0);
        Node b = g.addNode(plusOne("b", types.inte), 100, 0);
        g.connect(a.id(), 0, b.id(), 0);

        EvaluationResult result = evaluator.evaluateAll(g);

        assertEquals(2, result.evaluatedNodes().size());
        assertTrue(result.outputs().containsKey(a.id()));
        assertTrue(result.outputs().containsKey(b.id()));
        assertEquals(11, ((Number) result.outputsOf(b.id()).get("out")).intValue());
        assertThrows(IllegalArgumentException.class, () -> result.outputsOf(new com.nodegraph.nodegraph.api.model.NodeId(999)));
    }
}
