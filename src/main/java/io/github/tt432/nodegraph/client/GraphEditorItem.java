package io.github.tt432.nodegraph.client;

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

/**
 * 打开节点图编辑器的物品。右键使用打开编辑器（{@link EditorOpener}，与 Alt+C 键位同一入口）。
 *
 * <p>{@code use} 以 {@code level.isClientSide()} 守卫，真正的开屏逻辑在 {@code @OnlyIn(Dist.CLIENT)}
 * 的 {@link EditorOpener} 里——双重保险，专用端不会触碰客户端类。
 */
public class GraphEditorItem extends Item {
    public GraphEditorItem(Properties properties) {
        super(properties);
    }

    @Override
    public /*? if !modern {*/ InteractionResultHolder<ItemStack> /*?} else {*/ InteractionResult /*?}*/
            use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide()) {
            EditorOpener.open();
        }
        //? if !modern {
        ItemStack stack = player.getItemInHand(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        //?} else {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        //?}
    }
}
