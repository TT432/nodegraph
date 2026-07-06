package com.nodegraph.nodegraph.client.viewport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestViewport {
    private static final double EPS = 1e-9;

    @Test
    void identityTransform() {
        Viewport v = new Viewport();
        assertEquals(50, v.worldToScreenX(50, 0), EPS);
        assertEquals(30, v.worldToScreenY(30, 0), EPS);
        assertEquals(50, v.screenToWorldX(50, 0), EPS);
        assertEquals(30, v.screenToWorldY(30, 0), EPS);
    }

    @Test
    void defaultViewportIsIdentity() {
        Viewport v = new Viewport();
        assertEquals(1.0, v.scale(), EPS);
        assertEquals(0.0, v.panX(), EPS);
        assertEquals(0.0, v.panY(), EPS);
    }

    @Test
    void panMovesWorldOrigin() {
        Viewport v = new Viewport();
        v.pan(10, 0);
        assertEquals(-10.0, v.screenToWorldX(0, 0), EPS);
    }

    @Test
    void scaleAffectsScreenSize() {
        Viewport v = new Viewport();
        v.zoom(2.0, 0, 0, 0, 0);
        assertEquals(20.0, v.worldToScreenX(10, 0), EPS);
    }

    @Test
    void zoomKeepsAnchorWorldPoint() {
        Viewport v = new Viewport();
        v.pan(5, -3);
        double ax = 42.0, ay = 17.0, ox = 10.0, oy = 20.0;
        double beforeX = v.screenToWorldX(ax, ox);
        double beforeY = v.screenToWorldY(ay, oy);
        v.zoom(1.5, ax, ay, ox, oy);
        assertEquals(beforeX, v.screenToWorldX(ax, ox), EPS);
        assertEquals(beforeY, v.screenToWorldY(ay, oy), EPS);
    }

    @Test
    void scaleClampedToMax() {
        Viewport v = new Viewport();
        v.zoom(3.0, 0, 0, 0, 0);
        v.zoom(10.0, 0, 0, 0, 0);
        assertEquals(Viewport.MAX_SCALE, v.scale(), EPS);
    }

    @Test
    void scaleClampedToMin() {
        Viewport v = new Viewport();
        v.zoom(0.2, 0, 0, 0, 0);
        v.zoom(0.01, 0, 0, 0, 0);
        assertEquals(Viewport.MIN_SCALE, v.scale(), EPS);
    }

    @Test
    void setScaleClamps() {
        Viewport v = new Viewport();
        v.setScale(100.0, 0, 0, 0, 0);
        assertEquals(Viewport.MAX_SCALE, v.scale(), EPS);
        v.setScale(0.0, 0, 0, 0, 0);
        assertEquals(Viewport.MIN_SCALE, v.scale(), EPS);
    }

    @Test
    void zoomWithNonPositiveFactorThrows() {
        Viewport v = new Viewport();
        assertThrows(IllegalArgumentException.class, () -> v.zoom(0.0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> v.zoom(-1.0, 0, 0, 0, 0));
    }

    @Test
    void roundTripAfterPanAndZoom() {
        Viewport v = new Viewport();
        v.pan(7, -4);
        v.zoom(2.5, 100, 50, 30, 20);
        double ox = 30.0, oy = 20.0;
        for (double w : new double[]{0, 1, -5.5, 100, -100}) {
            assertEquals(w, v.screenToWorldX(v.worldToScreenX(w, ox), ox), EPS);
            assertEquals(w, v.screenToWorldY(v.worldToScreenY(w, oy), oy), EPS);
        }
    }
}
