package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;

public final class DelegatingMiningActivityRuntime implements MiningActivityRuntime {
    private final SkillingRuntimeTransitions skillingRuntimeTransitions;

    public DelegatingMiningActivityRuntime(SkillingRuntimeTransitions skillingRuntimeTransitions) {
        this.skillingRuntimeTransitions = skillingRuntimeTransitions;
    }

    @Override
    public void extendRetryWindow(long durationMs) {
        skillingRuntimeTransitions.extendMiningRetryWindow(durationMs);
    }

    @Override
    public void beginOutcomeWaitWindow(long durationMs) {
        skillingRuntimeTransitions.beginMiningOutcomeWaitWindow(durationMs);
    }

    @Override
    public void clearOutcomeWaitWindow() {
        skillingRuntimeTransitions.clearMiningOutcomeWaitWindow();
    }

    @Override
    public void clearInteractionWindows() {
        skillingRuntimeTransitions.clearMiningInteractionWindows();
    }

    @Override
    public void suppressRockTarget(WorldPoint worldPoint, long durationMs) {
        skillingRuntimeTransitions.suppressMiningRockTarget(worldPoint, durationMs);
    }

    @Override
    public boolean isRockSuppressed(WorldPoint worldPoint) {
        return skillingRuntimeTransitions.isMiningRockSuppressed(worldPoint);
    }

    @Override
    public void pruneRockSuppression() {
        skillingRuntimeTransitions.pruneMiningRockSuppression();
    }
}
