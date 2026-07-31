package io.github.tt432.nodegraph.api.model;

/**
 * Listener for node-level lifecycle events on a {@link NodeGraph}.
 *
 * <p>Connection-level cascades are reported separately through
 * {@link ConnectionListener}: removing a node first fires {@code REMOVED} for
 * every connection referencing it and only then {@link #onNodeRemoved}. This
 * ordering lets hosts settle connection-derived state before the node itself
 * disappears (mirrors the widget/connection listener isolation semantics:
 * synchronous dispatch, listener exceptions are isolated and logged).
 */
@FunctionalInterface
public interface NodeRemoveListener {
    /**
     * Called after a node has been removed from the graph (and after all its
     * connection cascades have been announced).
     *
     * @param graph the graph the node was removed from
     * @param node  the removed node (detached; still readable)
     */
    void onNodeRemoved(NodeGraph graph, Node node);
}
