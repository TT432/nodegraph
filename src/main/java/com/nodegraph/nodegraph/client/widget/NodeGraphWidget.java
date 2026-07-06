package com.nodegraph.nodegraph.client.widget;

import com.nodegraph.nodegraph.api.clipboard.Clipboard;
import com.nodegraph.nodegraph.api.command.AddNodeCommand;
import com.nodegraph.nodegraph.api.command.Command;
import com.nodegraph.nodegraph.api.command.CompositeCommand;
import com.nodegraph.nodegraph.api.command.ConnectCommand;
import com.nodegraph.nodegraph.api.command.GroupNodesCommand;
import com.nodegraph.nodegraph.api.command.RemoveGroupCommand;
import com.nodegraph.nodegraph.api.command.RemoveNodeCommand;
import com.nodegraph.nodegraph.api.command.SetGroupTransformCommand;
import com.nodegraph.nodegraph.api.command.SetWidgetValueCommand;
import com.nodegraph.nodegraph.api.command.UndoManager;
import com.nodegraph.nodegraph.api.def.NodeDefinition;
import com.nodegraph.nodegraph.api.def.NodeDefinitionCatalog;
import com.nodegraph.nodegraph.api.eval.EvaluationResult;
import com.nodegraph.nodegraph.api.eval.Evaluator;
import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.ConnectResult;
import com.nodegraph.nodegraph.api.model.InputWidget;
import com.nodegraph.nodegraph.api.model.InputWidgetKind;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.api.model.NodeGroup;
import com.nodegraph.nodegraph.api.model.NodeGroupId;
import com.nodegraph.nodegraph.api.model.NodeId;
import com.nodegraph.nodegraph.api.model.Port;
import com.nodegraph.nodegraph.api.type.Type;
import com.nodegraph.nodegraph.client.interaction.GroupPick;
import com.nodegraph.nodegraph.client.interaction.NodeInteractionController;
import com.nodegraph.nodegraph.client.interaction.SelectionController;
import com.nodegraph.nodegraph.client.layout.NodeLayout;
import com.nodegraph.nodegraph.client.render.ConnectionRenderer;
import com.nodegraph.nodegraph.client.render.NodeGroupRenderer;
import com.nodegraph.nodegraph.client.render.NodeRenderer;
import com.nodegraph.nodegraph.client.selection.SelectionModel;
import com.nodegraph.nodegraph.client.viewport.Viewport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 节点图画布 Widget。可经 {@code addRenderableWidget} 嵌入任意 Screen。
 *
 * <p>持有 {@link NodeGraph}（数据，只读）、{@link Viewport}（视口，可变）、
 * {@link SelectionModel}/{@link Clipboard}/{@link UndoManager}（编辑状态）。
 *
 * <p><b>事件路由（TaskI）</b>：
 * <ul>
 *   <li>右键(1) → 打开 {@link ContextMenu}（菜单打开期间吞掉后续点击）。</li>
 *   <li>左键(0) → 先委托 {@link NodeInteractionController}（节点头拖动/端口连线）；
 *       未命中 → {@link SelectionController} 框选。</li>
 *   <li>中键(2) → 平移画布。</li>
 *   <li>滚轮 → 平移（Shift=横向，无修饰=纵向）/缩放（Ctrl）。</li>
 * </ul>
 *
 * <p><b>派发契约</b>：默认 Screen 派发链不路由 {@code button != 0} 的 mouseDragged。
 * 宿主 {@link NodeGraphScreen} 直接转发全按钮事件到本 widget，故中键拖动可达。
 */
public class NodeGraphWidget extends AbstractWidget {
    public static final double GRID_SIZE = 20.0;
    public static final double SCROLL_SPEED = 16.0;
    public static final double ZOOM_FACTOR = 1.15;

    public static final int COLOR_BG = 0xFF1A1A1A;
    public static final int COLOR_GRID = 0xFF2B2B2B;
    public static final int COLOR_AXIS = 0xFF444444;
    public static final int SELECTED_COLOR = 0xFFFFFF00;
    public static final int BOX_FILL = 0x40FFFFFF;
    public static final int BOX_OUTLINE = 0xFFAAAAAA;

    private final NodeGraph graph;
    private final Viewport viewport;
    private final Font font;
    private final UndoManager undo;
    private final NodeInteractionController controller;
    private final Clipboard clipboard;
    private final SelectionModel selection;
    private final SelectionController selectionController;

    private enum State { IDLE, PANNING }

    private State state = State.IDLE;
    private int panButton = -1;
    private double lastMouseX;
    private double lastMouseY;

    /** 拖拽连线中的预览状态；null 时不渲染。 */
    private ConnectionDrag pending;
    /** 右键菜单；null 表示关闭。 */
    private ContextMenu menu;
    /** 添加节点浮层；null 表示关闭。 */
    private AddNodeOverlay addNodeOverlay;
    /** 正在编辑的 TEXT InputWidget 的 EditBox；null 表示无编辑。 */
    private EditBox activeEdit;
    private NodeId editNode;
    private int editWidgetIndex = -1;
    private String editKey;

    /** 连线拖拽预览状态。 */
    public static final class ConnectionDrag {
        private final NodeLayout fromLayout;
        private final int portIndex;
        private final boolean fromOutput;
        private final int color;

        public ConnectionDrag(NodeLayout fromLayout, int portIndex, boolean fromOutput, int color) {
            this.fromLayout = fromLayout;
            this.portIndex = portIndex;
            this.fromOutput = fromOutput;
            this.color = color;
        }

        public NodeLayout fromLayout() {
            return fromLayout;
        }

        public int portIndex() {
            return portIndex;
        }

        public boolean fromOutput() {
            return fromOutput;
        }

        public int color() {
            return color;
        }
    }

    public NodeGraphWidget(int x, int y, int width, int height, NodeGraph graph, UndoManager undo) {
        super(x, y, width, height, Component.empty());
        this.graph = Objects.requireNonNull(graph, "graph");
        this.undo = Objects.requireNonNull(undo, "undo");
        this.viewport = new Viewport();
        this.font = Minecraft.getInstance().font;
        this.controller = new NodeInteractionController(this);
        this.clipboard = new Clipboard();
        this.selection = new SelectionModel();
        this.selectionController = new SelectionController(this);
    }

    public NodeGraph graph() {
        return graph;
    }

    public Viewport viewport() {
        return viewport;
    }

    public UndoManager undo() {
        return undo;
    }

    public Clipboard clipboard() {
        return clipboard;
    }

    public SelectionModel selection() {
        return selection;
    }

    public ConnectionDrag pending() {
        return pending;
    }

    public void setPending(ConnectionDrag pending) {
        this.pending = pending;
    }

    public ContextMenu menu() {
        return menu;
    }

    @Override
    public boolean isValidClickButton(int button) {
        return button == 0 || button == 1 || button == 2;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (addNodeOverlay != null && addNodeOverlay.isOpen()) {
            addNodeOverlay.mouseClicked(mx, my);
            if (!addNodeOverlay.isOpen()) {
                addNodeOverlay = null;
            }
            return true;
        }
        if (activeEdit != null) {
            boolean inBox = mx >= activeEdit.getX() && mx <= activeEdit.getX() + activeEdit.getWidth()
                    && my >= activeEdit.getY() && my <= activeEdit.getY() + activeEdit.getHeight();
            if (inBox && button == 0) {
                activeEdit.mouseClicked(mx, my, button);
                return true;
            }
            confirmEdit();
        }
        if (!clicked(mx, my)) {
            return false;
        }
        // menu open: any click is consumed by the menu (close after handling)
        if (menu != null) {
            if (menu.contains(font, (int) mx, (int) my)) {
                menu.click(font, (int) mx, (int) my);
            }
            closeMenu();
            return true;
        }
        if (button == 1) {
            openMenu(mx, my);
            return true;
        }
        if (button == 0) {
            if (controller.onMouseClicked(mx, my, button)) {
                return true;
            }
            TextWidgetHit hit = pickTextWidget(worldX(mx), worldY(my));
            if (hit != null) {
                openEdit(hit.node(), hit.index());
                return true;
            }
            // empty canvas -> start box select
            selectionController.start(worldX(mx), worldY(my));
            return true;
        }
        if (button == 2) {
            state = State.PANNING;
            panButton = button;
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            if (controller.onMouseReleased(mx, my, button)) {
                return true;
            }
            if (selectionController.isSelecting()) {
                selectionController.finish(Screen.hasShiftDown());
                return true;
            }
        }
        if (state == State.PANNING && button == panButton) {
            state = State.IDLE;
            panButton = -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (menu != null) {
            return false;
        }
        if (button == 0) {
            if (controller.onMouseDragged(mx, my, button)) {
                return true;
            }
            if (selectionController.isSelecting()) {
                selectionController.drag(worldX(mx), worldY(my));
                return true;
            }
            return false;
        }
        if (state == State.PANNING && button == panButton) {
            viewport.pan(mx - lastMouseX, my - lastMouseY);
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (state == State.PANNING) {
            viewport.pan(mx - lastMouseX, my - lastMouseY);
            lastMouseX = mx;
            lastMouseY = my;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!clicked(mx, my)) {
            return false;
        }
        double s = viewport.scale();
        if (Screen.hasControlDown()) {
            Optional<NodeGroupId> gh = GroupPick.findGroupHeader(graph, worldX(mx), worldY(my));
            if (gh.isPresent()) {
                adjustGroupScale(gh.get(), delta);
                return true;
            }
            double factor = Math.pow(ZOOM_FACTOR, delta);
            viewport.zoom(factor, mx, my, getX(), getY());
        } else if (Screen.hasShiftDown()) {
            viewport.pan(delta * SCROLL_SPEED * s, 0);
        } else {
            viewport.pan(0, delta * SCROLL_SPEED * s);
        }
        return true;
    }

    private void adjustGroupScale(NodeGroupId gid, double delta) {
        NodeGroup grp = graph.group(gid);
        double factor = Math.pow(1.1, delta);
        double newScale = grp.scale() * factor;
        if (newScale < NodeGroupRenderer.MIN_GROUP_SCALE) newScale = NodeGroupRenderer.MIN_GROUP_SCALE;
        if (newScale > NodeGroupRenderer.MAX_GROUP_SCALE) newScale = NodeGroupRenderer.MAX_GROUP_SCALE;
        if (newScale == grp.scale()) {
            return;
        }
        undo.apply(new SetGroupTransformCommand(graph, gid, grp.x(), grp.y(), grp.width(), grp.height(), newScale));
    }

    // ---- selection / clipboard actions (shared by menu + keyboard) --------

    public void copy() {
        if (selection.isEmpty()) {
            return;
        }
        clipboard.copy(graph, selection.nodes(), selection.groups());
    }

    public void cut() {
        if (selection.isEmpty()) {
            return;
        }
        clipboard.cut(graph, selection.nodes(), selection.groups(), undo);
        selection.clear();
    }

    public void paste() {
        if (!clipboard.hasContent()) {
            return;
        }
        double ox = viewport.panX() + 16;
        double oy = viewport.panY() + 16;
        clipboard.paste(graph, undo, ox, oy);
    }

    public void delete() {
        if (selection.isEmpty()) {
            return;
        }
        List<Command> children = new ArrayList<>();
        for (NodeGroupId gid : selection.groups()) {
            children.add(new RemoveGroupCommand(graph, gid));
        }
        for (NodeId nid : selection.nodes()) {
            children.add(new RemoveNodeCommand(graph, nid));
        }
        undo.apply(new CompositeCommand("Delete", children));
        selection.clear();
    }

    public void groupSelection() {
        List<NodeId> nodes = selection.nodes();
        if (nodes.size() <= 1) {
            return;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (NodeId nid : nodes) {
            NodeLayout l = new NodeLayout(graph.node(nid));
            NodeLayout.Rect b = l.bounds();
            minX = Math.min(minX, b.x());
            minY = Math.min(minY, b.y());
            maxX = Math.max(maxX, b.x() + b.w());
            maxY = Math.max(maxY, b.y() + b.h());
        }
        undo.apply(new GroupNodesCommand(graph, Component.literal("Group"),
                minX, minY, maxX - minX, maxY - minY, nodes));
        selection.clear();
    }

    public void selectSingle(NodeId id) {
        selection.clear();
        selection.addNode(id);
    }

    public void selectSingleGroup(NodeGroupId id) {
        selection.clear();
        selection.addGroup(id);
    }

    public void clearSelection() {
        selection.clear();
    }

    /**
     * Handle a keyboard shortcut. Returns true if consumed. Key codes are raw
     * GLFW values (67=C, 86=V, 88=X, 90=Z, 261=Delete), matching MC 1.20.1
     * {@code Screen} internals.
     */
    public boolean handleKey(int keyCode) {
        boolean ctrl = Screen.hasControlDown();
        boolean shift = Screen.hasShiftDown();
        if (ctrl && keyCode == 67) { copy(); return true; }       // C
        if (ctrl && keyCode == 88) { cut(); return true; }        // X
        if (ctrl && keyCode == 86) { paste(); return true; }      // V
        if (keyCode == 261) { delete(); return true; }            // Delete
        if (ctrl && keyCode == 90) {                              // Z
            if (shift) { undo.redo(); } else { undo.undo(); }
            return true;
        }
        return false;
    }

    public void openMenu(double mx, double my) {
        double wx = worldX(mx);
        double wy = worldY(my);
        // ensure the right-clicked node is part of the selection
        NodeId hit = null;
        for (Node n : graph.nodes()) {
            if (new NodeLayout(n).bounds().contains(wx, wy)) {
                hit = n.id();
                break;
            }
        }
        if (hit != null && !selection.containsNode(hit)) {
            selectSingle(hit);
        }
        List<ContextMenu.MenuItem> items = new ArrayList<>();
        if (!selection.isEmpty()) {
            items.add(new ContextMenu.MenuItem(Component.literal("Copy"), true, this::copy));
            items.add(new ContextMenu.MenuItem(Component.literal("Cut"), true, this::cut));
        }
        if (clipboard.hasContent()) {
            items.add(new ContextMenu.MenuItem(Component.literal("Paste"), true, this::paste));
        }
        if (!selection.isEmpty()) {
            items.add(new ContextMenu.MenuItem(Component.literal("Delete"), true, this::delete));
        }
        if (selection.nodeCount() > 1) {
            items.add(new ContextMenu.MenuItem(Component.literal("Group"), true, this::groupSelection));
        }
        if (graph.catalog() != null && !graph.catalog().isEmpty()) {
            items.add(new ContextMenu.MenuItem(Component.literal("Add Node..."), true,
                    () -> openAddNodeOverlay(mx, my)));
        }
        if (items.isEmpty()) {
            return;
        }
        menu = new ContextMenu((int) mx, (int) my, items);
    }

    public void closeMenu() {
        menu = null;
    }

    public AddNodeOverlay addNodeOverlay() {
        return addNodeOverlay;
    }

    /** 打开添加节点浮层（无类型过滤，用于右键菜单"Add Node..."）。放置点 = 屏幕坐标对应的世界点。 */
    public void openAddNodeOverlay(double screenX, double screenY) {
        NodeDefinitionCatalog catalog = graph.catalog();
        if (catalog == null || catalog.isEmpty()) {
            return;
        }
        double wx = worldX(screenX);
        double wy = worldY(screenY);
        List<NodeDefinition> candidates = new ArrayList<>(catalog.all());
        openAddNodeOverlayCore(screenX, screenY, candidates, def ->
                undo.apply(new AddNodeCommand(graph, def, wx, wy)));
    }

    /**
     * 打开添加节点浮层，按源端口类型过滤候选，选中后自动添加节点并连线（拖线到空白场景）。
     *
     * @param fromOutput true=从输出端口拖出（找有同类型 input 的节点并连到该 input）；
     *                   false=从输入端口拖出（找有同类型 output 的节点，该 output 连到源 input）。
     */
    public void openAddNodeForConnection(double screenX, double screenY, double worldX, double worldY,
                                         NodeId sourceNode, int sourcePort, boolean fromOutput) {
        NodeDefinitionCatalog catalog = graph.catalog();
        if (catalog == null) {
            return;
        }
        Type t = portType(sourceNode, sourcePort, fromOutput);
        if (t == null) {
            return;
        }
        List<NodeDefinition> candidates = fromOutput ? catalog.withInputType(t) : catalog.withOutputType(t);
        if (candidates.isEmpty()) {
            return;
        }
        openAddNodeOverlayCore(screenX, screenY, candidates, def -> {
            AddNodeCommand add = new AddNodeCommand(graph, def, worldX, worldY);
            undo.apply(add);
            NodeId newNode = add.node().id();
            int matched = findPortOfType(newNode, !fromOutput, t);
            if (matched >= 0) {
                if (fromOutput) {
                    undo.apply(new ConnectCommand(graph, sourceNode, sourcePort, newNode, matched));
                } else {
                    undo.apply(new ConnectCommand(graph, newNode, matched, sourceNode, sourcePort));
                }
            }
        });
    }

    private void openAddNodeOverlayCore(double screenX, double screenY,
                                        List<NodeDefinition> candidates, Consumer<NodeDefinition> onPick) {
        addNodeOverlay = new AddNodeOverlay(font, (int) screenX, (int) screenY, candidates, onPick);
    }

    private Type portType(NodeId node, int portIndex, boolean output) {
        Node n = graph.node(node);
        if (output) {
            if (portIndex < 0 || portIndex >= n.outputs().size()) {
                return null;
            }
            return n.outputs().get(portIndex).value().type();
        }
        if (portIndex < 0 || portIndex >= n.inputs().size()) {
            return null;
        }
        return n.inputs().get(portIndex).value().type();
    }

    private int findPortOfType(NodeId node, boolean input, Type t) {
        Node n = graph.node(node);
        List<Port> ports = input ? n.inputs() : n.outputs();
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i).value().type().equals(t)) {
                return i;
            }
        }
        return -1;
    }

    public boolean overlayKey(int keyCode) {
        if (addNodeOverlay != null && addNodeOverlay.isOpen()) {
            if (addNodeOverlay.keyPressed(keyCode)) {
                if (!addNodeOverlay.isOpen()) {
                    addNodeOverlay = null;
                }
                return true;
            }
        }
        return false;
    }

    public boolean overlayChar(char codePoint) {
        if (addNodeOverlay != null && addNodeOverlay.isOpen()) {
            return addNodeOverlay.charTyped(codePoint);
        }
        return false;
    }

    // ---- widget editing --------------------------------------------------

    public boolean editKey(int keyCode) {
        if (activeEdit == null) {
            return false;
        }
        if (keyCode == 256) { // ESC
            cancelEdit();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // Enter / KP_Enter
            confirmEdit();
            return true;
        }
        activeEdit.keyPressed(keyCode, 0, 0);
        return true;
    }

    public boolean editChar(char codePoint) {
        if (activeEdit == null) {
            return false;
        }
        activeEdit.charTyped(codePoint, 0);
        return true;
    }

    private void openEdit(Node node, int widgetIndex) {
        InputWidget w = node.widgets().get(widgetIndex);
        int x0 = getX();
        int y0 = getY();
        double s = viewport.scale();
        double sx = viewport.worldToScreenX(node.x(), x0);
        double wy = node.y() + NodeLayout.HEADER_HEIGHT + widgetIndex * NodeLayout.ROW_HEIGHT;
        double sy = viewport.worldToScreenY(wy, y0);
        int sw = (int) Math.round(NodeLayout.NODE_WIDTH * s);
        int sh = (int) Math.round(NodeLayout.ROW_HEIGHT * s);
        EditBox box = new EditBox(font, (int) Math.round(sx), (int) Math.round(sy), sw, sh, Component.literal("edit"));
        box.setMaxLength(256);
        box.setValue(String.valueOf(w.currentValue()));
        box.setCanLoseFocus(false);
        box.setFocused(true);
        this.activeEdit = box;
        this.editNode = node.id();
        this.editWidgetIndex = widgetIndex;
        this.editKey = w.key();
    }

    private void confirmEdit() {
        if (activeEdit == null) {
            return;
        }
        String newVal = activeEdit.getValue();
        Node n = graph.node(editNode);
        String oldVal = String.valueOf(n.widgets().get(editWidgetIndex).currentValue());
        if (!newVal.equals(oldVal)) {
            undo.apply(new SetWidgetValueCommand(graph, editNode, editKey, newVal));
        }
        cancelEdit();
    }

    private void cancelEdit() {
        activeEdit = null;
        editNode = null;
        editWidgetIndex = -1;
        editKey = null;
    }

    private record TextWidgetHit(Node node, int index) {}

    private TextWidgetHit pickTextWidget(double wx, double wy) {
        for (Node n : graph.nodes()) {
            NodeLayout l = new NodeLayout(n);
            Optional<Integer> idx = l.pickInputWidget(wx, wy);
            if (idx.isPresent()) {
                if (n.widgets().get(idx.get()).kind() == InputWidgetKind.TEXT) {
                    return new TextWidgetHit(n, idx.get());
                }
                return null;
            }
        }
        return null;
    }

    // ---- rendering --------------------------------------------------------

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;
        g.fill(x0, y0, x1, y1, COLOR_BG);
        g.enableScissor(x0, y0, x1, y1);
        renderGrid(g);
        renderGroups(g);
        renderConnections(g);
        EvaluationResult evalResult;
        try {
            evalResult = new Evaluator().evaluateAll(graph);
        } catch (RuntimeException e) {
            evalResult = null;
        }
        Optional<List<Component>> tooltip = renderNodes(g, mouseX, mouseY, evalResult);
        renderSelectionBox(g);
        renderPending(g, mouseX, mouseY);
        g.disableScissor();
        if (tooltip.isPresent()) {
            g.renderComponentTooltip(font, tooltip.get(), mouseX, mouseY);
        }
        if (menu != null) {
            menu.render(g, font, mouseX, mouseY);
        }
        if (addNodeOverlay != null && addNodeOverlay.isOpen()) {
            addNodeOverlay.render(g, mouseX, mouseY);
        }
        if (activeEdit != null) {
            activeEdit.render(g, mouseX, mouseY, partialTick);
        }
    }

    protected void renderGroups(GuiGraphics g) {
        int x0 = getX();
        int y0 = getY();
        for (NodeGroup grp : graph.groups()) {
            boolean selected = selection.containsGroup(grp.id());
            NodeGroupRenderer.render(g, font, grp, viewport, x0, y0, selected);
        }
    }

    protected void renderConnections(GuiGraphics g) {
        int x0 = getX();
        int y0 = getY();
        for (Connection c : graph.connections()) {
            Node from = graph.node(c.fromNode());
            Node to = graph.node(c.toNode());
            NodeLayout fromLayout = new NodeLayout(from);
            NodeLayout toLayout = new NodeLayout(to);
            int color = c.isAutoConverted()
                    ? ConnectionRenderer.WARN_COLOR
                    : fromLayout.outputPortData(c.fromOutput()).value().type().color();
            double[] mid = ConnectionRenderer.render(g, viewport, x0, y0,
                    fromLayout, c.fromOutput(), toLayout, c.toInput(),
                    color, ConnectionRenderer.HALF_THICKNESS);
            if (c.isAutoConverted()) {
                ConnectionRenderer.renderWarnMark(g, mid[0], mid[1]);
            }
        }
    }

    protected void renderPending(GuiGraphics g, int mouseX, int mouseY) {
        if (pending == null) {
            return;
        }
        NodeLayout layout = pending.fromLayout();
        NodeLayout.PortAnchor a = pending.fromOutput()
                ? layout.outputPort(pending.portIndex())
                : layout.inputPort(pending.portIndex());
        int x0 = getX();
        int y0 = getY();
        double toWx = viewport.screenToWorldX(mouseX, x0);
        double toWy = viewport.screenToWorldY(mouseY, y0);
        int color = previewColor(toWx, toWy);
        ConnectionRenderer.renderPreview(g, viewport, x0, y0, a.x(), a.y(), toWx, toWy, color);
    }

    private int previewColor(double wx, double wy) {
        Optional<DropTarget> drop = pending.fromOutput()
                ? pickInputPortAt(wx, wy)
                : pickOutputPortAt(wx, wy);
        if (drop.isEmpty()) {
            return ConnectionRenderer.withAlpha(pending.color(), ConnectionRenderer.PREVIEW_ALPHA);
        }
        NodeId fromNode;
        int fromOutput;
        NodeId toNode;
        int toInput;
        if (pending.fromOutput()) {
            fromNode = pending.fromLayout().node().id();
            fromOutput = pending.portIndex();
            toNode = drop.get().nodeId();
            toInput = drop.get().index();
        } else {
            fromNode = drop.get().nodeId();
            fromOutput = drop.get().index();
            toNode = pending.fromLayout().node().id();
            toInput = pending.portIndex();
        }
        ConnectResult r = graph.canConnect(fromNode, fromOutput, toNode, toInput);
        if (r == ConnectResult.INCOMPATIBLE) {
            return ConnectionRenderer.withAlpha(0xFF0000, ConnectionRenderer.PREVIEW_ALPHA);
        }
        return pending.color() | 0xFF000000;
    }

    private Optional<DropTarget> pickInputPortAt(double wx, double wy) {
        for (Node n : graph.nodes()) {
            Optional<Integer> idx = new NodeLayout(n).pickInputPort(wx, wy);
            if (idx.isPresent()) {
                return Optional.of(new DropTarget(n.id(), idx.get()));
            }
        }
        return Optional.empty();
    }

    private Optional<DropTarget> pickOutputPortAt(double wx, double wy) {
        for (Node n : graph.nodes()) {
            Optional<Integer> idx = new NodeLayout(n).pickOutputPort(wx, wy);
            if (idx.isPresent()) {
                return Optional.of(new DropTarget(n.id(), idx.get()));
            }
        }
        return Optional.empty();
    }

    private record DropTarget(NodeId nodeId, int index) {}

    protected Optional<List<Component>> renderNodes(GuiGraphics g, int mouseX, int mouseY, EvaluationResult evalResult) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;
        double s = viewport.scale();
        Optional<List<Component>> firstHit = Optional.empty();
        for (Node node : graph.nodes()) {
            NodeLayout layout = new NodeLayout(node);
            double sx = viewport.worldToScreenX(node.x(), x0);
            double sy = viewport.worldToScreenY(node.y(), y0);
            double sw = NodeLayout.NODE_WIDTH * s;
            double sh = layout.height() * s;
            if (sx + sw < x0 || sx > x1 || sy + sh < y0 || sy > y1) {
                continue;
            }
            double wx = viewport.screenToWorldX(mouseX, x0);
            double wy = viewport.screenToWorldY(mouseY, y0);
            boolean hovered = layout.bounds().contains(wx, wy);
            Map<String, Object> outputs = null;
            boolean hasError = false;
            if (evalResult != null) {
                try {
                    outputs = evalResult.outputsOf(node.id());
                } catch (RuntimeException e) {
                    hasError = true;
                }
            }
            int editingIdx = (editNode != null && editNode.equals(node.id())) ? editWidgetIndex : -1;
            NodeRenderer.render(g, font, layout, viewport, x0, y0, hovered, outputs, hasError, editingIdx);
            if (selection.containsNode(node.id())) {
                int ix = (int) Math.floor(sx);
                int iy = (int) Math.floor(sy);
                int iw = (int) Math.round(sw);
                int ih = (int) Math.round(sh);
                g.renderOutline(ix - 1, iy - 1, iw + 2, ih + 2, SELECTED_COLOR);
            }
            if (firstHit.isEmpty()) {
                firstHit = NodeRenderer.pickHover(layout, viewport, x0, y0, mouseX, mouseY);
            }
        }
        return firstHit;
    }

    /** Render the in-progress box-select rectangle (screen space). */
    protected void renderSelectionBox(GuiGraphics g) {
        if (!selectionController.isSelecting()) {
            return;
        }
        double minX = selectionController.minWX();
        double minY = selectionController.minWY();
        double maxX = selectionController.maxWX();
        double maxY = selectionController.maxWY();
        if (minX == maxX || minY == maxY) {
            return;
        }
        int x0 = getX();
        int y0 = getY();
        int sMinX = (int) Math.floor(viewport.worldToScreenX(minX, x0));
        int sMinY = (int) Math.floor(viewport.worldToScreenY(minY, y0));
        int sMaxX = (int) Math.ceil(viewport.worldToScreenX(maxX, x0));
        int sMaxY = (int) Math.ceil(viewport.worldToScreenY(maxY, y0));
        g.fill(sMinX, sMinY, sMaxX, sMaxY, BOX_FILL);
        g.renderOutline(sMinX, sMinY, sMaxX - sMinX, sMaxY - sMinY, BOX_OUTLINE);
    }

    protected void renderGrid(GuiGraphics g) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;
        double worldLeft = viewport.screenToWorldX(x0, x0);
        double worldRight = viewport.screenToWorldX(x1, x0);
        double worldTop = viewport.screenToWorldY(y0, y0);
        double worldBottom = viewport.screenToWorldY(y1, y0);

        long firstV = (long) Math.ceil(worldLeft / GRID_SIZE);
        long lastV = (long) Math.floor(worldRight / GRID_SIZE);
        for (long i = firstV; i <= lastV; i++) {
            double wx = i * GRID_SIZE;
            int sx = (int) Math.round(viewport.worldToScreenX(wx, x0));
            int color = (i == 0) ? COLOR_AXIS : COLOR_GRID;
            g.fill(sx, y0, sx + 1, y1, color);
        }

        long firstH = (long) Math.ceil(worldTop / GRID_SIZE);
        long lastH = (long) Math.floor(worldBottom / GRID_SIZE);
        for (long j = firstH; j <= lastH; j++) {
            double wy = j * GRID_SIZE;
            int sy = (int) Math.round(viewport.worldToScreenY(wy, y0));
            int color = (j == 0) ? COLOR_AXIS : COLOR_GRID;
            g.fill(x0, sy, x1, sy + 1, color);
        }
    }

    private double worldX(double mx) {
        return viewport.screenToWorldX(mx, getX());
    }

    private double worldY(double my) {
        return viewport.screenToWorldY(my, getY());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
