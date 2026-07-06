package io.github.tt432.nodegraph.api.command;

import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeId;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the full constructor of {@link MoveNodeCommand} added in TaskH for
 * live-drag commits (old position supplied up front).
 */
class TestMoveNodeCommandFull {

    private static final double EPS = 1e-9;

    private NodeGraph newGraph() {
        return new NodeGraph(new TypeRegistry());
    }

    private Node newNode(NodeGraph graph, String id, double x, double y) {
        NodeDefinition def = new NodeDefinition(
                new ResourceLocation("nodegraph", id),
                Component.literal(id),
                List.of(), List.of(), List.of(),
                (inputs, widgets) -> Map.of());
        return graph.addNode(def, x, y);
    }

    @Test
    void fullConstructorCapturesProvidedOldAndAppliesNew() {
        NodeGraph graph = newGraph();
        Node n = newNode(graph, "n", 10, 20);
        NodeId id = n.id();
        MoveNodeCommand cmd = new MoveNodeCommand(graph, id, 10, 20, 100, 200);

        cmd.execute();
        assertEquals(100, graph.node(id).x(), EPS);
        assertEquals(200, graph.node(id).y(), EPS);

        cmd.undo();
        assertEquals(10, graph.node(id).x(), EPS);
        assertEquals(20, graph.node(id).y(), EPS);
    }

    @Test
    void fullConstructorRedoReappliesNew() {
        NodeGraph graph = newGraph();
        Node n = newNode(graph, "n", 0, 0);
        NodeId id = n.id();
        MoveNodeCommand cmd = new MoveNodeCommand(graph, id, 0, 0, 5, 7);

        cmd.execute();
        cmd.undo();
        cmd.redo();
        assertEquals(5, graph.node(id).x(), EPS);
        assertEquals(7, graph.node(id).y(), EPS);
    }

    @Test
    void fullConstructorIndependentOfCurrentPosition() {
        NodeGraph graph = newGraph();
        Node n = newNode(graph, "n", 10, 20);
        NodeId id = n.id();
        MoveNodeCommand cmd = new MoveNodeCommand(graph, id, 10, 20, 100, 200);

        // externally mutate position before execute; the full constructor must
        // still undo to the supplied (10,20), not the mutated (50,60).
        graph.node(id).setPosition(50, 60);
        cmd.execute();
        assertEquals(100, graph.node(id).x(), EPS);
        assertEquals(200, graph.node(id).y(), EPS);

        cmd.undo();
        assertEquals(10, graph.node(id).x(), EPS);
        assertEquals(20, graph.node(id).y(), EPS);
    }
}
