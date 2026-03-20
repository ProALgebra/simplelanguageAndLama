package com.oracle.truffle.lama.runtime;

import java.util.HashMap;
import java.util.Map;

public final class LamaEnv {
    private final LamaEnv parent;
    private final Map<String, LamaCell> cells = new HashMap<>();

    public LamaEnv(LamaEnv parent) {
        this.parent = parent;
    }

    public LamaEnv getParent() {
        return parent;
    }

    public LamaCell declare(String name, boolean mutable, Object initialValue) {
        LamaCell existing = cells.get(name);
        if (existing != null) {
            throw new RuntimeException("Name already declared in this scope: " + name);
        }
        LamaCell cell = new LamaCell(mutable, initialValue);
        cells.put(name, cell);
        return cell;
    }

    public LamaCell lookupCellLocal(String name) {
        return cells.get(name);
    }

    public LamaEnv snapshotForClosure() {
        if (parent == null) {
            // Share the global environment so that top-level definitions observe later global declarations.
            return this;
        }
        LamaEnv parentSnapshot = parent == null ? null : parent.snapshotForClosure();
        LamaEnv snapshot = new LamaEnv(parentSnapshot);
        for (var entry : cells.entrySet()) {
            LamaCell cell = entry.getValue();
            LamaCell copied = cell.isMutable() ? new LamaCell(true, cell.get()) : cell;
            snapshot.cells.put(entry.getKey(), copied);
        }
        return snapshot;
    }

    public LamaCell lookupCell(String name) {
        LamaCell cell = cells.get(name);
        if (cell != null) {
            return cell;
        }
        if (parent != null) {
            return parent.lookupCell(name);
        }
        throw new RuntimeException("Undefined name: " + name);
    }
}
