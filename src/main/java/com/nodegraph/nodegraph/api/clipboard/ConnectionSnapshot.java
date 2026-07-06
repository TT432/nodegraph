package com.nodegraph.nodegraph.api.clipboard;

import com.nodegraph.nodegraph.api.type.TypeConversionRule;

/**
 * Snapshot of one internal connection (both endpoints selected) within a
 * {@link SelectionSnapshot}. Endpoints reference node local ids.
 *
 * <p>The {@code rule} reference is carried verbatim so paste can rebuild the
 * wire with its original conversion semantics via
 * {@link com.nodegraph.nodegraph.api.model.NodeGraph#addConnection}, bypassing
 * re-validation against the live {@link com.nodegraph.nodegraph.api.type.TypeRegistry}.
 *
 * @param fromLocalId    source node local id
 * @param fromOutput     source output port index
 * @param toLocalId      target node local id
 * @param toInput        target input port index
 * @param autoConverted  whether the original wire was auto-converted
 * @param rule           conversion rule, non-null iff {@code autoConverted}
 */
public record ConnectionSnapshot(
        long fromLocalId,
        int fromOutput,
        long toLocalId,
        int toInput,
        boolean autoConverted,
        TypeConversionRule rule
) {}
