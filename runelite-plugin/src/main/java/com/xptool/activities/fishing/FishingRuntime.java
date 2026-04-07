package com.xptool.activities.fishing;

import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public final class FishingRuntime {
    private long retryWindowUntilMs = 0L;
    private long outcomeWaitUntilMs = 0L;
    private int lastAttemptNpcIndex = -1;
    private WorldPoint lastAttemptWorldPoint = null;
    private long approachWaitUntilMs = 0L;
    private int lastDispatchNpcIndex = -1;
    private long lastDispatchAtMs = 0L;
    private WorldPoint lastDispatchWorldPoint = null;

    public long retryWindowUntilMs() {
        return retryWindowUntilMs;
    }

    public long outcomeWaitUntilMs() {
        return outcomeWaitUntilMs;
    }

    public int lastAttemptNpcIndex() {
        return lastAttemptNpcIndex;
    }

    public WorldPoint lastAttemptWorldPoint() {
        return lastAttemptWorldPoint;
    }

    public long approachWaitUntilMs() {
        return approachWaitUntilMs;
    }

    public int lastDispatchNpcIndex() {
        return lastDispatchNpcIndex;
    }

    public long lastDispatchAtMs() {
        return lastDispatchAtMs;
    }

    public WorldPoint lastDispatchWorldPoint() {
        return lastDispatchWorldPoint;
    }

    public void extendRetryWindow(long durationMs) {
        long holdUntil = System.currentTimeMillis() + Math.max(0L, durationMs);
        retryWindowUntilMs = Math.max(retryWindowUntilMs, holdUntil);
    }

    public void beginOutcomeWaitWindow(long durationMs) {
        long holdUntil = System.currentTimeMillis() + Math.max(0L, durationMs);
        outcomeWaitUntilMs = Math.max(outcomeWaitUntilMs, holdUntil);
    }

    public void clearOutcomeWaitWindow() {
        outcomeWaitUntilMs = 0L;
    }

    public void noteTargetAttempt(
        Player local,
        NPC targetNpc,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long outcomeWaitWindowMs
    ) {
        if (targetNpc == null) {
            return;
        }
        lastAttemptNpcIndex = targetNpc.getIndex();
        lastAttemptWorldPoint = targetNpc.getWorldLocation();
        long holdMs = computeApproachHoldMs(
            local,
            targetNpc,
            approachBaseWaitMs,
            approachWaitPerTileMs,
            approachMaxWaitMs,
            outcomeWaitWindowMs
        );
        long holdUntil = System.currentTimeMillis() + holdMs;
        approachWaitUntilMs = Math.max(approachWaitUntilMs, holdUntil);
    }

    public void clearTargetAttempt() {
        lastAttemptNpcIndex = -1;
        lastAttemptWorldPoint = null;
        approachWaitUntilMs = 0L;
    }

    public void noteDispatchAttempt(NPC targetNpc, long now) {
        if (targetNpc == null) {
            return;
        }
        lastDispatchNpcIndex = targetNpc.getIndex();
        lastDispatchAtMs = Math.max(0L, now);
        lastDispatchWorldPoint = targetNpc.getWorldLocation();
    }

    public void clearDispatchAttempt() {
        lastDispatchNpcIndex = -1;
        lastDispatchAtMs = 0L;
        lastDispatchWorldPoint = null;
    }

    public void clearInteractionWindows() {
        clearInteractionWindowsPreserveDispatchSignal();
        clearDispatchAttempt();
    }

    public void clearInteractionWindowsPreserveDispatchSignal() {
        clearInteractionWindowsCore();
    }

    private void clearInteractionWindowsCore() {
        retryWindowUntilMs = 0L;
        clearOutcomeWaitWindow();
        clearTargetAttempt();
    }

    private static long computeApproachHoldMs(
        Player local,
        NPC targetNpc,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long outcomeWaitWindowMs
    ) {
        int distance = 6;
        WorldPoint localPos = local == null ? null : local.getWorldLocation();
        WorldPoint targetPos = targetNpc == null ? null : targetNpc.getWorldLocation();
        if (localPos != null && targetPos != null) {
            int dist = localPos.distanceTo(targetPos);
            if (dist >= 0) {
                distance = dist;
            }
        }
        long computed = approachBaseWaitMs + (Math.max(0, distance) * approachWaitPerTileMs);
        long minWindow = Math.max(0L, outcomeWaitWindowMs);
        return Math.max(minWindow, Math.min(approachMaxWaitMs, computed));
    }
}
