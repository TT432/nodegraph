package io.github.tt432.nodegraph.api.model;

import io.github.tt432.nodegraph.api.def.InputWidgetSpec;
import io.github.tt432.nodegraph.api.def.NodeDefinition;
import io.github.tt432.nodegraph.api.def.NodeDefinitionCatalog;
import io.github.tt432.nodegraph.api.def.PortSpec;
import io.github.tt432.nodegraph.api.type.Type;
import io.github.tt432.nodegraph.api.type.TypeConversionRule;
import io.github.tt432.nodegraph.api.type.TypeRegistry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The node-graph container: pure data holding nodes, node groups and
 * connections. Connection constraints (output fan-out, input single-source with
 * replacement) are enforced here. View state (pan / zoom) is intentionally not
 * stored here — it belongs to the view layer.
 */
public final class NodeGraph {
    private final TypeRegistry types;
    private final Map<NodeId, Node> nodes = new LinkedHashMap<>();
    private final Map<NodeGroupId, NodeGroup> groups = new LinkedHashMap<>();
    private final List<Connection> connections = new ArrayList<>();
    private final List<ConnectionListener> connectionListeners = new ArrayList<>();
    private long nextNodeId = 1;
    private long nextGroupId = 1;
    private NodeDefinitionCatalog catalog;

    public NodeGraph(TypeRegistry types) {
        this.types = Objects.requireNonNull(types, "types");
    }

    public TypeRegistry types() {
        return types;
    }

    /** Optional node-definition whitelist for the add-node UI; {@code null} = unrestricted. */
    public NodeDefinitionCatalog catalog() {
        return catalog;
    }

    public void setCatalog(NodeDefinitionCatalog catalog) {
        this.catalog = catalog;
    }

    // ---------------------------------------------------------------- nodes

    public Node addNode(NodeDefinition definition, double x, double y) {
        Objects.requireNonNull(definition, "definition");
        NodeId id = new NodeId(nextNodeId++);
        List<InputWidget> widgets = new ArrayList<>();
        for (InputWidgetSpec spec : definition.widgets()) {
            widgets.add(new InputWidget(spec.key(), spec.value(), spec.kind(), spec.defaultValue()));
        }
        List<Port> inputs = new ArrayList<>();
        for (PortSpec spec : definition.inputs()) {
            inputs.add(new Port(spec.key(), spec.value()));
        }
        List<Port> outputs = new ArrayList<>();
        for (PortSpec spec : definition.outputs()) {
            outputs.add(new Port(spec.key(), spec.value()));
        }
        Node node = new Node(id, definition, definition.header(), x, y, widgets, inputs, outputs);
        nodes.put(id, node);
        return node;
    }

    /**
     * Low-level recovery primitive: insert a fully-formed {@link Node} as-is,
     * <b>without</b> allocating a new id. Intended for the command system
     * (undo/redo) and serialization/deserialization, where object identity
     * (the {@link NodeId}) must survive round-trips so existing connections
     * and group memberships keep resolving.
     *
     * <p>Validates that the id does not collide with an existing node. Advances
     * {@code nextNodeId} past the inserted node's id to avoid future
     * collisions with factory-allocated ids.
     *
     * @throws IllegalStateException if a node with the same id already exists.
     */
    public void insertNode(Node node) {
        Objects.requireNonNull(node, "node");
        NodeId id = node.id();
        if (nodes.containsKey(id)) {
            throw new IllegalStateException("Node id already present: " + id);
        }
        nodes.put(id, node);
        if (id.value() >= nextNodeId) {
            nextNodeId = id.value() + 1;
        }
    }

    public void removeNode(NodeId id) {
        Node node = nodes.remove(id);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node: " + id);
        }
        // Drop every connection that references this node. Collect first so
        // each removal can be announced to listeners (removeIf cannot).
        List<Connection> victims = new ArrayList<>();
        for (Connection c : connections) {
            if (c.fromNode().equals(id) || c.toNode().equals(id)) {
                victims.add(c);
            }
        }
        for (Connection c : victims) {
            connections.remove(c);
            fireConnectionEvent(ConnectionEvent.Kind.REMOVED, c);
        }
    }

    public Node node(NodeId id) {
        Node n = nodes.get(id);
        if (n == null) {
            throw new IllegalArgumentException("Unknown node: " + id);
        }
        return n;
    }

    public Collection<Node> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    // --------------------------------------------------------------- groups

    public NodeGroup createGroup(Component header, double x, double y, double width, double height) {
        NodeGroupId id = new NodeGroupId(nextGroupId++);
        NodeGroup group = new NodeGroup(id, header, x, y, width, height);
        groups.put(id, group);
        return group;
    }

    /**
     * Low-level recovery primitive: insert a fully-formed {@link NodeGroup}
     * as-is, <b>without</b> allocating a new id. Intended for the command
     * system (undo/redo) and serialization/deserialization, where group
     * identity must survive round-trips so member nodes keep resolving.
     *
     * <p>Validates that the id does not collide. Advances {@code nextGroupId}.
     *
     * @throws IllegalStateException if a group with the same id already exists.
     */
    public void insertGroup(NodeGroup group) {
        Objects.requireNonNull(group, "group");
        NodeGroupId id = group.id();
        if (groups.containsKey(id)) {
            throw new IllegalStateException("Group id already present: " + id);
        }
        groups.put(id, group);
        if (id.value() >= nextGroupId) {
            nextGroupId = id.value() + 1;
        }
    }

    public void removeGroup(NodeGroupId id) {
        if (groups.remove(id) == null) {
            throw new IllegalArgumentException("Unknown group: " + id);
        }
        // Clear membership references (nodes belonging to the removed group).
        for (Node n : nodes.values()) {
            if (id.equals(n.groupId())) {
                n.setGroupId(null);
            }
        }
    }

    public NodeGroup group(NodeGroupId id) {
        NodeGroup g = groups.get(id);
        if (g == null) {
            throw new IllegalArgumentException("Unknown group: " + id);
        }
        return g;
    }

    public Collection<NodeGroup> groups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    /** Members are derived from {@link Node#groupId()} (single source of truth). */
    public List<Node> members(NodeGroupId group) {
        List<Node> result = new ArrayList<>();
        for (Node n : nodes.values()) {
            if (group.equals(n.groupId())) {
                result.add(n);
            }
        }
        return result;
    }

    public void setNodeGroup(NodeId nodeId, NodeGroupId group) {
        Node node = node(nodeId);
        if (group != null) {
            group(group); // validate existence
        }
        node.setGroupId(group);
    }

    // ---------------------------------------------------------- connections

    private Type outputType(NodeId node, int index) {
        Node n = node(node);
        if (index < 0 || index >= n.outputs().size()) {
            throw new IllegalArgumentException("Output index out of range: " + index);
        }
        return n.outputs().get(index).type();
    }

    private Type inputType(NodeId node, int index) {
        Node n = node(node);
        if (index < 0 || index >= n.inputs().size()) {
            throw new IllegalArgumentException("Input index out of range: " + index);
        }
        return n.inputs().get(index).type();
    }

    public ConnectResult canConnect(NodeId fromNode, int fromOutput, NodeId toNode, int toInput) {
        Type outT = outputType(fromNode, fromOutput);
        Type inT = inputType(toNode, toInput);
        if (outT.equals(inT)) {
            return ConnectResult.OK;
        }
        return types.canConvert(outT, inT) ? ConnectResult.AUTO_CONVERTED : ConnectResult.INCOMPATIBLE;
    }

    /**
     * Create a wire. Enforces input single-source: if the target input already
     * has a connection, it is replaced.
     *
     * @throws IllegalArgumentException if incompatible or endpoints invalid.
     */
    public Connection connect(NodeId fromNode, int fromOutput, NodeId toNode, int toInput) {
        ConnectResult result = canConnect(fromNode, fromOutput, toNode, toInput);
        if (result == ConnectResult.INCOMPATIBLE) {
            throw new IllegalArgumentException("Incompatible connection: "
                    + outputType(fromNode, fromOutput) + " -> " + inputType(toNode, toInput));
        }
        // Replace existing single-source connection on this input. The
        // delegate call to disconnect(Connection) also fires REMOVED so the
        // replacement is observed as REMOVED(old) then CREATED(new).
        Optional<Connection> existing = inputConnection(toNode, toInput);
        existing.ifPresent(this::disconnect);

        boolean auto = result == ConnectResult.AUTO_CONVERTED;
        TypeConversionRule rule = auto
                ? types.conversion(outputType(fromNode, fromOutput), inputType(toNode, toInput)).orElse(null)
                : null;
        Connection c = new Connection(fromNode, fromOutput, toNode, toInput, auto, rule);
        connections.add(c);
        fireConnectionEvent(ConnectionEvent.Kind.CREATED, c);
        return c;
    }

    public void disconnect(Connection c) {
        boolean removed = connections.remove(c);
        if (removed) {
            fireConnectionEvent(ConnectionEvent.Kind.REMOVED, c);
        }
    }

    /**
     * Low-level recovery primitive: append a fully-formed {@link Connection}
     * as-is, bypassing the single-source-replacement and type-checking
     * semantics of {@link #connect}. Intended for the command system
     * (undo/redo) and serialization/deserialization, where the connection
     * was already validated at original creation time.
     *
     * <p>Validates that both endpoints exist and that the target input is not
     * already driven by a <b>different</b> connection (preserves the input
     * single-source invariant). Does not perform type compatibility checks.
     *
     * @throws IllegalArgumentException  if either endpoint node does not exist.
     * @throws IllegalStateException     if a different connection already
     *                                  drives the target input.
     */
    public void addConnection(Connection c) {
        Objects.requireNonNull(c, "c");
        // Validate endpoints exist (throws IllegalArgumentException via node()).
        node(c.fromNode());
        node(c.toNode());
        // Preserve single-source invariant: allow re-adding the exact same
        // connection (idempotent), but reject a different one on this input.
        for (Connection existing : connections) {
            if (existing.toNode().equals(c.toNode()) && existing.toInput() == c.toInput()
                    && !existing.equals(c)) {
                throw new IllegalStateException("Input already driven by a different connection: " + existing);
            }
        }
        // Avoid duplicate if the exact connection is already present.
        if (!connections.contains(c)) {
            connections.add(c);
            fireConnectionEvent(ConnectionEvent.Kind.CREATED, c);
        }
    }

    public List<Connection> connections() {
        return Collections.unmodifiableList(connections);
    }

    /** The single incoming connection of an input port, if any. */
    public Optional<Connection> inputConnection(NodeId node, int toInput) {
        for (Connection c : connections) {
            if (c.toNode().equals(node) && c.toInput() == toInput) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /** All outgoing connections of an output port (fan-out). */
    public List<Connection> outputsOf(NodeId node, int fromOutput) {
        List<Connection> result = new ArrayList<>();
        for (Connection c : connections) {
            if (c.fromNode().equals(node) && c.fromOutput() == fromOutput) {
                result.add(c);
            }
        }
        return result;
    }

    // ------------------------------------------------------ event listeners

    /**
     * Register a {@link ConnectionListener} to be notified after every
     * connection creation / removal. Listeners are invoked synchronously on
     * the thread that performed the mutation; deregister via
     * {@link #removeConnectionListener}.
     *
     * @throws NullPointerException if {@code listener} is null.
     */
    public void addConnectionListener(ConnectionListener listener) {
        Objects.requireNonNull(listener, "listener");
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Dispatch an event to every registered listener. Structural changes to
     * the listener list made by a callback (add/remove) do not affect the
     * current dispatch pass — iteration runs over a snapshot. A throwing
     * listener is logged and skipped so one bad listener cannot starve the
     * others or corrupt the already-applied graph state.
     */
    private void fireConnectionEvent(ConnectionEvent.Kind kind, Connection connection) {
        if (connectionListeners.isEmpty()) {
            return;
        }
        ConnectionEvent event = new ConnectionEvent(kind, connection);
        for (ConnectionListener l : new ArrayList<>(connectionListeners)) {
            try {
                l.onConnectionEvent(event);
            } catch (RuntimeException ex) {
                System.err.println("[NodeGraph] ConnectionListener threw: " + ex);
                ex.printStackTrace(System.err);
            }
        }
    }
}
