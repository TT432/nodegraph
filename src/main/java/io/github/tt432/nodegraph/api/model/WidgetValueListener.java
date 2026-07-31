package io.github.tt432.nodegraph.api.model;

/**
 * Listener for node-body widget value changes. Registered on {@link NodeGraph};
 * invoked after a widget's current value is changed through the command system
 * ({@code SetWidgetValueCommand} execute / undo / redo).
 *
 * <p>Direct {@link InputWidget#setCurrentValue(Object)} calls (e.g. a host
 * pre-filling values while building the graph) do <b>not</b> fire this event —
 * the listener exists so hosts can react to <i>user edits</i> (write back to a
 * document model), not to their own initialization.
 */
@FunctionalInterface
public interface WidgetValueListener {
    /**
     * @param graph     the graph owning the node
     * @param node      the node whose widget changed
     * @param widgetKey the widget's key
     * @param oldValue  previous value (may be null)
     * @param newValue  new value (may be null)
     */
    void onWidgetValueChanged(NodeGraph graph, NodeId node, String widgetKey,
                              Object oldValue, Object newValue);
}
