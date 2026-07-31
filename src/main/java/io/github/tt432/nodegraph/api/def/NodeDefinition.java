package io.github.tt432.nodegraph.api.def;

import net.minecraft.network.chat.Component;
//? if !modern {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}

import java.util.List;
import java.util.Objects;

/**
 * Definition of a node kind: its header text, body-widget schema, input/output
 * port schema, and evaluation function. {@link io.github.tt432.nodegraph.api.model.Node}
 * instances are created from a definition.
 *
 * <p>Not final: {@link MultiInputNodeDefinition} extends this class to mark a
 * node kind whose input ports accept multiple wires.
 */
public class NodeDefinition {
    private final /*? if !modern {*/ ResourceLocation /*?} else {*/ Identifier /*?}*/ id;
    private final Component header;
    private final List<InputWidgetSpec> widgets;
    private final List<PortSpec> inputs;
    private final List<PortSpec> outputs;
    private final NodeFunction function;

    public NodeDefinition(/*? if !modern {*/ ResourceLocation /*?} else {*/ Identifier /*?}*/ id, Component header,
                          List<InputWidgetSpec> widgets,
                          List<PortSpec> inputs,
                          List<PortSpec> outputs,
                          NodeFunction function) {
        this.id = Objects.requireNonNull(id, "id");
        this.header = Objects.requireNonNull(header, "header");
        this.widgets = List.copyOf(widgets);
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.function = Objects.requireNonNull(function, "function");
    }

    public /*? if !modern {*/ ResourceLocation /*?} else {*/ Identifier /*?}*/ id() {
        return id;
    }

    public Component header() {
        return header;
    }

    public List<InputWidgetSpec> widgets() {
        return widgets;
    }

    public List<PortSpec> inputs() {
        return inputs;
    }

    public List<PortSpec> outputs() {
        return outputs;
    }

    public NodeFunction function() {
        return function;
    }

    /**
     * Whether each input port of this node kind accepts <b>multiple</b> wires.
     * {@code false} (the default) means single-source semantics: connecting to
     * an already-driven input replaces the existing wire. {@code true} means
     * wires accumulate and the node is evaluated through
     * {@link MultiInputNodeDefinition#multiFunction()} with one
     * {@code List<Object>} per input port.
     */
    public boolean isMultiInput() {
        return false;
    }
}
