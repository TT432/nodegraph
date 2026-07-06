package io.github.tt432.nodegraph.client;

import com.mojang.logging.LogUtils;
import io.github.tt432.nodegraph.api.eval.EvaluationResult;
import io.github.tt432.nodegraph.api.eval.Evaluator;
import io.github.tt432.nodegraph.api.model.Node;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.client.widget.NodeGraphScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            openEditor();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
            ResourceLocation toByteId = new ResourceLocation("nodegraph", "to_byte");
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
