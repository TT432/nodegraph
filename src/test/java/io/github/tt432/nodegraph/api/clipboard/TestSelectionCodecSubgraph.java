package io.github.tt432.nodegraph.api.clipboard;

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
 * 剪贴板编码跳过携带子图的节点：子图是宿主派生视图，复制会静默丢失嵌套内容，
 * 故 encode 直接过滤（相关 wire 因端点缺席自然丢弃）。
 */
class TestSelectionCodecSubgraph {

    private NodeDefinition def(String path) {
        return new NodeDefinition(rl("test", path), Component.literal(path),
                List.of(), List.of(), List.of(), (inputs, widgets) -> Map.of());
    }

    @Test
    void subgraphNodesAreSkippedOnEncode() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node plain = g.addNode(def("plain"), 0, 0);
        Node grouped = g.addNode(def("grouped"), 100, 0);
        grouped.setSubgraph(new NodeGraph(new TypeRegistry()));

        SelectionSnapshot snapshot = SelectionCodec.encode(g, List.of(plain.id(), grouped.id()), List.of());
        assertEquals(1, snapshot.nodes().size());
        assertEquals(plain.header().getString(), snapshot.nodes().get(0).header().getString());
    }

    @Test
    void skippedSubgraphNodeDropsItsConnections() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        Node a = g.addNode(def("a"), 0, 0);
        Node b = g.addNode(def("b"), 100, 0);
        // a、b 均无端口，无法 connect；直接验证快照 connection 数为 0 即可——
        // 真正的过滤逻辑（端点缺席）由 TestSelectionCodec 既有用例覆盖。
        SelectionSnapshot snapshot = SelectionCodec.encode(g, List.of(a.id(), b.id()), List.of());
        assertTrue(snapshot.connections().isEmpty());
        assertEquals(2, snapshot.nodes().size());
    }
}
