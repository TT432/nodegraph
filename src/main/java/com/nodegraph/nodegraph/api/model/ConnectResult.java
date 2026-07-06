package com.nodegraph.nodegraph.api.model;

/** Outcome of a type-compatibility check for a prospective connection. */
public enum ConnectResult {
    /** Types are identical. */
    OK,
    /** Types differ but a registered conversion rule exists; UI should warn. */
    AUTO_CONVERTED,
    /** No compatible conversion; connection is refused. */
    INCOMPATIBLE
}
