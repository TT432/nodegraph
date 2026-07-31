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
import net.minecraft.world.InteractionHand;
//? if !modern {
import net.minecraft.world.InteractionResultHolder;
//?} else {
import net.minecraft.world.InteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
//? if !modern {
import net.minecraft.world.item.ItemStack;
//?}
import net.minecraft.world.level.Level;
//? if legacy {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}
import org.slf4j.Logger;

/**
 * 打开节点图编辑器的物品。右键使用打开 {@link NodeGraphScreen}，展示 {@link DemoGraphFactory} 的演示图。
 *
 * <p>{@code use} 以 {@code level.isClientSide()} 守卫，真正的开屏逻辑在 {@code @OnlyIn(Dist.CLIENT)}
 * 的私有方法里——双重保险，专用端不会触碰客户端类。
 */
public class GraphEditorItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();

    public GraphEditorItem(Properties properties) {
        super(properties);
    }

    @Override
    public /*? if !modern {*/ InteractionResultHolder<ItemStack> /*?} else {*/ InteractionResult /*?}*/
            use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide()) {
            openEditor();
        }
        //? if !modern {
        ItemStack stack = player.getItemInHand(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        //?} else {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        //?}
    }

    @OnlyIn(Dist.CLIENT)
    private static void openEditor() {
        NodeGraph graph = DemoGraphFactory.create();
        evaluateAndLog(graph);
        Minecraft.getInstance().setScreen(new NodeGraphScreen(Component.literal("Node Graph"), graph));
    }

    @OnlyIn(Dist.CLIENT)
    private static void evaluateAndLog(NodeGraph graph) {
        try {
            EvaluationResult result = new Evaluator().evaluateAll(graph);
            //? if legacy {
            ResourceLocation toByteId = new ResourceLocation("nodegraph", "to_byte");
            //?} else if modern {
            Identifier toByteId = Identifier.fromNamespaceAndPath("nodegraph", "to_byte");
            //?} else {
            ResourceLocation toByteId = ResourceLocation.fromNamespaceAndPath("nodegraph", "to_byte");
            //?}
            for (Node n : graph.nodes()) {
                if (n.definition().id().equals(toByteId)) {
                    LOGGER.info("NodeGraph demo evaluated: to_byte.out = {}", result.outputsOf(n.id()).get("out"));
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("NodeGraph demo evaluation failed", e);
        }
    }
}
