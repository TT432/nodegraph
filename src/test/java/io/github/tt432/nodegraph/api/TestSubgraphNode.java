package io.github.tt432.nodegraph.api;

import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import static io.github.tt432.nodegraph.TestIds.rl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 子图与节点状态色：subgraph 可挂/可换/可清，hasSubgraph 反映非空；
 * statusColor 默认 0（不覆盖描边），可设可清。
 */
class TestSubgraphNode {

    private NodeDefinition def() {
        return new NodeDefinition(rl("test/sub"), Component.literal("n"),
                List.of(), List.of(), List.of(), (inputs, widgets) -> Map.of());
    }

    @Test
    void nodeHasNoSubgraphByDefault() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(), 0, 0);
        assertFalse(n.hasSubgraph());
        assertNull(n.subgraph());
    }

    @Test
    void subgraphAttachReplaceClear() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(), 0, 0);
        NodeGraph sub1 = new NodeGraph(new TypeRegistry());
        NodeGraph sub2 = new NodeGraph(new TypeRegistry());
        n.setSubgraph(sub1);
        assertTrue(n.hasSubgraph());
        assertSame(sub1, n.subgraph());
        n.setSubgraph(sub2);
        assertSame(sub2, n.subgraph());
        n.setSubgraph(null);
        assertFalse(n.hasSubgraph());
    }

    @Test
    void subgraphIsIndependentGraph() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node outer = g.addNode(def(), 0, 0);
        NodeGraph sub = new NodeGraph(new TypeRegistry());
        Node inner = sub.addNode(def(), 5, 5);
        outer.setSubgraph(sub);
        // 内外图 id 命名空间独立（都从 1 起），互不影响
        assertEquals(outer.id().value(), inner.id().value());
        assertSame(inner, outer.subgraph().node(inner.id()));
    }

    @Test
    void statusColorDefaultsToZero() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(), 0, 0);
        assertEquals(0, n.statusColor());
    }

    @Test
    void statusColorSetAndClear() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node n = g.addNode(def(), 0, 0);
        n.setStatusColor(0xFFFF5555);
        assertEquals(0xFFFF5555, n.statusColor());
        n.setStatusColor(0);
        assertEquals(0, n.statusColor());
    }
}
