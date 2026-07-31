package io.github.tt432.nodegraph.client.widget;

import io.github.tt432.nodegraph.api.command.UndoManager;
import io.github.tt432.nodegraph.api.model.NodeGraph;
//? if !modern {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
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

    //? if !modern {
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
    //?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return canvas != null && canvas.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return canvas != null && canvas.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return canvas != null && canvas.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }
    //?}

    //? if legacy {
    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return canvas != null && canvas.mouseScrolled(mx, my, delta);
    }
    //?} else if !modern {
    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        return canvas != null && canvas.mouseScrolled(mx, my, scrollX, scrollY);
    }
    //?} else {
    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        return canvas != null && canvas.mouseScrolled(mx, my, scrollY);
    }
    //?}

    @Override
    public void mouseMoved(double mx, double my) {
        if (canvas != null) {
            canvas.mouseMoved(mx, my);
        }
    }

    //? if !modern {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleKeyCode(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (handleCharCode(codePoint)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
    //?} else {
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (handleKeyCode(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (handleCharCode((char) event.codepoint())) {
            return true;
        }
        return super.charTyped(event);
    }
    //?}

    private boolean handleKeyCode(int keyCode) {
        if (canvas != null && canvas.editKey(keyCode)) {
            return true;
        }
        if (canvas != null && canvas.overlayKey(keyCode)) {
            return true;
        }
        if (canvas != null && canvas.handleKey(keyCode)) {
            return true;
        }
        if (keyCode == 256) { // ESC: close menu first, then pop subgraph, else let super close the screen
            if (canvas != null && canvas.menu() != null) {
                canvas.closeMenu();
                return true;
            }
            if (canvas != null && canvas.canPopSubgraph()) {
                canvas.popSubgraph();
                return true;
            }
        }
        return false;
    }

    private boolean handleCharCode(char codePoint) {
        if (canvas != null && canvas.editChar(codePoint)) {
            return true;
        }
        if (canvas != null && canvas.overlayChar(codePoint)) {
            return true;
        }
        return false;
    }

    //? if !modern {
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (canvas != null) {
            String hud = String.format("Zoom: %d%%", (int) Math.round(canvas.viewport().scale() * 100));
            g.drawString(font, hud, width - font.width(hud) - 4, height - font.lineHeight - 2, 0xFFFFFFFF);
        }
    }
    //?} else {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (canvas != null) {
            String hud = String.format("Zoom: %d%%", (int) Math.round(canvas.viewport().scale() * 100));
            graphics.text(font, hud, width - font.width(hud) - 4, height - font.lineHeight - 2, 0xFFFFFFFF);
        }
    }
    //?}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
