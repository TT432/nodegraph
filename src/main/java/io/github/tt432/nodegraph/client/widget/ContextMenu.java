package io.github.tt432.nodegraph.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Lightweight right-click context menu overlay. MC 1.20.1 has no built-in menu
 * widget, so this draws a flat list of labelled rows; the owning screen/widget
 * decides when to open/close and routes clicks via {@link #click}.
 *
 * <p>All coordinates are screen-space. The menu is rendered on top of the
 * canvas (outside any active scissor). Rows are fixed-height; a disabled row is
 * greyed out and never runs its action.
 */
public final class ContextMenu {

    /** A single clickable row. {@code enabled==false} greys it out. */
    public record MenuItem(Component label, boolean enabled, Runnable action) {
        public MenuItem {
            java.util.Objects.requireNonNull(label, "label");
            java.util.Objects.requireNonNull(action, "action");
        }
    }

    private static final int PADDING = 4;
    private static final int ROW_HEIGHT = 12;
    private static final int MIN_WIDTH = 80;

    private static final int COLOR_BG = 0xFF2A2A2A;
    private static final int COLOR_OUTLINE = 0xFF6A6A6A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DISABLED = 0xFF707070;
    private static final int COLOR_HOVER = 0xFF3A5A8A;

    private final int x;
    private final int y;
    private final List<MenuItem> items;

    public ContextMenu(int x, int y, List<MenuItem> items) {
        this.x = x;
        this.y = y;
        this.items = List.copyOf(items);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public List<MenuItem> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int width(Font font) {
        int w = MIN_WIDTH;
        for (MenuItem it : items) {
            w = Math.max(w, font.width(it.label()) + PADDING * 2);
        }
        return w;
    }

    public int height() {
        return items.size() * ROW_HEIGHT + PADDING * 2;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        int w = width(font);
        int h = height();
        g.fill(x, y, x + w, y + h, COLOR_BG);
        g.renderOutline(x, y, w, h, COLOR_OUTLINE);
        int rowY = y + PADDING;
        int hoverIdx = rowAt(font, mouseX, mouseY);
        for (int i = 0; i < items.size(); i++) {
            MenuItem it = items.get(i);
            if (i == hoverIdx && it.enabled()) {
                g.fill(x + 1, rowY, x + w - 1, rowY + ROW_HEIGHT, COLOR_HOVER);
            }
            int color = it.enabled() ? COLOR_TEXT : COLOR_DISABLED;
            g.drawString(font, it.label(), x + PADDING, rowY + 1, color);
            rowY += ROW_HEIGHT;
        }
    }

    /** Index of the row under the point, or -1 if outside the rows area. */
    private int rowAt(Font font, int mx, int my) {
        int w = width(font);
        if (mx < x || mx > x + w) {
            return -1;
        }
        int top = y + PADDING;
        if (my < top) {
            return -1;
        }
        int idx = (my - top) / ROW_HEIGHT;
        if (idx < 0 || idx >= items.size()) {
            return -1;
        }
        return idx;
    }

    /**
     * Run the enabled row under the point.
     *
     * @return true if a row action ran.
     */
    public boolean click(Font font, int mx, int my) {
        int idx = rowAt(font, mx, my);
        if (idx < 0) {
            return false;
        }
        MenuItem it = items.get(idx);
        if (!it.enabled()) {
            return false;
        }
        it.action().run();
        return true;
    }

    /** True if the point is anywhere inside the menu rectangle. */
    public boolean contains(Font font, int mx, int my) {
        return mx >= x && mx <= x + width(font) && my >= y && my <= y + height();
    }
}
