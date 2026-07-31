package io.github.tt432.nodegraph.client;

import com.mojang.logging.LogUtils;
import io.github.tt432.nodegraph.api.eval.EvaluationResult;
import io.github.tt432.nodegraph.api.eval.Evaluator;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.client.widget.NodeGraphScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
//? if !modern {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
//? if legacy {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}
import org.slf4j.Logger;

/**
 * 打开节点图编辑器的共享入口：{@link GraphEditorItem} 右键与 {@link ClientKeybinds} 的
 * Alt+C 键位均走这里。每次打开构造一份新的演示图（{@link DemoGraphFactory#create}）。
 */
@OnlyIn(Dist.CLIENT)
public final class EditorOpener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private EditorOpener() {
    }

    public static void open() {
        NodeGraph graph = DemoGraphFactory.create();
        evaluateAndLog(graph);
        Minecraft.getInstance().setScreen(new NodeGraphScreen(Component.literal("Node Graph"), graph));
    }

    private static void evaluateAndLog(NodeGraph graph) {
        try {
            EvaluationResult result = new Evaluator().evaluateAll(graph);
            //? if legacy {
            ResourceLocation multiSumId = new ResourceLocation("nodegraph", "multi_sum");
            //?} else if modern {
            Identifier multiSumId = Identifier.fromNamespaceAndPath("nodegraph", "multi_sum");
            //?} else {
            ResourceLocation multiSumId = ResourceLocation.fromNamespaceAndPath("nodegraph", "multi_sum");
            //?}
            for (Node n : graph.nodes()) {
                if (n.definition().id().equals(multiSumId)) {
                    LOGGER.info("NodeGraph demo evaluated: multi_sum.sum = {}", result.outputsOf(n.id()).get("sum"));
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("NodeGraph demo evaluation failed", e);
        }
    }
}
