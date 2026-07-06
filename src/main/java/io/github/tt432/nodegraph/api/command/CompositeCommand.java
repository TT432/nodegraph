package io.github.tt432.nodegraph.api.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A macro command composed of an ordered list of child commands.
 *
 * <p>{@link #execute()} runs children in order; {@link #undo()} runs them in
 * <b>reverse</b> order. {@link #redo()} defaults to {@link #execute()} (forward
 * order), as defined by {@link Command#redo()}.
 *
 * <p>Use this for atomic multi-step actions such as "combine selection into a
 * group" (one {@code CreateGroupCommand} + N {@code SetNodeGroupCommand}s) or
 * clipboard paste of multiple nodes.
 */
public final class CompositeCommand implements Command {
    private final String description;
    private final List<Command> children;

    public CompositeCommand(String description, List<Command> children) {
        this.description = Objects.requireNonNull(description, "description");
        this.children = new ArrayList<>(Objects.requireNonNull(children, "children"));
    }

    /** Convenience vararg constructor. */
    public CompositeCommand(String description, Command... children) {
        this(description, List.of(children));
    }

    public List<Command> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void execute() {
        for (Command c : children) {
            c.execute();
        }
    }

    @Override
    public void undo() {
        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).undo();
        }
    }

    @Override
    public String description() {
        return description;
    }
}
