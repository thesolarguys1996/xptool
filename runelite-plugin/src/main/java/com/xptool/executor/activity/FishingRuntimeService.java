package com.xptool.executor.activity;

import net.runelite.api.NPC;
import net.runelite.api.Player;

public final class FishingRuntimeService {
    @FunctionalInterface
    public interface DurationSampler {
        long sample(long baseMs, long jitterMs, double spread);
    }

    private final FishingActivityRuntime fishingActivityRuntime;
    private final DurationSampler durationSampler;
    private final long retryWindowMs;
    private final long outcomeWaitWindowMs;
    private final long approachBaseWaitMs;
    private final long approachWaitPerTileMs;
    private final long approachMaxWaitMs;
    private final double spreadDefault;
    private final double spreadNarrow;
    private final double spreadWide;

    public FishingRuntimeService(
        FishingActivityRuntime fishingActivityRuntime,
        DurationSampler durationSampler,
        long retryWindowMs,
        long outcomeWaitWindowMs,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        double spreadDefault,
        double spreadNarrow,
        double spreadWide
    ) {
        this.fishingActivityRuntime = fishingActivityRuntime;
        this.durationSampler = durationSampler;
        this.retryWindowMs = retryWindowMs;
        this.outcomeWaitWindowMs = outcomeWaitWindowMs;
        this.approachBaseWaitMs = approachBaseWaitMs;
        this.approachWaitPerTileMs = approachWaitPerTileMs;
        this.approachMaxWaitMs = approachMaxWaitMs;
        this.spreadDefault = spreadDefault;
        this.spreadNarrow = spreadNarrow;
        this.spreadWide = spreadWide;
    }

    public void extendRetryWindow() {
        fishingActivityRuntime.extendRetryWindow(durationSampler.sample(retryWindowMs, 200L, spreadDefault));
    }

    public void beginOutcomeWaitWindow() {
        fishingActivityRuntime.beginOutcomeWaitWindow(durationSampler.sample(outcomeWaitWindowMs, 300L, spreadNarrow));
    }

    public void clearOutcomeWaitWindow() {
        fishingActivityRuntime.clearOutcomeWaitWindow();
    }

    public void noteTargetAttempt(Player localPlayer, NPC targetNpc) {
        fishingActivityRuntime.noteTargetAttempt(
            localPlayer,
            targetNpc,
            durationSampler.sample(approachBaseWaitMs, 300L, spreadDefault),
            durationSampler.sample(approachWaitPerTileMs, 40L, spreadWide),
            durationSampler.sample(approachMaxWaitMs, 500L, spreadNarrow),
            durationSampler.sample(outcomeWaitWindowMs, 300L, spreadNarrow)
        );
    }

    public void clearTargetAttempt() {
        fishingActivityRuntime.clearTargetAttempt();
    }

    public void noteDispatchAttempt(NPC targetNpc, long nowMs) {
        fishingActivityRuntime.noteDispatchAttempt(targetNpc, nowMs);
    }

    public void clearInteractionWindows() {
        fishingActivityRuntime.clearInteractionWindows();
    }

    public void clearInteractionWindowsPreserveDispatchSignal() {
        fishingActivityRuntime.clearInteractionWindowsPreserveDispatchSignal();
    }
}
