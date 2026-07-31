package io.github.tt432.nodegraph.client.widget;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 面包屑布局与拾取：项 x 区间连续（含分隔符），pick 命中返回层索引，
 * 末项（当前层）不可导航返回 -1，界外返回 -1。
 */
class TestBreadcrumbBar {

    /** 定宽字体：每字符 6px。 */
    private static int width(String s) {
        return s.length() * 6;
    }

    @Test
    void itemsAreLaidOutLeftToRightWithSeparators() {
        BreadcrumbBar bar = BreadcrumbBar.layout(List.of("root", "RC: abc", "state"), TestBreadcrumbBar::width, 12, 100);
        List<BreadcrumbBar.Item> items = bar.items();
        assertEquals(3, items.size());
        assertEquals(100 + BreadcrumbBar.PADDING, items.get(0).x0());
        assertEquals(items.get(0).x0() + width("root"), items.get(0).x1());
        assertEquals(items.get(0).x1() + 12, items.get(1).x0());
        assertEquals(items.get(1).x1() + 12, items.get(2).x0());
    }

    @Test
    void pickReturnsLayerIndex() {
        BreadcrumbBar bar = BreadcrumbBar.layout(List.of("root", "RC: abc"), TestBreadcrumbBar::width, 12, 0);
        BreadcrumbBar.Item root = bar.items().get(0);
        assertEquals(0, bar.pick(root.x0()));
        assertEquals(0, bar.pick(root.x1() - 1));
    }

    @Test
    void pickSeparatorBelongsToLeftItem() {
        BreadcrumbBar bar = BreadcrumbBar.layout(List.of("root", "RC: abc"), TestBreadcrumbBar::width, 12, 0);
        BreadcrumbBar.Item root = bar.items().get(0);
        // 分隔符区间 [x1, x1+separatorW) 归属左侧项
        assertEquals(0, bar.pick(root.x1() + 3));
    }

    @Test
    void pickLastItemReturnsMinusOne() {
        BreadcrumbBar bar = BreadcrumbBar.layout(List.of("root", "RC: abc"), TestBreadcrumbBar::width, 12, 0);
        BreadcrumbBar.Item last = bar.items().get(1);
        assertEquals(-1, bar.pick(last.x0() + 2));
    }

    @Test
    void pickOutsideReturnsMinusOne() {
        BreadcrumbBar bar = BreadcrumbBar.layout(List.of("root"), TestBreadcrumbBar::width, 12, 0);
        assertEquals(-1, bar.pick(bar.totalWidth() + 50));
        assertEquals(-1, bar.pick(-500));
    }

    @Test
    void singleLayerBarHasNoNavigableItem() {
        BreadcrumbBar bar = BreadcrumbBar.layout(List.of("root"), TestBreadcrumbBar::width, 12, 0);
        BreadcrumbBar.Item only = bar.items().get(0);
        assertEquals(-1, bar.pick(only.x0() + 1));
    }

    @Test
    void emptyLabelsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BreadcrumbBar.layout(List.of(), TestBreadcrumbBar::width, 12, 0));
    }
}
