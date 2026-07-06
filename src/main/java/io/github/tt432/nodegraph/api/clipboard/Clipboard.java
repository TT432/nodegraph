package io.github.tt432.nodegraph.api.clipboard;

import io.github.tt432.nodegraph.api.command.CompositeCommand;
import io.github.tt432.nodegraph.api.command.RemoveGroupCommand;
import io.github.tt432.nodegraph.api.command.RemoveNodeCommand;
import io.github.tt432.nodegraph.api.command.UndoManager;
import io.github.tt432.nodegraph.api.model.NodeGraph;
import io.github.tt432.nodegraph.api.model.NodeGroupId;
import io.github.tt432.nodegraph.api.model.NodeId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * In-process clipboard holding the most recent {@link SelectionSnapshot}.
 *
 * <ul>
 *   <li>{@link #copy} encodes the selection and stores it.</li>
 *   <li>{@link #cut} copies, then removes the selection as a single
 *       {@link CompositeCommand} of {@link RemoveNodeCommand}s and
 *       {@link RemoveGroupCommand}s (one undo unit).</li>
 *   <li>{@link #paste} decodes the stored snapshot into a {@link PasteCommand}
 *       and applies it via the {@link UndoManager}, returning the executed
 *       command so the caller can read {@link PasteCommand#pastedNodeIds()}.</li>
 * </ul>
 *
 * <p>Stateful single-slot clipboard: a new {@code copy}/{@code cut} overwrites
 * the previous content.
 */
public final class Clipboard {
    private SelectionSnapshot snapshot;

    /**
     * Encode the selection into this clipboard. An empty selection yields an
     * empty (but non-null) snapshot; {@link #hasContent()} returns true
     * afterwards regardless.
     */
    public void copy(NodeGraph graph, Collection<NodeId> nodes, Collection<NodeGroupId> groups) {
        this.snapshot = SelectionCodec.encode(graph, nodes, groups);
    }

    /**
     * Copy then remove the selection as a single undoable CompositeCommand.
     * No-op on an empty selection (nothing is pushed onto {@code undo}).
     */
    public void cut(NodeGraph graph, Collection<NodeId> nodes, Collection<NodeGroupId> groups, UndoManager undo) {
        Objects.requireNonNull(undo, "undo");
        copy(graph, nodes, groups);
        if (nodes.isEmpty() && groups.isEmpty()) {
            return;
        }
        List<io.github.tt432.nodegraph.api.command.Command> children = new ArrayList<>();
        for (NodeGroupId gid : groups) {
            children.add(new RemoveGroupCommand(graph, gid));
        }
        for (NodeId nid : nodes) {
            children.add(new RemoveNodeCommand(graph, nid));
        }
        undo.apply(new CompositeCommand("Cut", children));
    }

    /**
     * Apply the stored snapshot to {@code graph} via {@code undo}.
     *
     * @return the executed {@link PasteCommand}, or {@code null} if the
     *         clipboard is empty (snapshot was {@code null}); in the latter
     *         case nothing is pushed onto the undo stack.
     */
    public PasteCommand paste(NodeGraph graph, UndoManager undo, double offsetX, double offsetY) {
        Objects.requireNonNull(undo, "undo");
        if (snapshot == null) {
            return null;
        }
        PasteCommand cmd = SelectionCodec.decode(snapshot, graph, offsetX, offsetY);
        undo.apply(cmd);
        return cmd;
    }

    /** The stored snapshot, or {@code null} if nothing has been copied. */
    public SelectionSnapshot snapshot() {
        return snapshot;
    }

    /** True if {@link #copy}/{@code cut} has been called at least once. */
    public boolean hasContent() {
        return snapshot != null;
    }

    /** Drop the stored snapshot. */
    public void clear() {
        this.snapshot = null;
    }
}
