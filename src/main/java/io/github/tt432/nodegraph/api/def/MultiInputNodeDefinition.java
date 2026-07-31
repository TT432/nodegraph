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
 * Definition of a <b>multi-input</b> node kind: every input port accepts any
 * number of wires (they accumulate in creation order instead of replacing
 * each other), and at evaluation time the {@link MultiInputNodeFunction}
 * receives one {@code List<Object>} per input port keyed by port key, in
 * connection-creation order.
 * <p>
 * The {@link NodeFunction} exposed via {@link #function()} is an inert
 * adapter that throws {@link UnsupportedOperationException}: the evaluator
 * dispatches multi-input nodes to {@link #multiFunction()} and never invokes
 * the single-input path for them.
 */
public final class MultiInputNodeDefinition extends NodeDefinition {
    private final MultiInputNodeFunction multiFunction;

    public MultiInputNodeDefinition(/*? if !modern {*/ ResourceLocation /*?} else {*/ Identifier /*?}*/ id, Component header,
                                    List<InputWidgetSpec> widgets,
                                    List<PortSpec> inputs,
                                    List<PortSpec> outputs,
                                    MultiInputNodeFunction function) {
        super(id, header, widgets, inputs, outputs, (inputValues, widgetValues) -> {
            throw new UnsupportedOperationException(
                    "Multi-input nodes are evaluated via MultiInputNodeFunction");
        });
        this.multiFunction = Objects.requireNonNull(function, "function");
    }

    @Override
    public boolean isMultiInput() {
        return true;
    }

    public MultiInputNodeFunction multiFunction() {
        return multiFunction;
    }
}
