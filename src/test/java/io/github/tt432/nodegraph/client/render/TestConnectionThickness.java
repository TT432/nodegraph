package io.github.tt432.nodegraph.client.render;

import io.github.tt432.nodegraph.client.viewport.Viewport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 线宽的世界→屏幕换算：随视口缩放等比变化（线宽相对图内容固定），
 * 返回 float 屏幕半宽（不取整，供旋转矩形渲染），下限 0.5px（厚度 ≥1px 可见性保底）。
 */
class TestConnectionThickness {

    private Viewport viewportAt(double scale) {
        Viewport vp = new Viewport();
        vp.setState(0, 0, scale);
        return vp;
    }

    @Test
    void screenHalfScalesWithViewport() {
        assertEquals(2.0, ConnectionRenderer.screenHalf(1.0, viewportAt(2.0)));
        assertEquals(1.0, ConnectionRenderer.screenHalf(1.0, viewportAt(1.0)));
        // scale=0.5：1.0 世界半宽 → 0.5px 屏幕半宽（厚度 1px，保下限）
        assertEquals(0.5, ConnectionRenderer.screenHalf(1.0, viewportAt(0.5)));
    }

    @Test
    void screenHalfKeepsProportionAtExtremeZoom() {
        // 最小缩放 0.1：2.0 世界线宽（半宽 1.0）→ 0.1px → 0.5px 下限（厚度 1px 可见性保底）
        assertEquals(0.5, ConnectionRenderer.screenHalf(ConnectionRenderer.HALF_THICKNESS, viewportAt(0.1)));
        // 最大缩放 4.0：半宽 1.0 → 4.0px
        assertEquals(4.0, ConnectionRenderer.screenHalf(ConnectionRenderer.HALF_THICKNESS, viewportAt(4.0)));
    }

    @Test
    void neverReturnsZero() {
        assertTrue(ConnectionRenderer.screenHalf(0.01, viewportAt(0.1)) >= 0.5);
    }
}
