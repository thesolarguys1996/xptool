package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;

public interface MiningActivityRuntime extends ActivityRuntime {
    @Override
    default String activityKey() {
        return "mining";
    }

    void extendRetryWindow(long durationMs);

    void beginOutcomeWaitWindow(long durationMs);

    void clearOutcomeWaitWindow();

    void clearInteractionWindows();

    void suppressRockTarget(WorldPoint worldPoint, long durationMs);

    boolean isRockSuppressed(WorldPoint worldPoint);

    void pruneRockSuppression();
}
