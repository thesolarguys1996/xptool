package com.xptool.executor;

final class RuntimeTickOrchestrator {
    interface Host {
        void setCurrentExecutorTick(int tick);
        void resetTickWorkState();
        boolean isShadowOnlyWithoutBridgeLiveDispatch();
        void processShadowCommandRows(int tick);
        void refreshSceneCacheForTick(int tick);
        void suppressIdleMotionIfCommandTrafficActive();
        void advancePendingMouseMove(int tick);
        void tickMotorProgram(int tick);
        void setProcessedCommandRowsThisTick(int count);
        int processGameTickCommandRows(int tick);
        void onLayeredRuntimeGameTick();
        void maybeRunRandomEventRuntimeOnGameTick(int tick);
        void runDropSessionOnGameTickWithOwner(int tick);
        void runInteractionSessionOnGameTickWithOwner(int tick);
        void maybeRunIdleRuntimeOnGameTick(int tick);
        void maybeAdvanceLoginBreakRuntimeOnGameTick(int tick);
        void maybeEmitExecutorDebugCounters();
        void resetClientPumpState();
        void pumpPendingCommandOnClientTickWhenLoggedOut(int tick);
        void maybeAdvanceLoginRuntimeOnClientTickWhenLoggedOut(int tick);
        void advanceLogoutRuntimeOnClientTick(int tick);
        void maybeAdvanceDropSessionOnClientTick(int tick);
    }

    private final Host host;

    RuntimeTickOrchestrator(Host host) {
        this.host = host;
    }

    void onGameTick(int tick) {
        host.setCurrentExecutorTick(tick);
        host.resetTickWorkState();
        if (host.isShadowOnlyWithoutBridgeLiveDispatch()) {
            host.processShadowCommandRows(tick);
            return;
        }
        host.refreshSceneCacheForTick(tick);
        host.suppressIdleMotionIfCommandTrafficActive();
        host.advancePendingMouseMove(tick);
        host.tickMotorProgram(tick);

        host.setProcessedCommandRowsThisTick(0);
        try {
            host.setProcessedCommandRowsThisTick(host.processGameTickCommandRows(tick));
            host.onLayeredRuntimeGameTick();
            host.maybeRunRandomEventRuntimeOnGameTick(tick);
            host.runDropSessionOnGameTickWithOwner(tick);
            host.runInteractionSessionOnGameTickWithOwner(tick);
            host.maybeRunIdleRuntimeOnGameTick(tick);
            host.maybeAdvanceLoginBreakRuntimeOnGameTick(tick);
        } finally {
            host.maybeEmitExecutorDebugCounters();
        }
    }

    void onClientTick(int tick) {
        host.setCurrentExecutorTick(tick);
        if (host.isShadowOnlyWithoutBridgeLiveDispatch()) {
            host.processShadowCommandRows(tick);
            return;
        }
        host.resetClientPumpState();
        host.suppressIdleMotionIfCommandTrafficActive();
        host.advancePendingMouseMove(tick);
        host.tickMotorProgram(tick);
        host.pumpPendingCommandOnClientTickWhenLoggedOut(tick);
        host.maybeAdvanceLoginRuntimeOnClientTickWhenLoggedOut(tick);
        host.advanceLogoutRuntimeOnClientTick(tick);
        host.maybeAdvanceDropSessionOnClientTick(tick);
    }
}

