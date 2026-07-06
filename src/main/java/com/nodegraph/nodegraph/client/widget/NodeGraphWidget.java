package com.nodegraph.nodegraph.client.widget;

import com.nodegraph.nodegraph.api.model.Connection;
import com.nodegraph.nodegraph.api.model.Node;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import com.nodegraph.nodegraph.client.layout.NodeLayout;
import com.nodegraph.nodegraph.client.render.ConnectionRenderer;
import com.nodegraph.nodegraph.client.render.NodeRenderer;
import com.nodegraph.nodegraph.client.viewport.Viewport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 节点图画布 Widget。可经 {@code addRenderableWidget} 嵌入任意 Screen。
 *
 * <p>持有 {@link NodeGraph}（数据，只读）与 {@link Viewport}（视口，可变）。本类负责：
 * 世界↔屏幕坐标变换的宿主、网格背景渲染、平移（中键/左键拖空白/滚轮）、缩放（Ctrl+滚轮）。
 *
 * <p><b>事件派发契约</b>：默认 Screen 派发链不路由 {@code button != 0} 的 mouseDragged。
 * 对左键拖、滚轮、全按钮 mouseClicked/mouseReleased，本 widget 自洽；对中键拖动，依赖宿主 Screen
 * 转发 mouseDragged(button=2) 或 mouseMoved（见 {@link NodeGraphScreen}）。
 */
public class NodeGraphWidget extends AbstractWidget {
    public static final double GRID_SIZE = 20.0;
    public static final double SCROLL_SPEED = 16.0;
    public static final double ZOOM_FACTOR = 1.15;

    public static final int COLOR_BG = 0xFF1A1A1A;
    public static final int COLOR_GRID = 0xFF2B2B2B;
    public static final int COLOR_AXIS = 0xFF444444;

    private final NodeGraph graph;
    private final Viewport viewport;
    private final Font font;

    private enum State { IDLE, PANNING }

    private State state = State.IDLE;
    private int panButton = -1;
    private double lastMouseX;
    private double lastMouseY;

    /** 拖拽连线中的预览状态；null 时不渲染。由 TaskH 交互层设置/清空。 */
    private ConnectionDrag pending;

    /** 连线拖拽预览状态（TaskG 交付渲染能力，数据源归 TaskH）。 */
    public static final class ConnectionDrag {
        private final NodeLayout fromLayout;
        private final int outIdx;
        private final int color;

        public ConnectionDrag(NodeLayout fromLayout, int outIdx, int color) {
            this.fromLayout = fromLayout;
            this.outIdx = outIdx;
            this.color = color;
        }

        public NodeLayout fromLayout() {
            return fromLayout;
        }

        public int outIdx() {
            return outIdx;
        }

        public int color() {
            return color;
        }
    }

    public NodeGraphWidget(int x, int y, int width, int height, NodeGraph graph) {
        super(x, y, width, height, Component.empty());
        this.graph = Objects.requireNonNull(graph, "graph");
        this.viewport = new Viewport();
        this.font = Minecraft.getInstance().font;
    }

    public NodeGraph graph() {
        return graph;
    }

    public Viewport viewport() {
        return viewport;
    }

    public ConnectionDrag pending() {
        return pending;
    }

    public void setPending(ConnectionDrag pending) {
        this.pending = pending;
    }

    @Override
    public boolean isValidClickButton(int button) {
        return button == 0 || button == 1 || button == 2;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isValidClickButton(button) || !clicked(mx, my)) {
            return false;
        }
        state = State.PANNING;
        panButton = button;
        lastMouseX = mx;
        lastMouseY = my;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (state == State.PANNING && button == panButton) {
            state = State.IDLE;
            panButton = -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
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
            double factor = Math.pow(ZOOM_FACTOR, delta);
            viewport.zoom(factor, mx, my, getX(), getY());
        } else if (Screen.hasShiftDown()) {
            viewport.pan(delta * SCROLL_SPEED * s, 0);
        } else {
            viewport.pan(0, delta * SCROLL_SPEED * s);
        }
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;
        g.fill(x0, y0, x1, y1, COLOR_BG);
        g.enableScissor(x0, y0, x1, y1);
        renderGrid(g);
        renderConnections(g);
        Optional<List<Component>> tooltip = renderNodes(g, mouseX, mouseY);
        renderPending(g, mouseX, mouseY);
        g.disableScissor();
        if (tooltip.isPresent()) {
            g.renderComponentTooltip(font, tooltip.get(), mouseX, mouseY);
        }
    }

    /**
     * 渲染图中所有连线（贝塞尔）。在节点之下绘制（节点端口方块盖住线端）。
     * 自动转换连线用警告色 + 中点警告方块。
     */
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

    /**
     * 渲染拖拽预览线（pending != null 时）。从源输出端口到当前鼠标位置。
     */
    protected void renderPending(GuiGraphics g, int mouseX, int mouseY) {
        if (pending == null) {
            return;
        }
        NodeLayout.PortAnchor a = pending.fromLayout().outputPort(pending.outIdx());
        int x0 = getX();
        int y0 = getY();
        double toWx = viewport.screenToWorldX(mouseX, x0);
        double toWy = viewport.screenToWorldY(mouseY, y0);
        int color = ConnectionRenderer.withAlpha(pending.color(), ConnectionRenderer.PREVIEW_ALPHA);
        ConnectionRenderer.renderPreview(g, viewport, x0, y0, a.x(), a.y(), toWx, toWy, color);
    }

    /**
     * 渲染图中所有（可见）节点。返回首个命中鼠标的端口/组件 tooltip 行，供调用方在 scissor 外绘制。
     */
    protected Optional<List<Component>> renderNodes(GuiGraphics g, int mouseX, int mouseY) {
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
            NodeRenderer.render(g, font, layout, viewport, x0, y0, hovered);
            if (firstHit.isEmpty()) {
                firstHit = NodeRenderer.pickHover(layout, viewport, x0, y0, mouseX, mouseY);
            }
        }
        return firstHit;
    }

    /**
     * 在世界坐标系中绘制网格（间距 {@link #GRID_SIZE}）。仅画视口可见范围。原点轴（i==0/j==0）用稍亮色。
     */
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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
