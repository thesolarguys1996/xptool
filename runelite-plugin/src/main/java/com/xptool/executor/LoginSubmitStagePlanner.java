package com.xptool.executor;

final class LoginSubmitStagePlanner {
    enum Stage {
        PRIMARY,
        SECONDARY
    }

    private final long secondaryGraceWindowMs;
    private long lastPrimaryDispatchAtMs = Long.MIN_VALUE;

    LoginSubmitStagePlanner(long secondaryGraceWindowMs) {
        this.secondaryGraceWindowMs = Math.max(1L, secondaryGraceWindowMs);
    }

    void reset() {
        lastPrimaryDispatchAtMs = Long.MIN_VALUE;
    }

    Stage chooseStage(boolean primaryPromptVisible, boolean secondaryPromptVisible, long nowMs) {
        if (secondaryPromptVisible) {
            return Stage.SECONDARY;
        }
        if (lastPrimaryDispatchAtMs != Long.MIN_VALUE) {
            long sincePrimaryDispatchMs = Math.max(0L, nowMs - lastPrimaryDispatchAtMs);
            if (sincePrimaryDispatchMs <= secondaryGraceWindowMs) {
                return Stage.SECONDARY;
            }
            // Even after the grace window, a follow-up submit after a primary dispatch
            // should remain on the secondary region until the planner is reset.
            return Stage.SECONDARY;
        }
        if (primaryPromptVisible) {
            return Stage.PRIMARY;
        }
        return Stage.PRIMARY;
    }

    void noteDispatched(Stage stage, long nowMs) {
        if (stage == Stage.PRIMARY) {
            lastPrimaryDispatchAtMs = nowMs;
        }
    }
}
