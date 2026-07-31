package io.github.tt432.nodegraph.api.model;

/**
 * Kind of inline editor rendered for a node-body input component.
 *
 * <ul>
 *   <li>{@link #TEXT} / {@link #SLIDER} / {@link #BUTTON_GROUP} / {@link #DROPDOWN}：
 *       常规可编辑组件（当前仅 TEXT 有内联编辑实现）。</li>
 *   <li>{@link #DISPLAY}：只读文本——渲染名字与值，但不响应点击编辑。宿主可在创建节点后
 *       将个别 {@link InputWidget#setKind(InputWidgetKind)} 从 TEXT 覆盖为 DISPLAY，
 *       获得 per 实例的只读化（定义不变）。</li>
 *   <li>{@link #CUSTOM}：宿主自定义渲染区域——NodeRenderer 跳过该行的名字/值文本，
 *       由注册到 {@code NodeGraphWidget} 的 {@code CustomWidgetRenderer} 在屏幕坐标下绘制
 *       （预览缩略图等）。行高由 {@code InputWidgetSpec} 的 {@code height} 决定。</li>
 * </ul>
 */
public enum InputWidgetKind {
    TEXT,
    SLIDER,
    BUTTON_GROUP,
    DROPDOWN,
    DISPLAY,
    CUSTOM
}
