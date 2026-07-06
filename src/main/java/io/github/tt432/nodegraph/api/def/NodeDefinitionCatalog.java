package io.github.tt432.nodegraph.api.def;

import io.github.tt432.nodegraph.api.type.Type;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 可选的节点定义白名单：声明一张节点图"允许使用"的节点定义集合。
 *
 * <p>由 {@link io.github.tt432.nodegraph.api.model.NodeGraph} 持有（默认 {@code null} = 不限制）。
 * 数据层（{@code addNode}）<b>不</b>强制校验——catalog 是添加 UI 的过滤器：用户通过界面只能
 * 添加 catalog 内的节点，但程序化构造与 paste 不受限。这与"规定允许使用的节点集"一致
 * （从用户交互视角的允许集合）。
 *
 * <p>查询方法（{@link #matching} / {@link #withInputType} / {@link #withOutputType}）供添加节点
 * 的搜索面板使用，按注册（迭代）序返回。
 */
public final class NodeDefinitionCatalog {
    private final Map<ResourceLocation, NodeDefinition> defs = new LinkedHashMap<>();

    public NodeDefinition register(NodeDefinition def) {
        Objects.requireNonNull(def, "def");
        if (defs.putIfAbsent(def.id(), def) != null) {
            throw new IllegalArgumentException("Definition already registered: " + def.id());
        }
        return def;
    }

    public NodeDefinition get(ResourceLocation id) {
        NodeDefinition d = defs.get(Objects.requireNonNull(id, "id"));
        if (d == null) {
            throw new IllegalArgumentException("Unknown definition: " + id);
        }
        return d;
    }

    public boolean contains(ResourceLocation id) {
        return defs.containsKey(id);
    }

    public Collection<NodeDefinition> all() {
        return defs.values();
    }

    public int size() {
        return defs.size();
    }

    public boolean isEmpty() {
        return defs.isEmpty();
    }

    /**
     * 按 header 文本子串匹配（忽略大小写）。空/{@code null} query 返回全部。
     */
    public List<NodeDefinition> matching(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<NodeDefinition> result = new ArrayList<>();
        for (NodeDefinition d : defs.values()) {
            if (q.isEmpty() || d.header().getString().toLowerCase(Locale.ROOT).contains(q)) {
                result.add(d);
            }
        }
        return result;
    }

    /** 至少一个输入端口类型等于 {@code t} 的定义（按注册序）。 */
    public List<NodeDefinition> withInputType(Type t) {
        Objects.requireNonNull(t, "t");
        List<NodeDefinition> result = new ArrayList<>();
        for (NodeDefinition d : defs.values()) {
            for (PortSpec p : d.inputs()) {
                if (p.value().type().equals(t)) {
                    result.add(d);
                    break;
                }
            }
        }
        return result;
    }

    /** 至少一个输出端口类型等于 {@code t} 的定义（按注册序）。 */
    public List<NodeDefinition> withOutputType(Type t) {
        Objects.requireNonNull(t, "t");
        List<NodeDefinition> result = new ArrayList<>();
        for (NodeDefinition d : defs.values()) {
            for (PortSpec p : d.outputs()) {
                if (p.value().type().equals(t)) {
                    result.add(d);
                    break;
                }
            }
        }
        return result;
    }
}
