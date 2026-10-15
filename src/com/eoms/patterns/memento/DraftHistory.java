package com.eoms.patterns.memento;

import java.util.HashMap;
import java.util.Map;

/** Caretaker: named, in-memory draft versions for the current desktop session. */
public final class DraftHistory {
    private final Map<String, ExamDraft.Snapshot> versions = new HashMap<>();

    public void save(String name, ExamDraft draft) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Version name is required");
        versions.put(name.trim(), draft.capture());
    }

    public void restore(String name, ExamDraft draft) {
        ExamDraft.Snapshot version = versions.get(name);
        if (version == null) throw new IllegalArgumentException("Draft version not found: " + name);
        draft.restore(version);
    }
}
