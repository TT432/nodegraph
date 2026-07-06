package com.nodegraph.nodegraph.client.render;

import com.nodegraph.nodegraph.client.layout.NodeLayout;
import com.nodegraph.nodegraph.client.viewport.Viewport;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 连线渲染（贝塞尔曲线）。屏幕坐标 + 段包围盒 fill 近似，复用 {@link GuiGraphics#fill}
 * （MC 1.20.1 无任意角度画线原语；LINES 模式需 POSITION_COLOR_NORMAL + shader，本环境无法 runClient 验证）。
 *
 * <p>三次贝塞尔：P0=源输出锚点，P3=目标输入锚点，控制点水平偏移使两端切线水平。
 * 采样得屏幕点序列，相邻点画轴对齐包围盒 fill（重叠区域自然填充 → 无缝连续）。
 *
 * <p>不变量见 {@docRoot docs/task/nodegraph/TaskG/规格.md}。
 */
public final class ConnectionRenderer {
    public static final double STEP = 2.0;
    public static final int MIN_SEGMENTS = 12;
    public static final int MAX_SEGMENTS = 64;
    public static final double MIN_CURVE_DX = 24.0;
    public static final int THICKNESS = 2;
    public static final int HALF_THICKNESS = THICKNESS / 2;
    public static final int PREVIEW_THICKNESS = 1;
    public static final int PREVIEW_HALF = PREVIEW_THICKNESS / 2;
    public static final int WARN_COLOR = 0xFFFFAA00;
    public static final int WARN_MARK_SIZE = 4;
    public static final int PREVIEW_ALPHA = 0x80;

    private ConnectionRenderer() {
    }

    /**
     * 渲染贝塞尔连线。返回中点屏幕坐标 {@code [midSx, midSy]}，供调用方叠加警告标记。
     */
    public static double[] render(GuiGraphics g, Viewport vp, int originX, int originY,
                                  NodeLayout from, int outIdx, NodeLayout to, int inIdx,
                                  int color, int halfThickness) {
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
        for (int i = 1; i <= n; i++) {
            double t = (double) i / n;
            double u = 1 - t;
            double qx = u * u * u * sx0 + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * sx3;
            double qy = u * u * u * sy0 + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * sy3;
            fillSegment(g, px, py, qx, qy, halfThickness, color);
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
    public static void renderPreview(GuiGraphics g, Viewport vp, int originX, int originY,
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
        for (int i = 1; i <= n; i++) {
            double t = (double) i / n;
            double u = 1 - t;
            double qx = u * u * u * sx0 + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * sx3;
            double qy = u * u * u * sy0 + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * sy3;
            fillSegment(g, px, py, qx, qy, PREVIEW_HALF, color);
            px = qx;
            py = qy;
        }
    }

    /** 自动转换警告方块标记。 */
    public static void renderWarnMark(GuiGraphics g, double sx, double sy) {
        int half = WARN_MARK_SIZE / 2;
        int ix = (int) Math.round(sx);
        int iy = (int) Math.round(sy);
        g.fill(ix - half, iy - half, ix + half, iy + half, WARN_COLOR);
    }

    /** 将颜色的 alpha 通道替换为给定值（保留 RGB）。 */
    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static double controlDx(double sx0, double sx3) {
        return Math.max(Math.abs(sx3 - sx0) * 0.5, MIN_CURVE_DX);
    }

    private static void fillSegment(GuiGraphics g, double x0, double y0, double x1, double y1,
                                    int halfThickness, int color) {
        int minX = (int) Math.floor(Math.min(x0, x1) - halfThickness);
        int minY = (int) Math.floor(Math.min(y0, y1) - halfThickness);
        int maxX = (int) Math.ceil(Math.max(x0, x1) + halfThickness);
        int maxY = (int) Math.ceil(Math.max(y0, y1) + halfThickness);
        g.fill(minX, minY, maxX, maxY, color);
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}
