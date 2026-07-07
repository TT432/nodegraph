package io.github.tt432.nodegraph.api.model;

/**
 * Functional callback for {@link ConnectionEvent}s raised by a
 * {@link NodeGraph}. Register via {@link NodeGraph#addConnectionListener}.
 *
 * <p>Listeners are invoked <em>after</em> the graph mutation has taken
 * effect and only for informational purposes; they cannot veto a change.
 * Any {@link RuntimeException} thrown by an implementation is caught and
 * logged by the dispatcher and does not prevent other listeners from
 * being notified.
 */
@FunctionalInterface
public interface ConnectionListener {
    void onConnectionEvent(ConnectionEvent event);
}
