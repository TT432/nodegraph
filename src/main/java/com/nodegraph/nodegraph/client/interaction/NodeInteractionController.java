package com.nodegraph.nodegraph.client.interaction;

import com.nodegraph.nodegraph.api.command.Command;
import com.nodegraph.nodegraph.api.command.CompositeCommand;
import com.nodegraph.nodegraph.api.command.ConnectCommand;
import com.nodegraph.nodegraph.api.command.MoveNodeCommand;
import com.nodegraph.nodegraph.api.command.SetGroupTransformCommand;
import com.nodegraph.nodegraph.api.command.SetNodeGroupCommand;
import com.nodegraph.nodegraph.api.model.ConnectResult;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroup;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeId;
import com.nodegraph.nodegraph.client.layout.NodeLayout;
import com.nodegraph.nodegraph.client.widget.NodeGraphWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles left-click interactions on {@link NodeGraphWidget}:
 * <ul>
 *   <li>drag a node header → move the node (live {@code setPosition} during drag,
 *       a single {@link MoveNodeCommand} on release; on release the node's group
 *       membership is re-evaluated — drag into a group frame → join, drag out → leave);</li>
 *   <li>drag an output/input port → pull a connection (preview rendered by the
 *       widget; on release the drop target is type-checked and a
 *       {@link ConnectCommand} committed when compatible);</li>
 *   <li>drag a group header → move the group frame together with all its members
 *       (one {@link CompositeCommand} on release);</li>
 *   <li>drag a group's bottom-right resize grip → resize the frame (one
 *       {@link SetGroupTransformCommand} on release).</li>
 * </ul>
 *
 * <p>Pick priority (button 0): port → node header → group resize grip → group
 * header. First hit wins; misses fall through to the widget's box-select.
 *
 * <p>State is mutually exclusive; only entered via left-click (button==0).
 */
public final class NodeInteractionController {
    private static final double MIN_GROUP_W = 40.0;
    private static final double MIN_GROUP_H = 34.0;

    private final NodeGraphWidget widget;

    private enum State { IDLE, DRAG_NODE, DRAG_CONNECTION, DRAG_GROUP, DRAG_RESIZE_GROUP }

    private State state = State.IDLE;

    // shared press point (states are mutually exclusive)
    private double pressWX;
    private double pressWY;

    // DRAG_NODE
    private NodeId dragTarget;
    private double grabNX;
    private double grabNY;

    // DRAG_CONNECTION
    private NodeLayout connFromLayout;
    private int connPortIndex;
    private boolean connFromOutput;
    private int connColor;

    // DRAG_GROUP
    private NodeGroupId groupTarget;
    private double groupGrabX;
    private double groupGrabY;
    private List<MemberOrig> groupMembers;

    // DRAG_RESIZE_GROUP
    private NodeGroupId resizeTarget;
    private double resizeOrigX;
    private double resizeOrigY;
    private double resizeOrigW;
    private double resizeOrigH;

    public NodeInteractionController(NodeGraphWidget widget) {
        this.widget = widget;
    }

    public boolean onMouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        double wx = worldX(mx);
        double wy = worldY(my);
        NodeGraph graph = widget.graph();

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
                widget.selection().toggleNode(header.get().node().id());
                return true;
            }
            startDragNode(header.get(), wx, wy);
            return true;
        }
        Optional<NodeGroupId> resize = GroupPick.findResizeHandle(graph, wx, wy);
        if (resize.isPresent()) {
            startDragResize(resize.get(), wx, wy);
            return true;
        }
        Optional<NodeGroupId> gh = GroupPick.findGroupHeader(graph, wx, wy);
        if (gh.isPresent()) {
            if (Screen.hasShiftDown()) {
                widget.selection().toggleGroup(gh.get());
                return true;
            }
            startDragGroup(gh.get(), wx, wy);
            return true;
        }
        return false;
    }

    public boolean onMouseDragged(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        double wx = worldX(mx);
        double wy = worldY(my);
        double dx = wx - pressWX;
        double dy = wy - pressWY;
        switch (state) {
            case DRAG_NODE: {
                Node n = widget.graph().node(dragTarget);
                n.setPosition(grabNX + dx, grabNY + dy);
                return true;
            }
            case DRAG_CONNECTION:
                return true;
            case DRAG_GROUP: {
                NodeGraph graph = widget.graph();
                graph.group(groupTarget).setPosition(groupGrabX + dx, groupGrabY + dy);
                for (MemberOrig m : groupMembers) {
                    graph.node(m.id).setPosition(m.x + dx, m.y + dy);
                }
                return true;
            }
            case DRAG_RESIZE_GROUP: {
                double newW = Math.max(MIN_GROUP_W, resizeOrigW + dx);
                double newH = Math.max(MIN_GROUP_H, resizeOrigH + dy);
                widget.graph().group(resizeTarget).setSize(newW, newH);
                return true;
            }
            default:
                return false;
        }
    }

    public boolean onMouseReleased(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        double wx = worldX(mx);
        double wy = worldY(my);
        double dx = wx - pressWX;
        double dy = wy - pressWY;
        switch (state) {
            case DRAG_NODE: {
                double endX = grabNX + dx;
                double endY = grabNY + dy;
                if (endX != grabNX || endY != grabNY) {
                    finishNodeMove(endX, endY);
                } else {
                    widget.selectSingle(dragTarget);
                }
                resetDragNode();
                return true;
            }
            case DRAG_CONNECTION: {
                tryConnect(mx, my, wx, wy);
                widget.setPending(null);
                resetDragConnection();
                return true;
            }
            case DRAG_GROUP: {
                if (dx != 0 || dy != 0) {
                    finishGroupMove(dx, dy);
                } else {
                    widget.selectSingleGroup(groupTarget);
                }
                resetDragGroup();
                return true;
            }
            case DRAG_RESIZE_GROUP: {
                double newW = Math.max(MIN_GROUP_W, resizeOrigW + dx);
                double newH = Math.max(MIN_GROUP_H, resizeOrigH + dy);
                if (newW != resizeOrigW || newH != resizeOrigH) {
                    NodeGraph graph = widget.graph();
                    graph.group(resizeTarget); // existence check
                    widget.undo().apply(new SetGroupTransformCommand(
                            graph, resizeTarget,
                            resizeOrigX, resizeOrigY, newW, newH,
                            graph.group(resizeTarget).scale()));
                }
                resetDragResize();
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Commit a node move. If the node's final center falls inside a different
     * group frame, a {@link SetNodeGroupCommand} is appended (drag-into-group /
     * drag-out-of-group).
     */
    private void finishNodeMove(double endX, double endY) {
        NodeGraph graph = widget.graph();
        NodeLayout layout = new NodeLayout(graph.node(dragTarget));
        double cx = endX + NodeLayout.NODE_WIDTH / 2.0;
        double cy = endY + layout.height() / 2.0;
        NodeGroupId newGroup = GroupPick.findGroupContaining(graph, cx, cy).orElse(null);
        NodeGroupId currentGroup = graph.node(dragTarget).groupId();
        boolean membershipChanged = !Objects.equals(newGroup, currentGroup);
        if (membershipChanged) {
            List<Command> children = new ArrayList<>();
            children.add(new MoveNodeCommand(graph, dragTarget, grabNX, grabNY, endX, endY));
            children.add(new SetNodeGroupCommand(graph, dragTarget, newGroup));
            widget.undo().apply(new CompositeCommand("Move node", children));
        } else {
            widget.undo().apply(new MoveNodeCommand(graph, dragTarget, grabNX, grabNY, endX, endY));
        }
    }

    private void finishGroupMove(double dx, double dy) {
        NodeGraph graph = widget.graph();
        double newX = groupGrabX + dx;
        double newY = groupGrabY + dy;
        double w = graph.group(groupTarget).width();
        double h = graph.group(groupTarget).height();
        double sc = graph.group(groupTarget).scale();
        List<Command> children = new ArrayList<>();
        children.add(new SetGroupTransformCommand(graph, groupTarget, newX, newY, w, h, sc));
        for (MemberOrig m : groupMembers) {
            children.add(new MoveNodeCommand(graph, m.id, m.x, m.y, m.x + dx, m.y + dy));
        }
        widget.undo().apply(new CompositeCommand("Move group", children));
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

    private void startDragGroup(NodeGroupId gid, double wx, double wy) {
        state = State.DRAG_GROUP;
        groupTarget = gid;
        NodeGraph graph = widget.graph();
        NodeGroup grp = graph.group(gid);
        groupGrabX = grp.x();
        groupGrabY = grp.y();
        pressWX = wx;
        pressWY = wy;
        groupMembers = new ArrayList<>();
        for (Node n : graph.members(gid)) {
            groupMembers.add(new MemberOrig(n.id(), n.x(), n.y()));
        }
    }

    private void startDragResize(NodeGroupId gid, double wx, double wy) {
        state = State.DRAG_RESIZE_GROUP;
        resizeTarget = gid;
        NodeGroup grp = widget.graph().group(gid);
        resizeOrigX = grp.x();
        resizeOrigY = grp.y();
        resizeOrigW = grp.width();
        resizeOrigH = grp.height();
        pressWX = wx;
        pressWY = wy;
    }

    private void tryConnect(double mx, double my, double wx, double wy) {
        NodeGraph graph = widget.graph();
        NodeId fromNode;
        int fromOutput;
        NodeId toNode;
        int toInput;
        if (connFromOutput) {
            Optional<PortHit> drop = pickInputPort(wx, wy);
            if (drop.isEmpty()) {
                // 拖输出到空白：弹出"有同类型 input 的节点"添加菜单
                widget.openAddNodeForConnection(mx, my, wx, wy,
                        connFromLayout.node().id(), connPortIndex, true);
                return;
            }
            fromNode = connFromLayout.node().id();
            fromOutput = connPortIndex;
            toNode = drop.get().layout().node().id();
            toInput = drop.get().index();
        } else {
            Optional<PortHit> drop = pickOutputPort(wx, wy);
            if (drop.isEmpty()) {
                // 拖输入到空白：弹出"有同类型 output 的节点"添加菜单
                widget.openAddNodeForConnection(mx, my, wx, wy,
                        connFromLayout.node().id(), connPortIndex, false);
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

    private void resetDragGroup() {
        state = State.IDLE;
        groupTarget = null;
        groupGrabX = 0;
        groupGrabY = 0;
        groupMembers = null;
        pressWX = 0;
        pressWY = 0;
    }

    private void resetDragResize() {
        state = State.IDLE;
        resizeTarget = null;
        resizeOrigX = 0;
        resizeOrigY = 0;
        resizeOrigW = 0;
        resizeOrigH = 0;
        pressWX = 0;
        pressWY = 0;
    }

    private record PortHit(NodeLayout layout, int index) {}

    private record MemberOrig(NodeId id, double x, double y) {}
}
