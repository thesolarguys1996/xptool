package com.xptool.executor;

final class ManualMetricsRuntimeTuning {
    private static final long BASE_RANDOM_EVENT_PRE_ATTEMPT_MIN_MS = 260L;
    private static final long BASE_RANDOM_EVENT_PRE_ATTEMPT_MAX_MS = 520L;
    private static final long BASE_RANDOM_EVENT_SUCCESS_MIN_MS = 820L;
    private static final long BASE_RANDOM_EVENT_SUCCESS_MAX_MS = 1480L;
    private static final long BASE_RANDOM_EVENT_FAILURE_MIN_MS = 420L;
    private static final long BASE_RANDOM_EVENT_FAILURE_MAX_MS = 980L;
    private static final long BASE_RANDOM_EVENT_CURSOR_READY_HOLD_MS = 170L;

    boolean hasSignal(IdleCadenceTuning tuning) {
        return tuning != null
            && tuning.hasFishingDbParityIdleCadenceWindow()
            && tuning.hasPostDropIdleDbParityCooldownWindow();
    }

    LoginProfile resolveLoginProfile(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return null;
        }
        double cadenceScale = cadenceScale(tuning);
        double pauseScale = pauseScale(tuning);
        int retryMinTicks = clampInt((int) Math.round(6.0 * cadenceScale), 4, 14);
        int retryMaxTicks = clampInt((int) Math.round(11.0 * pauseScale), retryMinTicks + 2, 20);
        int waitMinTicks = clampInt((int) Math.round(120.0 * pauseScale), 90, 220);
        int waitMaxTicks = clampInt((int) Math.round(165.0 * pauseScale), waitMinTicks + 20, 280);
        return new LoginProfile(3, retryMinTicks, retryMaxTicks, waitMinTicks, waitMaxTicks, 14);
    }

    LogoutProfile resolveLogoutProfile(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return null;
        }
        double cadenceScale = cadenceScale(tuning);
        double pauseScale = pauseScale(tuning);
        int retryMinTicks = clampInt((int) Math.round(3.0 * cadenceScale), 2, 10);
        int retryMaxTicks = clampInt((int) Math.round(7.0 * pauseScale), retryMinTicks + 1, 16);
        int waitMinTicks = clampInt((int) Math.round(9.0 * pauseScale), 6, 18);
        int waitMaxTicks = clampInt((int) Math.round(14.0 * pauseScale), waitMinTicks + 2, 24);
        return new LogoutProfile(5, retryMinTicks, retryMaxTicks, waitMinTicks, waitMaxTicks);
    }

    BreakProfile resolveBreakProfile(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return null;
        }
        double cadenceScale = cadenceScale(tuning);
        double pauseScale = pauseScale(tuning);
        double workProgressStepPctPerTick = clampDouble(0.08 / pauseScale, 0.05, 0.12);
        double breakProgressStepPctPerTick = clampDouble(0.45 / cadenceScale, 0.28, 0.62);
        int breakStartChancePct = clampInt((int) Math.round(22.0 * cadenceScale), 12, 38);
        int breakEndChancePct = clampInt((int) Math.round(35.0 * cadenceScale), 18, 55);
        int resumeCooldownTicks = clampInt((int) Math.round(3.0 * pauseScale), 2, 8);
        return new BreakProfile(
            workProgressStepPctPerTick,
            breakProgressStepPctPerTick,
            breakStartChancePct,
            breakEndChancePct,
            resumeCooldownTicks,
            true
        );
    }

    long resolveRandomEventPreAttemptCooldownMinMs(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return -1L;
        }
        double cadenceScale = cadenceScale(tuning);
        return clampLong(Math.round(BASE_RANDOM_EVENT_PRE_ATTEMPT_MIN_MS * cadenceScale), 140L, 820L);
    }

    long resolveRandomEventPreAttemptCooldownMaxMs(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return -1L;
        }
        double pauseScale = pauseScale(tuning);
        long min = resolveRandomEventPreAttemptCooldownMinMs(tuning);
        long candidate = clampLong(Math.round(BASE_RANDOM_EVENT_PRE_ATTEMPT_MAX_MS * pauseScale), 220L, 1400L);
        return Math.max(min + 60L, candidate);
    }

    long resolveRandomEventSuccessCooldownMinMs(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return -1L;
        }
        double cadenceScale = cadenceScale(tuning);
        return clampLong(Math.round(BASE_RANDOM_EVENT_SUCCESS_MIN_MS * cadenceScale), 360L, 2200L);
    }

    long resolveRandomEventSuccessCooldownMaxMs(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return -1L;
        }
        double pauseScale = pauseScale(tuning);
        long min = resolveRandomEventSuccessCooldownMinMs(tuning);
        long candidate = clampLong(Math.round(BASE_RANDOM_EVENT_SUCCESS_MAX_MS * pauseScale), 680L, 3400L);
        return Math.max(min + 120L, candidate);
    }

    long resolveRandomEventFailureRetryCooldownMinMs(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return -1L;
        }
        double cadenceScale = cadenceScale(tuning);
        return clampLong(Math.round(BASE_RANDOM_EVENT_FAILURE_MIN_MS * cadenceScale), 220L, 1600L);
    }

    long resolveRandomEventFailureRetryCooldownMaxMs(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return -1L;
        }
        double pauseScale = pauseScale(tuning);
        long min = resolveRandomEventFailureRetryCooldownMinMs(tuning);
        long candidate = clampLong(Math.round(BASE_RANDOM_EVENT_FAILURE_MAX_MS * pauseScale), 420L, 2600L);
        return Math.max(min + 100L, candidate);
    }

    long resolveRandomEventCursorReadyHoldMs(IdleCadenceTuning tuning) {
        if (!hasSignal(tuning)) {
            return -1L;
        }
        double cadenceScale = cadenceScale(tuning);
        return clampLong(Math.round(BASE_RANDOM_EVENT_CURSOR_READY_HOLD_MS * cadenceScale), 90L, 420L);
    }

    private static double cadenceScale(IdleCadenceTuning tuning) {
        int dbMin = tuning.resolveFishingDbParityIdleMinIntervalTicks(5);
        int dbMax = tuning.resolveFishingDbParityIdleMaxIntervalTicks(8, dbMin);
        double medianTicks = (dbMin + dbMax) / 2.0;
        return clampDouble(medianTicks / 5.5, 0.75, 1.35);
    }

    private static double pauseScale(IdleCadenceTuning tuning) {
        int postDropMin = tuning.resolvePostDropIdleCooldownMinTicks(3, true);
        int postDropMax = tuning.resolvePostDropIdleCooldownMaxTicks(5, postDropMin, true);
        double avgTicks = (postDropMin + postDropMax) / 2.0;
        return clampDouble(avgTicks / 4.0, 0.80, 1.45);
    }

    private static int clampInt(int value, int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        return Math.max(lo, Math.min(hi, value));
    }

    private static long clampLong(long value, long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        return Math.max(lo, Math.min(hi, value));
    }

    private static double clampDouble(double value, double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return lo;
        }
        return Math.max(lo, Math.min(hi, value));
    }
}
