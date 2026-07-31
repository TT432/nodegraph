package io.github.tt432.nodegraph.client.interaction;

/**
 * 双击判定器（纯逻辑，可单测）：记录上次点击的时间与目标，当前点击与上次同目标且
 * 间隔不超过阈值时判定为双击。每次点击都会成为新的"上次"（三连击的第三击仍与第二击
 * 构成双击——调用方在首次双击触发导航后目标即变化，实际不会误判）。
 */
public final class DoubleClickTracker {
    /** 默认双击阈值（ms），与 vanilla 列表双击手感一致。 */
    public static final long DEFAULT_THRESHOLD_MS = 400;

    private long lastTime = Long.MIN_VALUE;
    private Object lastTarget;

    /**
     * 记录一次点击并判定是否为双击。
     *
     * @param timeMillis     当前时间（生产 = {@code System.currentTimeMillis()}）
     * @param target         点击目标（equals 比较；null 永不构成双击）
     * @param thresholdMillis 双击阈值
     * @return true = 本次点击与上次同目标且在阈值内
     */
    public boolean click(long timeMillis, Object target, long thresholdMillis) {
        boolean isDouble = target != null
                && target.equals(lastTarget)
                && timeMillis - lastTime <= thresholdMillis;
        lastTime = timeMillis;
        lastTarget = target;
        return isDouble;
    }

    /** 清空记录（导航/屏幕切换后调用，避免跨场景误判）。 */
    public void reset() {
        lastTime = Long.MIN_VALUE;
        lastTarget = null;
    }
}
