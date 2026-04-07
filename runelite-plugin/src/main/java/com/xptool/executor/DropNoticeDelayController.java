package com.xptool.executor;

import java.util.concurrent.ThreadLocalRandom;

final class DropNoticeDelayController {
    private static final NoticeDelayProfileConfig NOTICE_DELAY_PROFILE_DB_PARITY =
        new NoticeDelayProfileConfig(
            14,
            10,
            700L,
            2800L,
            900L,
            1200L,
            3200L,
            7000L,
            18000L,
            12,
            5,
            120L,
            560L,
            120L
        );

    private NoticeDelayProfileConfig activeProfile = NOTICE_DELAY_PROFILE_DB_PARITY;
    private long noticeDelayUntilMs = 0L;
    private long nextEligibleRollAtMs = 0L;
    private int pendingItemId = -1;
    private long releaseLingerUntilMs = 0L;
    private long releaseLingerArmedForDelayUntilMs = 0L;

    Decision maybeDeferBeforeSessionStart(int itemId, FatigueSnapshot fatigue, long nowMs) {
        if (itemId <= 0 || nowMs <= 0L) {
            return Decision.proceed();
        }
        long waitMsRemaining = activeDelayRemainingMs(nowMs, fatigue);
        if (waitMsRemaining > 0L && (pendingItemId <= 0 || pendingItemId == itemId)) {
            return Decision.defer(waitMsRemaining, false);
        }
        if (nowMs < nextEligibleRollAtMs) {
            return Decision.proceed();
        }
        return maybeRollAndArm(itemId, fatigue, nowMs);
    }

    void armForSessionStart(int itemId, FatigueSnapshot fatigue, long nowMs) {
        if (itemId <= 0 || nowMs <= 0L) {
            return;
        }
        long waitMsRemaining = activeDelayRemainingMs(nowMs, fatigue);
        if (waitMsRemaining > 0L && pendingItemId == itemId) {
            return;
        }
        if (nowMs < nextEligibleRollAtMs) {
            return;
        }
        maybeRollAndArm(itemId, fatigue, nowMs);
    }

    Decision maybeDeferWhileAwaitingFirstDispatch(int itemId, boolean awaitingFirstDispatch, long nowMs) {
        if (!awaitingFirstDispatch || itemId <= 0 || nowMs <= 0L) {
            clearActiveDelay();
            return Decision.proceed();
        }
        long waitMsRemaining = activeDelayRemainingMs(nowMs, null);
        if (waitMsRemaining > 0L && (pendingItemId <= 0 || pendingItemId == itemId)) {
            return Decision.defer(waitMsRemaining, false);
        }
        return Decision.proceed();
    }

    void clearActiveDelay() {
        noticeDelayUntilMs = 0L;
        pendingItemId = -1;
        resetReleaseLingerState();
    }

    void configureProfile(String profileKey) {
        activeProfile = NOTICE_DELAY_PROFILE_DB_PARITY;
    }

    private Decision maybeRollAndArm(int itemId, FatigueSnapshot fatigue, long nowMs) {
        int chancePercent =
            clampPercent(activeProfile.baseNoticeChancePercent + scaledByFatiguePercent(fatigue, activeProfile.fatigueNoticeChanceBonusMaxPercent));
        if (ThreadLocalRandom.current().nextInt(100) >= chancePercent) {
            nextEligibleRollAtMs = nowMs + randomLongInclusive(activeProfile.rollCooldownMinMs, activeProfile.rollCooldownMaxMs);
            return Decision.proceed();
        }

        long delayMs =
            randomLongInclusive(activeProfile.noticeDelayMinMs, activeProfile.noticeDelayMaxMs)
                + scaledByFatiguePercentLong(fatigue, activeProfile.fatigueNoticeDelayExtraMaxMs)
                + randomLongInclusive(-220L, 360L);
        delayMs = Math.max(600L, delayMs);
        noticeDelayUntilMs = nowMs + delayMs;
        pendingItemId = itemId;
        resetReleaseLingerState();
        nextEligibleRollAtMs =
            noticeDelayUntilMs + randomLongInclusive(activeProfile.postNoticeCooldownMinMs, activeProfile.postNoticeCooldownMaxMs);
        return Decision.defer(Math.max(1L, delayMs), true);
    }

    private long activeDelayRemainingMs(long nowMs, FatigueSnapshot fatigue) {
        if (nowMs <= 0L || noticeDelayUntilMs <= 0L) {
            return 0L;
        }
        if (nowMs < noticeDelayUntilMs) {
            return Math.max(1L, noticeDelayUntilMs - nowMs);
        }
        if (releaseLingerArmedForDelayUntilMs != noticeDelayUntilMs) {
            releaseLingerArmedForDelayUntilMs = noticeDelayUntilMs;
            int lingerChancePercent = clampPercent(
                activeProfile.releaseLingerChancePercent
                    + scaledByFatiguePercent(fatigue, activeProfile.fatigueReleaseLingerChanceBonusMaxPercent)
            );
            if (ThreadLocalRandom.current().nextInt(100) < lingerChancePercent) {
                long lingerMs =
                    randomLongInclusive(activeProfile.releaseLingerMinMs, activeProfile.releaseLingerMaxMs)
                        + scaledByFatiguePercentLong(fatigue, activeProfile.fatigueReleaseLingerExtraMaxMs)
                        + randomLongInclusive(-30L, 80L);
                lingerMs = Math.max(150L, lingerMs);
                releaseLingerUntilMs = nowMs + lingerMs;
            } else {
                releaseLingerUntilMs = 0L;
            }
        }
        if (releaseLingerUntilMs > nowMs) {
            return Math.max(1L, releaseLingerUntilMs - nowMs);
        }
        clearActiveDelay();
        return 0L;
    }

    private void resetReleaseLingerState() {
        releaseLingerUntilMs = 0L;
        releaseLingerArmedForDelayUntilMs = 0L;
    }

    private static int scaledByFatiguePercent(FatigueSnapshot fatigue, int maxValue) {
        int boundedMax = Math.max(0, maxValue);
        if (boundedMax <= 0 || fatigue == null) {
            return 0;
        }
        int loadPercent = clampPercent(fatigue.loadPercent());
        return (int) Math.round((loadPercent / 100.0) * boundedMax);
    }

    private static long scaledByFatiguePercentLong(FatigueSnapshot fatigue, long maxValue) {
        long boundedMax = Math.max(0L, maxValue);
        if (boundedMax <= 0L || fatigue == null) {
            return 0L;
        }
        int loadPercent = clampPercent(fatigue.loadPercent());
        return Math.round(((double) loadPercent / 100.0) * (double) boundedMax);
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

    private static final class NoticeDelayProfileConfig {
        private final int baseNoticeChancePercent;
        private final int fatigueNoticeChanceBonusMaxPercent;
        private final long noticeDelayMinMs;
        private final long noticeDelayMaxMs;
        private final long fatigueNoticeDelayExtraMaxMs;
        private final long rollCooldownMinMs;
        private final long rollCooldownMaxMs;
        private final long postNoticeCooldownMinMs;
        private final long postNoticeCooldownMaxMs;
        private final int releaseLingerChancePercent;
        private final int fatigueReleaseLingerChanceBonusMaxPercent;
        private final long releaseLingerMinMs;
        private final long releaseLingerMaxMs;
        private final long fatigueReleaseLingerExtraMaxMs;

        private NoticeDelayProfileConfig(
            int baseNoticeChancePercent,
            int fatigueNoticeChanceBonusMaxPercent,
            long noticeDelayMinMs,
            long noticeDelayMaxMs,
            long fatigueNoticeDelayExtraMaxMs,
            long rollCooldownMinMs,
            long rollCooldownMaxMs,
            long postNoticeCooldownMinMs,
            long postNoticeCooldownMaxMs,
            int releaseLingerChancePercent,
            int fatigueReleaseLingerChanceBonusMaxPercent,
            long releaseLingerMinMs,
            long releaseLingerMaxMs,
            long fatigueReleaseLingerExtraMaxMs
        ) {
            this.baseNoticeChancePercent = clampPercent(baseNoticeChancePercent);
            this.fatigueNoticeChanceBonusMaxPercent = clampPercent(fatigueNoticeChanceBonusMaxPercent);
            this.noticeDelayMinMs = Math.max(0L, noticeDelayMinMs);
            this.noticeDelayMaxMs = Math.max(this.noticeDelayMinMs, noticeDelayMaxMs);
            this.fatigueNoticeDelayExtraMaxMs = Math.max(0L, fatigueNoticeDelayExtraMaxMs);
            this.rollCooldownMinMs = Math.max(0L, rollCooldownMinMs);
            this.rollCooldownMaxMs = Math.max(this.rollCooldownMinMs, rollCooldownMaxMs);
            this.postNoticeCooldownMinMs = Math.max(0L, postNoticeCooldownMinMs);
            this.postNoticeCooldownMaxMs = Math.max(this.postNoticeCooldownMinMs, postNoticeCooldownMaxMs);
            this.releaseLingerChancePercent = clampPercent(releaseLingerChancePercent);
            this.fatigueReleaseLingerChanceBonusMaxPercent =
                clampPercent(fatigueReleaseLingerChanceBonusMaxPercent);
            this.releaseLingerMinMs = Math.max(0L, releaseLingerMinMs);
            this.releaseLingerMaxMs = Math.max(this.releaseLingerMinMs, releaseLingerMaxMs);
            this.fatigueReleaseLingerExtraMaxMs = Math.max(0L, fatigueReleaseLingerExtraMaxMs);
        }
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
