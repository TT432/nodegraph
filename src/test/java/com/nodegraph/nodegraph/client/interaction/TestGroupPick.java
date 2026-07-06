package com.nodegraph.nodegraph.client.interaction;

import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestGroupPick {
    private NodeGraph newGraph() {
        return new NodeGraph(new TypeRegistry());
    }

    @Test
    void groupContainingHit() {
        NodeGraph g = newGraph();
        NodeGroupId id = g.createGroup(Component.literal("G"), 0, 0, 100, 100).id();
        Optional<NodeGroupId> hit = GroupPick.findGroupContaining(g, 50, 50);
        assertTrue(hit.isPresent());
        assertEquals(id, hit.get());
    }

    @Test
    void groupContainingMiss() {
        NodeGraph g = newGraph();
        g.createGroup(Component.literal("G"), 0, 0, 100, 100);
        assertTrue(GroupPick.findGroupContaining(g, 150, 150).isEmpty());
    }

    @Test
    void groupContainingOverlappingPicksFirst() {
        NodeGraph g = newGraph();
        NodeGroupId first = g.createGroup(Component.literal("G1"), 0, 0, 100, 100).id();
        g.createGroup(Component.literal("G2"), 0, 0, 200, 200); // larger, overlaps
        // point inside both -> iteration order returns the first created
        Optional<NodeGroupId> hit = GroupPick.findGroupContaining(g, 50, 50);
        assertTrue(hit.isPresent());
        assertEquals(first, hit.get());
    }

    @Test
    void groupHeaderHit() {
        NodeGraph g = newGraph();
        NodeGroupId id = g.createGroup(Component.literal("G"), 0, 0, 100, 100).id();
        // header strip = top GROUP_HEADER (14) rows
        Optional<NodeGroupId> hit = GroupPick.findGroupHeader(g, 50, 5);
        assertTrue(hit.isPresent());
        assertEquals(id, hit.get());
    }

    @Test
    void groupHeaderMiss() {
        NodeGraph g = newGraph();
        g.createGroup(Component.literal("G"), 0, 0, 100, 100);
        // body (below header) is not a header hit
        assertTrue(GroupPick.findGroupHeader(g, 50, 50).isEmpty());
    }

    @Test
    void resizeHandleHitAndMiss() {
        NodeGraph g = newGraph();
        NodeGroupId id = g.createGroup(Component.literal("G"), 0, 0, 100, 100).id();
        // grip = bottom-right 8x8 of the frame: [92,92 .. 100,100]
        Optional<NodeGroupId> hit = GroupPick.findResizeHandle(g, 96, 96);
        assertTrue(hit.isPresent());
        assertEquals(id, hit.get());
        // center of frame is not a grip hit
        assertTrue(GroupPick.findResizeHandle(g, 50, 50).isEmpty());
    }
}
