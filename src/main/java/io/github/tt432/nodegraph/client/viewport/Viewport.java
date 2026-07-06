package io.github.tt432.nodegraph.client.viewport;

/**
 * 视口：世界 ↔ 屏幕坐标变换的纯 Java 数学。
 *
 * <p>世界坐标 = NodeGraph 中节点所在的逻辑坐标系；屏幕坐标 = widget 内 GUI 像素。
 * 变换公式（{@code scale > 0}）：
 * <pre>
 *   screenX = (worldX - panX) * scale + originX
 *   worldX  = (screenX - originX) / scale + panX
 * </pre>
 * {@code panX/panY} 是世界系中视口左上角对应的世界点；{@code originX/originY} 是 widget 在
 * Screen 中的左上屏幕坐标。
 *
 * <p>本类无 MC 依赖，便于 JUnit 验证坐标数学。{@code scale ∈ [MIN_SCALE, MAX_SCALE]} 恒成立。
 */
public final class Viewport {
    public static final double MIN_SCALE = 0.1;
    public static final double MAX_SCALE = 4.0;

    private double panX;
    private double panY;
    private double scale;

    public Viewport() {
        this.panX = 0.0;
        this.panY = 0.0;
        this.scale = 1.0;
    }

    public double panX() { return panX; }
    public double panY() { return panY; }
    public double scale() { return scale; }

    public double screenToWorldX(double screenX, double originX) {
        return (screenX - originX) / scale + panX;
    }

    public double screenToWorldY(double screenY, double originY) {
        return (screenY - originY) / scale + panY;
    }

    public double worldToScreenX(double worldX, double originX) {
        return (worldX - panX) * scale + originX;
    }

    public double worldToScreenY(double worldY, double originY) {
        return (worldY - panY) * scale + originY;
    }

    /**
     * 按屏幕拖动向量平移视口。屏幕右拖 → 内容跟随右移 → 看左边内容，故 {@code panX -= dx/scale}。
     */
    public void pan(double dxScreen, double dyScreen) {
        this.panX -= dxScreen / scale;
        this.panY -= dyScreen / scale;
    }

    /**
     * 围绕屏幕锚点缩放。缩放前后该屏幕锚点对应的世界坐标不变（数值精度内）。{@code factor <= 0} 抛异常；
     * 结果 scale 限幅到 [{@link #MIN_SCALE}, {@link #MAX_SCALE}]。
     */
    public void zoom(double factor, double anchorScreenX, double anchorScreenY,
                     double originX, double originY) {
        if (factor <= 0.0) {
            throw new IllegalArgumentException("factor must be > 0, got " + factor);
        }
        double anchorWorldX = screenToWorldX(anchorScreenX, originX);
        double anchorWorldY = screenToWorldY(anchorScreenY, originY);
        double newScale = scale * factor;
        newScale = clamp(newScale);
        this.scale = newScale;
        this.panX = anchorWorldX - (anchorScreenX - originX) / newScale;
        this.panY = anchorWorldY - (anchorScreenY - originY) / newScale;
    }

    /**
     * 直接设置 scale（限幅）并保持屏幕锚点对应的世界坐标不变。
     *
     * <p>注：本方法对任意输入（含 {@code <= 0}）一律限幅处理，不抛异常。规格异常条款对 setScale 的
     * "scale &lt;= 0 抛异常"与验证清单第 7 项 "setScale(0,...) → MIN_SCALE" 冲突，以验证清单为准。
     */
    public void setScale(double newScale, double anchorScreenX, double anchorScreenY,
                         double originX, double originY) {
        double clamped = clamp(newScale);
        double anchorWorldX = screenToWorldX(anchorScreenX, originX);
        double anchorWorldY = screenToWorldY(anchorScreenY, originY);
        this.scale = clamped;
        this.panX = anchorWorldX - (anchorScreenX - originX) / clamped;
        this.panY = anchorWorldY - (anchorScreenY - originY) / clamped;
    }

    private static double clamp(double s) {
        if (s < MIN_SCALE) return MIN_SCALE;
        if (s > MAX_SCALE) return MAX_SCALE;
        return s;
    }
}
