package io.github.tt432.nodegraph.client.render;

import io.github.tt432.nodegraph.api.model.InputWidget;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.client.viewport.Viewport;
//? if !modern {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}

/**
 * {@link io.github.tt432.nodegraph.api.model.InputWidgetKind#CUSTOM} widget 的宿主渲染回调。
 * 经 {@code NodeGraphWidget#registerWidgetRenderer(String, CustomWidgetRenderer)} 按 widget key
 * 注册；NodeRenderer 跳过 CUSTOM 行的名字/值文本，本接口在<b>屏幕坐标</b>下绘制该行区域
 * （缩略图、预览等）。
 *
 * <p>绘制区域已在画布的 scissor 内；宿主如需 3D 绘制（PoseStack）可自行再压 scissor。
 * 渲染每帧调用，实现须保持轻量（建议缓存解析结果，按 widget 当前值失效）。
 */
@FunctionalInterface
public interface CustomWidgetRenderer {
    /**
     * @param g       GUI 绘制上下文
     * @param node    所在节点（读取节点/其他 widget 状态）
     * @param widget  本 CUSTOM widget（currentValue 可作为缓存键）
     * @param screenX 行区域左上屏幕 x
     * @param screenY 行区域左上屏幕 y
     * @param screenW 区域宽（= 节点宽 × scale）
     * @param screenH 区域高（= widget 行高 × scale）
     * @param vp      视口（需要世界↔屏幕换算时）
     * @param mouseX  鼠标屏幕 x（悬停效果，可选）
     * @param mouseY  鼠标屏幕 y
     */
    void render(/*? if !modern {*/ GuiGraphics /*?} else {*/ GuiGraphicsExtractor /*?}*/ g,
                Node node, InputWidget widget,
                double screenX, double screenY, double screenW, double screenH,
                Viewport vp, int mouseX, int mouseY);
}
