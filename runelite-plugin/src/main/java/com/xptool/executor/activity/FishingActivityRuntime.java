package com.xptool.executor.activity;

import net.runelite.api.NPC;
import net.runelite.api.Player;

public interface FishingActivityRuntime extends ActivityRuntime {
    @Override
    default String activityKey() {
        return "fishing";
    }

    void extendRetryWindow(long durationMs);

    void beginOutcomeWaitWindow(long durationMs);

    void clearOutcomeWaitWindow();

    void noteTargetAttempt(
        Player localPlayer,
        NPC targetNpc,
        long approachBaseWaitMs,
        long approachWaitPerTileMs,
        long approachMaxWaitMs,
        long outcomeWaitWindowMs
    );

    void clearTargetAttempt();

    void noteDispatchAttempt(NPC targetNpc, long nowMs);

    void clearInteractionWindows();

    void clearInteractionWindowsPreserveDispatchSignal();
}
