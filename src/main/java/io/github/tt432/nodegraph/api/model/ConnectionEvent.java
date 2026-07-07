package io.github.tt432.nodegraph.api.model;

import java.util.Objects;

/**
 * Lifecycle event fired by {@link NodeGraph} whenever a {@link Connection}
 * is created or removed. Carries the affected connection and the kind of
 * transition.
 *
 * <p>Replacement semantics in {@link NodeGraph#connect} are surfaced as two
 * independent events: first {@link Kind#REMOVED} for the displaced connection,
 * then {@link Kind#CREATED} for the new one.
 *
 * <p>Immutable and thread-safe for publication.
 */
public final class ConnectionEvent {
    /** The kind of lifecycle transition. */
    public enum Kind {
        /** A connection was added to the graph. */
        CREATED,
        /** A connection was removed from the graph. */
        REMOVED
    }

    private final Kind kind;
    private final Connection connection;

    public ConnectionEvent(Kind kind, Connection connection) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    public Kind kind() {
        return kind;
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public String toString() {
        return "ConnectionEvent[" + kind + " " + connection + "]";
    }
}
