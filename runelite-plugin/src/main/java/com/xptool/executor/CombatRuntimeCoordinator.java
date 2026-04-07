package com.xptool.executor;

import com.xptool.executor.activity.CombatRuntimeTransitions;
import net.runelite.api.NPC;

final class CombatRuntimeCoordinator implements CombatRuntimeTransitions {
    private final CombatRuntime combatRuntime;

    CombatRuntimeCoordinator(CombatRuntime combatRuntime) {
        this.combatRuntime = combatRuntime;
    }

    @Override
    public void extendCombatRetryWindow(long durationMs) {
        combatRuntime.extendRetryWindow(durationMs);
    }

    @Override
    public void beginCombatOutcomeWaitWindow(long durationMs) {
        combatRuntime.beginOutcomeWaitWindow(durationMs);
    }

    @Override
    public void clearCombatOutcomeWaitWindow() {
        combatRuntime.clearOutcomeWaitWindow();
    }

    @Override
    public void clearCombatInteractionWindows() {
        combatRuntime.clearInteractionWindows();
    }

    @Override
    public void updateBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance) {
        combatRuntime.updateBoundary(targetWorldX, targetWorldY, targetMaxDistance);
    }

    @Override
    public void noteCombatTargetAttempt(NPC npc) {
        combatRuntime.noteTargetAttempt(npc);
    }

    @Override
    public void clearCombatTargetAttempt() {
        combatRuntime.clearTargetAttempt();
    }

    @Override
    public boolean isPostOutcomeSettleGraceActive(long nowMs, long graceWindowMs) {
        return combatRuntime.isPostOutcomeSettleGraceActive(nowMs, graceWindowMs);
    }

    @Override
    public void suppressCombatNpcTarget(int npcIndex, long durationMs) {
        combatRuntime.suppressNpcTarget(npcIndex, durationMs);
    }

    @Override
    public boolean isCombatNpcSuppressed(int npcIndex) {
        return combatRuntime.isNpcSuppressed(npcIndex);
    }

    @Override
    public void pruneCombatNpcSuppression() {
        combatRuntime.pruneNpcSuppression();
    }
}
