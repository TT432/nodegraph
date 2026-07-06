package io.github.tt432.nodegraph.client.selection;

import io.github.tt432.nodegraph.api.model.NodeGroupId;
import io.github.tt432.nodegraph.api.model.NodeId;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Mutable selection state: the currently selected node ids and group ids.
 * Pure id set — no {@link io.github.tt432.nodegraph.api.model.NodeGraph}
 * dependency, so it is fully unit-testable. Callers (controllers/widget) are
 * responsible for keeping ids consistent with the live graph (e.g. clearing
 * after a delete).
 *
 * <p>Backed by {@link LinkedHashSet}s so {@link #nodes()} / {@link #groups()}
 * return insertion-ordered snapshots.
 */
public final class SelectionModel {
    private final Set<NodeId> nodes = new LinkedHashSet<>();
    private final Set<NodeGroupId> groups = new LinkedHashSet<>();

    public boolean addNode(NodeId id) {
        return nodes.add(Objects.requireNonNull(id, "id"));
    }

    public boolean addGroup(NodeGroupId id) {
        return groups.add(Objects.requireNonNull(id, "id"));
    }

    public boolean removeNode(NodeId id) {
        return nodes.remove(id);
    }

    public boolean removeGroup(NodeGroupId id) {
        return groups.remove(id);
    }

    /** @return true if the node is now selected (was not before). */
    public boolean toggleNode(NodeId id) {
        Objects.requireNonNull(id, "id");
        if (nodes.contains(id)) {
            nodes.remove(id);
            return false;
        }
        nodes.add(id);
        return true;
    }

    public boolean toggleGroup(NodeGroupId id) {
        Objects.requireNonNull(id, "id");
        if (groups.contains(id)) {
            groups.remove(id);
            return false;
        }
        groups.add(id);
        return true;
    }

    public void setNodes(Collection<NodeId> c) {
        nodes.clear();
        for (NodeId id : c) {
            nodes.add(Objects.requireNonNull(id, "id"));
        }
    }

    public void setGroups(Collection<NodeGroupId> c) {
        groups.clear();
        for (NodeGroupId id : c) {
            groups.add(Objects.requireNonNull(id, "id"));
        }
    }

    public boolean containsNode(NodeId id) {
        return nodes.contains(id);
    }

    public boolean containsGroup(NodeGroupId id) {
        return groups.contains(id);
    }

    public void clear() {
        nodes.clear();
        groups.clear();
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && groups.isEmpty();
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int groupCount() {
        return groups.size();
    }

    public List<NodeId> nodes() {
        return List.copyOf(nodes);
    }

    public List<NodeGroupId> groups() {
        return List.copyOf(groups);
    }
}
