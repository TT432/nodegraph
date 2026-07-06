package io.github.tt432.nodegraph.client.widget;

import io.github.tt432.nodegraph.api.def.NodeDefinition;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 浮动的"添加节点"面板：一个搜索框 + 按名字过滤的候选定义列表。
 *
 * <p>由 {@link NodeGraphWidget} 在右键菜单"Add Node…"或"拖线到空白"时打开。
 * 候选列表在构造时固定（已按 catalog + 可选类型过滤）；面板内部再按搜索文本子串过滤。
 * 选中（回车/点击）触发 {@code onPick} 并自动关闭；ESC 或点外部也关闭。
 *
 * <p>所有坐标为屏幕坐标。键盘事件经持有者转发到 {@link #keyPressed}/{@link #charTyped}。
 */
public final class AddNodeOverlay {

    private static final int PADDING = 4;
    private static final int ROW_HEIGHT = 12;
    private static final int MAX_ROWS = 8;
    private static final int WIDTH = 140;

    private static final int COLOR_BG = 0xFF2A2A2A;
    private static final int COLOR_OUTLINE = 0xFF6A6A6A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_HOVER = 0xFF3A5A8A;
    private static final int COLOR_SELECTED = 0xFF2A4A6A;

    // raw GLFW key codes (match MC 1.20.1 Screen internals)
    private static final int KEY_ESC = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_KP_ENTER = 335;
    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;

    private final Font font;
    private final int x;
    private final int y;
    private final List<NodeDefinition> candidates;
    private final Consumer<NodeDefinition> onPick;
    private final EditBox searchBox;
    private List<NodeDefinition> filtered;
    private int selected = -1;
    private boolean open = true;

    public AddNodeOverlay(Font font, int x, int y, List<NodeDefinition> candidates, Consumer<NodeDefinition> onPick) {
        this.font = Objects.requireNonNull(font, "font");
        this.x = x;
        this.y = y;
        this.candidates = List.copyOf(candidates);
        this.onPick = Objects.requireNonNull(onPick, "onPick");
        this.searchBox = new EditBox(font, x + PADDING, y + PADDING, WIDTH - PADDING * 2, ROW_HEIGHT, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Type to search..."));
        this.searchBox.setCanLoseFocus(false);
        this.searchBox.setFocused(true);
        refresh();
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    public boolean keyPressed(int keyCode) {
        if (searchBox.keyPressed(keyCode, 0, 0)) {
            refresh();
            return true;
        }
        if (keyCode == KEY_ENTER || keyCode == KEY_KP_ENTER) {
            confirm();
            return true;
        }
        if (keyCode == KEY_ESC) {
            close();
            return true;
        }
        if (!filtered.isEmpty()) {
            if (keyCode == KEY_UP) {
                selected = (selected <= 0 ? filtered.size() : selected) - 1;
                return true;
            }
            if (keyCode == KEY_DOWN) {
                selected = (selected < 0 ? 0 : selected + 1) % filtered.size();
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char codePoint) {
        if (searchBox.charTyped(codePoint, 0)) {
            refresh();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mx, double my) {
        int ix = (int) mx;
        int iy = (int) my;
        if (ix >= searchBox.getX() && ix <= searchBox.getX() + searchBox.getWidth()
                && iy >= searchBox.getY() && iy <= searchBox.getY() + searchBox.getHeight()) {
            searchBox.setFocused(true);
            return true;
        }
        int idx = rowAt(ix, iy);
        if (idx >= 0) {
            selected = idx;
            confirm();
            return true;
        }
        if (!contains(ix, iy)) {
            close();
        }
        return true;
    }

    private void confirm() {
        if (selected >= 0 && selected < filtered.size()) {
            onPick.accept(filtered.get(selected));
        }
        close();
    }

    private void refresh() {
        String q = searchBox.getValue().toLowerCase(Locale.ROOT);
        List<NodeDefinition> result = new ArrayList<>();
        for (NodeDefinition d : candidates) {
            if (q.isEmpty() || d.header().getString().toLowerCase(Locale.ROOT).contains(q)) {
                result.add(d);
            }
        }
        filtered = result;
        selected = result.isEmpty() ? -1 : 0;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        int w = WIDTH;
        int h = height();
        g.fill(x, y, x + w, y + h, COLOR_BG);
        g.renderOutline(x, y, w, h, COLOR_OUTLINE);
        searchBox.render(g, mouseX, mouseY, 0);
        int rowY = rowsTop();
        int limit = Math.min(filtered.size(), MAX_ROWS);
        for (int i = 0; i < limit; i++) {
            boolean hover = rowAt(mouseX, mouseY) == i;
            int bg = (i == selected) ? COLOR_SELECTED : (hover ? COLOR_HOVER : COLOR_BG);
            g.fill(x + 1, rowY, x + w - 1, rowY + ROW_HEIGHT, bg);
            g.drawString(font, filtered.get(i).header(), x + PADDING, rowY + 1, COLOR_TEXT);
            rowY += ROW_HEIGHT;
        }
    }

    private int rowsTop() {
        return y + PADDING + ROW_HEIGHT + PADDING;
    }

    private int height() {
        return rowsTop() + Math.min(filtered.size(), MAX_ROWS) * ROW_HEIGHT + PADDING;
    }

    private boolean contains(int mx, int my) {
        return mx >= x && mx <= x + WIDTH && my >= y && my <= y + height();
    }

    private int rowAt(int mx, int my) {
        if (mx < x || mx > x + WIDTH) {
            return -1;
        }
        int top = rowsTop();
        if (my < top) {
            return -1;
        }
        int idx = (my - top) / ROW_HEIGHT;
        if (idx < 0 || idx >= Math.min(filtered.size(), MAX_ROWS)) {
            return -1;
        }
        return idx;
    }
}
