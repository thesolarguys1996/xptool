package com.xptool.executor;

import java.util.concurrent.ThreadLocalRandom;

final class FishingDispatchConfirmationGate {
    private static final long STALE_DISPATCH_RESET_MS = 16000L;
    private static final int LONG_DISTANCE_TILES = 6;
    private static final int MID_DISTANCE_TILES = 4;
    private static final int NEAR_DISTANCE_TILES = 2;
    // Long-walk retries should be governed by long-move/retry gates, not confirmation hold.
    private static final int CONFIRMATION_ARM_MAX_DISTANCE_TILES = MID_DISTANCE_TILES;
    private static final long LONG_HOLD_MIN_MS = 5200L;
    private static final long LONG_HOLD_MAX_MS = 7600L;
    private static final long MID_HOLD_MIN_MS = 3600L;
    private static final long MID_HOLD_MAX_MS = 5400L;
    private static final long NEAR_HOLD_MIN_MS = 2200L;
    private static final long NEAR_HOLD_MAX_MS = 3600L;
    private static final long FIRST_RETRY_NEAR_EXTRA_MIN_MS = 900L;
    private static final long FIRST_RETRY_NEAR_EXTRA_MAX_MS = 1600L;
    private static final long UNKNOWN_DISTANCE_HOLD_MIN_MS = 4200L;
    private static final long UNKNOWN_DISTANCE_HOLD_MAX_MS = 6200L;
    private static final long RETRY_STREAK_STEP_MS = 900L;
    private static final long RETRY_STREAK_CAP_MS = 2800L;
    private static final int FATIGUE_EXTRA_MAX_MS = 220;
    private static final long MAX_HOLD_CAP_MS = 9800L;

    private long observedDispatchAtMs = 0L;
    private int observedDispatchNpcIndex = -1;
    private long confirmationHoldUntilMs = 0L;

    void noteAnimationActive() {
        clear();
    }

    Decision maybeDeferUntilConfirmed(
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
        if (lastDispatchAtMs <= 0L || (nowMs - lastDispatchAtMs) > STALE_DISPATCH_RESET_MS) {
            clear();
            return Decision.proceed();
        }
        if (lastDispatchAtMs != observedDispatchAtMs || lastDispatchNpcIndex != observedDispatchNpcIndex) {
            observedDispatchAtMs = lastDispatchAtMs;
            observedDispatchNpcIndex = lastDispatchNpcIndex;
            if (shouldArmForDistance(localDistanceToTarget)) {
                long holdMs = computeHoldMs(localDistanceToTarget, noAnimationRetryStreak, fatigue);
                confirmationHoldUntilMs = Math.min(lastDispatchAtMs + holdMs, lastDispatchAtMs + MAX_HOLD_CAP_MS);
            } else {
                confirmationHoldUntilMs = 0L;
            }
        }
        if (!sameDispatchTarget) {
            return Decision.proceed();
        }
        if (confirmationHoldUntilMs > nowMs) {
            long effectiveHoldMs = 0L;
            if (observedDispatchAtMs > 0L && confirmationHoldUntilMs > observedDispatchAtMs) {
                effectiveHoldMs = confirmationHoldUntilMs - observedDispatchAtMs;
            }
            return Decision.defer(
                Math.max(1L, confirmationHoldUntilMs - nowMs),
                Math.max(-1, localDistanceToTarget),
                Math.max(0, noAnimationRetryStreak),
                effectiveHoldMs,
                distanceBucketFor(localDistanceToTarget)
            );
        }
        return Decision.proceed();
    }

    private static long computeHoldMs(int localDistanceToTarget, int noAnimationRetryStreak, FatigueSnapshot fatigue) {
        long minMs;
        long maxMs;
        if (localDistanceToTarget < 0) {
            minMs = UNKNOWN_DISTANCE_HOLD_MIN_MS;
            maxMs = UNKNOWN_DISTANCE_HOLD_MAX_MS;
        } else if (localDistanceToTarget >= LONG_DISTANCE_TILES) {
            minMs = LONG_HOLD_MIN_MS;
            maxMs = LONG_HOLD_MAX_MS;
        } else if (localDistanceToTarget >= MID_DISTANCE_TILES) {
            minMs = MID_HOLD_MIN_MS;
            maxMs = MID_HOLD_MAX_MS;
        } else {
            minMs = NEAR_HOLD_MIN_MS;
            maxMs = NEAR_HOLD_MAX_MS;
        }
        long holdMs = randomLongInclusive(minMs, maxMs);
        if (noAnimationRetryStreak <= 1
            && localDistanceToTarget >= 0
            && localDistanceToTarget <= NEAR_DISTANCE_TILES) {
            holdMs += randomLongInclusive(FIRST_RETRY_NEAR_EXTRA_MIN_MS, FIRST_RETRY_NEAR_EXTRA_MAX_MS);
        }
        if (noAnimationRetryStreak > 0) {
            long streakExtra = Math.min(
                RETRY_STREAK_CAP_MS,
                (long) noAnimationRetryStreak * RETRY_STREAK_STEP_MS
            );
            holdMs += streakExtra;
        }
        int fatigueExtra = fatigue == null ? 0 : fatigue.fishingReclickCooldownBiasMs(FATIGUE_EXTRA_MAX_MS);
        holdMs += Math.max(0L, fatigueExtra);
        holdMs = Math.max(1500L, holdMs + randomLongInclusive(-90L, 140L));
        return Math.min(MAX_HOLD_CAP_MS, holdMs);
    }

    private static boolean shouldArmForDistance(int localDistanceToTarget) {
        if (localDistanceToTarget < 0) {
            return true;
        }
        return localDistanceToTarget <= CONFIRMATION_ARM_MAX_DISTANCE_TILES;
    }

    private void clear() {
        observedDispatchAtMs = 0L;
        observedDispatchNpcIndex = -1;
        confirmationHoldUntilMs = 0L;
    }

    private static String distanceBucketFor(int localDistanceToTarget) {
        if (localDistanceToTarget < 0) {
            return "unknown";
        }
        if (localDistanceToTarget >= LONG_DISTANCE_TILES) {
            return "long";
        }
        if (localDistanceToTarget >= MID_DISTANCE_TILES) {
            return "mid";
        }
        return "near";
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
        final int localDistanceToTarget;
        final int noAnimationRetryStreak;
        final long effectiveHoldMs;
        final String distanceBucket;

        private Decision(
            boolean defer,
            long waitMsRemaining,
            int localDistanceToTarget,
            int noAnimationRetryStreak,
            long effectiveHoldMs,
            String distanceBucket
        ) {
            this.defer = defer;
            this.waitMsRemaining = Math.max(0L, waitMsRemaining);
            this.localDistanceToTarget = localDistanceToTarget;
            this.noAnimationRetryStreak = Math.max(0, noAnimationRetryStreak);
            this.effectiveHoldMs = Math.max(0L, effectiveHoldMs);
            this.distanceBucket = distanceBucket == null ? "unknown" : distanceBucket;
        }

        static Decision proceed() {
            return new Decision(false, 0L, -1, 0, 0L, "unknown");
        }

        static Decision defer(
            long waitMsRemaining,
            int localDistanceToTarget,
            int noAnimationRetryStreak,
            long effectiveHoldMs,
            String distanceBucket
        ) {
            return new Decision(
                true,
                waitMsRemaining,
                localDistanceToTarget,
                noAnimationRetryStreak,
                effectiveHoldMs,
                distanceBucket
            );
        }
    }
}
