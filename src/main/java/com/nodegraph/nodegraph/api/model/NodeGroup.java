package com.nodegraph.nodegraph.api.model;

import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * A frame grouping several nodes. Membership is stored on each {@link Node}
 * (its {@code groupId}); this class holds only the frame's own visual state.
 */
public final class NodeGroup {
    private final NodeGroupId id;
    private Component header;
    private double x;
    private double y;
    private double width;
    private double height;
    private double scale;

    public NodeGroup(NodeGroupId id, Component header, double x, double y, double width, double height) {
        this(id, header, x, y, width, height, 1.0);
    }

    public NodeGroup(NodeGroupId id, Component header, double x, double y, double width, double height, double scale) {
        this.id = Objects.requireNonNull(id, "id");
        this.header = Objects.requireNonNull(header, "header");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scale = scale;
    }

    public NodeGroupId id() {
        return id;
    }

    public Component header() {
        return header;
    }

    public void setHeader(Component header) {
        this.header = Objects.requireNonNull(header, "header");
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double scale() {
        return scale;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
