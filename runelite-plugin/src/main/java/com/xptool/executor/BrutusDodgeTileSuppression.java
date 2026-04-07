package com.xptool.executor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

final class BrutusDodgeTileSuppression {
    private final Map<WorldPoint, Long> suppressedUntilMs = new HashMap<>();

    void suppress(WorldPoint worldPoint, long durationMs) {
        if (worldPoint == null || durationMs <= 0L) {
            return;
        }
        suppressedUntilMs.put(worldPoint, System.currentTimeMillis() + durationMs);
    }

    boolean isSuppressed(WorldPoint worldPoint, long now) {
        if (worldPoint == null) {
            return false;
        }
        Long until = suppressedUntilMs.get(worldPoint);
        if (until == null) {
            return false;
        }
        if (now <= until) {
            return true;
        }
        suppressedUntilMs.remove(worldPoint);
        return false;
    }

    void prune(long now) {
        if (suppressedUntilMs.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<WorldPoint, Long>> it = suppressedUntilMs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<WorldPoint, Long> entry = it.next();
            if (entry == null || entry.getKey() == null || entry.getValue() == null || now > entry.getValue()) {
                it.remove();
            }
        }
    }
}
