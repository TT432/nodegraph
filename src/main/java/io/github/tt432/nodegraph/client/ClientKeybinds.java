package io.github.tt432.nodegraph.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.tt432.nodegraph.NodeGraph;
import net.minecraft.client.KeyMapping;
//? if legacy {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
//?}
//? if modern {
import net.minecraft.resources.Identifier;
//?}
import org.lwjgl.glfw.GLFW;

/**
 * 客户端键位：Alt+C 打开节点图编辑器（{@link EditorOpener}）。
 * 以 {@code @EventBusSubscriber(dist = CLIENT)} 自注册，专用端不会加载本类。
 */
//? if legacy {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = NodeGraph.MODID,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
//?} else if !modern {
@net.neoforged.fml.common.EventBusSubscriber(
        modid = NodeGraph.MODID,
        bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
//?} else {
// 26.1 起 EventBusSubscriber 移除 Bus 枚举（按事件类型自动分流到 mod/game bus）。
@net.neoforged.fml.common.EventBusSubscriber(modid = NodeGraph.MODID, value = Dist.CLIENT)
//?}
public final class ClientKeybinds {
    //? if modern {
    /** 26.1 起键位分类为 {@link KeyMapping.Category} 记录，需在注册键位前注册分类。 */
    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(NodeGraph.MODID, "main"));
    //?}

    private static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.nodegraph.open_editor",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C),
            //? if !modern {
            "key.categories.nodegraph");
    //?} else {
            CATEGORY);
    //?}

    private ClientKeybinds() {
    }

    @SubscribeEvent
    public static void onRegisterMappings(RegisterKeyMappingsEvent event) {
        //? if modern {
        event.registerCategory(CATEGORY);
        //?}
        event.register(OPEN_EDITOR);
        //? if legacy {
        MinecraftForge.EVENT_BUS.addListener(ClientKeybinds::onClientTick);
        //?} else {
        NeoForge.EVENT_BUS.addListener(ClientKeybinds::onClientTick);
        //?}
    }

    //? if legacy {
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (OPEN_EDITOR.consumeClick()) {
            EditorOpener.open();
        }
    }
    //?} else {
    private static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_EDITOR.consumeClick()) {
            EditorOpener.open();
        }
    }
    //?}
}
