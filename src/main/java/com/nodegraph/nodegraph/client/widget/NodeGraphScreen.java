package com.nodegraph.nodegraph.client.widget;

import com.nodegraph.nodegraph.api.command.UndoManager;
import com.nodegraph.nodegraph.api.model.NodeGraph;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * 节点图编辑器便利宿主 Screen。持有占满全屏的单个 {@link NodeGraphWidget}。
 *
 * <p>本类把所有按钮的鼠标事件直接转发到 canvas，确保中键拖动等在默认 MC 派发链下不可达的事件能被画布
 * 处理（MC 默认仅路由 button==0 的 mouseDragged 到 focused 子元素）。其他想嵌入 {@code NodeGraphWidget}
 * 的 Screen 必须按同样契约转发。
 *
 * <p>{@link #isPauseScreen()} 返回 false，编辑器不暂停游戏。
 */
public class NodeGraphScreen extends Screen {
    private final NodeGraph graph;
    private final UndoManager undo = new UndoManager();
    private NodeGraphWidget canvas;

    public NodeGraphScreen(Component title, NodeGraph graph) {
        super(title);
        this.graph = Objects.requireNonNull(graph, "graph");
    }

    public NodeGraphWidget canvas() {
        return canvas;
    }

    public UndoManager undo() {
        return undo;
    }

    @Override
    protected void init() {
        canvas = addRenderableWidget(new NodeGraphWidget(0, 0, width, height, graph, undo));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return canvas != null && canvas.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return canvas != null && canvas.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        return canvas != null && canvas.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return canvas != null && canvas.mouseScrolled(mx, my, delta);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (canvas != null) {
            canvas.mouseMoved(mx, my);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (canvas != null) {
            String hud = String.format("Zoom: %d%%", (int) Math.round(canvas.viewport().scale() * 100));
            g.drawString(font, hud, width - font.width(hud) - 4, height - font.lineHeight - 2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
