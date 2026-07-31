package io.github.tt432.nodegraph.api.def;

import java.util.List;
import java.util.Map;

/**
 * Evaluation function of a <b>multi-input</b> node kind.
 * <p>
 * Each input port of a multi-input node may be driven by several wires at
 * once, so instead of a single value per port the function receives one
 * {@code List<Object>} per input port (keyed by input port key). The element
 * order of each list is the creation order of the connections feeding that
 * port; a port with no wires maps to an empty list. Auto-conversion rules
 * are applied per wire before the values reach the function.
 * <p>
 * The second argument carries the current values of the node-body input
 * widgets (keyed by widget key), and the function returns output-port values
 * keyed by output port key — same as {@link NodeFunction}.
 */
@FunctionalInterface
public interface MultiInputNodeFunction {
    Map<String, Object> evaluate(Map<String, List<Object>> inputValues, Map<String, Object> widgetValues);
}
