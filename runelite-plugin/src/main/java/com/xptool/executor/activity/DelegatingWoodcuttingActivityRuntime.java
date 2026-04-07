package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;

public final class DelegatingWoodcuttingActivityRuntime implements WoodcuttingActivityRuntime {
    private final SkillingRuntimeTransitions skillingRuntimeTransitions;

    public DelegatingWoodcuttingActivityRuntime(SkillingRuntimeTransitions skillingRuntimeTransitions) {
        this.skillingRuntimeTransitions = skillingRuntimeTransitions;
    }

    @Override
    public void extendRetryWindow(long durationMs) {
        skillingRuntimeTransitions.extendWoodcutRetryWindow(durationMs);
    }

    @Override
    public void beginOutcomeWaitWindow(long durationMs) {
        skillingRuntimeTransitions.beginWoodcutOutcomeWaitWindow(durationMs);
    }

    @Override
    public void clearOutcomeWaitWindow() {
        skillingRuntimeTransitions.clearWoodcutOutcomeWaitWindow();
    }

    @Override
    public void noteTargetAttempt(
        WorldPoint localWorldPoint,
        WorldPoint targetWorldPoint,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long approachMinHoldMs
    ) {
        skillingRuntimeTransitions.noteWoodcutTargetAttempt(
            localWorldPoint,
            targetWorldPoint,
            approachBaseWaitMs,
            approachWaitPerTileMs,
            approachMaxWaitMs,
            approachMinHoldMs
        );
    }

    @Override
    public void clearTargetAttempt() {
        skillingRuntimeTransitions.clearWoodcutTargetAttempt();
    }

    @Override
    public void noteDispatchAttempt(WorldPoint targetWorldPoint, long nowMs) {
        skillingRuntimeTransitions.noteWoodcutDispatchAttempt(targetWorldPoint, nowMs);
    }

    @Override
    public void clearDispatchAttempt() {
        skillingRuntimeTransitions.clearWoodcutDispatchAttempt();
    }

    @Override
    public void clearInteractionWindows() {
        skillingRuntimeTransitions.clearWoodcutInteractionWindows();
    }

    @Override
    public void clearInteractionWindowsPreserveDispatchSignal() {
        skillingRuntimeTransitions.clearWoodcutInteractionWindowsPreserveDispatchSignal();
    }
}
