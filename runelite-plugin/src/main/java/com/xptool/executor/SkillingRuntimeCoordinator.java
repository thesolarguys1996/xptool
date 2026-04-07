package com.xptool.executor;

import com.xptool.activities.fishing.FishingRuntime;
import com.xptool.executor.activity.SkillingRuntimeTransitions;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

final class SkillingRuntimeCoordinator implements SkillingRuntimeTransitions {
    private final WoodcuttingRuntime woodcuttingRuntime;
    private final MiningRuntime miningRuntime;
    private final FishingRuntime fishingRuntime;

    SkillingRuntimeCoordinator(
        WoodcuttingRuntime woodcuttingRuntime,
        MiningRuntime miningRuntime,
        FishingRuntime fishingRuntime
    ) {
        this.woodcuttingRuntime = woodcuttingRuntime;
        this.miningRuntime = miningRuntime;
        this.fishingRuntime = fishingRuntime;
    }

    @Override
    public void extendWoodcutRetryWindow(long durationMs) {
        woodcuttingRuntime.extendRetryWindow(durationMs);
    }

    @Override
    public void beginWoodcutOutcomeWaitWindow(long durationMs) {
        woodcuttingRuntime.beginOutcomeWaitWindow(durationMs);
    }

    @Override
    public void clearWoodcutOutcomeWaitWindow() {
        woodcuttingRuntime.clearOutcomeWaitWindow();
    }

    @Override
    public void noteWoodcutTargetAttempt(
        WorldPoint localWorldPoint,
        WorldPoint targetWorldPoint,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long approachMinHoldMs
    ) {
        woodcuttingRuntime.noteTargetAttempt(
            localWorldPoint,
            targetWorldPoint,
            approachBaseWaitMs,
            approachWaitPerTileMs,
            approachMaxWaitMs,
            approachMinHoldMs
        );
    }

    @Override
    public void clearWoodcutTargetAttempt() {
        woodcuttingRuntime.clearTargetAttempt();
    }

    @Override
    public void noteWoodcutDispatchAttempt(WorldPoint targetWorldPoint, long nowMs) {
        woodcuttingRuntime.noteDispatchAttempt(targetWorldPoint, nowMs);
    }

    @Override
    public void clearWoodcutDispatchAttempt() {
        woodcuttingRuntime.clearDispatchAttempt();
    }

    @Override
    public void clearWoodcutInteractionWindows() {
        woodcuttingRuntime.clearInteractionWindows();
    }

    @Override
    public void clearWoodcutInteractionWindowsPreserveDispatchSignal() {
        woodcuttingRuntime.clearInteractionWindowsPreserveDispatchSignal();
    }

    @Override
    public void extendMiningRetryWindow(long durationMs) {
        miningRuntime.extendRetryWindow(durationMs);
    }

    @Override
    public void beginMiningOutcomeWaitWindow(long durationMs) {
        miningRuntime.beginOutcomeWaitWindow(durationMs);
    }

    @Override
    public void clearMiningOutcomeWaitWindow() {
        miningRuntime.clearOutcomeWaitWindow();
    }

    @Override
    public void clearMiningInteractionWindows() {
        miningRuntime.clearInteractionWindows();
    }

    @Override
    public void suppressMiningRockTarget(WorldPoint worldPoint, long durationMs) {
        miningRuntime.suppressRockTarget(worldPoint, durationMs);
    }

    @Override
    public boolean isMiningRockSuppressed(WorldPoint worldPoint) {
        return miningRuntime.isRockSuppressed(worldPoint);
    }

    @Override
    public void pruneMiningRockSuppression() {
        miningRuntime.pruneRockSuppression();
    }

    @Override
    public void extendFishingRetryWindow(long durationMs) {
        fishingRuntime.extendRetryWindow(durationMs);
    }

    @Override
    public void beginFishingOutcomeWaitWindow(long durationMs) {
        fishingRuntime.beginOutcomeWaitWindow(durationMs);
    }

    @Override
    public void clearFishingOutcomeWaitWindow() {
        fishingRuntime.clearOutcomeWaitWindow();
    }

    @Override
    public void noteFishingTargetAttempt(
        Player localPlayer,
        NPC targetNpc,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long outcomeWaitWindowMs
    ) {
        fishingRuntime.noteTargetAttempt(
            localPlayer,
            targetNpc,
            approachBaseWaitMs,
            approachWaitPerTileMs,
            approachMaxWaitMs,
            outcomeWaitWindowMs
        );
    }

    @Override
    public void clearFishingTargetAttempt() {
        fishingRuntime.clearTargetAttempt();
    }

    @Override
    public void noteFishingDispatchAttempt(NPC targetNpc, long nowMs) {
        fishingRuntime.noteDispatchAttempt(targetNpc, nowMs);
    }

    @Override
    public void clearFishingInteractionWindows() {
        fishingRuntime.clearInteractionWindows();
    }

    @Override
    public void clearFishingInteractionWindowsPreserveDispatchSignal() {
        fishingRuntime.clearInteractionWindowsPreserveDispatchSignal();
    }
}
