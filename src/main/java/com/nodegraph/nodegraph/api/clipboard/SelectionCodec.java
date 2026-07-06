package com.nodegraph.nodegraph.api.clipboard;

import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeGroup;
import com.nodegraph.nodegraph.api.model.NodeId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encode a selection to a self-describing {@link SelectionSnapshot} and decode
 * one back into a {@link PasteCommand} (not yet executed).
 *
 * <p><b>Encode</b> captures:
 * <ul>
 *   <li>the selected nodes, each with header, position, widget values, and a
 *       local id;</li>
 *   <li>the selected groups, each with full transform and a local id;</li>
 *   <li>the connections among the selected nodes (both endpoints selected) —
 *       external wires are dropped;</li>
 *   <li>for each node, a local group id only if its group is also selected
 *       (otherwise the node pastes as a free node);</li>
 *   <li>the distinct {@link NodeDefinition}s referenced by the selected nodes.</li>
 * </ul>
 *
 * <p><b>Decode</b> builds a {@link PasteCommand} that, when executed, allocates
 * fresh graph ids and rebuilds the topology. The {@code offsetX/Y} are added to
 * every node and group position so paste can offset the result visually.
 */
public final class SelectionCodec {

    private SelectionCodec() {}

    /**
     * Encode the given selection. {@code nodes} and {@code groups} order is
     * preserved (LinkedHashMap order), which drives local-id assignment.
     *
     * @throws IllegalArgumentException if any id is unknown to {@code graph}
     *                                  (via {@link NodeGraph#node} / {@link NodeGraph#group}).
     */
    public static SelectionSnapshot encode(NodeGraph graph,
                                           Collection<NodeId> nodes,
                                           Collection<NodeGroupId> groups) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(groups, "groups");

        // Assign local ids in iteration order (deterministic).
        Map<NodeId, Long> nodeLocalId = new LinkedHashMap<>();
        long nextNodeId = 0;
        for (NodeId id : nodes) {
            nodeLocalId.put(id, nextNodeId++);
        }
        Map<NodeGroupId, Long> groupLocalId = new LinkedHashMap<>();
        long nextGroupId = 0;
        for (NodeGroupId id : groups) {
            groupLocalId.put(id, nextGroupId++);
        }

        // Collect distinct definitions (decode resolves defId against this map).
        Map<net.minecraft.resources.ResourceLocation, NodeDefinition> definitions = new LinkedHashMap<>();
        List<NodeSnapshot> nodeSnaps = new ArrayList<>();
        for (Map.Entry<NodeId, Long> e : nodeLocalId.entrySet()) {
            Node n = graph.node(e.getKey());
            definitions.put(n.definition().id(), n.definition());
            nodeSnaps.add(new NodeSnapshot(
                    e.getValue(),
                    n.definition().id(),
                    n.header(),
                    n.x(),
                    n.y(),
                    collectWidgetValues(n),
                    localGroupIdOf(n.groupId(), groupLocalId)
            ));
        }

        List<GroupSnapshot> groupSnaps = new ArrayList<>();
        for (Map.Entry<NodeGroupId, Long> e : groupLocalId.entrySet()) {
            NodeGroup g = graph.group(e.getKey());
            groupSnaps.add(new GroupSnapshot(
                    e.getValue(),
                    g.header(),
                    g.x(),
                    g.y(),
                    g.width(),
                    g.height(),
                    g.scale()
            ));
        }

        // Internal connections only.
        List<ConnectionSnapshot> connSnaps = new ArrayList<>();
        for (Connection c : graph.connections()) {
            Long fromLocal = nodeLocalId.get(c.fromNode());
            Long toLocal = nodeLocalId.get(c.toNode());
            if (fromLocal != null && toLocal != null) {
                connSnaps.add(new ConnectionSnapshot(
                        fromLocal, c.fromOutput(),
                        toLocal, c.toInput(),
                        c.isAutoConverted(),
                        c.rule()
                ));
            }
        }

        return new SelectionSnapshot(definitions, nodeSnaps, groupSnaps, connSnaps);
    }

    /**
     * Build a {@link PasteCommand} for {@code snapshot} on {@code graph}. The
     * command is returned un-executed; the caller (typically {@link Clipboard})
     * is responsible for applying it via an {@code UndoManager}.
     */
    public static PasteCommand decode(SelectionSnapshot snapshot,
                                      NodeGraph graph,
                                      double offsetX, double offsetY) {
        return new PasteCommand(graph, snapshot, offsetX, offsetY);
    }

    private static Map<String, Object> collectWidgetValues(Node n) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (com.nodegraph.nodegraph.api.model.InputWidget w : n.widgets()) {
            values.put(w.key(), w.currentValue());
        }
        return values;
    }

    private static Long localGroupIdOf(NodeGroupId gid, Map<NodeGroupId, Long> groupLocalId) {
        if (gid == null) {
            return null;
        }
        return groupLocalId.get(gid);
    }
}
