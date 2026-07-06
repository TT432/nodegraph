package com.nodegraph.nodegraph.api.clipboard;

import com.nodegraph.nodegraph.api.command.Command;
import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeId;
import com.nodegraph.nodegraph.api.model.NodeGroup;
import com.nodegraph.nodegraph.api.type.TypeConversionRule;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Replays a {@link SelectionSnapshot} into a {@link NodeGraph} as fresh nodes,
 * groups, and connections. All global ids are allocated by the graph (so they
 * never collide with existing ids); identity inside the snapshot uses local ids.
 *
 * <p>Lazy-capture idiom (same as {@code AddNodeCommand}): the first
 * {@link #execute()} allocates new ids, builds membership + wires and stashes
 * the created objects. Subsequent calls (redo) reuse the stashed objects via
 * the low-level recovery primitives ({@link NodeGraph#insertNode} /
 * {@link NodeGraph#insertGroup} / {@link NodeGraph#addConnection}) so ids stay
 * stable across undo/redo and existing references keep resolving.
 *
 * <p>Connections are rebuilt with {@link NodeGraph#addConnection} (bypassing
 * {@link NodeGraph#connect}) so the snapshot's {@code autoConverted} flag and
 * {@code rule} reference are preserved verbatim rather than being re-derived
 * from the live type registry.
 *
 * <p>Not composed of {@code AddNodeCommand}s: the per-node id is allocated
 * during execute, so it cannot be known at construction time when wiring up
 * connections. Doing it self-contained avoids the id-pre-resolution problem.
 */
public final class PasteCommand implements Command {
    private final NodeGraph graph;
    private final SelectionSnapshot snapshot;
    private final double offsetX;
    private final double offsetY;

    private boolean executed = false;
    private final List<NodeGroup> savedGroups = new ArrayList<>();
    private final List<Node> savedNodes = new ArrayList<>();
    private final List<Connection> savedConnections = new ArrayList<>();

    public PasteCommand(NodeGraph graph, SelectionSnapshot snapshot, double offsetX, double offsetY) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void execute() {
        if (!executed) {
            firstExecute();
            executed = true;
        } else {
            redoExecute();
        }
    }

    private void firstExecute() {
        // Validate all definition references up front (fail-fast, no half paste).
        Map<ResourceLocation, NodeDefinition> defs = snapshot.definitions();
        for (NodeSnapshot ns : snapshot.nodes()) {
            if (!defs.containsKey(ns.defId())) {
                throw new IllegalArgumentException("Unknown definition in snapshot: " + ns.defId());
            }
        }

        // Groups: allocate fresh ids, apply scale, stash.
        Map<Long, NodeGroupId> groupIds = new LinkedHashMap<>();
        for (GroupSnapshot gs : snapshot.groups()) {
            NodeGroup g = graph.createGroup(gs.header(), gs.x() + offsetX, gs.y() + offsetY, gs.w(), gs.h());
            g.setScale(gs.scale());
            groupIds.put(gs.localId(), g.id());
            savedGroups.add(g);
        }

        // Nodes: allocate fresh ids, apply header/widget overrides, stash.
        Map<Long, NodeId> nodeIds = new LinkedHashMap<>();
        for (NodeSnapshot ns : snapshot.nodes()) {
            NodeDefinition def = defs.get(ns.defId());
            Node node = graph.addNode(def, ns.x() + offsetX, ns.y() + offsetY);
            node.setHeader(ns.header());
            applyWidgetValues(node, ns.widgetValues());
            nodeIds.put(ns.localId(), node.id());
            savedNodes.add(node);
        }

        // Group membership: for nodes whose original group was selected.
        for (int i = 0; i < snapshot.nodes().size(); i++) {
            NodeSnapshot ns = snapshot.nodes().get(i);
            if (ns.localGroupId() != null) {
                NodeGroupId gid = groupIds.get(ns.localGroupId());
                Node saved = savedNodes.get(i);
                graph.setNodeGroup(saved.id(), gid);
            }
        }

        // Connections: rebuild with original auto/rule, remapped ids.
        for (ConnectionSnapshot cs : snapshot.connections()) {
            NodeId from = nodeIds.get(cs.fromLocalId());
            NodeId to = nodeIds.get(cs.toLocalId());
            Connection conn = new Connection(from, cs.fromOutput(), to, cs.toInput(),
                    cs.autoConverted(), cs.rule());
            graph.addConnection(conn);
            savedConnections.add(conn);
        }
    }

    private void redoExecute() {
        for (NodeGroup g : savedGroups) {
            graph.insertGroup(g);
        }
        for (Node n : savedNodes) {
            graph.insertNode(n);
        }
        for (Connection c : savedConnections) {
            graph.addConnection(c);
        }
    }

    private static void applyWidgetValues(Node node, Map<String, Object> values) {
        for (com.nodegraph.nodegraph.api.model.InputWidget w : node.widgets()) {
            if (values.containsKey(w.key())) {
                w.setCurrentValue(values.get(w.key()));
            }
        }
    }

    @Override
    public void undo() {
        for (Node n : savedNodes) {
            graph.removeNode(n.id());
        }
        for (NodeGroup g : savedGroups) {
            graph.removeGroup(g.id());
        }
    }

    @Override
    public String description() {
        return "Paste";
    }

    /** Ids of the nodes created by the most recent {@link #execute()}. */
    public List<NodeId> pastedNodeIds() {
        List<NodeId> ids = new ArrayList<>(savedNodes.size());
        for (Node n : savedNodes) {
            ids.add(n.id());
        }
        return Collections.unmodifiableList(ids);
    }
}
