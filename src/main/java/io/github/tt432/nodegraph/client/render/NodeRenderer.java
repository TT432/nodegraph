package io.github.tt432.nodegraph.client.render;

import io.github.tt432.nodegraph.api.model.InputWidget;
import io.github.tt432.nodegraph.api.model.InputWidgetKind;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.Port;
import io.github.tt432.nodegraph.api.model.TypedValue;
import io.github.tt432.nodegraph.client.layout.NodeLayout;
import io.github.tt432.nodegraph.client.viewport.Viewport;
import net.minecraft.client.gui.Font;
//? if !modern {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 单节点渲染器（无状态工具类）。用 {@link NodeLayout} 几何 + PoseStack 缩放在世界单位下绘制，
 * 文字随之缩放。仅渲染，不做交互。
 *
 * <p>Tooltip 不在此处绘制（需在画布 scissor 之外）；本类提供 {@link #pickHover} 返回命中目标的
 * tooltip 行，由 widget 在 disableScissor 后用 {@code renderComponentTooltip} 绘制。
 */
public final class NodeRenderer {
    private static final int FONT_HEIGHT = 9;
    private static final int ERROR_COLOR = 0xFFFF5555;

    private NodeRenderer() {}

    /**
     * 渲染单个节点。{@code hovered} 控制描边高亮（鼠标悬停其 bounds）。
     *
     * @param outputs            该节点的求值输出（key=port key → value）；null=未求值/无值，不显示
     * @param hasError           该节点求值异常（环/求值失败）→ output 标签显示 {@code name = !}
     * @param editingWidgetIndex 正在用 EditBox 编辑的 widget 行号；-1=无。该行跳过文本，由 EditBox 接管
     */
    public static void render(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g, Font font, NodeLayout layout, Viewport vp,
                              int originX, int originY, boolean hovered,
                              Map<String, Object> outputs, boolean hasError, int editingWidgetIndex) {
        Objects.requireNonNull(g, "g");
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(vp, "vp");

        Node node = layout.node();
        double s = vp.scale();
        double sx = vp.worldToScreenX(node.x(), originX);
        double sy = vp.worldToScreenY(node.y(), originY);

        //? if !modern {
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0.0);
        g.pose().scale((float) s, (float) s, 1.0f);
        //?} else {
        g.pose().pushMatrix();
        g.pose().translate((float) sx, (float) sy);
        g.pose().scale((float) s, (float) s);
        //?}

        double h = layout.height();
        int outline = hovered ? NodeLayout.OUTLINE_HOVER : NodeLayout.OUTLINE_COLOR;
        if (node.statusColor() != 0) {
            outline = node.statusColor();
        }

        // 体背景 + 头部
        g.fill(0, 0, (int) Math.round(NodeLayout.NODE_WIDTH), (int) Math.round(h), NodeLayout.BODY_COLOR);
        g.fill(0, 0, (int) Math.round(NodeLayout.NODE_WIDTH), (int) Math.round(NodeLayout.HEADER_HEIGHT), NodeLayout.HEADER_COLOR);
        //? if !modern {
        g.pose().pushPose();
        g.pose().translate(0.0, 0.0, 1.0);
        //?}
        g./*? if !modern {*/ drawString /*?} else {*/ text /*?}*/(font, node.header(),
                (int) Math.round(NodeLayout.PADDING),
                (int) Math.round((NodeLayout.HEADER_HEIGHT - FONT_HEIGHT) / 2.0),
                NodeLayout.TEXT_COLOR);
        // 子图标识：header 右侧嵌套双方框（双击可进入子图）
        if (node.hasSubgraph()) {
            int ix = (int) Math.round(NodeLayout.NODE_WIDTH - NodeLayout.PADDING - 8);
            int iy = (int) Math.round((NodeLayout.HEADER_HEIGHT - 8) / 2.0);
            g.fill(ix, iy, ix + 8, iy + 8, NodeLayout.TEXT_COLOR);
            g.fill(ix + 2, iy + 2, ix + 6, iy + 6, NodeLayout.HEADER_COLOR);
            g.fill(ix + 3, iy + 3, ix + 5, iy + 5, NodeLayout.TEXT_COLOR);
        }
        //? if !modern {
        g.pose().popPose();
        //?}

        // InputWidget 行（名字左 + 当前值文本右对齐；CUSTOM 行跳过，由宿主渲染器绘制）
        {
            double rowTop = NodeLayout.HEADER_HEIGHT;
            for (int i = 0; i < layout.widgetRowCount(); i++) {
                InputWidget w = node.widgets().get(i);
                double rowH = w.height();
                if (i != editingWidgetIndex && w.kind() != InputWidgetKind.CUSTOM) {
                    int localRowY = (int) Math.round(rowTop + (rowH - FONT_HEIGHT) / 2.0);
                    g./*? if !modern {*/ drawString /*?} else {*/ text /*?}*/(font, w.name(),
                            (int) Math.round(NodeLayout.PADDING),
                            localRowY,
                            NodeLayout.TEXT_COLOR);
                    String valStr = String.valueOf(w.currentValue());
                    int valWidth = font.width(valStr);
                    g./*? if !modern {*/ drawString /*?} else {*/ text /*?}*/(font, valStr,
                            (int) Math.round(NodeLayout.NODE_WIDTH - NodeLayout.PADDING) - valWidth,
                            localRowY,
                            w.kind() == InputWidgetKind.DISPLAY ? NodeLayout.DISPLAY_VALUE_COLOR : NodeLayout.WIDGET_VALUE_COLOR);
                }
                rowTop += rowH;
            }
        }

        // 端口区
        int portTopLocal = (int) Math.round(NodeLayout.HEADER_HEIGHT + layout.widgetsHeight());
        for (int i = 0; i < node.inputs().size(); i++) {
            NodeLayout.PortAnchor a = layout.inputPort(i);
            int ax = (int) Math.round(a.x() - node.x());
            int ay = portTopLocal + i * (int) Math.round(NodeLayout.ROW_HEIGHT)
                    + (int) Math.round(NodeLayout.ROW_HEIGHT / 2.0);
            drawPortDot(g, ax, ay, node.inputs().get(i).type().color());
            Component label = node.inputs().get(i).name();
            g./*? if !modern {*/ drawString /*?} else {*/ text /*?}*/(font, label,
                    ax + (int) Math.round(NodeLayout.PORT_RADIUS) + (int) Math.round(NodeLayout.PADDING / 2.0),
                    ay - (int) Math.round((double) FONT_HEIGHT / 2.0),
                    NodeLayout.TEXT_COLOR);
        }
        for (int i = 0; i < node.outputs().size(); i++) {
            NodeLayout.PortAnchor a = layout.outputPort(i);
            int ax = (int) Math.round(a.x() - node.x());
            int ay = portTopLocal + i * (int) Math.round(NodeLayout.ROW_HEIGHT)
                    + (int) Math.round(NodeLayout.ROW_HEIGHT / 2.0);
            drawPortDot(g, ax, ay, node.outputs().get(i).type().color());
            Port port = node.outputs().get(i);
            String labelText = port.name().getString();
            int labelColor = NodeLayout.TEXT_COLOR;
            if (hasError) {
                labelText = labelText + " = !";
                labelColor = ERROR_COLOR;
            } else if (outputs != null) {
                Object v = outputs.get(port.key());
                if (v != null) {
                    labelText = labelText + " = " + String.valueOf(v);
                }
            }
            int lw = font.width(labelText);
            g./*? if !modern {*/ drawString /*?} else {*/ text /*?}*/(font, Component.literal(labelText),
                    ax - (int) Math.round(NodeLayout.PORT_RADIUS) - (int) Math.round(NodeLayout.PADDING / 2.0) - lw,
                    ay - (int) Math.round((double) FONT_HEIGHT / 2.0),
                    labelColor);
        }

        // 描边（在节点局部坐标，覆盖全节点）
        //? if !modern {
        g.pose().pushPose();
        g.pose().translate(0.0, 0.0, 2.0);
        g.renderOutline(0, 0,
                (int) Math.round(NodeLayout.NODE_WIDTH),
                (int) Math.round(h),
                outline);
        g.pose().popPose();

        g.pose().popPose();
        //?} else {
        g.outline(0, 0,
                (int) Math.round(NodeLayout.NODE_WIDTH),
                (int) Math.round(h),
                outline);

        g.pose().popMatrix();
        //?}
    }

    private static void drawPortDot(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g, int cx, int cy, int color) {
        int r = (int) Math.round(NodeLayout.PORT_RADIUS);
        g.fill(cx - r, cy - r, cx + r, cy + r, color);
    }

    /**
     * 选中描边（屏幕 float 坐标）：4 条边各自 pose 平移到 float 起点后轴对齐 fill，
     * 与节点体边缘精确贴合（不做屏幕坐标 int 取整）；宽度 1px 恒定。
     */
    public static void renderSelectionOutline(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g,
                                              NodeLayout layout, Viewport vp, int originX, int originY, int color) {
        Objects.requireNonNull(g, "g");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(vp, "vp");
        Node node = layout.node();
        double s = vp.scale();
        double sx = vp.worldToScreenX(node.x(), originX);
        double sy = vp.worldToScreenY(node.y(), originY);
        double sw = NodeLayout.NODE_WIDTH * s;
        double sh = layout.height() * s;
        int iw = Math.max(1, (int) Math.round(sw));
        int ih = Math.max(1, (int) Math.round(sh));
        edge(g, sx - 1, sy - 1, iw + 2, 1, color);
        edge(g, sx - 1, sy + sh, iw + 2, 1, color);
        edge(g, sx - 1, sy - 1, 1, ih + 2, color);
        edge(g, sx + sw, sy - 1, 1, ih + 2, color);
    }

    /** 单条描边边：float 起点 translate + int 尺寸 fill。 */
    private static void edge(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g,
                             double x, double y, int w, int h, int color) {
        //? if !modern {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0);
        g.fill(0, 0, w, h, color);
        g.pose().popPose();
        //?} else {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.fill(0, 0, w, h, color);
        g.pose().popMatrix();
        //?}
    }

    /**
     * 在屏幕坐标 {@code (mouseX,mouseY)} 处检测命中的端口/输入组件（端口优先）。
     * 返回该目标的 tooltip 行（name / Type: id / description）；无命中→empty。
     */
    public static Optional<List<Component>> pickHover(NodeLayout layout, Viewport vp,
                                                     int originX, int originY,
                                                     int mouseX, int mouseY) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(vp, "vp");
        double wx = vp.screenToWorldX(mouseX, originX);
        double wy = vp.screenToWorldY(mouseY, originY);

        Optional<Integer> in = layout.pickInputPort(wx, wy);
        if (in.isPresent()) {
            Port p = layout.inputPortData(in.get());
            return Optional.of(tooltipLines(p.value()));
        }
        Optional<Integer> out = layout.pickOutputPort(wx, wy);
        if (out.isPresent()) {
            Port p = layout.outputPortData(out.get());
            return Optional.of(tooltipLines(p.value()));
        }
        Optional<Integer> w = layout.pickInputWidget(wx, wy);
        if (w.isPresent()) {
            TypedValue tv = layout.node().widgets().get(w.get()).value();
            return Optional.of(tooltipLines(tv));
        }
        return Optional.empty();
    }

    private static List<Component> tooltipLines(TypedValue tv) {
        return List.of(
                tv.name(),
                Component.literal("Type: " + tv.type().id()),
                tv.description()
        );
    }
}
