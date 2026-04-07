package com.xptool.executor;

import java.util.concurrent.ThreadLocalRandom;

final class FishingLongMoveGate {
    private static final long DISPATCH_SIGNAL_STALE_RESET_MS = 12000L;
    private static final int LONG_MOVE_DISTANCE_TILES = 8;
    private static final int RETARGET_MOVE_DISTANCE_TILES = 4;
    private static final int ARRIVAL_DISTANCE_TILES = 2;
    private static final int SAME_TARGET_RETRY_STREAK_LONG_MOVE_MIN = 2;
    private static final long BASE_HOLD_MIN_MS = 950L;
    private static final long BASE_HOLD_MAX_MS = 1650L;
    private static final long HOLD_PER_TILE_MIN_MS = 170L;
    private static final long HOLD_PER_TILE_MAX_MS = 300L;
    private static final long HOLD_RETRY_STREAK_STEP_MS = 1200L;
    private static final long HOLD_RETRY_STREAK_CAP_MS = 4200L;
    private static final int HOLD_DISTANCE_STEPS_MAX = 10;
    private static final long MAX_TOTAL_HOLD_MS = 7800L;
    private static final long EXTEND_COOLDOWN_MS = 240L;
    private static final long EXTEND_CHUNK_MIN_MS = 120L;
    private static final long EXTEND_CHUNK_MAX_MS = 280L;
    private static final long EXTEND_TOTAL_MAX_MS = 1900L;
    private static final int FATIGUE_HOLD_EXTRA_MAX_MS = 180;

    private long activeHoldUntilMs = 0L;
    private long activeHoldHardMaxUntilMs = 0L;
    private long activeDispatchAtMs = 0L;
    private int activeDispatchNpcIndex = -1;
    private int lastObservedDistanceTiles = -1;
    private long lastExtensionAtMs = 0L;
    private long totalExtensionMs = 0L;

    void noteAnimationActive() {
        clear();
    }

    Decision maybeDeferForLongMove(
        long nowMs,
        long lastDispatchAtMs,
        int lastDispatchNpcIndex,
        int targetNpcIndex,
        boolean sameDispatchTarget,
        int localDistanceToTarget,
        int noAnimationRetryStreak,
        FatigueSnapshot fatigue
    ) {
        if (nowMs <= 0L) {
            return Decision.proceed();
        }
        if (lastDispatchAtMs <= 0L || (nowMs - lastDispatchAtMs) > DISPATCH_SIGNAL_STALE_RESET_MS) {
            clear();
            return Decision.proceed();
        }
        boolean newlyArmed = false;
        boolean extended = false;
        if (dispatchChanged(lastDispatchAtMs, lastDispatchNpcIndex)) {
            clear();
            activeDispatchAtMs = lastDispatchAtMs;
            activeDispatchNpcIndex = lastDispatchNpcIndex;
            if (shouldArm(
                localDistanceToTarget,
                sameDispatchTarget,
                targetNpcIndex,
                lastDispatchNpcIndex,
                noAnimationRetryStreak
            )) {
                long holdMs = computeBaseHoldMs(localDistanceToTarget, noAnimationRetryStreak, fatigue);
                activeHoldUntilMs = nowMs + holdMs;
                activeHoldHardMaxUntilMs = lastDispatchAtMs + MAX_TOTAL_HOLD_MS;
                activeHoldUntilMs = Math.min(activeHoldUntilMs, activeHoldHardMaxUntilMs);
                lastObservedDistanceTiles = Math.max(-1, localDistanceToTarget);
                lastExtensionAtMs = nowMs;
                totalExtensionMs = 0L;
                newlyArmed = activeHoldUntilMs > nowMs;
            } else {
                return Decision.proceed();
            }
        }
        if (activeHoldUntilMs <= nowMs) {
            return Decision.proceed();
        }
        if (localDistanceToTarget >= 0 && localDistanceToTarget <= ARRIVAL_DISTANCE_TILES) {
            // Keep the current long-move hold active through near-arrival distance
            // so we don't suddenly release into rapid retry clicks around the target.
            lastObservedDistanceTiles = localDistanceToTarget;
            return Decision.defer(
                Math.max(1L, activeHoldUntilMs - nowMs),
                newlyArmed,
                false,
                localDistanceToTarget,
                totalExtensionMs
            );
        }
        if (shouldExtendForProgress(nowMs, localDistanceToTarget)) {
            long remainingExtensionBudgetMs = Math.max(0L, EXTEND_TOTAL_MAX_MS - totalExtensionMs);
            if (remainingExtensionBudgetMs > 0L) {
                long extensionMs = randomLongInclusive(EXTEND_CHUNK_MIN_MS, EXTEND_CHUNK_MAX_MS);
                extensionMs = Math.min(extensionMs, remainingExtensionBudgetMs);
                int fatigueExtraMs = fatigue == null ? 0 : fatigue.fishingReclickCooldownBiasMs(80);
                extensionMs += Math.max(0L, fatigueExtraMs);
                extensionMs = Math.max(60L, extensionMs + randomLongInclusive(-20L, 35L));
                long extendedUntilMs = Math.min(activeHoldHardMaxUntilMs, activeHoldUntilMs + extensionMs);
                if (extendedUntilMs > activeHoldUntilMs) {
                    totalExtensionMs += (extendedUntilMs - activeHoldUntilMs);
                    activeHoldUntilMs = extendedUntilMs;
                    lastExtensionAtMs = nowMs;
                    extended = true;
                }
            }
        }
        if (localDistanceToTarget >= 0) {
            lastObservedDistanceTiles = localDistanceToTarget;
        }
        if (activeHoldUntilMs > nowMs) {
            return Decision.defer(
                Math.max(1L, activeHoldUntilMs - nowMs),
                newlyArmed,
                extended,
                Math.max(0, localDistanceToTarget),
                totalExtensionMs
            );
        }
        return Decision.proceed();
    }

    private boolean dispatchChanged(long dispatchAtMs, int dispatchNpcIndex) {
        return dispatchAtMs != activeDispatchAtMs || dispatchNpcIndex != activeDispatchNpcIndex;
    }

    private static boolean shouldArm(
        int localDistanceToTarget,
        boolean sameDispatchTarget,
        int targetNpcIndex,
        int lastDispatchNpcIndex,
        int noAnimationRetryStreak
    ) {
        if (localDistanceToTarget < 0) {
            return false;
        }
        // Same-target retry streaks should not arm long-move hold below true long-distance travel.
        // This avoids premature retry loops while the player is still closing a short/mid gap.
        if (sameDispatchTarget
            && noAnimationRetryStreak >= SAME_TARGET_RETRY_STREAK_LONG_MOVE_MIN
            && localDistanceToTarget < LONG_MOVE_DISTANCE_TILES) {
            return false;
        }
        if (localDistanceToTarget >= LONG_MOVE_DISTANCE_TILES) {
            return true;
        }
        boolean targetChanged = targetNpcIndex >= 0 && lastDispatchNpcIndex >= 0 && targetNpcIndex != lastDispatchNpcIndex;
        return targetChanged && !sameDispatchTarget && localDistanceToTarget >= RETARGET_MOVE_DISTANCE_TILES;
    }

    private static long computeBaseHoldMs(int localDistanceToTarget, int noAnimationRetryStreak, FatigueSnapshot fatigue) {
        int effectiveDistance = localDistanceToTarget < 0
            ? (LONG_MOVE_DISTANCE_TILES + 2)
            : localDistanceToTarget;
        int distanceSteps = Math.max(0, Math.min(HOLD_DISTANCE_STEPS_MAX, effectiveDistance - LONG_MOVE_DISTANCE_TILES));
        long holdMs = randomLongInclusive(BASE_HOLD_MIN_MS, BASE_HOLD_MAX_MS);
        for (int idx = 0; idx < distanceSteps; idx++) {
            holdMs += randomLongInclusive(HOLD_PER_TILE_MIN_MS, HOLD_PER_TILE_MAX_MS);
        }
        if (noAnimationRetryStreak > 0) {
            long streakExtra = (long) noAnimationRetryStreak * HOLD_RETRY_STREAK_STEP_MS;
            holdMs += Math.min(HOLD_RETRY_STREAK_CAP_MS, streakExtra);
        }
        int fatigueExtraMs = fatigue == null ? 0 : fatigue.fishingReclickCooldownBiasMs(FATIGUE_HOLD_EXTRA_MAX_MS);
        holdMs += Math.max(0L, fatigueExtraMs);
        holdMs = Math.max(700L, holdMs + randomLongInclusive(-70L, 110L));
        return Math.min(MAX_TOTAL_HOLD_MS, holdMs);
    }

    private boolean shouldExtendForProgress(long nowMs, int localDistanceToTarget) {
        if (activeHoldUntilMs <= nowMs || localDistanceToTarget < 0) {
            return false;
        }
        if ((nowMs - lastExtensionAtMs) < EXTEND_COOLDOWN_MS) {
            return false;
        }
        if (lastObservedDistanceTiles < 0) {
            return false;
        }
        return localDistanceToTarget < lastObservedDistanceTiles;
    }

    private void clear() {
        activeHoldUntilMs = 0L;
        activeHoldHardMaxUntilMs = 0L;
        activeDispatchAtMs = 0L;
        activeDispatchNpcIndex = -1;
        lastObservedDistanceTiles = -1;
        lastExtensionAtMs = 0L;
        totalExtensionMs = 0L;
    }

    private static long randomLongInclusive(long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    static final class Decision {
        final boolean defer;
        final long waitMsRemaining;
        final boolean newlyArmed;
        final boolean extended;
        final int localDistanceToTarget;
        final long extensionTotalMs;

        private Decision(
            boolean defer,
            long waitMsRemaining,
            boolean newlyArmed,
            boolean extended,
            int localDistanceToTarget,
            long extensionTotalMs
        ) {
            this.defer = defer;
            this.waitMsRemaining = Math.max(0L, waitMsRemaining);
            this.newlyArmed = newlyArmed;
            this.extended = extended;
            this.localDistanceToTarget = Math.max(0, localDistanceToTarget);
            this.extensionTotalMs = Math.max(0L, extensionTotalMs);
        }

        static Decision proceed() {
            return new Decision(false, 0L, false, false, 0, 0L);
        }

        static Decision defer(
            long waitMsRemaining,
            boolean newlyArmed,
            boolean extended,
            int localDistanceToTarget,
            long extensionTotalMs
        ) {
            return new Decision(
                true,
                waitMsRemaining,
                newlyArmed,
                extended,
                localDistanceToTarget,
                extensionTotalMs
            );
        }
    }
}
