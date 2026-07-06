package io.github.tt432.nodegraph.api.def;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * Definition of a node kind: its header text, body-widget schema, input/output
 * port schema, and evaluation function. {@link io.github.tt432.nodegraph.api.model.Node}
 * instances are created from a definition.
 */
public final class NodeDefinition {
    private final ResourceLocation id;
    private final Component header;
    private final List<InputWidgetSpec> widgets;
    private final List<PortSpec> inputs;
    private final List<PortSpec> outputs;
    private final NodeFunction function;

    public NodeDefinition(ResourceLocation id, Component header,
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

    public ResourceLocation id() {
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
}
