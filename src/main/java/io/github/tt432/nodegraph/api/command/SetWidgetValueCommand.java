package io.github.tt432.nodegraph.api.command;

import io.github.tt432.nodegraph.api.model.InputWidget;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeId;

import java.util.Objects;

/**
 * Sets {@link InputWidget#setCurrentValue(Object)} on the widget identified by
 * {@code widgetKey} within the target node. Captures the previous value on
 * first {@link #execute()} for {@link #undo()}.
 */
public final class SetWidgetValueCommand implements Command {
    private final NodeGraph graph;
    private final NodeId target;
    private final String widgetKey;
    private final Object newValue;
    private Object oldValue;
    private boolean captured = false;

    public SetWidgetValueCommand(NodeGraph graph, NodeId target, String widgetKey, Object newValue) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.target = Objects.requireNonNull(target, "target");
        this.widgetKey = Objects.requireNonNull(widgetKey, "widgetKey");
        this.newValue = newValue;
    }

    private InputWidget lookup() {
        Node node = graph.node(target);
        for (InputWidget w : node.widgets()) {
            if (w.key().equals(widgetKey)) {
                return w;
            }
        }
        throw new IllegalArgumentException(
                "Node " + target + " has no widget with key '" + widgetKey + "'");
    }

    @Override
    public void execute() {
        InputWidget w = lookup();
        if (!captured) {
            oldValue = w.currentValue();
            captured = true;
        }
        w.setCurrentValue(newValue);
    }

    @Override
    public void undo() {
        lookup().setCurrentValue(oldValue);
    }

    @Override
    public String description() {
        return "Set widget value";
    }
}
