package io.github.tt432.nodegraph.api.def;

import java.util.Map;

/**
 * Evaluation function of a node kind.
 * <p>
 * Receives resolved input-port values (keyed by input port key) and the
 * current values of the node-body input widgets (keyed by widget key), and
 * returns output-port values keyed by output port key.
 */
@FunctionalInterface
public interface NodeFunction {
    Map<String, Object> evaluate(Map<String, Object> inputValues, Map<String, Object> widgetValues);
}
