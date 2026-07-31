package io.github.tt432.nodegraph.api;

import io.github.tt432.nodegraph.api.command.SetWidgetValueCommand;
import io.github.tt432.nodegraph.api.command.UndoManager;
import io.github.tt432.nodegraph.api.def.InputWidgetSpec;
import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.model.InputWidgetKind;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.TypedValue;
import io.github.tt432.nodegraph.api.model.WidgetValueListener;
import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;
import static io.github.tt432.nodegraph.TestIds.rl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Widget 值监听：命令路径（execute/undo/redo）触发事件，载荷含新旧值与目标；
 * 直接 setCurrentValue（宿主建图填充）不触发；监听器可卸载；
 * 监听器异常不影响图状态也不阻断其他监听器。
 */
class TestWidgetValueListener {

    private static final Type T = new Type("t", 0xFF112233);

    private NodeGraph graphWithTextNode() {
        NodeGraph g = new NodeGraph(new TypeRegistry());
        g.addNode(new NodeDefinition(rl("test/w"), Component.literal("n"),
                List.of(new InputWidgetSpec("expr",
                        new TypedValue(Component.literal("expr"), T, Component.literal("")),
                        InputWidgetKind.TEXT, "initial")),
                List.of(), List.of(), (inputs, widgets) -> Map.of()), 0, 0);
        return g;
    }

    private record Event(String key, Object oldValue, Object newValue) {
    }

    @Test
    void commandExecuteFiresWithOldAndNew() {
        NodeGraph g = graphWithTextNode();
        Node node = g.nodes().iterator().next();
        List<Event> events = new ArrayList<>();
        g.addWidgetListener((graph, n, key, oldV, newV) -> events.add(new Event(key, oldV, newV)));
        new SetWidgetValueCommand(g, node.id(), "expr", "v.kk > 1").execute();
        assertEquals(1, events.size());
        assertEquals(new Event("expr", "initial", "v.kk > 1"), events.get(0));
    }

    @Test
    void undoAndRedoFireAgain() {
        NodeGraph g = graphWithTextNode();
        Node node = g.nodes().iterator().next();
        List<Event> events = new ArrayList<>();
        g.addWidgetListener((graph, n, key, oldV, newV) -> events.add(new Event(key, oldV, newV)));
        UndoManager undo = new UndoManager();
        undo.apply(new SetWidgetValueCommand(g, node.id(), "expr", "a"));
        undo.undo();
        undo.redo();
        assertEquals(3, events.size());
        assertEquals(new Event("expr", "initial", "a"), events.get(0));
        assertEquals(new Event("expr", "a", "initial"), events.get(1));
        assertEquals(new Event("expr", "initial", "a"), events.get(2));
    }

    @Test
    void directSetCurrentValueDoesNotFire() {
        NodeGraph g = graphWithTextNode();
        Node node = g.nodes().iterator().next();
        List<Event> events = new ArrayList<>();
        g.addWidgetListener((graph, n, key, oldV, newV) -> events.add(new Event(key, oldV, newV)));
        node.widgets().get(0).setCurrentValue("silent");
        assertTrue(events.isEmpty());
    }

    @Test
    void removedListenerStopsFiring() {
        NodeGraph g = graphWithTextNode();
        Node node = g.nodes().iterator().next();
        List<Event> events = new ArrayList<>();
        WidgetValueListener listener = (graph, n, key, oldV, newV) -> events.add(new Event(key, oldV, newV));
        g.addWidgetListener(listener);
        new SetWidgetValueCommand(g, node.id(), "expr", "a").execute();
        g.removeWidgetListener(listener);
        new SetWidgetValueCommand(g, node.id(), "expr", "b").execute();
        assertEquals(1, events.size());
    }

    @Test
    void throwingListenerDoesNotCorruptStateOrOthers() {
        NodeGraph g = graphWithTextNode();
        Node node = g.nodes().iterator().next();
        List<Event> events = new ArrayList<>();
        g.addWidgetListener((graph, n, key, oldV, newV) -> {
            throw new RuntimeException("boom");
        });
        g.addWidgetListener((graph, n, key, oldV, newV) -> events.add(new Event(key, oldV, newV)));
        new SetWidgetValueCommand(g, node.id(), "expr", "x").execute();
        assertEquals("x", node.widgets().get(0).currentValue());
        assertEquals(1, events.size());
    }
}
