package io.github.tt432.nodegraph.client.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 双击判定：同目标阈值内为双击；异目标、超时、null 均不构成；reset 清除记录；
 * 每次点击滚动成为新的基准（三连击第二、三击可连续构成双击——调用方负责在首次
 * 双击后切换目标语境）。
 */
class TestDoubleClickTracker {

    @Test
    void sameTargetWithinThresholdIsDouble() {
        DoubleClickTracker tracker = new DoubleClickTracker();
        assertFalse(tracker.click(1000, "n1", 400));
        assertTrue(tracker.click(1300, "n1", 400));
    }

    @Test
    void differentTargetIsNotDouble() {
        DoubleClickTracker tracker = new DoubleClickTracker();
        assertFalse(tracker.click(1000, "n1", 400));
        assertFalse(tracker.click(1100, "n2", 400));
    }

    @Test
    void beyondThresholdIsNotDouble() {
        DoubleClickTracker tracker = new DoubleClickTracker();
        assertFalse(tracker.click(1000, "n1", 400));
        assertFalse(tracker.click(1401, "n1", 400));
    }

    @Test
    void nullTargetNeverDouble() {
        DoubleClickTracker tracker = new DoubleClickTracker();
        assertFalse(tracker.click(1000, null, 400));
        assertFalse(tracker.click(1050, null, 400));
        // null 点击后同目标记录被覆盖，后续首击重新计为单击
        assertFalse(tracker.click(1060, "n1", 400));
        assertTrue(tracker.click(1100, "n1", 400));
    }

    @Test
    void resetClearsRecord() {
        DoubleClickTracker tracker = new DoubleClickTracker();
        assertFalse(tracker.click(1000, "n1", 400));
        tracker.reset();
        assertFalse(tracker.click(1100, "n1", 400));
    }

    @Test
    void thirdClickFormsNewDouble() {
        DoubleClickTracker tracker = new DoubleClickTracker();
        assertFalse(tracker.click(1000, "n1", 400));
        assertTrue(tracker.click(1200, "n1", 400));
        // 第三击与第二击同目标且在阈值内 → 仍为双击（目标语境由调用方切换）
        assertTrue(tracker.click(1500, "n1", 400));
    }
}
