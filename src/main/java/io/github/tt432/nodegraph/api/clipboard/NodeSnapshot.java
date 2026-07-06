package io.github.tt432.nodegraph.api.clipboard;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Snapshot of one node within a {@link SelectionSnapshot}.
 *
 * @param localId       local identity within this snapshot (0..N)
 * @param defId         {@link io.github.tt432.nodegraph.api.def.NodeDefinition#id()}
 * @param header        the (possibly renamed) current header
 * @param x             world x
 * @param y             world y
 * @param widgetValues  key -> current value for every widget on the node
 * @param localGroupId  local group id if the node's group is also selected,
 *                      otherwise {@code null} (pasted as a free node)
 */
public record NodeSnapshot(
        long localId,
        ResourceLocation defId,
        Component header,
        double x,
        double y,
        Map<String, Object> widgetValues,
        Long localGroupId
) {
    public NodeSnapshot {
        widgetValues = Map.copyOf(widgetValues);
    }
}
