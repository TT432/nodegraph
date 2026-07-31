package io.github.tt432.nodegraph;

import io.github.tt432.nodegraph.client.GraphEditorItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
//? if legacy {
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?} else {
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
//?}

@Mod(NodeGraph.MODID)
public class NodeGraph {
    public static final String MODID = "nodegraph";

    //? if legacy {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> GRAPH_EDITOR_ITEM =
            ITEMS.register("graph_editor", () -> new GraphEditorItem(new Item.Properties()));
    //?} else {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);

    public static final DeferredHolder<Item, Item> GRAPH_EDITOR_ITEM =
            ITEMS.register("graph_editor", () -> new GraphEditorItem(new Item.Properties()));
    //?}

    //? if legacy {
    public NodeGraph() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
    //?} else {
    public NodeGraph(IEventBus modEventBus) {
    //?}
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            //? if legacy {
            event.accept(GRAPH_EDITOR_ITEM);
            //?} else {
            event.accept(GRAPH_EDITOR_ITEM.get());
            //?}
        }
    }
}
