package io.github.tt432.nodegraph.client.interaction;

import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGroupId;
import io.github.tt432.nodegraph.api.model.NodeId;
import io.github.tt432.nodegraph.api.model.NodeGroup;
import io.github.tt432.nodegraph.client.layout.NodeLayout;
import io.github.tt432.nodegraph.client.selection.SelectionModel;
import io.github.tt432.nodegraph.client.widget.NodeGraphWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Rubber-band box selection on {@link NodeGraphWidget}. Entered when a left
 * click lands on empty canvas (after {@link NodeInteractionController} declined
 * the event). While dragging, {@link #drag} updates the current corner; on
 * release {@link #finish} computes the world-space rectangle and selects every
 * node whose {@link NodeLayout#bounds()} intersects it.
 *
 * <p>A zero-area box (pure click on empty space) clears the selection (unless
 * {@code additive}). {@code additive} (Shift) unions hits with the current
 * selection instead of replacing it.
 *
 * <p>The widget reads {@link #isSelecting()} + the min/max accessors to render
 * the box overlay.
 */
public final class SelectionController {
    private final NodeGraphWidget widget;

    private boolean selecting = false;
    private double startWX;
    private double startWY;
    private double curWX;
    private double curWY;

    public SelectionController(NodeGraphWidget widget) {
        this.widget = widget;
    }

    public boolean isSelecting() {
        return selecting;
    }

    public double minWX() {
        return Math.min(startWX, curWX);
    }

    public double minWY() {
        return Math.min(startWY, curWY);
    }

    public double maxWX() {
        return Math.max(startWX, curWX);
    }

    public double maxWY() {
        return Math.max(startWY, curWY);
    }

    public void start(double wx, double wy) {
        selecting = true;
        startWX = wx;
        startWY = wy;
        curWX = wx;
        curWY = wy;
    }

    public void drag(double wx, double wy) {
        curWX = wx;
        curWY = wy;
    }

    /**
     * Finalize the box. Replaces (or, when {@code additive}, unions) the
     * selection with the nodes intersecting the box. A zero-area box clears the
     * selection unless {@code additive}.
     */
    public void finish(boolean additive) {
        if (!selecting) {
            return;
        }
        selecting = false;
        SelectionModel sel = widget.selection();
        boolean zeroArea = startWX == curWX || startWY == curWY;
        if (zeroArea) {
            if (!additive) {
                sel.clear();
            }
            return;
        }
        double minX = minWX();
        double minY = minWY();
        double maxX = maxWX();
        double maxY = maxWY();
        List<NodeId> hits = new ArrayList<>();
        for (Node n : widget.graph().nodes()) {
            NodeLayout l = new NodeLayout(n);
            NodeLayout.Rect b = l.bounds();
            if (b.x() <= maxX && b.x() + b.w() >= minX
                    && b.y() <= maxY && b.y() + b.h() >= minY) {
                hits.add(n.id());
            }
        }
        List<NodeGroupId> groupHits = new ArrayList<>();
        for (NodeGroup grp : widget.graph().groups()) {
            double gx = grp.x();
            double gy = grp.y();
            double gx2 = gx + grp.width();
            double gy2 = gy + grp.height();
            if (gx <= maxX && gx2 >= minX && gy <= maxY && gy2 >= minY) {
                groupHits.add(grp.id());
            }
        }
        if (additive) {
            for (NodeId id : hits) {
                sel.addNode(id);
            }
            for (NodeGroupId gid : groupHits) {
                sel.addGroup(gid);
            }
        } else {
            sel.setNodes(hits);
            sel.setGroups(groupHits);
        }
    }

    /** Abort an in-progress box without changing selection (e.g. menu opening). */
    public void cancel() {
        selecting = false;
    }
}
