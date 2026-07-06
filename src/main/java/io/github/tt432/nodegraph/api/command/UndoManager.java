package io.github.tt432.nodegraph.api.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Maintains the undo / redo stacks for a stream of {@link Command}s.
 *
 * <p>{@link #apply(Command)} executes the command, pushes it onto the undo
 * stack and clears the redo stack (new action forks a new branch). {@link #undo()}
 * pops the undo stack top, runs its {@link Command#undo()} and pushes onto the
 * redo stack. {@link #redo()} reverses that.
 *
 * <p>An {@code depthLimit} (default 100) trims the oldest entries from the
 * undo stack to bound memory in long sessions — once trimmed, those commands
 * are no longer undoable.
 *
 * <p>Listeners are notified after every state change ({@link #apply},
 * {@link #undo}, {@link #redo}, {@link #clear}) so the UI can refresh the
 * enabled state of undo/redo controls.
 */
public final class UndoManager {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int depthLimit = 100;

    public int getDepthLimit() {
        return depthLimit;
    }

    public void setDepthLimit(int depthLimit) {
        if (depthLimit <= 0) {
            throw new IllegalArgumentException("depthLimit must be > 0: " + depthLimit);
        }
        this.depthLimit = depthLimit;
        trimUndo();
        fireChanged();
    }

    /** Execute {@code cmd}, push it on the undo stack, clear the redo stack. */
    public void apply(Command cmd) {
        Objects.requireNonNull(cmd, "cmd");
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        trimUndo();
        fireChanged();
    }

    /** @return true if an undo was performed, false if the undo stack is empty. */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
        fireChanged();
        return true;
    }

    /** @return true if a redo was performed, false if the redo stack is empty. */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        Command c = redoStack.pop();
        c.redo();
        undoStack.push(c);
        trimUndo();
        fireChanged();
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** Empty both stacks. */
    public void clear() {
        if (undoStack.isEmpty() && redoStack.isEmpty()) {
            return;
        }
        undoStack.clear();
        redoStack.clear();
        fireChanged();
    }

    public int undoDepth() {
        return undoStack.size();
    }

    public int redoDepth() {
        return redoStack.size();
    }

    /** A short description of the next command to undo, or "" if none. */
    public String nextUndoDescription() {
        return undoStack.isEmpty() ? "" : undoStack.peek().description();
    }

    /** A short description of the next command to redo, or "" if none. */
    public String nextRedoDescription() {
        return redoStack.isEmpty() ? "" : redoStack.peek().description();
    }

    public void addListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void trimUndo() {
        while (undoStack.size() > depthLimit) {
            // ArrayDeque with push() puts newest at head; oldest is at tail.
            undoStack.removeLast();
        }
    }

    private void fireChanged() {
        for (Runnable l : listeners) {
            l.run();
        }
    }
}
