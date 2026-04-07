package com.xptool.executor;

import java.util.concurrent.ThreadLocalRandom;

final class FishingDispatchRetryGate {
    private static final long DISPATCH_SIGNAL_STALE_RESET_MS = 16000L;
    private static final long BASE_SETTLE_WAIT_MIN_MS = 1700L;
    private static final long BASE_SETTLE_WAIT_MAX_MS = 3000L;
    private static final long RETRY_STREAK_EXTRA_WAIT_MIN_MS = 420L;
    private static final long RETRY_STREAK_EXTRA_WAIT_MAX_MS = 900L;
    private static final int RETRY_STREAK_EXTRA_CAP = 2;
    private static final int RETRY_SOFTCAP_START_STREAK = 3;
    private static final long RETRY_SOFTCAP_BACKOFF_MIN_MS = 700L;
    private static final long RETRY_SOFTCAP_BACKOFF_MAX_MS = 1800L;
    private static final int FATIGUE_SETTLE_EXTRA_MAX_MS = 360;
    private static final int DEFER_SOFTCAP_START_COUNT = 2;
    private static final int DEFER_SOFTCAP_BASE_RELEASE_CHANCE_PERCENT = 34;
    private static final int DEFER_SOFTCAP_RELEASE_CHANCE_STEP_PERCENT = 20;
    private static final int DEFER_SOFTCAP_RELEASE_CHANCE_MAX_PERCENT = 94;
    private static final int DEFER_SOFTCAP_FORCE_RELEASE_COUNT = 5;
    private static final long SOFT_RELEASE_HARD_MIN_FROM_DISPATCH_MS = 950L;
    private static final int SOFT_RELEASE_FATIGUE_HARD_MIN_EXTRA_MAX_MS = 120;

    private long settleWaitUntilMs = 0L;
    private long lastObservedDispatchAtMs = 0L;
    private int noAnimationRetryStreak = 0;
    private long deferSoftcapWindowUntilMs = 0L;
    private int consecutiveDefersInWindow = 0;

    void noteAnimationActive(long nowMs, long lastDispatchAtMs) {
        if (nowMs <= 0L) {
            return;
        }
        noAnimationRetryStreak = 0;
        settleWaitUntilMs = 0L;
        resetDeferSoftcapTracking();
        if (lastDispatchAtMs > 0L) {
            lastObservedDispatchAtMs = lastDispatchAtMs;
        }
    }

    Decision maybeDeferAfterDispatch(long nowMs, long lastDispatchAtMs, FatigueSnapshot fatigue) {
        if (nowMs <= 0L) {
            return Decision.proceed(0, false);
        }
        if (lastDispatchAtMs <= 0L || (nowMs - lastDispatchAtMs) > DISPATCH_SIGNAL_STALE_RESET_MS) {
            resetState();
            return Decision.proceed(0, false);
        }
        boolean newlyArmed = false;
        boolean softcapActive = false;
        if (lastDispatchAtMs != lastObservedDispatchAtMs) {
            newlyArmed = true;
            lastObservedDispatchAtMs = lastDispatchAtMs;
            noAnimationRetryStreak = Math.min(Integer.MAX_VALUE, noAnimationRetryStreak + 1);
            softcapActive = noAnimationRetryStreak >= RETRY_SOFTCAP_START_STREAK;
            long holdMs = computeSettleHoldMs(lastDispatchAtMs, noAnimationRetryStreak, fatigue);
            settleWaitUntilMs = Math.max(settleWaitUntilMs, lastDispatchAtMs + holdMs);
            resetDeferSoftcapTracking();
        }
        if (settleWaitUntilMs > nowMs) {
            if (deferSoftcapWindowUntilMs != settleWaitUntilMs) {
                deferSoftcapWindowUntilMs = settleWaitUntilMs;
                consecutiveDefersInWindow = 0;
            }
            consecutiveDefersInWindow++;
            if (shouldReleaseFromDeferSoftcap(nowMs, lastDispatchAtMs, fatigue)) {
                settleWaitUntilMs = 0L;
                resetDeferSoftcapTracking();
                return Decision.proceed(
                    noAnimationRetryStreak,
                    noAnimationRetryStreak >= RETRY_SOFTCAP_START_STREAK
                );
            }
            if (!softcapActive && noAnimationRetryStreak >= RETRY_SOFTCAP_START_STREAK) {
                softcapActive = true;
            }
            if (!softcapActive && consecutiveDefersInWindow >= DEFER_SOFTCAP_START_COUNT) {
                softcapActive = true;
            }
            return Decision.defer(settleWaitUntilMs - nowMs, noAnimationRetryStreak, newlyArmed, softcapActive);
        }
        resetDeferSoftcapTracking();
        return Decision.proceed(
            noAnimationRetryStreak,
            noAnimationRetryStreak >= RETRY_SOFTCAP_START_STREAK
        );
    }

    private void resetState() {
        settleWaitUntilMs = 0L;
        lastObservedDispatchAtMs = 0L;
        noAnimationRetryStreak = 0;
        resetDeferSoftcapTracking();
    }

    private void resetDeferSoftcapTracking() {
        deferSoftcapWindowUntilMs = 0L;
        consecutiveDefersInWindow = 0;
    }

    private boolean shouldReleaseFromDeferSoftcap(long nowMs, long dispatchAtMs, FatigueSnapshot fatigue) {
        if (consecutiveDefersInWindow < DEFER_SOFTCAP_START_COUNT) {
            return false;
        }
        long elapsedSinceDispatchMs = Math.max(0L, nowMs - Math.max(0L, dispatchAtMs));
        int fatigueHardMinExtraMs = fatigue == null
            ? 0
            : Math.max(0, fatigue.fishingReclickCooldownBiasMs(SOFT_RELEASE_FATIGUE_HARD_MIN_EXTRA_MAX_MS));
        long hardMinElapsedMs = SOFT_RELEASE_HARD_MIN_FROM_DISPATCH_MS + fatigueHardMinExtraMs;
        if (elapsedSinceDispatchMs < hardMinElapsedMs) {
            return false;
        }
        long remainingMs = Math.max(0L, settleWaitUntilMs - nowMs);
        if (remainingMs <= 650L && consecutiveDefersInWindow >= DEFER_SOFTCAP_START_COUNT) {
            return true;
        }
        if (consecutiveDefersInWindow >= DEFER_SOFTCAP_FORCE_RELEASE_COUNT) {
            return true;
        }
        int over = consecutiveDefersInWindow - DEFER_SOFTCAP_START_COUNT;
        int chancePercent = DEFER_SOFTCAP_BASE_RELEASE_CHANCE_PERCENT + (over * DEFER_SOFTCAP_RELEASE_CHANCE_STEP_PERCENT);
        chancePercent = Math.min(DEFER_SOFTCAP_RELEASE_CHANCE_MAX_PERCENT, chancePercent);
        chancePercent = clampPercent(chancePercent + randomIntInclusive(-4, 5));
        return randomIntInclusive(0, 99) < chancePercent;
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static int randomIntInclusive(int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private static long computeSettleHoldMs(long dispatchAtMs, int retryStreak, FatigueSnapshot fatigue) {
        long baseHoldMs = rangedHashDuration(
            dispatchAtMs ^ 0xD6E8FEB86659FD93L,
            BASE_SETTLE_WAIT_MIN_MS,
            BASE_SETTLE_WAIT_MAX_MS
        );
        int streakExtraSteps = Math.max(0, Math.min(RETRY_STREAK_EXTRA_CAP, retryStreak - 1));
        long streakExtraMs = 0L;
        for (int idx = 0; idx < streakExtraSteps; idx++) {
            long seed = dispatchAtMs + (idx * 1_315_423_911L);
            streakExtraMs += rangedHashDuration(seed, RETRY_STREAK_EXTRA_WAIT_MIN_MS, RETRY_STREAK_EXTRA_WAIT_MAX_MS);
        }
        if (retryStreak >= RETRY_SOFTCAP_START_STREAK) {
            long softcapSeed = dispatchAtMs ^ ((long) retryStreak * 0x9E3779B97F4A7C15L);
            streakExtraMs += rangedHashDuration(
                softcapSeed,
                RETRY_SOFTCAP_BACKOFF_MIN_MS,
                RETRY_SOFTCAP_BACKOFF_MAX_MS
            );
        }
        int fatigueExtraMs = fatigue == null
            ? 0
            : fatigue.fishingReclickCooldownBiasMs(FATIGUE_SETTLE_EXTRA_MAX_MS);
        return Math.max(1000L, baseHoldMs + streakExtraMs + Math.max(0, fatigueExtraMs));
    }

    private static long rangedHashDuration(long seed, long minMs, long maxMs) {
        long lo = Math.min(minMs, maxMs);
        long hi = Math.max(minMs, maxMs);
        if (hi <= lo) {
            return Math.max(0L, lo);
        }
        double unit = normalizedHashUnit(seed);
        return lo + Math.round((double) (hi - lo) * unit);
    }

    private static double normalizedHashUnit(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        long positive = z & Long.MAX_VALUE;
        return (double) positive / (double) Long.MAX_VALUE;
    }

    static final class Decision {
        final boolean defer;
        final long waitMsRemaining;
        final int noAnimationRetryStreak;
        final boolean newlyArmed;
        final boolean softcapActive;

        private Decision(
            boolean defer,
            long waitMsRemaining,
            int noAnimationRetryStreak,
            boolean newlyArmed,
            boolean softcapActive
        ) {
            this.defer = defer;
            this.waitMsRemaining = Math.max(0L, waitMsRemaining);
            this.noAnimationRetryStreak = Math.max(0, noAnimationRetryStreak);
            this.newlyArmed = newlyArmed;
            this.softcapActive = softcapActive;
        }

        static Decision proceed() {
            return proceed(0, false);
        }

        static Decision proceed(int noAnimationRetryStreak, boolean softcapActive) {
            return new Decision(false, 0L, noAnimationRetryStreak, false, softcapActive);
        }

        static Decision defer(
            long waitMsRemaining,
            int noAnimationRetryStreak,
            boolean newlyArmed,
            boolean softcapActive
        ) {
            return new Decision(true, waitMsRemaining, noAnimationRetryStreak, newlyArmed, softcapActive);
        }
    }
}
