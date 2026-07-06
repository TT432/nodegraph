package com.nodegraph.nodegraph.api.clipboard;

import net.minecraft.network.chat.Component;

/**
 * Snapshot of one node group within a {@link SelectionSnapshot}.
 *
 * @param localId  local identity within this snapshot (0..M, separate namespace
 *                 from nodes)
 * @param header   group header
 * @param x        world x
 * @param y        world y
 * @param w        width
 * @param h        height
 * @param scale    group scale
 */
public record GroupSnapshot(
        long localId,
        Component header,
        double x,
        double y,
        double w,
        double h,
        double scale
) {}
