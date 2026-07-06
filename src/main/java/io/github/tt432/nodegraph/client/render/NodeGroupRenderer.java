package io.github.tt432.nodegraph.client.render;

import io.github.tt432.nodegraph.api.model.NodeGroup;
import io.github.tt432.nodegraph.client.viewport.Viewport;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Stateless renderer for {@link NodeGroup} frames. Draws a translucent fill, an
 * outline (color = selected ? {@code SELECTED_COLOR} : {@code COLOR_GROUP_OUTLINE};
 * line width scaled by {@link NodeGroup#scale()}), a header strip with the group
 * title and an optional scale-percentage label, and a bottom-right resize grip.
 *
 * <p>Rendering model matches {@link NodeRenderer}: {@code PoseStack} is translated
 * to the group's screen origin and scaled by {@link Viewport#scale()}, then all
 * geometry is emitted in world units so it naturally scales with the canvas zoom.
 */
public final class NodeGroupRenderer {
    public static final double GROUP_HEADER = 14.0;
    public static final double RESIZE_HANDLE = 8.0;
    public static final double BASE_LINE = 1.5;

    public static final int COLOR_GROUP_FILL = 0x284060A0;
    public static final int COLOR_GROUP_OUTLINE = 0xFF6080C0;
    public static final int COLOR_GROUP_HEADER = 0xFF3A5AA0;
    public static final int COLOR_GROUP_TEXT = 0xFFFFFFFF;
    public static final int COLOR_RESIZE_HANDLE = 0xFF6080C0;
    public static final int COLOR_SELECTED = 0xFFFFFF00;

    public static final double MIN_GROUP_SCALE = 0.5;
    public static final double MAX_GROUP_SCALE = 2.0;

    private static final double PADDING = 4.0;

    private NodeGroupRenderer() {}

    public static void render(GuiGraphics g, Font font, NodeGroup group,
                              Viewport vp, int originX, int originY, boolean selected) {
        double sx = vp.worldToScreenX(group.x(), originX);
        double sy = vp.worldToScreenY(group.y(), originY);
        double s = vp.scale();
        double w = group.width();
        double h = group.height();

        var pose = g.pose();
        pose.pushPose();
        pose.translate(sx, sy, 0);
        pose.scale((float) s, (float) s, 1.0f);

        int outline = selected ? COLOR_SELECTED : COLOR_GROUP_OUTLINE;
        int lw = clampLine(group.scale());

        // translucent fill
        g.fill(0, 0, (int) Math.round(w), (int) Math.round(h), COLOR_GROUP_FILL);
        // outline (4 edges) in world units so on-screen width ≈ lw * scale
        g.fill(0, 0, (int) Math.round(w), lw, outline);                                   // top
        g.fill(0, (int) Math.round(h) - lw, (int) Math.round(w), (int) Math.round(h), outline); // bottom
        g.fill(0, 0, lw, (int) Math.round(h), outline);                                   // left
        g.fill((int) Math.round(w) - lw, 0, (int) Math.round(w), (int) Math.round(h), outline); // right
        // header strip
        g.fill(0, 0, (int) Math.round(w), (int) Math.round(GROUP_HEADER), COLOR_GROUP_HEADER);
        // title
        Component header = group.header();
        g.drawString(font, header, (int) PADDING, (int) ((GROUP_HEADER - font.lineHeight) / 2.0), COLOR_GROUP_TEXT);
        // scale percentage label (only when scale != 1.0)
        int pct = (int) Math.round(group.scale() * 100);
        if (pct != 100) {
            Component label = Component.literal("(" + pct + "%)");
            int labelW = font.width(label);
            g.drawString(font, label,
                    (int) Math.round(w) - labelW - (int) PADDING,
                    (int) ((GROUP_HEADER - font.lineHeight) / 2.0),
                    COLOR_GROUP_TEXT);
        }
        // resize grip (bottom-right)
        int hs = (int) Math.round(RESIZE_HANDLE);
        g.fill((int) Math.round(w) - hs, (int) Math.round(h) - hs,
                (int) Math.round(w), (int) Math.round(h), COLOR_RESIZE_HANDLE);

        pose.popPose();
    }

    private static int clampLine(double scale) {
        int lw = (int) Math.round(BASE_LINE * scale);
        if (lw < 1) lw = 1;
        if (lw > 4) lw = 4;
        return lw;
    }
}
