package com.nodegraph.nodegraph.client.selection;

import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestSelectionModel {

    private static NodeId nid(long v) {
        return new NodeId(v);
    }

    private static NodeGroupId gid(long v) {
        return new NodeGroupId(v);
    }

    @Test
    void addAndContains() {
        SelectionModel s = new SelectionModel();
        assertTrue(s.addNode(nid(1)));
        assertFalse(s.addNode(nid(1))); // duplicate -> no change
        assertTrue(s.containsNode(nid(1)));
        assertFalse(s.containsNode(nid(2)));
        assertFalse(s.isEmpty());
    }

    @Test
    void remove() {
        SelectionModel s = new SelectionModel();
        s.addNode(nid(1));
        assertTrue(s.removeNode(nid(1)));
        assertFalse(s.containsNode(nid(1)));
        assertFalse(s.removeNode(nid(1)));
        assertTrue(s.isEmpty());
    }

    @Test
    void toggle() {
        SelectionModel s = new SelectionModel();
        assertTrue(s.toggleNode(nid(1))); // now selected
        assertTrue(s.containsNode(nid(1)));
        assertFalse(s.toggleNode(nid(1))); // now deselected
        assertFalse(s.containsNode(nid(1)));
    }

    @Test
    void setNodesReplaces() {
        SelectionModel s = new SelectionModel();
        s.addNode(nid(1));
        s.setNodes(List.of(nid(2), nid(3)));
        assertEquals(List.of(nid(2), nid(3)), s.nodes());
        assertEquals(2, s.nodeCount());
    }

    @Test
    void clearDropsBoth() {
        SelectionModel s = new SelectionModel();
        s.addNode(nid(1));
        s.addGroup(gid(7));
        s.clear();
        assertTrue(s.isEmpty());
        assertEquals(0, s.nodeCount());
        assertEquals(0, s.groupCount());
    }

    @Test
    void groupsAddAndContains() {
        SelectionModel s = new SelectionModel();
        s.addGroup(gid(1));
        assertTrue(s.containsGroup(gid(1)));
        assertEquals(List.of(gid(1)), s.groups());
        assertEquals(1, s.groupCount());
    }

    @Test
    void insertionOrderPreserved() {
        SelectionModel s = new SelectionModel();
        s.addNode(nid(3));
        s.addNode(nid(1));
        s.addNode(nid(2));
        assertEquals(List.of(nid(3), nid(1), nid(2)), s.nodes());
    }
}
