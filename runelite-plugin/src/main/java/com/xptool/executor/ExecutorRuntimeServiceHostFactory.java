package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.sessions.BankSession;
import com.xptool.sessions.DropSession;
import com.xptool.sessions.InteractionSession;
import com.xptool.sessions.idle.FishingIdleMode;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import net.runelite.api.GameState;

final class ExecutorRuntimeServiceHostFactory {
    @FunctionalInterface
    interface CommandRowTickEvaluator {
        ExecutionOutcome evaluate(CommandRow row, int tick);
    }

    @FunctionalInterface
    interface EmitFn {
        void emit(String status, CommandRow row, String reason, JsonObject details, String eventType);
    }

    @FunctionalInterface
    interface ManualMetricsSignalFn {
        boolean hasSignal(String consumer, boolean emitWhenMissing);
    }

    @FunctionalInterface
    interface AcquireMotorFn {
        boolean acquire(String owner, long leaseMs);
    }

    private ExecutorRuntimeServiceHostFactory() {
    }

    static RuntimeShutdownService.Host createRuntimeShutdownHost(
        Runnable releasePendingIdleCameraDrag,
        Runnable clearPendingCommands,
        Runnable clearPendingMouseMove,
        Supplier<MotorProgram> activeMotorProgram,
        BiConsumer<MotorProgram, String> cancelMotorProgram,
        Runnable clearActiveMotorProgram,
        Runnable endDropSweepSession,
        Runnable shutdownInteractionSession,
        Consumer<String> releaseSessionMotor,
        Runnable releaseIdleMotorOwnershipForRuntimeTeardown,
        Runnable disarmAllIdleActivities,
        Runnable clearIdleTraversalBankSuppressionGate,
        Runnable clearWoodcuttingInteractionWindows,
        Runnable clearMiningInteractionWindows,
        Runnable clearFishingInteractionWindows,
        Runnable clearFishingTargetLock,
        Runnable clearCombatTargetAttempt,
        Runnable resetInventoryDropPointHistory,
        Runnable resetFatigueRuntime,
        Runnable resetLoginSubmitState
    ) {
        return new RuntimeShutdownService.Host() {
            @Override
            public void releasePendingIdleCameraDrag() {
                releasePendingIdleCameraDrag.run();
            }

            @Override
            public void clearPendingCommands() {
                clearPendingCommands.run();
            }

            @Override
            public void clearPendingMouseMove() {
                clearPendingMouseMove.run();
            }

            @Override
            public MotorProgram activeMotorProgram() {
                return activeMotorProgram.get();
            }

            @Override
            public void cancelMotorProgram(MotorProgram program, String reason) {
                cancelMotorProgram.accept(program, reason);
            }

            @Override
            public void clearActiveMotorProgram() {
                clearActiveMotorProgram.run();
            }

            @Override
            public void endDropSweepSession() {
                endDropSweepSession.run();
            }

            @Override
            public void shutdownInteractionSession() {
                shutdownInteractionSession.run();
            }

            @Override
            public void releaseSessionMotor(String owner) {
                releaseSessionMotor.accept(owner);
            }

            @Override
            public void releaseIdleMotorOwnershipForRuntimeTeardown() {
                releaseIdleMotorOwnershipForRuntimeTeardown.run();
            }

            @Override
            public void disarmAllIdleActivities() {
                disarmAllIdleActivities.run();
            }

            @Override
            public void clearIdleTraversalBankSuppressionGate() {
                clearIdleTraversalBankSuppressionGate.run();
            }

            @Override
            public void clearWoodcuttingInteractionWindows() {
                clearWoodcuttingInteractionWindows.run();
            }

            @Override
            public void clearMiningInteractionWindows() {
                clearMiningInteractionWindows.run();
            }

            @Override
            public void clearFishingInteractionWindows() {
                clearFishingInteractionWindows.run();
            }

            @Override
            public void clearFishingTargetLock() {
                clearFishingTargetLock.run();
            }

            @Override
            public void clearCombatTargetAttempt() {
                clearCombatTargetAttempt.run();
            }

            @Override
            public void resetInventoryDropPointHistory() {
                resetInventoryDropPointHistory.run();
            }

            @Override
            public void resetFatigueRuntime() {
                resetFatigueRuntime.run();
            }

            @Override
            public void resetLoginSubmitState() {
                resetLoginSubmitState.run();
            }
        };
    }

    static CommandIngestLifecycleService.Host createCommandIngestLifecycleHost(
        Supplier<CommandRow> peekPendingCommand,
        Runnable pollPendingCommand,
        CommandRowTickEvaluator evaluateCommandRow,
        CommandRowTickEvaluator evaluateCommandRowShadow,
        BiConsumer<CommandRow, ExecutionOutcome> maybeExtendIdleTraversalOrBankSuppression,
        Predicate<ExecutionOutcome> isShadowWouldDispatchOutcome,
        IntSupplier maxMechanicalDispatchesPerTick,
        Function<Object[], JsonObject> details,
        Runnable noteInteractionActivityNow,
        EmitFn emit,
        Supplier<GameState> gameState
    ) {
        return new CommandIngestLifecycleService.Host() {
            @Override
            public CommandRow peekPendingCommand() {
                return peekPendingCommand.get();
            }

            @Override
            public void pollPendingCommand() {
                pollPendingCommand.run();
            }

            @Override
            public ExecutionOutcome evaluateCommandRow(CommandRow row, int currentTick) {
                return evaluateCommandRow.evaluate(row, currentTick);
            }

            @Override
            public ExecutionOutcome evaluateCommandRowShadow(CommandRow row, int currentTick) {
                return evaluateCommandRowShadow.evaluate(row, currentTick);
            }

            @Override
            public void maybeExtendIdleTraversalOrBankSuppression(CommandRow row, ExecutionOutcome outcome) {
                maybeExtendIdleTraversalOrBankSuppression.accept(row, outcome);
            }

            @Override
            public boolean isShadowWouldDispatchOutcome(ExecutionOutcome outcome) {
                return isShadowWouldDispatchOutcome.test(outcome);
            }

            @Override
            public int maxMechanicalDispatchesPerTick() {
                return maxMechanicalDispatchesPerTick.getAsInt();
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public void noteInteractionActivityNow() {
                noteInteractionActivityNow.run();
            }

            @Override
            public void emit(String status, CommandRow row, String reason, JsonObject detailsObj, String eventType) {
                emit.emit(status, row, reason, detailsObj, eventType);
            }

            @Override
            public GameState gameState() {
                return gameState.get();
            }
        };
    }

    static CommandDispatchService.Host createCommandDispatchHost(
        Supplier<Integer> currentExecutorTick,
        java.util.function.BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        Supplier<CommandExecutor.CommandDecision> stopAllRuntime,
        java.util.function.BooleanSupplier loginBreakRuntimeEnabled,
        Consumer<JsonObject> applyIdleCadenceTuningFromPayload,
        ManualMetricsSignalFn hasManualMetricsRuntimeSignalFor,
        Supplier<GameState> gameState,
        java.util.function.BooleanSupplier isLoginRuntimeActive,
        Supplier<String> loginRuntimeStateName,
        java.util.function.BooleanSupplier startLoginRuntime,
        java.util.function.BooleanSupplier startLogoutRuntime,
        Runnable advanceLogoutRuntimeOnCurrentTick,
        java.util.function.BooleanSupplier isLogoutRuntimeFailedHardStop,
        java.util.function.BooleanSupplier isLogoutRuntimeSuccessful,
        Supplier<String> logoutRuntimeStateName,
        java.util.function.BooleanSupplier isLogoutRuntimeActive,
        Supplier<String> logoutRuntimeLastFailureReason,
        Function<String, Optional<FishingIdleMode>> tryParseFishingIdleMode,
        Supplier<FishingIdleMode> configuredFishingIdleMode,
        Consumer<FishingIdleMode> setConfiguredFishingIdleMode,
        Consumer<Boolean> setFishingIdleModeOverrideEnabled,
        java.util.function.BooleanSupplier idleActivityGateEnabled,
        Supplier<Set<String>> idleActivityAllowlist,
        Supplier<IdleArmingService> idleArmingService,
        Supplier<String> idleArmSourceFishingModeOverride,
        Function<String, String> pushMotorOwnerContext,
        Function<String, String> pushClickTypeContext,
        Consumer<String> popMotorOwnerContext,
        Consumer<String> popClickTypeContext,
        AcquireMotorFn acquireOrRenewMotorOwner,
        ToLongFunction<String> motorLeaseMsForOwner,
        Supplier<BankSession> bankSession,
        Supplier<DropSession> dropSession,
        Supplier<InteractionSession> interactionSession,
        Supplier<CommandExecutor.CommandDecision> rejectUnsupportedCommandType
    ) {
        return new CommandDispatchService.Host() {
            @Override
            public int currentExecutorTick() {
                return currentExecutorTick.get();
            }

            @Override
            public CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject detailsObj) {
                return acceptDecision.apply(reason, detailsObj);
            }

            @Override
            public CommandExecutor.CommandDecision rejectDecision(String reason) {
                return rejectDecision.apply(reason);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public String safeString(String value) {
                return safeString.apply(value);
            }

            @Override
            public CommandExecutor.CommandDecision stopAllRuntime() {
                return stopAllRuntime.get();
            }

            @Override
            public boolean loginBreakRuntimeEnabled() {
                return loginBreakRuntimeEnabled.getAsBoolean();
            }

            @Override
            public void applyIdleCadenceTuningFromPayload(JsonObject payload) {
                applyIdleCadenceTuningFromPayload.accept(payload);
            }

            @Override
            public boolean hasManualMetricsRuntimeSignalFor(String consumer, boolean emitWhenMissing) {
                return hasManualMetricsRuntimeSignalFor.hasSignal(consumer, emitWhenMissing);
            }

            @Override
            public GameState gameState() {
                return gameState.get();
            }

            @Override
            public boolean isLoginRuntimeActive() {
                return isLoginRuntimeActive.getAsBoolean();
            }

            @Override
            public String loginRuntimeStateName() {
                return loginRuntimeStateName.get();
            }

            @Override
            public boolean startLoginRuntime() {
                return startLoginRuntime.getAsBoolean();
            }

            @Override
            public boolean startLogoutRuntime() {
                return startLogoutRuntime.getAsBoolean();
            }

            @Override
            public void advanceLogoutRuntimeOnCurrentTick() {
                advanceLogoutRuntimeOnCurrentTick.run();
            }

            @Override
            public boolean isLogoutRuntimeFailedHardStop() {
                return isLogoutRuntimeFailedHardStop.getAsBoolean();
            }

            @Override
            public boolean isLogoutRuntimeSuccessful() {
                return isLogoutRuntimeSuccessful.getAsBoolean();
            }

            @Override
            public String logoutRuntimeStateName() {
                return logoutRuntimeStateName.get();
            }

            @Override
            public boolean isLogoutRuntimeActive() {
                return isLogoutRuntimeActive.getAsBoolean();
            }

            @Override
            public String logoutRuntimeLastFailureReason() {
                return logoutRuntimeLastFailureReason.get();
            }

            @Override
            public Optional<FishingIdleMode> tryParseFishingIdleMode(String raw) {
                return tryParseFishingIdleMode.apply(raw);
            }

            @Override
            public FishingIdleMode configuredFishingIdleMode() {
                return configuredFishingIdleMode.get();
            }

            @Override
            public void setConfiguredFishingIdleMode(FishingIdleMode mode) {
                setConfiguredFishingIdleMode.accept(mode);
            }

            @Override
            public void setFishingIdleModeOverrideEnabled(boolean enabled) {
                setFishingIdleModeOverrideEnabled.accept(enabled);
            }

            @Override
            public boolean idleActivityGateEnabled() {
                return idleActivityGateEnabled.getAsBoolean();
            }

            @Override
            public Set<String> idleActivityAllowlist() {
                return idleActivityAllowlist.get();
            }

            @Override
            public IdleArmingService idleArmingService() {
                return idleArmingService.get();
            }

            @Override
            public String idleArmSourceFishingModeOverride() {
                return idleArmSourceFishingModeOverride.get();
            }

            @Override
            public String pushMotorOwnerContext(String owner) {
                return pushMotorOwnerContext.apply(owner);
            }

            @Override
            public String pushClickTypeContext(String clickType) {
                return pushClickTypeContext.apply(clickType);
            }

            @Override
            public void popMotorOwnerContext(String previousContext) {
                popMotorOwnerContext.accept(previousContext);
            }

            @Override
            public void popClickTypeContext(String previousContext) {
                popClickTypeContext.accept(previousContext);
            }

            @Override
            public boolean acquireOrRenewMotorOwner(String owner, long leaseMs) {
                return acquireOrRenewMotorOwner.acquire(owner, leaseMs);
            }

            @Override
            public long motorLeaseMsForOwner(String owner) {
                return motorLeaseMsForOwner.applyAsLong(owner);
            }

            @Override
            public BankSession bankSession() {
                return bankSession.get();
            }

            @Override
            public DropSession dropSession() {
                return dropSession.get();
            }

            @Override
            public InteractionSession interactionSession() {
                return interactionSession.get();
            }

            @Override
            public CommandExecutor.CommandDecision rejectUnsupportedCommandType() {
                return rejectUnsupportedCommandType.get();
            }
        };
    }
}
