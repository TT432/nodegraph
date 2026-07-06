package io.github.tt432.nodegraph.api.command;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TestUndoManager {

    /** A trivial command that increments / decrements a counter. */
    private static final class IncCommand implements Command {
        final AtomicInteger target;
        final int delta;
        IncCommand(AtomicInteger target, int delta) {
            this.target = target;
            this.delta = delta;
        }
        @Override public void execute() { target.addAndGet(delta); }
        @Override public void undo() { target.addAndGet(-delta); }
        @Override public String description() { return "inc " + delta; }
    }

    @Test
    void applyUndoRedoBasicCycle() {
        UndoManager um = new UndoManager();
        AtomicInteger n = new AtomicInteger(0);

        assertFalse(um.canUndo());
        assertFalse(um.canRedo());

        um.apply(new IncCommand(n, 5));
        assertEquals(5, n.get());
        assertTrue(um.canUndo());
        assertFalse(um.canRedo());

        assertTrue(um.undo());
        assertEquals(0, n.get());
        assertFalse(um.canUndo());
        assertTrue(um.canRedo());

        assertTrue(um.redo());
        assertEquals(5, n.get());
        assertTrue(um.canUndo());
        assertFalse(um.canRedo());
    }

    @Test
    void multipleCommandsUndoRedoInLifo() {
        UndoManager um = new UndoManager();
        AtomicInteger n = new AtomicInteger(0);
        um.apply(new IncCommand(n, 1));
        um.apply(new IncCommand(n, 10));
        um.apply(new IncCommand(n, 100));
        assertEquals(111, n.get());

        um.undo(); // undo +100
        assertEquals(11, n.get());
        um.undo(); // undo +10
        assertEquals(1, n.get());
        um.undo(); // undo +1
        assertEquals(0, n.get());
        assertFalse(um.canUndo());

        um.redo(); // redo +1
        assertEquals(1, n.get());
        um.redo(); // redo +10
        assertEquals(11, n.get());
        um.redo(); // redo +100
        assertEquals(111, n.get());
        assertFalse(um.canRedo());
    }

    @Test
    void newApplyClearsRedoStack() {
        UndoManager um = new UndoManager();
        AtomicInteger n = new AtomicInteger(0);
        um.apply(new IncCommand(n, 1));
        um.apply(new IncCommand(n, 2));
        um.undo(); // redo stack has +2
        assertEquals(1, n.get());
        assertTrue(um.canRedo());

        // new branch
        um.apply(new IncCommand(n, 1000));
        assertEquals(1001, n.get());
        assertFalse(um.canRedo(), "redo stack must be cleared by new apply");
        assertEquals(2, um.undoDepth(), "undo stack has +1 and +1000");
    }

    @Test
    void clearEmptiesBothStacks() {
        UndoManager um = new UndoManager();
        AtomicInteger n = new AtomicInteger(0);
        um.apply(new IncCommand(n, 1));
        um.apply(new IncCommand(n, 2));
        um.undo();

        um.clear();
        assertEquals(0, um.undoDepth());
        assertEquals(0, um.redoDepth());
        assertFalse(um.canUndo());
        assertFalse(um.canRedo());
    }

    @Test
    void depthLimitTrimsOldestUndoable() {
        UndoManager um = new UndoManager();
        um.setDepthLimit(5);
        AtomicInteger n = new AtomicInteger(0);
        for (int i = 0; i < 8; i++) {
            um.apply(new IncCommand(n, 1));
        }
        assertEquals(8, n.get());
        assertEquals(5, um.undoDepth(), "undo stack should be trimmed to limit");

        // Undoing 5 times should work; the 6th should fail (oldest 3 are gone).
        for (int i = 0; i < 5; i++) {
            assertTrue(um.undo());
        }
        assertEquals(3, n.get(), "only the 5 newest could be undone");
        assertFalse(um.undo());
    }

    @Test
    void listenersNotifiedForEachStateChange() {
        UndoManager um = new UndoManager();
        AtomicInteger n = new AtomicInteger(0);
        List<String> events = new ArrayList<>();
        um.addListener(() -> events.add("changed"));

        um.apply(new IncCommand(n, 1));
        um.undo();
        um.redo();
        um.clear();

        assertEquals(4, events.size(), "one notification per apply/undo/redo/clear");
    }

    @Test
    void listenerCanBeRemoved() {
        UndoManager um = new UndoManager();
        AtomicInteger n = new AtomicInteger(0);
        AtomicInteger events = new AtomicInteger(0);
        Runnable l = events::incrementAndGet;
        um.addListener(l);
        um.apply(new IncCommand(n, 1));
        assertEquals(1, events.get());
        um.removeListener(l);
        um.undo();
        assertEquals(1, events.get(), "no further notifications after removal");
    }

    @Test
    void emptyStackUndoAndRedoReturnFalse() {
        UndoManager um = new UndoManager();
        assertFalse(um.undo());
        assertFalse(um.redo());
        // nothing throws
    }

    @Test
    void descriptionsExposeNextUndoAndRedo() {
        UndoManager um = new UndoManager();
        AtomicInteger n = new AtomicInteger(0);
        assertEquals("", um.nextUndoDescription());
        assertEquals("", um.nextRedoDescription());

        um.apply(new IncCommand(n, 1));
        assertEquals("inc 1", um.nextUndoDescription());
        assertEquals("", um.nextRedoDescription());

        um.undo();
        assertEquals("", um.nextUndoDescription());
        assertEquals("inc 1", um.nextRedoDescription());
    }

    @Test
    void setDepthLimitRejectsZeroOrNegative() {
        UndoManager um = new UndoManager();
        assertThrows(IllegalArgumentException.class, () -> um.setDepthLimit(0));
        assertThrows(IllegalArgumentException.class, () -> um.setDepthLimit(-1));
    }
}
