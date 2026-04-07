package com.xptool.executor;

import net.runelite.api.coords.WorldPoint;

final class WoodcuttingRuntime {
    private long retryWindowUntilMs = 0L;
    private long outcomeWaitUntilMs = 0L;
    private WorldPoint lastAttemptWorldPoint = null;
    private long approachWaitUntilMs = 0L;
    private WorldPoint lastDispatchWorldPoint = null;
    private long lastDispatchAtMs = 0L;

    long retryWindowUntilMs() {
        return retryWindowUntilMs;
    }

    long outcomeWaitUntilMs() {
        return outcomeWaitUntilMs;
    }

    WorldPoint lastAttemptWorldPoint() {
        return lastAttemptWorldPoint;
    }

    long approachWaitUntilMs() {
        return approachWaitUntilMs;
    }

    WorldPoint lastDispatchWorldPoint() {
        return lastDispatchWorldPoint;
    }

    long lastDispatchAtMs() {
        return lastDispatchAtMs;
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

    void noteTargetAttempt(
        WorldPoint localWorldPoint,
        WorldPoint targetWorldPoint,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long outcomeWaitWindowMs
    ) {
        if (targetWorldPoint == null) {
            return;
        }
        lastAttemptWorldPoint = targetWorldPoint;
        long holdMs = computeApproachHoldMs(
            localWorldPoint,
            targetWorldPoint,
            approachBaseWaitMs,
            approachWaitPerTileMs,
            approachMaxWaitMs,
            outcomeWaitWindowMs
        );
        long holdUntil = System.currentTimeMillis() + holdMs;
        approachWaitUntilMs = Math.max(approachWaitUntilMs, holdUntil);
    }

    void clearTargetAttempt() {
        lastAttemptWorldPoint = null;
        approachWaitUntilMs = 0L;
    }

    void noteDispatchAttempt(WorldPoint targetWorldPoint, long now) {
        if (targetWorldPoint == null) {
            return;
        }
        lastDispatchWorldPoint = targetWorldPoint;
        lastDispatchAtMs = Math.max(0L, now);
    }

    void clearDispatchAttempt() {
        lastDispatchWorldPoint = null;
        lastDispatchAtMs = 0L;
    }

    void clearInteractionWindows() {
        clearInteractionWindowsPreserveDispatchSignal();
        clearDispatchAttempt();
    }

    void clearInteractionWindowsPreserveDispatchSignal() {
        clearInteractionWindowsCore();
    }

    private void clearInteractionWindowsCore() {
        retryWindowUntilMs = 0L;
        clearOutcomeWaitWindow();
        clearTargetAttempt();
    }

    private static long computeApproachHoldMs(
        WorldPoint localWorldPoint,
        WorldPoint targetWorldPoint,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long outcomeWaitWindowMs
    ) {
        int distance = 6;
        if (localWorldPoint != null && targetWorldPoint != null) {
            int dist = localWorldPoint.distanceTo(targetWorldPoint);
            if (dist >= 0) {
                distance = dist;
            }
        }
        long computed = approachBaseWaitMs + (Math.max(0, distance) * approachWaitPerTileMs);
        long minWindow = Math.max(0L, outcomeWaitWindowMs);
        return Math.max(minWindow, Math.min(approachMaxWaitMs, computed));
    }
}
