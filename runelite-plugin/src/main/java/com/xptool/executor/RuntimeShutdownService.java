package com.xptool.executor;

final class RuntimeShutdownService {
    interface Host {
        void releasePendingIdleCameraDrag();
        void clearPendingCommands();
        void clearPendingMouseMove();
        MotorProgram activeMotorProgram();
        void cancelMotorProgram(MotorProgram program, String reason);
        void clearActiveMotorProgram();
        void endDropSweepSession();
        void shutdownInteractionSession();
        void releaseSessionMotor(String owner);
        void releaseIdleMotorOwnershipForRuntimeTeardown();
        void disarmAllIdleActivities();
        void clearIdleTraversalBankSuppressionGate();
        void clearWoodcuttingInteractionWindows();
        void clearMiningInteractionWindows();
        void clearFishingInteractionWindows();
        void clearFishingTargetLock();
        void clearCombatTargetAttempt();
        void resetInventoryDropPointHistory();
        void resetFatigueRuntime();
        void resetLoginSubmitState();
    }

    private final Host host;

    RuntimeShutdownService(Host host) {
        this.host = host;
    }

    void stopOperationalRuntimeState(String cancelReason) {
        host.releasePendingIdleCameraDrag();
        host.clearPendingCommands();
        host.clearPendingMouseMove();
        MotorProgram program = host.activeMotorProgram();
        if (program != null && !program.toHandle().isTerminal()) {
            host.cancelMotorProgram(
                program,
                cancelReason == null || cancelReason.isBlank() ? "stop_all_runtime" : cancelReason
            );
        } else {
            host.clearActiveMotorProgram();
        }
        host.endDropSweepSession();
        host.shutdownInteractionSession();
        host.releaseSessionMotor(ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP);
        host.releaseSessionMotor(ExecutorMotorProfileCatalog.MOTOR_OWNER_BANK);
        host.releaseSessionMotor(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);
        host.releaseIdleMotorOwnershipForRuntimeTeardown();
        host.disarmAllIdleActivities();
        host.clearIdleTraversalBankSuppressionGate();
        host.clearWoodcuttingInteractionWindows();
        host.clearMiningInteractionWindows();
        host.clearFishingInteractionWindows();
        host.clearFishingTargetLock();
        host.clearCombatTargetAttempt();
        host.resetInventoryDropPointHistory();
        host.resetFatigueRuntime();
        host.resetLoginSubmitState();
    }
}
