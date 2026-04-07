package com.xptool.executor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.runelite.api.NPC;

final class CombatRuntime {
    private long retryWindowUntilMs = 0L;
    private long outcomeWaitUntilMs = 0L;
    private final Map<Integer, Long> npcSuppressedUntilMs = new HashMap<>();
    private int lastAttemptNpcIndex = -1;
    private long recenterCooldownUntilMs = 0L;
    private volatile int boundaryCenterX = -1;
    private volatile int boundaryCenterY = -1;
    private volatile int boundaryRadiusTiles = -1;
    private int targetUnavailableStreak = 0;

    long retryWindowUntilMs() {
        return retryWindowUntilMs;
    }

    long outcomeWaitUntilMs() {
        return outcomeWaitUntilMs;
    }

    int lastAttemptNpcIndex() {
        return lastAttemptNpcIndex;
    }

    int suppressedNpcCount() {
        return npcSuppressedUntilMs.size();
    }

    long recenterCooldownUntilMs() {
        return recenterCooldownUntilMs;
    }

    void setRecenterCooldownUntilMs(long cooldownUntilMs) {
        recenterCooldownUntilMs = Math.max(0L, cooldownUntilMs);
    }

    boolean hasBoundary() {
        return boundaryCenterX > 0 && boundaryCenterY > 0 && boundaryRadiusTiles > 0;
    }

    int boundaryCenterX() {
        return boundaryCenterX;
    }

    int boundaryCenterY() {
        return boundaryCenterY;
    }

    int boundaryRadiusTiles() {
        return boundaryRadiusTiles;
    }

    void updateBoundary(int centerX, int centerY, int radiusTiles) {
        if (centerX > 0 && centerY > 0) {
            boundaryCenterX = centerX;
            boundaryCenterY = centerY;
            boundaryRadiusTiles = Math.max(1, radiusTiles);
            return;
        }
        boundaryCenterX = -1;
        boundaryCenterY = -1;
        boundaryRadiusTiles = -1;
    }

    int targetUnavailableStreak() {
        return targetUnavailableStreak;
    }

    void incrementTargetUnavailableStreak() {
        targetUnavailableStreak++;
    }

    void resetTargetUnavailableStreak() {
        targetUnavailableStreak = 0;
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
        clearTargetAttempt();
    }

    void noteTargetAttempt(NPC npc) {
        if (npc == null || npc.getIndex() < 0) {
            return;
        }
        lastAttemptNpcIndex = npc.getIndex();
    }

    void clearTargetAttempt() {
        lastAttemptNpcIndex = -1;
    }

    boolean isPostOutcomeSettleGraceActive(long now, long settleGraceMs) {
        if (lastAttemptNpcIndex < 0 || outcomeWaitUntilMs <= 0L) {
            return false;
        }
        if (now <= outcomeWaitUntilMs) {
            return false;
        }
        long elapsedMs = now - outcomeWaitUntilMs;
        return elapsedMs >= 0L && elapsedMs < Math.max(0L, settleGraceMs);
    }

    void suppressNpcTarget(int npcIndex, long durationMs) {
        if (npcIndex < 0 || durationMs <= 0L) {
            return;
        }
        long until = System.currentTimeMillis() + durationMs;
        npcSuppressedUntilMs.put(npcIndex, until);
    }

    boolean isNpcSuppressed(int npcIndex) {
        if (npcIndex < 0) {
            return false;
        }
        Long until = npcSuppressedUntilMs.get(npcIndex);
        if (until == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= until) {
            npcSuppressedUntilMs.remove(npcIndex);
            return false;
        }
        return true;
    }

    void pruneNpcSuppression() {
        if (npcSuppressedUntilMs.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> it = npcSuppressedUntilMs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
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
