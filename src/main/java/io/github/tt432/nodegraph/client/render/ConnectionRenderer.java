package io.github.tt432.nodegraph.client.render;

import io.github.tt432.nodegraph.client.layout.NodeLayout;
import io.github.tt432.nodegraph.client.viewport.Viewport;
import net.minecraft.client.gui.Font;
//? if !modern {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}

/**
 * 连线渲染（贝塞尔曲线）。屏幕坐标 + 段包围盒 fill 近似，复用 {@link GuiGraphics#fill}
 * （MC 1.20.1 无任意角度画线原语；LINES 模式需 POSITION_COLOR_NORMAL + shader，本环境无法 runClient 验证）。
 *
 * <p>三次贝塞尔：P0=源输出锚点，P3=目标输入锚点，控制点水平偏移使两端切线水平。
 * 采样得屏幕点序列，相邻点画<b>旋转矩形</b>（pose 平移到 float 端点 → 旋转至段方向 →
 * 轴对齐 fill），采样点补缝方块覆盖转角——全程屏幕 float 坐标，无包围盒近似。
 *
 * <p>不变量见 {@docRoot docs/task/nodegraph/TaskG/规格.md}。
 */
public final class ConnectionRenderer {
    public static final double STEP = 2.0;
    public static final int MIN_SEGMENTS = 12;
    public static final int MAX_SEGMENTS = 64;
    public static final double MIN_CURVE_DX = 24.0;
    /** 线宽（<b>世界单位</b>）：随画布缩放等比变化，与节点保持恒定视觉比例。 */
    public static final double THICKNESS = 2.0;
    public static final double HALF_THICKNESS = THICKNESS / 2.0;
    public static final double PREVIEW_THICKNESS = 1.0;
    public static final double PREVIEW_HALF = PREVIEW_THICKNESS / 2.0;
    public static final int WARN_COLOR = 0xFFFFAA00;
    public static final int WARN_MARK_SIZE = 4;
    public static final int PREVIEW_ALPHA = 0x80;
    public static final int LABEL_MAX_CHARS = 48;
    public static final int LABEL_BG = 0xA0101010;
    public static final int LABEL_COLOR = 0xFFE8E8E8;

    private ConnectionRenderer() {
    }

    /**
     * 渲染贝塞尔连线。返回中点屏幕坐标 {@code [midSx, midSy]}，供调用方叠加警告标记。
     *
     * @param halfThicknessWorld 半线宽（世界单位）；屏幕半宽 = halfThicknessWorld × viewport scale，
     *                           下限 1px 保证任何缩放下可见。
     */
    public static double[] render(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g, Viewport vp, int originX, int originY,
                                  NodeLayout from, int outIdx, NodeLayout to, int inIdx,
                                  int color, double halfThicknessWorld) {
        double halfScreen = screenHalf(halfThicknessWorld, vp);
        NodeLayout.PortAnchor fa = from.outputPort(outIdx);
        NodeLayout.PortAnchor ta = to.inputPort(inIdx);
        double sx0 = vp.worldToScreenX(fa.x(), originX);
        double sy0 = vp.worldToScreenY(fa.y(), originY);
        double sx3 = vp.worldToScreenX(ta.x(), originX);
        double sy3 = vp.worldToScreenY(ta.y(), originY);
        double c1x = sx0 + controlDx(sx0, sx3);
        double c1y = sy0;
        double c2x = sx3 - controlDx(sx0, sx3);
        double c2y = sy3;
        double dist = Math.hypot(sx3 - sx0, sy3 - sy0);
        int n = clamp(Math.round((float) (dist / STEP)), MIN_SEGMENTS, MAX_SEGMENTS);
        double px = sx0, py = sy0;
        fillJoint(g, px, py, halfScreen, color);
        for (int i = 1; i <= n; i++) {
            double t = (double) i / n;
            double u = 1 - t;
            double qx = u * u * u * sx0 + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * sx3;
            double qy = u * u * u * sy0 + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * sy3;
            fillSegment(g, px, py, qx, qy, halfScreen, color);
            fillJoint(g, qx, qy, halfScreen, color);
            px = qx;
            py = qy;
        }
        double midT = 0.5;
        double mu = 1 - midT;
        double midSx = mu * mu * mu * sx0 + 3 * mu * mu * midT * c1x + 3 * mu * midT * midT * c2x + midT * midT * midT * sx3;
        double midSy = mu * mu * mu * sy0 + 3 * mu * mu * midT * c1y + 3 * mu * midT * midT * c2y + midT * midT * midT * sy3;
        return new double[]{midSx, midSy};
    }

    /**
     * 渲染预览贝塞尔（拖拽中）。起点与终点为世界坐标。
     */
    public static void renderPreview(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g, Viewport vp, int originX, int originY,
                                     double fromWx, double fromWy, double toWx, double toWy, int color) {
        double sx0 = vp.worldToScreenX(fromWx, originX);
        double sy0 = vp.worldToScreenY(fromWy, originY);
        double sx3 = vp.worldToScreenX(toWx, originX);
        double sy3 = vp.worldToScreenY(toWy, originY);
        double c1x = sx0 + controlDx(sx0, sx3);
        double c1y = sy0;
        double c2x = sx3 - controlDx(sx0, sx3);
        double c2y = sy3;
        double dist = Math.hypot(sx3 - sx0, sy3 - sy0);
        int n = clamp(Math.round((float) (dist / STEP)), MIN_SEGMENTS, MAX_SEGMENTS);
        double px = sx0, py = sy0;
        double halfScreen = screenHalf(PREVIEW_HALF, vp);
        fillJoint(g, px, py, halfScreen, color);
        for (int i = 1; i <= n; i++) {
            double t = (double) i / n;
            double u = 1 - t;
            double qx = u * u * u * sx0 + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * sx3;
            double qy = u * u * u * sy0 + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * sy3;
            fillSegment(g, px, py, qx, qy, halfScreen, color);
            fillJoint(g, qx, qy, halfScreen, color);
            px = qx;
            py = qy;
        }
    }

    /** 世界半宽 → 屏幕半宽（float，随缩放等比变化；下限 0.5px 保证任何缩放下可见）。 */
    static double screenHalf(double halfThicknessWorld, Viewport vp) {
        return Math.max(0.5, halfThicknessWorld * vp.scale());
    }

    /** 自动转换警告方块标记。 */
    public static void renderWarnMark(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g, double sx, double sy) {
        int half = WARN_MARK_SIZE / 2;
        int ix = (int) Math.round(sx);
        int iy = (int) Math.round(sy);
        g.fill(ix - half, iy - half, ix + half, iy + half, WARN_COLOR);
    }

    /**
     * 连线中点标签（如引用条件 Molang 原文），暗色底衬、居中、略高于连线。
     * 超长文本截断到 {@link #LABEL_MAX_CHARS}。
     */
    public static void renderLabel(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g, Font font,
                                   String label, double midSx, double midSy) {
        String text = label.length() <= LABEL_MAX_CHARS
                ? label
                : label.substring(0, LABEL_MAX_CHARS - 1) + "\u2026";
        int w = font.width(text);
        int x = (int) Math.round(midSx) - w / 2;
        int y = (int) Math.round(midSy) - font.lineHeight - 3;
        g.fill(x - 2, y - 1, x + w + 2, y + font.lineHeight + 1, LABEL_BG);
        g./*? if !modern {*/ drawString /*?} else {*/ text /*?}*/(font, text, x, y, LABEL_COLOR);
    }

    /** 将颜色的 alpha 通道替换为给定值（保留 RGB）。 */
    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static double controlDx(double sx0, double sx3) {
        return Math.max(Math.abs(sx3 - sx0) * 0.5, MIN_CURVE_DX);
    }

    /**
     * 斜线段：pose 平移到 float 起点 → 旋转至段方向 → 轴对齐 fill 矩形。
     * 矩形垂直中心对齐线段（translate(0,-halfScreen)），厚度为最接近 2×halfScreen 的整数（≥1px）。
     */
    private static void fillSegment(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g, double x0, double y0, double x1, double y1,
                                    double halfScreen, int color) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double len = Math.hypot(dx, dy);
        if (len < 1.0e-4) {
            fillJoint(g, x0, y0, halfScreen, color);
            return;
        }
        int thickness = Math.max(1, (int) Math.round(halfScreen * 2));
        int li = Math.max(1, (int) Math.round(len));
        float angle = (float) Math.atan2(dy, dx);
        //? if !modern {
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0.0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(angle));
        g.pose().translate(0.0, -halfScreen, 0.0);
        g.fill(0, 0, li, thickness, color);
        g.pose().popPose();
        //?} else {
        g.pose().pushMatrix();
        g.pose().translate((float) x0, (float) y0);
        g.pose().rotate(angle);
        g.pose().translate(0.0f, (float) -halfScreen);
        g.fill(0, 0, li, thickness, color);
        g.pose().popMatrix();
        //?}
    }

    /** 段间补缝/端帽方块：覆盖相邻旋转矩形在转角外侧的缺口。 */
    private static void fillJoint(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g, double x, double y,
                                  double halfScreen, int color) {
        int thickness = Math.max(1, (int) Math.round(halfScreen * 2));
        int h = thickness / 2;
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        g.fill(ix - h, iy - h, ix - h + thickness, iy - h + thickness, color);
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}
