package io.github.tt432.nodegraph.client.interaction;

import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeGroupId;

import java.util.Optional;

/**
 * Pure-logic hit testing for {@link io.github.tt432.nodegraph.api.model.NodeGroup}.
 * Depends only on the data model (no MC rendering classes), so it is JUnit-testable
 * and serves as the single geometric source for the interaction controller and the widget.
 *
 * <ul>
 *   <li>{@link #findGroupContaining} — point inside a group's frame (membership test
 *       for drag-into-group on node release).</li>
 *   <li>{@link #findGroupHeader} — point inside a group's header strip (drag group /
 *       Ctrl+wheel scale).</li>
 *   <li>{@link #findResizeHandle} — point inside a group's bottom-right resize grip.</li>
 * </ul>
 *
 * <p>When multiple overlapping groups match, the first in {@code graph.groups()}
 * iteration order is returned (deterministic; nested-group semantics deferred).
 */
public final class GroupPick {
    public static final double GROUP_HEADER = 14.0;
    public static final double RESIZE_HANDLE = 8.0;

    private GroupPick() {}

    /** Point inside the group's full frame rectangle. */
    public static Optional<NodeGroupId> findGroupContaining(NodeGraph graph, double wx, double wy) {
        for (var g : graph.groups()) {
            if (g.contains(wx, wy)) {
                return Optional.of(g.id());
            }
        }
        return Optional.empty();
    }

    /** Point inside the group's header strip (top {@link #GROUP_HEADER} rows of the frame). */
    public static Optional<NodeGroupId> findGroupHeader(NodeGraph graph, double wx, double wy) {
        for (var g : graph.groups()) {
            boolean inX = wx >= g.x() && wx <= g.x() + g.width();
            boolean inY = wy >= g.y() && wy <= g.y() + GROUP_HEADER;
            if (inX && inY) {
                return Optional.of(g.id());
            }
        }
        return Optional.empty();
    }

    /** Point inside the group's bottom-right resize grip. */
    public static Optional<NodeGroupId> findResizeHandle(NodeGraph graph, double wx, double wy) {
        for (var g : graph.groups()) {
            double hx = g.x() + g.width() - RESIZE_HANDLE;
            double hy = g.y() + g.height() - RESIZE_HANDLE;
            if (wx >= hx && wx <= hx + RESIZE_HANDLE && wy >= hy && wy <= hy + RESIZE_HANDLE) {
                return Optional.of(g.id());
            }
        }
        return Optional.empty();
    }
}
