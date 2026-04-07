package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

final class ExecutorRuntimeCoordinatorHostFactory {
    @FunctionalInterface
    interface ManualMetricsSignalFn {
        boolean hasSignal(String consumer, boolean emitWhenMissing);
    }

    @FunctionalInterface
    interface ManualMetricsGateEventFn {
        void emit(String consumer, String reason, JsonObject details);
    }

    private ExecutorRuntimeCoordinatorHostFactory() {
    }

    static LoginBreakRuntimeCoordinator.Host createLoginBreakRuntimeHost(
        ManualMetricsSignalFn hasManualMetricsRuntimeSignalFor,
        Supplier<BreakRuntimeState> breakRuntimeState,
        Runnable breakRuntimeDisarm,
        ManualMetricsGateEventFn maybeEmitManualMetricsRuntimeGateEvent,
        Function<Object[], JsonObject> details,
        BooleanSupplier loginBreakRuntimeEnabled,
        BooleanSupplier loginBreakRuntimeAutoArm,
        Supplier<BreakProfile> resolveManualMetricsBreakProfile,
        Consumer<BreakProfile> breakRuntimeArm,
        IntConsumer breakRuntimeOnGameTick
    ) {
        return new LoginBreakRuntimeCoordinator.Host() {
            @Override
            public boolean hasManualMetricsRuntimeSignalFor(String consumer, boolean emitWhenMissing) {
                return hasManualMetricsRuntimeSignalFor.hasSignal(consumer, emitWhenMissing);
            }

            @Override
            public BreakRuntimeState breakRuntimeState() {
                return breakRuntimeState.get();
            }

            @Override
            public void breakRuntimeDisarm() {
                breakRuntimeDisarm.run();
            }

            @Override
            public void maybeEmitManualMetricsRuntimeGateEvent(String consumer, String reason, JsonObject detailsObj) {
                maybeEmitManualMetricsRuntimeGateEvent.emit(consumer, reason, detailsObj);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public boolean loginBreakRuntimeEnabled() {
                return loginBreakRuntimeEnabled.getAsBoolean();
            }

            @Override
            public boolean loginBreakRuntimeAutoArm() {
                return loginBreakRuntimeAutoArm.getAsBoolean();
            }

            @Override
            public BreakProfile resolveManualMetricsBreakProfile() {
                return resolveManualMetricsBreakProfile.get();
            }

            @Override
            public void breakRuntimeArm(BreakProfile profile) {
                breakRuntimeArm.accept(profile);
            }

            @Override
            public void breakRuntimeOnGameTick(int tick) {
                breakRuntimeOnGameTick.accept(tick);
            }
        };
    }

    static RuntimeTickOrchestrator.Host createRuntimeTickOrchestratorHost(
        IntConsumer setCurrentExecutorTick,
        Runnable resetTickWorkState,
        BooleanSupplier isShadowOnlyWithoutBridgeLiveDispatch,
        IntConsumer processShadowCommandRows,
        IntConsumer refreshSceneCacheForTick,
        Runnable suppressIdleMotionIfCommandTrafficActive,
        IntConsumer advancePendingMouseMove,
        IntConsumer tickMotorProgram,
        IntConsumer setProcessedCommandRowsThisTick,
        IntUnaryOperator processGameTickCommandRows,
        IntConsumer maybeRunRandomEventRuntimeOnGameTick,
        IntConsumer runDropSessionOnGameTickWithOwner,
        IntConsumer runInteractionSessionOnGameTickWithOwner,
        IntConsumer maybeRunIdleRuntimeOnGameTick,
        IntConsumer maybeAdvanceLoginBreakRuntimeOnGameTick,
        Runnable maybeEmitExecutorDebugCounters,
        Runnable resetClientPumpState,
        IntConsumer pumpPendingCommandOnClientTickWhenLoggedOut,
        IntConsumer maybeAdvanceLoginRuntimeOnClientTickWhenLoggedOut,
        IntConsumer advanceLogoutRuntimeOnClientTick,
        IntConsumer maybeAdvanceDropSessionOnClientTick,
        Runnable onLayeredRuntimeGameTick
    ) {
        return new RuntimeTickOrchestrator.Host() {
            @Override
            public void setCurrentExecutorTick(int tick) {
                setCurrentExecutorTick.accept(tick);
            }

            @Override
            public void resetTickWorkState() {
                resetTickWorkState.run();
            }

            @Override
            public boolean isShadowOnlyWithoutBridgeLiveDispatch() {
                return isShadowOnlyWithoutBridgeLiveDispatch.getAsBoolean();
            }

            @Override
            public void processShadowCommandRows(int tick) {
                processShadowCommandRows.accept(tick);
            }

            @Override
            public void refreshSceneCacheForTick(int tick) {
                refreshSceneCacheForTick.accept(tick);
            }

            @Override
            public void suppressIdleMotionIfCommandTrafficActive() {
                suppressIdleMotionIfCommandTrafficActive.run();
            }

            @Override
            public void advancePendingMouseMove(int tick) {
                advancePendingMouseMove.accept(tick);
            }

            @Override
            public void tickMotorProgram(int tick) {
                tickMotorProgram.accept(tick);
            }

            @Override
            public void setProcessedCommandRowsThisTick(int count) {
                setProcessedCommandRowsThisTick.accept(count);
            }

            @Override
            public int processGameTickCommandRows(int tick) {
                return processGameTickCommandRows.applyAsInt(tick);
            }

            @Override
            public void onLayeredRuntimeGameTick() {
                onLayeredRuntimeGameTick.run();
            }

            @Override
            public void maybeRunRandomEventRuntimeOnGameTick(int tick) {
                maybeRunRandomEventRuntimeOnGameTick.accept(tick);
            }

            @Override
            public void runDropSessionOnGameTickWithOwner(int tick) {
                runDropSessionOnGameTickWithOwner.accept(tick);
            }

            @Override
            public void runInteractionSessionOnGameTickWithOwner(int tick) {
                runInteractionSessionOnGameTickWithOwner.accept(tick);
            }

            @Override
            public void maybeRunIdleRuntimeOnGameTick(int tick) {
                maybeRunIdleRuntimeOnGameTick.accept(tick);
            }

            @Override
            public void maybeAdvanceLoginBreakRuntimeOnGameTick(int tick) {
                maybeAdvanceLoginBreakRuntimeOnGameTick.accept(tick);
            }

            @Override
            public void maybeEmitExecutorDebugCounters() {
                maybeEmitExecutorDebugCounters.run();
            }

            @Override
            public void resetClientPumpState() {
                resetClientPumpState.run();
            }

            @Override
            public void pumpPendingCommandOnClientTickWhenLoggedOut(int tick) {
                pumpPendingCommandOnClientTickWhenLoggedOut.accept(tick);
            }

            @Override
            public void maybeAdvanceLoginRuntimeOnClientTickWhenLoggedOut(int tick) {
                maybeAdvanceLoginRuntimeOnClientTickWhenLoggedOut.accept(tick);
            }

            @Override
            public void advanceLogoutRuntimeOnClientTick(int tick) {
                advanceLogoutRuntimeOnClientTick.accept(tick);
            }

            @Override
            public void maybeAdvanceDropSessionOnClientTick(int tick) {
                maybeAdvanceDropSessionOnClientTick.accept(tick);
            }
        };
    }

    static LifecycleShutdownService.Host createLifecycleShutdownHost(
        Runnable releasePendingIdleCameraDrag,
        Runnable stopCommandIngestor,
        Runnable clearPendingCommands,
        Runnable clearDropSweepSessionRegistration,
        BooleanSupplier isLogoutRuntimeActiveOrSuccessful,
        Runnable requestLogoutRuntimeStop,
        BooleanSupplier isLoginBreakRuntimeEnabled,
        Runnable disarmBreakRuntime,
        Runnable requestLoginRuntimeStop,
        Runnable cancelResumePlanner,
        Runnable cancelHumanTyping,
        Runnable emitPendingTelemetryRollup,
        Runnable shutdownInteractionSession
    ) {
        return new LifecycleShutdownService.Host() {
            @Override
            public void releasePendingIdleCameraDrag() {
                releasePendingIdleCameraDrag.run();
            }

            @Override
            public void stopCommandIngestor() {
                stopCommandIngestor.run();
            }

            @Override
            public void clearPendingCommands() {
                clearPendingCommands.run();
            }

            @Override
            public void clearDropSweepSessionRegistration() {
                clearDropSweepSessionRegistration.run();
            }

            @Override
            public boolean isLogoutRuntimeActiveOrSuccessful() {
                return isLogoutRuntimeActiveOrSuccessful.getAsBoolean();
            }

            @Override
            public void requestLogoutRuntimeStop() {
                requestLogoutRuntimeStop.run();
            }

            @Override
            public boolean isLoginBreakRuntimeEnabled() {
                return isLoginBreakRuntimeEnabled.getAsBoolean();
            }

            @Override
            public void disarmBreakRuntime() {
                disarmBreakRuntime.run();
            }

            @Override
            public void requestLoginRuntimeStop() {
                requestLoginRuntimeStop.run();
            }

            @Override
            public void cancelResumePlanner() {
                cancelResumePlanner.run();
            }

            @Override
            public void cancelHumanTyping() {
                cancelHumanTyping.run();
            }

            @Override
            public void emitPendingTelemetryRollup() {
                emitPendingTelemetryRollup.run();
            }

            @Override
            public void shutdownInteractionSession() {
                shutdownInteractionSession.run();
            }
        };
    }
}
