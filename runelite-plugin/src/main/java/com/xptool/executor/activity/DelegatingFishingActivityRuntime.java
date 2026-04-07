package com.xptool.executor.activity;

import net.runelite.api.NPC;
import net.runelite.api.Player;

public final class DelegatingFishingActivityRuntime implements FishingActivityRuntime {
    private final SkillingRuntimeTransitions skillingRuntimeTransitions;

    public DelegatingFishingActivityRuntime(SkillingRuntimeTransitions skillingRuntimeTransitions) {
        this.skillingRuntimeTransitions = skillingRuntimeTransitions;
    }

    @Override
    public void extendRetryWindow(long durationMs) {
        skillingRuntimeTransitions.extendFishingRetryWindow(durationMs);
    }

    @Override
    public void beginOutcomeWaitWindow(long durationMs) {
        skillingRuntimeTransitions.beginFishingOutcomeWaitWindow(durationMs);
    }

    @Override
    public void clearOutcomeWaitWindow() {
        skillingRuntimeTransitions.clearFishingOutcomeWaitWindow();
    }

    @Override
    public void noteTargetAttempt(
        Player localPlayer,
        NPC targetNpc,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long outcomeWaitWindowMs
    ) {
        skillingRuntimeTransitions.noteFishingTargetAttempt(
            localPlayer,
            targetNpc,
            approachBaseWaitMs,
            approachWaitPerTileMs,
            approachMaxWaitMs,
            outcomeWaitWindowMs
        );
    }

    @Override
    public void clearTargetAttempt() {
        skillingRuntimeTransitions.clearFishingTargetAttempt();
    }

    @Override
    public void noteDispatchAttempt(NPC targetNpc, long nowMs) {
        skillingRuntimeTransitions.noteFishingDispatchAttempt(targetNpc, nowMs);
    }

    @Override
    public void clearInteractionWindows() {
        skillingRuntimeTransitions.clearFishingInteractionWindows();
    }

    @Override
    public void clearInteractionWindowsPreserveDispatchSignal() {
        skillingRuntimeTransitions.clearFishingInteractionWindowsPreserveDispatchSignal();
    }
}
