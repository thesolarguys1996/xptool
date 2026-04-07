package com.xptool.executor.activity;

import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public interface SkillingRuntimeTransitions {
    void extendWoodcutRetryWindow(long durationMs);

    void beginWoodcutOutcomeWaitWindow(long durationMs);

    void clearWoodcutOutcomeWaitWindow();

    void noteWoodcutTargetAttempt(
        WorldPoint localWorldPoint,
        WorldPoint targetWorldPoint,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long approachMinHoldMs
    );

    void clearWoodcutTargetAttempt();

    void noteWoodcutDispatchAttempt(WorldPoint targetWorldPoint, long nowMs);

    void clearWoodcutDispatchAttempt();

    void clearWoodcutInteractionWindows();

    void clearWoodcutInteractionWindowsPreserveDispatchSignal();

    void extendMiningRetryWindow(long durationMs);

    void beginMiningOutcomeWaitWindow(long durationMs);

    void clearMiningOutcomeWaitWindow();

    void clearMiningInteractionWindows();

    void suppressMiningRockTarget(WorldPoint worldPoint, long durationMs);

    boolean isMiningRockSuppressed(WorldPoint worldPoint);

    void pruneMiningRockSuppression();

    void extendFishingRetryWindow(long durationMs);

    void beginFishingOutcomeWaitWindow(long durationMs);

    void clearFishingOutcomeWaitWindow();

    void noteFishingTargetAttempt(
        Player localPlayer,
        NPC targetNpc,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long outcomeWaitWindowMs
    );

    void clearFishingTargetAttempt();

    void noteFishingDispatchAttempt(NPC targetNpc, long nowMs);

    void clearFishingInteractionWindows();

    void clearFishingInteractionWindowsPreserveDispatchSignal();
}
