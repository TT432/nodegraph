package io.github.tt432.nodegraph.api.command;

/**
 * An invertible action performed on a {@link io.github.tt432.nodegraph.api.model.NodeGraph}.
 *
 * <p>Each command captures enough state to undo itself. {@link #execute()} must
 * be <b>repeatable</b>: after {@link #undo()}, calling {@code execute()} again
 * (as the default {@link #redo()}) must correctly re-apply the effect. The
 * recommended idiom is <i>lazy capture</i>: the first {@code execute()} call
 * allocates and stashes the created object (id, position, replaced connection,
 * etc.); subsequent calls reuse that stashed state via the low-level recovery
 * primitives on {@code NodeGraph}.
 *
 * <p>Commands rely on {@link UndoManager}'s <b>linear</b> undo sequence — they
 * do not defend against out-of-order undo/redo.
 */
public interface Command {
    /** Apply the effect. Must be safe to call again after {@link #undo()}. */
    void execute();

    /** Revert the effect of {@link #execute()}. */
    void undo();

    /** Re-apply after an {@link #undo()}. Default delegates to {@link #execute()}. */
    default void redo() {
        execute();
    }

    /** Short human-readable label for UI (e.g. "Move node"). */
    String description();
}
