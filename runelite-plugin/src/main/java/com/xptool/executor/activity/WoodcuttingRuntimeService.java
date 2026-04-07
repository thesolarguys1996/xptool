package com.xptool.executor.activity;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class WoodcuttingRuntimeService {
    @FunctionalInterface
    public interface DurationSampler {
        long sample(long baseMs, long jitterMs, double spread);
    }

    private final WoodcuttingActivityRuntime woodcuttingActivityRuntime;
    private final DurationSampler durationSampler;
    private final long retryWindowMs;
    private final long outcomeWaitWindowMs;
    private final long approachBaseWaitMs;
    private final long approachWaitPerTileMs;
    private final long approachMaxWaitMs;
    private final long approachMinHoldMs;
    private final double spreadDefault;
    private final double spreadNarrow;
    private final double spreadWide;

    public WoodcuttingRuntimeService(
        WoodcuttingActivityRuntime woodcuttingActivityRuntime,
        DurationSampler durationSampler,
        long retryWindowMs,
        long outcomeWaitWindowMs,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long approachMinHoldMs,
        double spreadDefault,
        double spreadNarrow,
        double spreadWide
    ) {
        this.woodcuttingActivityRuntime = woodcuttingActivityRuntime;
        this.durationSampler = durationSampler;
        this.retryWindowMs = retryWindowMs;
        this.outcomeWaitWindowMs = outcomeWaitWindowMs;
        this.approachBaseWaitMs = approachBaseWaitMs;
        this.approachWaitPerTileMs = approachWaitPerTileMs;
        this.approachMaxWaitMs = approachMaxWaitMs;
        this.approachMinHoldMs = approachMinHoldMs;
        this.spreadDefault = spreadDefault;
        this.spreadNarrow = spreadNarrow;
        this.spreadWide = spreadWide;
    }

    public void extendRetryWindow() {
        woodcuttingActivityRuntime.extendRetryWindow(durationSampler.sample(retryWindowMs, 200L, spreadDefault));
    }

    public void beginOutcomeWaitWindow() {
        woodcuttingActivityRuntime.beginOutcomeWaitWindow(durationSampler.sample(outcomeWaitWindowMs, 300L, spreadNarrow));
    }

    public void clearOutcomeWaitWindow() {
        woodcuttingActivityRuntime.clearOutcomeWaitWindow();
    }

    public void noteTargetAttempt(WorldPoint localWorldPoint, TileObject targetObject) {
        WorldPoint targetWorldPoint = targetObject == null ? null : targetObject.getWorldLocation();
        if (targetWorldPoint == null) {
            return;
        }
        woodcuttingActivityRuntime.noteTargetAttempt(
            localWorldPoint,
            targetWorldPoint,
            durationSampler.sample(approachBaseWaitMs, 90L, spreadDefault),
            durationSampler.sample(approachWaitPerTileMs, 20L, spreadWide),
            durationSampler.sample(approachMaxWaitMs, 220L, spreadNarrow),
            durationSampler.sample(approachMinHoldMs, 70L, spreadDefault)
        );
    }

    public void clearTargetAttempt() {
        woodcuttingActivityRuntime.clearTargetAttempt();
    }

    public void noteDispatchAttempt(TileObject targetObject, long nowMs) {
        WorldPoint targetWorldPoint = targetObject == null ? null : targetObject.getWorldLocation();
        woodcuttingActivityRuntime.noteDispatchAttempt(targetWorldPoint, nowMs);
    }

    public void clearDispatchAttempt() {
        woodcuttingActivityRuntime.clearDispatchAttempt();
    }

    public void clearInteractionWindows() {
        woodcuttingActivityRuntime.clearInteractionWindows();
    }

    public void clearInteractionWindowsPreserveDispatchSignal() {
        woodcuttingActivityRuntime.clearInteractionWindowsPreserveDispatchSignal();
    }
}
