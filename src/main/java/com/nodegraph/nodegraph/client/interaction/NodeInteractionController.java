package com.nodegraph.nodegraph.client.interaction;

import com.nodegraph.nodegraph.api.command.ConnectCommand;
import com.nodegraph.nodegraph.api.command.MoveNodeCommand;
import com.nodegraph.nodegraph.api.model.ConnectResult;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeId;
import com.nodegraph.nodegraph.client.layout.NodeLayout;
import com.nodegraph.nodegraph.client.widget.NodeGraphWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

/**
 * Handles left-click node interactions on {@link NodeGraphWidget}:
 * <ul>
 *   <li>drag a node header → move the node (live {@code setPosition} during drag,
 *       a single {@link MoveNodeCommand} committed on release);</li>
 *   <li>drag an output/input port → pull a connection (preview rendered by the
 *       widget; on release the drop target is type-checked via
 *       {@link NodeGraph#canConnect} and a {@link ConnectCommand} committed when
 *       compatible).</li>
 * </ul>
 *
 * <p>The widget delegates {@code mouseClicked/Dragged/Released} to this
 * controller first; when it returns {@code false} (no node hit / non-left
 * button) the widget falls back to its own pan logic. Mid-button panning and
 * right-click (TaskI menu) stay outside this controller.
 *
 * <p>State is mutually exclusive: at most one of DRAG_NODE / DRAG_CONNECTION is
 * active, and only entered via left-click (button==0).
 */
public final class NodeInteractionController {
    private final NodeGraphWidget widget;

    private enum State { IDLE, DRAG_NODE, DRAG_CONNECTION }

    private State state = State.IDLE;

    // DRAG_NODE
    private NodeId dragTarget;
    private double grabNX;
    private double grabNY;
    private double pressWX;
    private double pressWY;

    // DRAG_CONNECTION
    private NodeLayout connFromLayout;
    private int connPortIndex;
    private boolean connFromOutput;
    private int connColor;

    public NodeInteractionController(NodeGraphWidget widget) {
        this.widget = widget;
    }

    public boolean onMouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        double wx = worldX(mx);
        double wy = worldY(my);
        Optional<PortHit> out = pickOutputPort(wx, wy);
        if (out.isPresent()) {
            startConnection(out.get().layout(), out.get().index(), true);
            return true;
        }
        Optional<PortHit> in = pickInputPort(wx, wy);
        if (in.isPresent()) {
            startConnection(in.get().layout(), in.get().index(), false);
            return true;
        }
        Optional<NodeLayout> header = pickHeader(wx, wy);
        if (header.isPresent()) {
            if (Screen.hasShiftDown()) {
                // Shift+click header toggles membership without dragging.
                widget.selection().toggleNode(header.get().node().id());
                return true;
            }
            startDragNode(header.get(), wx, wy);
            return true;
        }
        return false;
    }

    public boolean onMouseDragged(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        switch (state) {
            case DRAG_NODE: {
                double wx = worldX(mx);
                double wy = worldY(my);
                Node n = widget.graph().node(dragTarget);
                n.setPosition(grabNX + (wx - pressWX), grabNY + (wy - pressWY));
                return true;
            }
            case DRAG_CONNECTION:
                // preview is drawn by the widget from pending + current mouse; nothing to mutate
                return true;
            default:
                return false;
        }
    }

    public boolean onMouseReleased(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        switch (state) {
            case DRAG_NODE: {
                double wx = worldX(mx);
                double wy = worldY(my);
                double endX = grabNX + (wx - pressWX);
                double endY = grabNY + (wy - pressWY);
                if (endX != grabNX || endY != grabNY) {
                    widget.undo().apply(new MoveNodeCommand(
                            widget.graph(), dragTarget, grabNX, grabNY, endX, endY));
                } else {
                    // pure click on header (no drag) -> single-select it
                    widget.selectSingle(dragTarget);
                }
                resetDragNode();
                return true;
            }
            case DRAG_CONNECTION: {
                double wx = worldX(mx);
                double wy = worldY(my);
                tryConnect(wx, wy);
                widget.setPending(null);
                resetDragConnection();
                return true;
            }
            default:
                return false;
        }
    }

    private void startDragNode(NodeLayout layout, double wx, double wy) {
        state = State.DRAG_NODE;
        dragTarget = layout.node().id();
        grabNX = layout.node().x();
        grabNY = layout.node().y();
        pressWX = wx;
        pressWY = wy;
    }

    private void startConnection(NodeLayout layout, int portIndex, boolean fromOutput) {
        state = State.DRAG_CONNECTION;
        connFromLayout = layout;
        connPortIndex = portIndex;
        connFromOutput = fromOutput;
        connColor = (fromOutput ? layout.outputPortData(portIndex) : layout.inputPortData(portIndex))
                .value().type().color();
        widget.setPending(new NodeGraphWidget.ConnectionDrag(layout, portIndex, fromOutput, connColor));
    }

    private void tryConnect(double wx, double wy) {
        NodeGraph graph = widget.graph();
        NodeId fromNode;
        int fromOutput;
        NodeId toNode;
        int toInput;
        if (connFromOutput) {
            Optional<PortHit> drop = pickInputPort(wx, wy);
            if (drop.isEmpty()) {
                return;
            }
            fromNode = connFromLayout.node().id();
            fromOutput = connPortIndex;
            toNode = drop.get().layout().node().id();
            toInput = drop.get().index();
        } else {
            Optional<PortHit> drop = pickOutputPort(wx, wy);
            if (drop.isEmpty()) {
                return;
            }
            fromNode = drop.get().layout().node().id();
            fromOutput = drop.get().index();
            toNode = connFromLayout.node().id();
            toInput = connPortIndex;
        }
        ConnectResult r = graph.canConnect(fromNode, fromOutput, toNode, toInput);
        if (r == ConnectResult.INCOMPATIBLE) {
            return;
        }
        widget.undo().apply(new ConnectCommand(graph, fromNode, fromOutput, toNode, toInput));
    }

    private Optional<PortHit> pickOutputPort(double wx, double wy) {
        for (Node n : widget.graph().nodes()) {
            NodeLayout l = new NodeLayout(n);
            Optional<Integer> idx = l.pickOutputPort(wx, wy);
            if (idx.isPresent()) {
                return Optional.of(new PortHit(l, idx.get()));
            }
        }
        return Optional.empty();
    }

    private Optional<PortHit> pickInputPort(double wx, double wy) {
        for (Node n : widget.graph().nodes()) {
            NodeLayout l = new NodeLayout(n);
            Optional<Integer> idx = l.pickInputPort(wx, wy);
            if (idx.isPresent()) {
                return Optional.of(new PortHit(l, idx.get()));
            }
        }
        return Optional.empty();
    }

    private Optional<NodeLayout> pickHeader(double wx, double wy) {
        for (Node n : widget.graph().nodes()) {
            NodeLayout l = new NodeLayout(n);
            if (l.headerContains(wx, wy)) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    private double worldX(double mx) {
        return widget.viewport().screenToWorldX(mx, widget.getX());
    }

    private double worldY(double my) {
        return widget.viewport().screenToWorldY(my, widget.getY());
    }

    private void resetDragNode() {
        state = State.IDLE;
        dragTarget = null;
        grabNX = 0;
        grabNY = 0;
        pressWX = 0;
        pressWY = 0;
    }

    private void resetDragConnection() {
        state = State.IDLE;
        connFromLayout = null;
        connPortIndex = 0;
        connFromOutput = false;
        connColor = 0;
    }

    private record PortHit(NodeLayout layout, int index) {}
}
