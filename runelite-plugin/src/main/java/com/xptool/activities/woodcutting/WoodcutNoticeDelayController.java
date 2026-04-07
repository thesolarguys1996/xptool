package com.xptool.activities.woodcutting;

import com.xptool.core.runtime.FatigueSnapshot;
import java.util.concurrent.ThreadLocalRandom;

final class WoodcutNoticeDelayController {
    private static final int NOTICE_CHANCE_PERCENT = 48;
    private static final int FATIGUE_NOTICE_CHANCE_BONUS_MAX_PERCENT = 18;
    private static final long BASE_NOTICE_DELAY_MIN_MS = 1400L;
    private static final long BASE_NOTICE_DELAY_MAX_MS = 2600L;
    private static final long FATIGUE_BASE_DELAY_EXTRA_MAX_MS = 680L;
    private static final long NOTICE_DELAY_MIN_MS = 2400L;
    private static final long NOTICE_DELAY_MAX_MS = 6600L;
    private static final long FATIGUE_NOTICE_DELAY_EXTRA_MAX_MS = 1500L;
    private static final long RECENT_DISPATCH_WINDOW_MS = 28000L;
    private static final long ANIMATION_SIGNAL_STALE_MAX_MS = 180000L;
    private static final long ROLL_COOLDOWN_MIN_MS = 1800L;
    private static final long ROLL_COOLDOWN_MAX_MS = 5600L;
    private static final long POST_NOTICE_COOLDOWN_MIN_MS = 5500L;
    private static final long POST_NOTICE_COOLDOWN_MAX_MS = 17000L;
    private static final int RELEASE_LINGER_CHANCE_PERCENT = 32;
    private static final int FATIGUE_RELEASE_LINGER_CHANCE_BONUS_MAX_PERCENT = 12;
    private static final long RELEASE_LINGER_MIN_MS = 180L;
    private static final long RELEASE_LINGER_MAX_MS = 900L;
    private static final long FATIGUE_RELEASE_LINGER_EXTRA_MAX_MS = 240L;
    private static final int DEFER_SOFTCAP_START_STREAK = 4;
    private static final int DEFER_SOFTCAP_BASE_RELEASE_CHANCE_PERCENT = 18;
    private static final int DEFER_SOFTCAP_RELEASE_CHANCE_STEP_PERCENT = 14;
    private static final int DEFER_SOFTCAP_RELEASE_CHANCE_MAX_PERCENT = 82;
    private static final int DEFER_SOFTCAP_FORCE_RELEASE_STREAK = 9;

    private boolean animationActiveObserved = false;
    private long animationActiveObservedAtMs = 0L;
    private long noticeDelayUntilMs = 0L;
    private long nextEligibleRollAtMs = 0L;
    private long lastObservedDispatchAtMs = 0L;
    private long releaseLingerUntilMs = 0L;
    private long releaseLingerArmedForDelayUntilMs = 0L;
    private long deferSoftcapWindowUntilMs = 0L;
    private int consecutiveDeferStreak = 0;

    void armForDispatch(long dispatchAtMs, FatigueSnapshot fatigue) {
        if (dispatchAtMs <= 0L || dispatchAtMs == lastObservedDispatchAtMs) {
            return;
        }
        lastObservedDispatchAtMs = dispatchAtMs;
        armDelay(dispatchAtMs, fatigue);
    }

    void noteAnimationActive(long nowMs) {
        animationActiveObserved = true;
        if (nowMs > 0L) {
            animationActiveObservedAtMs = nowMs;
        }
    }

    void resetForDropTeardown(long lastDispatchAtMs) {
        animationActiveObserved = false;
        animationActiveObservedAtMs = 0L;
        clearActiveDelayWindow();
        nextEligibleRollAtMs = 0L;
        lastObservedDispatchAtMs = Math.max(0L, lastDispatchAtMs);
    }

    Decision maybeDeferAfterAnimationEnd(long nowMs, long lastDispatchAtMs, FatigueSnapshot fatigue) {
        if (nowMs <= 0L) {
            return Decision.proceed();
        }
        long remainingBeforeSignalsMs = activeDelayRemainingMs(nowMs, fatigue);
        if (remainingBeforeSignalsMs > 0L) {
            return Decision.defer(remainingBeforeSignalsMs, false);
        }
        boolean armedNow = false;
        boolean consumeAnimationSignal = animationActiveObserved
            && (animationActiveObservedAtMs <= 0L
                || (nowMs - animationActiveObservedAtMs) <= ANIMATION_SIGNAL_STALE_MAX_MS);
        if (animationActiveObserved) {
            animationActiveObserved = false;
        }
        if (consumeAnimationSignal) {
            armedNow = armDelay(nowMs, fatigue);
        }
        if (lastDispatchAtMs > 0L
            && lastDispatchAtMs != lastObservedDispatchAtMs
            && (nowMs - lastDispatchAtMs) <= RECENT_DISPATCH_WINDOW_MS) {
            lastObservedDispatchAtMs = lastDispatchAtMs;
            armedNow = armDelay(nowMs, fatigue) || armedNow;
        }
        long remainingMs = activeDelayRemainingMs(nowMs, fatigue);
        if (remainingMs > 0L) {
            return Decision.defer(remainingMs, armedNow);
        }
        return Decision.proceed();
    }

    private boolean armDelay(long nowMs, FatigueSnapshot fatigue) {
        if (nowMs <= 0L) {
            return false;
        }
        clearExpiredDelay(nowMs);
        long baseDelayMs =
            randomLongInclusive(BASE_NOTICE_DELAY_MIN_MS, BASE_NOTICE_DELAY_MAX_MS)
                + scaledByFatiguePercentLong(fatigue, FATIGUE_BASE_DELAY_EXTRA_MAX_MS)
                + randomLongInclusive(-60L, 110L);
        baseDelayMs = Math.max(700L, baseDelayMs);
        if (nowMs < nextEligibleRollAtMs) {
            setOrExtendNoticeDelayUntil(nowMs + baseDelayMs);
            return noticeDelayUntilMs > nowMs;
        }
        int chancePercent = clampPercent(
            NOTICE_CHANCE_PERCENT + scaledByFatiguePercent(fatigue, FATIGUE_NOTICE_CHANCE_BONUS_MAX_PERCENT)
        );
        if (ThreadLocalRandom.current().nextInt(100) >= chancePercent) {
            nextEligibleRollAtMs = nowMs + randomLongInclusive(ROLL_COOLDOWN_MIN_MS, ROLL_COOLDOWN_MAX_MS);
            setOrExtendNoticeDelayUntil(nowMs + baseDelayMs);
            return noticeDelayUntilMs > nowMs;
        }
        long delayMs =
            randomLongInclusive(NOTICE_DELAY_MIN_MS, NOTICE_DELAY_MAX_MS)
                + scaledByFatiguePercentLong(fatigue, FATIGUE_NOTICE_DELAY_EXTRA_MAX_MS)
                + randomLongInclusive(-120L, 220L);
        delayMs = Math.max(1000L, delayMs);
        setOrExtendNoticeDelayUntil(nowMs + delayMs);
        nextEligibleRollAtMs =
            noticeDelayUntilMs + randomLongInclusive(POST_NOTICE_COOLDOWN_MIN_MS, POST_NOTICE_COOLDOWN_MAX_MS);
        return noticeDelayUntilMs > nowMs;
    }

    private void clearExpiredDelay(long nowMs) {
        if (noticeDelayUntilMs > 0L && nowMs >= noticeDelayUntilMs) {
            clearActiveDelayWindow();
        }
    }

    private long activeDelayRemainingMs(long nowMs, FatigueSnapshot fatigue) {
        if (nowMs <= 0L || noticeDelayUntilMs <= 0L) {
            return 0L;
        }
        if (nowMs < noticeDelayUntilMs) {
            long remainingMs = Math.max(1L, noticeDelayUntilMs - nowMs);
            if (shouldReleaseFromDeferSoftcap(remainingMs)) {
                clearActiveDelayWindow();
                return 0L;
            }
            return remainingMs;
        }
        if (releaseLingerArmedForDelayUntilMs != noticeDelayUntilMs) {
            releaseLingerArmedForDelayUntilMs = noticeDelayUntilMs;
            int lingerChancePercent = clampPercent(
                RELEASE_LINGER_CHANCE_PERCENT
                    + scaledByFatiguePercent(fatigue, FATIGUE_RELEASE_LINGER_CHANCE_BONUS_MAX_PERCENT)
            );
            if (ThreadLocalRandom.current().nextInt(100) < lingerChancePercent) {
                long lingerMs =
                    randomLongInclusive(RELEASE_LINGER_MIN_MS, RELEASE_LINGER_MAX_MS)
                        + scaledByFatiguePercentLong(fatigue, FATIGUE_RELEASE_LINGER_EXTRA_MAX_MS)
                        + randomLongInclusive(-30L, 60L);
                lingerMs = Math.max(120L, lingerMs);
                releaseLingerUntilMs = nowMs + lingerMs;
            } else {
                releaseLingerUntilMs = 0L;
            }
        }
        if (releaseLingerUntilMs > nowMs) {
            long remainingMs = Math.max(1L, releaseLingerUntilMs - nowMs);
            if (shouldReleaseFromDeferSoftcap(remainingMs)) {
                clearActiveDelayWindow();
                return 0L;
            }
            return remainingMs;
        }
        clearActiveDelayWindow();
        return 0L;
    }

    private void setOrExtendNoticeDelayUntil(long candidateUntilMs) {
        long bounded = Math.max(0L, candidateUntilMs);
        if (bounded <= noticeDelayUntilMs) {
            return;
        }
        noticeDelayUntilMs = bounded;
        resetReleaseLingerState();
    }

    private void clearActiveDelayWindow() {
        noticeDelayUntilMs = 0L;
        resetReleaseLingerState();
        resetDeferSoftcapState();
    }

    private void resetReleaseLingerState() {
        releaseLingerUntilMs = 0L;
        releaseLingerArmedForDelayUntilMs = 0L;
    }

    private void resetDeferSoftcapState() {
        deferSoftcapWindowUntilMs = 0L;
        consecutiveDeferStreak = 0;
    }

    private boolean shouldReleaseFromDeferSoftcap(long remainingMs) {
        if (remainingMs <= 0L || noticeDelayUntilMs <= 0L) {
            return false;
        }
        if (deferSoftcapWindowUntilMs != noticeDelayUntilMs) {
            deferSoftcapWindowUntilMs = noticeDelayUntilMs;
            consecutiveDeferStreak = 0;
        }
        consecutiveDeferStreak++;
        if (consecutiveDeferStreak >= DEFER_SOFTCAP_FORCE_RELEASE_STREAK) {
            return true;
        }
        if (consecutiveDeferStreak < DEFER_SOFTCAP_START_STREAK) {
            return false;
        }
        int over = consecutiveDeferStreak - DEFER_SOFTCAP_START_STREAK;
        int chancePercent = clampPercent(
            DEFER_SOFTCAP_BASE_RELEASE_CHANCE_PERCENT + (over * DEFER_SOFTCAP_RELEASE_CHANCE_STEP_PERCENT)
        );
        chancePercent = Math.min(DEFER_SOFTCAP_RELEASE_CHANCE_MAX_PERCENT, chancePercent);
        chancePercent = clampPercent(chancePercent + ThreadLocalRandom.current().nextInt(-4, 6));
        return ThreadLocalRandom.current().nextInt(100) < chancePercent;
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

    private static long randomLongInclusive(long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static int scaledByFatiguePercent(FatigueSnapshot fatigue, int maxValue) {
        int boundedMax = Math.max(0, maxValue);
        if (fatigue == null || boundedMax <= 0) {
            return 0;
        }
        return fatigue.woodcutNoticeChanceBonusPercent(boundedMax);
    }

    private static long scaledByFatiguePercentLong(FatigueSnapshot fatigue, long maxValue) {
        long boundedMax = Math.max(0L, maxValue);
        if (fatigue == null || boundedMax <= 0L) {
            return 0L;
        }
        return Math.max(0L, fatigue.woodcutNoticeDelayExtraMs((int) Math.min(Integer.MAX_VALUE, boundedMax)));
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
