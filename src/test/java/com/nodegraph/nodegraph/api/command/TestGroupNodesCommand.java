package com.nodegraph.nodegraph.api.command;

import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.def.PortSpec;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeId;
import com.nodegraph.nodegraph.api.model.TypedValue;
import com.nodegraph.nodegraph.api.type.Type;
import com.nodegraph.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestGroupNodesCommand {

    private static final class Types {
        final TypeRegistry reg = new TypeRegistry();
        final Type inte = reg.register("int", 0xFFAA0000);
    }

    private final Types types = new Types();

    private static ResourceLocation rl(String id) {
        return new ResourceLocation("nodegraph", id);
    }

    private static PortSpec port(String key, Type t) {
        return new PortSpec(key, new TypedValue(Component.literal(key), t, Component.literal("d")));
    }

    private NodeDefinition srcNode(String id, Type t) {
        return new NodeDefinition(rl(id), Component.literal(id),
                List.of(),
                List.of(),
                List.of(port("out", t)),
                (inputs, widgets) -> java.util.Map.of("out", 1));
    }

    private NodeId add(NodeGraph g, String id) {
        return g.addNode(srcNode(id, types.inte), 0, 0).id();
    }

    @Test
    void executeCreatesGroupAndJoinsMembers() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeId a = add(g, "a");
        NodeId b = add(g, "b");
        GroupNodesCommand cmd = new GroupNodesCommand(g, Component.literal("G"), 0, 0, 100, 50, List.of(a, b));
        cmd.execute();
        NodeGroupId gid = cmd.group().id();
        assertNotNull(gid);
        assertEquals(2, g.members(gid).size());
        assertEquals(gid, g.node(a).groupId());
        assertEquals(gid, g.node(b).groupId());
    }

    @Test
    void undoRemovesGroupAndClearsMembership() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeId a = add(g, "a");
        NodeId b = add(g, "b");
        GroupNodesCommand cmd = new GroupNodesCommand(g, Component.literal("G"), 0, 0, 100, 50, List.of(a, b));
        cmd.execute();
        cmd.undo();
        assertEquals(0, g.groups().size());
        assertNull(g.node(a).groupId());
        assertNull(g.node(b).groupId());
    }

    @Test
    void redoRestoresGroupAndMembers() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeId a = add(g, "a");
        GroupNodesCommand cmd = new GroupNodesCommand(g, Component.literal("G"), 0, 0, 100, 50, List.of(a));
        cmd.execute();
        NodeGroupId gid = cmd.group().id();
        cmd.undo();
        cmd.redo();
        assertEquals(1, g.groups().size());
        assertEquals(gid, g.groups().iterator().next().id());
        assertEquals(gid, g.node(a).groupId());
    }

    @Test
    void idStableAcrossCycles() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeId a = add(g, "a");
        NodeId b = add(g, "b");
        GroupNodesCommand cmd = new GroupNodesCommand(g, Component.literal("G"), 0, 0, 100, 50, List.of(a, b));
        cmd.execute();
        NodeGroupId gid = cmd.group().id();
        for (int i = 0; i < 5; i++) {
            cmd.undo();
            cmd.redo();
        }
        assertEquals(gid, cmd.group().id());
        assertEquals(gid, g.node(a).groupId());
        assertEquals(gid, g.node(b).groupId());
    }

    @Test
    void emptyMembersOnlyCreatesGroup() {
        NodeGraph g = new NodeGraph(types.reg);
        GroupNodesCommand cmd = new GroupNodesCommand(g, Component.literal("G"), 0, 0, 100, 50, List.of());
        cmd.execute();
        NodeGroupId gid = cmd.group().id();
        assertEquals(1, g.groups().size());
        assertEquals(0, g.members(gid).size());
        cmd.undo();
        assertEquals(0, g.groups().size());
    }

    @Test
    void crossGroupMembershipRestored() {
        NodeGraph g = new NodeGraph(types.reg);
        NodeId a = add(g, "a");
        // a originally belongs to group A
        NodeGroupId groupA = g.createGroup(Component.literal("A"), 0, 0, 50, 50).id();
        g.setNodeGroup(a, groupA);
        assertEquals(groupA, g.node(a).groupId());

        // now group a (alone) into a new group B
        GroupNodesCommand cmd = new GroupNodesCommand(g, Component.literal("B"), 10, 10, 80, 80, List.of(a));
        cmd.execute();
        NodeGroupId groupB = cmd.group().id();
        assertNotEquals(groupA, groupB);
        assertEquals(groupB, g.node(a).groupId());

        // undo: a must return to group A, not become ungrouped
        cmd.undo();
        assertEquals(groupA, g.node(a).groupId());
        assertEquals(1, g.groups().size());
        assertEquals(groupA, g.groups().iterator().next().id());

        // redo back to B
        cmd.redo();
        assertEquals(groupB, g.node(a).groupId());
    }
}
