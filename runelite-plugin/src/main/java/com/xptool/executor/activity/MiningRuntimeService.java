package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;

public final class MiningRuntimeService {
    @FunctionalInterface
    public interface DurationSampler {
        long sample(long baseMs, long jitterMs, double spread);
    }

    private final MiningActivityRuntime miningActivityRuntime;
    private final DurationSampler durationSampler;
    private final long retryWindowMs;
    private final long outcomeWaitWindowMs;
    private final double spreadDefault;
    private final double spreadNarrow;

    public MiningRuntimeService(
        MiningActivityRuntime miningActivityRuntime,
        DurationSampler durationSampler,
        long retryWindowMs,
        long outcomeWaitWindowMs,
        double spreadDefault,
        double spreadNarrow
    ) {
        this.miningActivityRuntime = miningActivityRuntime;
        this.durationSampler = durationSampler;
        this.retryWindowMs = retryWindowMs;
        this.outcomeWaitWindowMs = outcomeWaitWindowMs;
        this.spreadDefault = spreadDefault;
        this.spreadNarrow = spreadNarrow;
    }

    public void extendRetryWindow() {
        miningActivityRuntime.extendRetryWindow(durationSampler.sample(retryWindowMs, 200L, spreadDefault));
    }

    public void beginOutcomeWaitWindow() {
        miningActivityRuntime.beginOutcomeWaitWindow(durationSampler.sample(outcomeWaitWindowMs, 300L, spreadNarrow));
    }

    public void clearOutcomeWaitWindow() {
        miningActivityRuntime.clearOutcomeWaitWindow();
    }

    public void clearInteractionWindows() {
        miningActivityRuntime.clearInteractionWindows();
    }

    public void suppressRockTarget(WorldPoint worldPoint, long durationMs) {
        miningActivityRuntime.suppressRockTarget(worldPoint, durationMs);
    }

    public boolean isRockSuppressed(WorldPoint worldPoint) {
        return miningActivityRuntime.isRockSuppressed(worldPoint);
    }

    public void pruneRockSuppression() {
        miningActivityRuntime.pruneRockSuppression();
    }
}
