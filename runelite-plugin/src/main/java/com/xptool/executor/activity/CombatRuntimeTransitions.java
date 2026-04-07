package com.xptool.executor.activity;

import net.runelite.api.NPC;

public interface CombatRuntimeTransitions {
    void extendCombatRetryWindow(long durationMs);

    void beginCombatOutcomeWaitWindow(long durationMs);

    void clearCombatOutcomeWaitWindow();

    void clearCombatInteractionWindows();

    void updateBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance);

    void noteCombatTargetAttempt(NPC npc);

    void clearCombatTargetAttempt();

    boolean isPostOutcomeSettleGraceActive(long nowMs, long graceWindowMs);

    void suppressCombatNpcTarget(int npcIndex, long durationMs);

    boolean isCombatNpcSuppressed(int npcIndex);

    void pruneCombatNpcSuppression();
}
