package io.github.tt432.nodegraph;

//? if !modern {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}

/**
 * 测试用 id 工厂：26.1 起 ResourceLocation 改名 Identifier，1.21 起构造器私有化。
 * 三版本分叉在此收敛一次，测试体统一调用 {@code rl(...)}。
 */
public final class TestIds {
    private TestIds() {}

    public static /*? if !modern {*/ ResourceLocation /*?} else {*/ Identifier /*?}*/ rl(String path) {
        return rl("nodegraph", path);
    }

    public static /*? if !modern {*/ ResourceLocation /*?} else {*/ Identifier /*?}*/ rl(String namespace, String path) {
        //? if legacy {
        return new ResourceLocation(namespace, path);
        //?} else if modern {
        return Identifier.fromNamespaceAndPath(namespace, path);
        //?} else {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?}
    }
}
