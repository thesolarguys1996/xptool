package com.xptool.executor;

import java.util.concurrent.ThreadLocalRandom;

final class FishingCadenceVariationGate {
    private static final long ACTIVE_WINDOW_MAX_MS = 14000L;
    private static final long NEXT_ROLL_COOLDOWN_MIN_MS = 700L;
    private static final long NEXT_ROLL_COOLDOWN_MAX_MS = 2200L;
    private static final int BASE_PAUSE_CHANCE_PERCENT = 14;
    private static final int SAME_TARGET_PAUSE_CHANCE_BONUS_PERCENT = 12;
    private static final int FATIGUE_PAUSE_CHANCE_BONUS_MAX_PERCENT = 8;
    private static final long PAUSE_MIN_MS = 80L;
    private static final long PAUSE_MAX_MS = 220L;
    private static final long SAME_TARGET_PAUSE_MIN_MS = 110L;
    private static final long SAME_TARGET_PAUSE_MAX_MS = 300L;
    private static final int FATIGUE_PAUSE_EXTRA_MAX_MS = 95;
    private static final int FORCE_PAUSE_ELIGIBLE_MIN_COUNT = 4;
    private static final int FORCE_PAUSE_ELIGIBLE_MAX_COUNT = 8;

    private long cadencePauseUntilMs = 0L;
    private long nextEligibleRollAtMs = 0L;
    private long lastObservedDispatchAtMs = 0L;
    private int consecutivePauseCount = 0;
    private int eligibleDispatchesSincePause = 0;
    private int forcePauseAfterEligibleCount = nextForcePauseAfterEligibleCount();

    Decision maybeDeferBeforeDispatch(
        long nowMs,
        long lastDispatchAtMs,
        int targetNpcIndex,
        boolean sameDispatchTarget,
        FatigueSnapshot fatigue
    ) {
        if (nowMs <= 0L) {
            return Decision.proceed();
        }
        if (cadencePauseUntilMs > nowMs) {
            return Decision.defer(
                Math.max(1L, cadencePauseUntilMs - nowMs),
                false,
                "active_pause",
                consecutivePauseCount
            );
        }
        if (lastDispatchAtMs <= 0L || (nowMs - lastDispatchAtMs) > ACTIVE_WINDOW_MAX_MS) {
            resetState();
            return Decision.proceed();
        }
        if (nowMs < nextEligibleRollAtMs) {
            return Decision.proceed();
        }
        if (lastDispatchAtMs == lastObservedDispatchAtMs && (nowMs - lastDispatchAtMs) < 450L) {
            return Decision.proceed();
        }
        lastObservedDispatchAtMs = lastDispatchAtMs;
        eligibleDispatchesSincePause = Math.min(Integer.MAX_VALUE, eligibleDispatchesSincePause + 1);
        boolean forcePause = eligibleDispatchesSincePause >= forcePauseAfterEligibleCount;
        int chancePercent = BASE_PAUSE_CHANCE_PERCENT;
        if (sameDispatchTarget) {
            chancePercent += SAME_TARGET_PAUSE_CHANCE_BONUS_PERCENT;
        }
        if (fatigue != null) {
            chancePercent += fatigue.fishingNoticeChanceBonusPercent(FATIGUE_PAUSE_CHANCE_BONUS_MAX_PERCENT);
        }
        chancePercent = clampPercent(chancePercent + ThreadLocalRandom.current().nextInt(-3, 4));
        boolean rollPause = ThreadLocalRandom.current().nextInt(100) < chancePercent;
        if (!forcePause && !rollPause) {
            consecutivePauseCount = 0;
            nextEligibleRollAtMs = nowMs + randomLongInclusive(NEXT_ROLL_COOLDOWN_MIN_MS, NEXT_ROLL_COOLDOWN_MAX_MS);
            return Decision.proceed();
        }
        long pauseMs = sameDispatchTarget
            ? randomLongInclusive(SAME_TARGET_PAUSE_MIN_MS, SAME_TARGET_PAUSE_MAX_MS)
            : randomLongInclusive(PAUSE_MIN_MS, PAUSE_MAX_MS);
        if (fatigue != null) {
            pauseMs += Math.max(0L, fatigue.fishingReclickCooldownBiasMs(FATIGUE_PAUSE_EXTRA_MAX_MS));
        }
        pauseMs = Math.max(70L, pauseMs + randomLongInclusive(-25L, 35L));
        cadencePauseUntilMs = nowMs + pauseMs;
        consecutivePauseCount = Math.min(Integer.MAX_VALUE, consecutivePauseCount + 1);
        eligibleDispatchesSincePause = 0;
        forcePauseAfterEligibleCount = nextForcePauseAfterEligibleCount();
        nextEligibleRollAtMs = cadencePauseUntilMs + randomLongInclusive(NEXT_ROLL_COOLDOWN_MIN_MS, NEXT_ROLL_COOLDOWN_MAX_MS);
        String cadenceMode;
        if (forcePause) {
            cadenceMode = sameDispatchTarget ? "forced_same_target_micro_hesitation" : "forced_micro_hesitation";
        } else {
            cadenceMode = sameDispatchTarget ? "same_target_micro_hesitation" : "micro_hesitation";
        }
        return Decision.defer(
            Math.max(1L, cadencePauseUntilMs - nowMs),
            true,
            cadenceMode,
            consecutivePauseCount
        );
    }

    private void resetState() {
        cadencePauseUntilMs = 0L;
        nextEligibleRollAtMs = 0L;
        lastObservedDispatchAtMs = 0L;
        consecutivePauseCount = 0;
        eligibleDispatchesSincePause = 0;
        forcePauseAfterEligibleCount = nextForcePauseAfterEligibleCount();
    }

    private static int nextForcePauseAfterEligibleCount() {
        return randomIntInclusive(FORCE_PAUSE_ELIGIBLE_MIN_COUNT, FORCE_PAUSE_ELIGIBLE_MAX_COUNT);
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static long randomLongInclusive(long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static int randomIntInclusive(int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    static final class Decision {
        final boolean defer;
        final long waitMsRemaining;
        final boolean newlyArmed;
        final String cadenceMode;
        final int consecutivePauseCount;

        private Decision(
            boolean defer,
            long waitMsRemaining,
            boolean newlyArmed,
            String cadenceMode,
            int consecutivePauseCount
        ) {
            this.defer = defer;
            this.waitMsRemaining = Math.max(0L, waitMsRemaining);
            this.newlyArmed = newlyArmed;
            this.cadenceMode = cadenceMode == null ? "" : cadenceMode;
            this.consecutivePauseCount = Math.max(0, consecutivePauseCount);
        }

        static Decision proceed() {
            return new Decision(false, 0L, false, "", 0);
        }

        static Decision defer(
            long waitMsRemaining,
            boolean newlyArmed,
            String cadenceMode,
            int consecutivePauseCount
        ) {
            return new Decision(true, waitMsRemaining, newlyArmed, cadenceMode, consecutivePauseCount);
        }
    }
}
