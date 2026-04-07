package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;

public interface WoodcuttingActivityRuntime extends ActivityRuntime {
    @Override
    default String activityKey() {
        return "woodcutting";
    }

    void extendRetryWindow(long durationMs);

    void beginOutcomeWaitWindow(long durationMs);

    void clearOutcomeWaitWindow();

    void noteTargetAttempt(
        WorldPoint localWorldPoint,
        WorldPoint targetWorldPoint,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long approachMinHoldMs
    );

    void clearTargetAttempt();

    void noteDispatchAttempt(WorldPoint targetWorldPoint, long nowMs);

    void clearDispatchAttempt();

    void clearInteractionWindows();

    void clearInteractionWindowsPreserveDispatchSignal();
}
