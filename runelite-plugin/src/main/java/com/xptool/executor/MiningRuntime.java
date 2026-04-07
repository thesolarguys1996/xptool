package com.xptool.executor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

final class MiningRuntime {
    private long retryWindowUntilMs = 0L;
    private long outcomeWaitUntilMs = 0L;
    private final Map<WorldPoint, Long> rockSuppressedUntilMs = new HashMap<>();

    long retryWindowUntilMs() {
        return retryWindowUntilMs;
    }

    long outcomeWaitUntilMs() {
        return outcomeWaitUntilMs;
    }

    void extendRetryWindow(long durationMs) {
        long holdUntil = System.currentTimeMillis() + Math.max(0L, durationMs);
        retryWindowUntilMs = Math.max(retryWindowUntilMs, holdUntil);
    }

    void beginOutcomeWaitWindow(long durationMs) {
        long holdUntil = System.currentTimeMillis() + Math.max(0L, durationMs);
        outcomeWaitUntilMs = Math.max(outcomeWaitUntilMs, holdUntil);
    }

    void clearOutcomeWaitWindow() {
        outcomeWaitUntilMs = 0L;
    }

    void clearInteractionWindows() {
        retryWindowUntilMs = 0L;
        clearOutcomeWaitWindow();
    }

    void suppressRockTarget(WorldPoint worldPoint, long durationMs) {
        if (worldPoint == null || durationMs <= 0L) {
            return;
        }
        long until = System.currentTimeMillis() + durationMs;
        rockSuppressedUntilMs.put(worldPoint, until);
    }

    boolean isRockSuppressed(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return false;
        }
        Long until = rockSuppressedUntilMs.get(worldPoint);
        if (until == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= until) {
            rockSuppressedUntilMs.remove(worldPoint);
            return false;
        }
        return true;
    }

    void pruneRockSuppression() {
        if (rockSuppressedUntilMs.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<WorldPoint, Long>> it = rockSuppressedUntilMs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<WorldPoint, Long> entry = it.next();
            if (entry == null) {
                continue;
            }
            Long until = entry.getValue();
            if (until == null || now >= until) {
                it.remove();
            }
        }
    }
}
