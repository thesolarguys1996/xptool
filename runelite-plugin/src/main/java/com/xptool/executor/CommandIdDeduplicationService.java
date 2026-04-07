package com.xptool.executor;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

final class CommandIdDeduplicationService {
    private final Set<String> seenCommandIds = new LinkedHashSet<>();
    private final int maxSeenCommandIds;

    CommandIdDeduplicationService(int maxSeenCommandIds) {
        this.maxSeenCommandIds = Math.max(1, maxSeenCommandIds);
    }

    boolean isDuplicateCommandId(String commandId) {
        synchronized (seenCommandIds) {
            if (seenCommandIds.contains(commandId)) {
                return true;
            }
            rememberCommandId(commandId);
            return false;
        }
    }

    void clearSeenCommandIds() {
        synchronized (seenCommandIds) {
            seenCommandIds.clear();
        }
    }

    private void rememberCommandId(String commandId) {
        seenCommandIds.add(commandId);
        if (seenCommandIds.size() <= maxSeenCommandIds) {
            return;
        }
        Iterator<String> it = seenCommandIds.iterator();
        while (seenCommandIds.size() > maxSeenCommandIds && it.hasNext()) {
            it.next();
            it.remove();
        }
    }
}
