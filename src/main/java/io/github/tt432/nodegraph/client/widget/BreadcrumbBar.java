package io.github.tt432.nodegraph.client.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * 子图导航面包屑的纯数据布局与拾取（无 MC 依赖，可单测）。
 *
 * <p>面包屑项 = 导航栈从根到当前层的标签，项间以分隔符隔开；每项记录其屏幕 x 区间，
 * {@link #pick(int)} 命中测试返回层索引（0 = 根）。渲染由 {@link NodeGraphWidget} 负责
 * （需要 Font/GuiGraphics），本类只产出几何。
 */
public final class BreadcrumbBar {
    /** 面包屑条高度（屏幕 px）。 */
    public static final int BAR_HEIGHT = 16;
    /** 项与分隔符的左右内边距（屏幕 px）。 */
    public static final int PADDING = 4;

    /** 单个面包屑项：标签 + 屏幕 x 区间 [x0, x1)。 */
    public record Item(String label, int x0, int x1) {
    }

    private final List<Item> items;
    private final int separatorWidth;

    private BreadcrumbBar(List<Item> items, int separatorWidth) {
        this.items = items;
        this.separatorWidth = separatorWidth;
    }

    /**
     * 布局一列标签。{@code widthOf} 为文本宽度计算（生产 = {@code font::width}，测试可用定值）。
     *
     * @param labels       根 → 当前层的标签序列（至少 1 项）
     * @param widthOf      文本宽度函数
     * @param separatorW   分隔符宽度（如 " / " 的宽度）
     * @param originX      首项起始屏幕 x
     */
    public static BreadcrumbBar layout(List<String> labels, ToIntFunction<String> widthOf,
                                       int separatorW, int originX) {
        Objects.requireNonNull(labels, "labels");
        if (labels.isEmpty()) {
            throw new IllegalArgumentException("labels must not be empty");
        }
        List<Item> items = new ArrayList<>(labels.size());
        int x = originX + PADDING;
        for (String label : labels) {
            int w = widthOf.applyAsInt(label);
            items.add(new Item(label, x, x + w));
            x += w + separatorW;
        }
        return new BreadcrumbBar(items, separatorW);
    }

    public List<Item> items() {
        return items;
    }

    public int separatorWidth() {
        return separatorWidth;
    }

    /** 条的总宽（末项右缘 + padding）。 */
    public int totalWidth() {
        Item last = items.get(items.size() - 1);
        return last.x1() + PADDING;
    }

    /**
     * 命中测试：返回点击的层索引；未命中任何项或点击末项（当前层）返回 -1。
     * 点击分隔符区域归属其左侧项。
     */
    public int pick(int screenX) {
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            int right = i + 1 < items.size() ? item.x1() + separatorWidth : item.x1() + PADDING;
            if (screenX >= item.x0() - PADDING && screenX < right) {
                return i == items.size() - 1 ? -1 : i;
            }
        }
        return -1;
    }
}
