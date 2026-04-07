package com.xptool.executor;

import java.util.concurrent.ThreadLocalRandom;

final class FishingPostDropDelayController {
    private static final long DROP_SIGNAL_STALE_RESET_MS = 22000L;
    private static final long RESUME_DELAY_MIN_MS = 360L;
    private static final long RESUME_DELAY_MAX_MS = 920L;
    private static final int FATIGUE_RESUME_DELAY_EXTRA_MAX_MS = 190;

    private long lastObservedDropEndAtMs = 0L;
    private long resumeDelayUntilMs = 0L;

    Decision maybeDeferAfterDropTeardown(long nowMs, long lastDropSweepSessionEndedAtMs, FatigueSnapshot fatigue) {
        if (nowMs <= 0L) {
            return Decision.proceed();
        }
        if (lastDropSweepSessionEndedAtMs <= 0L
            || (nowMs - lastDropSweepSessionEndedAtMs) > DROP_SIGNAL_STALE_RESET_MS) {
            reset();
            return Decision.proceed();
        }
        boolean newlyArmed = false;
        if (lastDropSweepSessionEndedAtMs != lastObservedDropEndAtMs) {
            lastObservedDropEndAtMs = lastDropSweepSessionEndedAtMs;
            long delayMs = randomLongInclusive(RESUME_DELAY_MIN_MS, RESUME_DELAY_MAX_MS);
            if (fatigue != null) {
                delayMs += Math.max(0L, fatigue.fishingReclickCooldownBiasMs(FATIGUE_RESUME_DELAY_EXTRA_MAX_MS));
            }
            delayMs = Math.max(240L, delayMs + randomLongInclusive(-35L, 65L));
            resumeDelayUntilMs = Math.max(resumeDelayUntilMs, lastDropSweepSessionEndedAtMs + delayMs);
            newlyArmed = resumeDelayUntilMs > nowMs;
        }
        if (resumeDelayUntilMs > nowMs) {
            return Decision.defer(Math.max(1L, resumeDelayUntilMs - nowMs), newlyArmed);
        }
        return Decision.proceed();
    }

    private void reset() {
        lastObservedDropEndAtMs = 0L;
        resumeDelayUntilMs = 0L;
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

        private Decision(boolean defer, long waitMsRemaining, boolean newlyArmed) {
            this.defer = defer;
            this.waitMsRemaining = Math.max(0L, waitMsRemaining);
            this.newlyArmed = newlyArmed;
        }

        static Decision proceed() {
            return new Decision(false, 0L, false);
        }

        static Decision defer(long waitMsRemaining, boolean newlyArmed) {
            return new Decision(true, waitMsRemaining, newlyArmed);
        }
    }
}
