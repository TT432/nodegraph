package com.nodegraph.nodegraph.api.clipboard;

import com.nodegraph.nodegraph.api.def.NodeDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Immutable, self-describing snapshot of a selection (a set of nodes, a set of
 * node groups, and the internal connections among the selected nodes).
 *
 * <p>Self-describing means it carries the {@link NodeDefinition}s referenced by
 * the captured nodes, so paste does not need an external definition registry.
 * Definitions cannot be disk-serialized (they contain lambdas), but as an
 * in-process clipboard object the references are valid and complete.
 *
 * <p>Node and group identity inside the snapshot uses <b>local ids</b>
 * (longs, allocated per-namespace starting at 0); global graph ids are
 * allocated fresh on paste.
 */
public record SelectionSnapshot(
        Map<ResourceLocation, NodeDefinition> definitions,
        List<NodeSnapshot> nodes,
        List<GroupSnapshot> groups,
        List<ConnectionSnapshot> connections
) {
    public SelectionSnapshot {
        definitions = Map.copyOf(definitions);
        nodes = List.copyOf(nodes);
        groups = List.copyOf(groups);
        connections = List.copyOf(connections);
    }
}
