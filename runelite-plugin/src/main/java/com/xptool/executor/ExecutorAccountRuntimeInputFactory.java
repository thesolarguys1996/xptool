package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.GameState;

final class ExecutorAccountRuntimeInputFactory {
    @FunctionalInterface
    interface ManualMetricsSignalFn {
        boolean hasSignal(String consumer, boolean emitWhenMissing);
    }

    @FunctionalInterface
    interface ManualMetricsEventFn {
        void emit(String consumer, String reason, JsonObject details);
    }

    private ExecutorAccountRuntimeInputFactory() {
    }

    static ExecutorAccountRuntimeInputs.Inputs create(
        Supplier<GameState> gameStateSupplier,
        ExecutorAccountRuntimeWiring.KeywordWidgetFinder findVisibleWidgetByKeywords,
        Predicate<String> isGameStateNamed,
        BooleanSupplier isPrimaryLoginSubmitPromptVisible,
        BooleanSupplier isSecondaryLoginSubmitPromptVisible,
        BooleanSupplier ensureTypingFocus,
        ExecutorAccountRuntimeInputs.KeyPressDispatcher pressLoginKeyChord,
        Function<Object[], JsonObject> details,
        BooleanSupplier submitLogin,
        BooleanSupplier openWorldSelect,
        Supplier<LogoutInteractionController.AttemptStatus> attemptLogout,
        BooleanSupplier isGameReadyForRuntime,
        BooleanSupplier requestStopAllRuntime,
        BooleanSupplier requestLogoutForBreakStart,
        BooleanSupplier startLoginRuntime,
        BiConsumer<String, JsonObject> emitTypingEvent,
        BiConsumer<String, JsonObject> emitLoginEvent,
        BiConsumer<String, JsonObject> emitLogoutEvent,
        BiConsumer<String, JsonObject> emitResumeEvent,
        BiConsumer<String, JsonObject> emitBreakEvent
    ) {
        ExecutorAccountRuntimeInputs.Inputs inputs = new ExecutorAccountRuntimeInputs.Inputs();
        inputs.gameStateSupplier = gameStateSupplier;
        inputs.findVisibleWidgetByKeywords = findVisibleWidgetByKeywords;
        inputs.isGameStateNamed = isGameStateNamed;
        inputs.isPrimaryLoginSubmitPromptVisible = isPrimaryLoginSubmitPromptVisible;
        inputs.isSecondaryLoginSubmitPromptVisible = isSecondaryLoginSubmitPromptVisible;
        inputs.ensureTypingFocus = ensureTypingFocus;
        inputs.pressLoginKeyChord = pressLoginKeyChord;
        inputs.details = details;
        inputs.submitLogin = submitLogin;
        inputs.openWorldSelect = openWorldSelect;
        inputs.attemptLogout = attemptLogout;
        inputs.isGameReadyForRuntime = isGameReadyForRuntime;
        inputs.requestStopAllRuntime = requestStopAllRuntime;
        inputs.requestLogoutForBreakStart = requestLogoutForBreakStart;
        inputs.startLoginRuntime = startLoginRuntime;
        inputs.emitTypingEvent = emitTypingEvent;
        inputs.emitLoginEvent = emitLoginEvent;
        inputs.emitLogoutEvent = emitLogoutEvent;
        inputs.emitResumeEvent = emitResumeEvent;
        inputs.emitBreakEvent = emitBreakEvent;
        return inputs;
    }

    static AccountRuntimeTickCoordinator.Host createAccountRuntimeTickHost(
        Supplier<GameState> gameState,
        BooleanSupplier isLoginRuntimeActive,
        BooleanSupplier isLogoutRuntimeActive,
        LongSupplier currentTimeMs,
        LongSupplier loginClientTickAdvanceMinIntervalMs,
        LongSupplier logoutClientTickAdvanceMinIntervalMs,
        LongSupplier lastLoginClientAdvanceAtMs,
        LongConsumer setLastLoginClientAdvanceAtMs,
        LongSupplier lastLogoutClientAdvanceAtMs,
        LongConsumer setLastLogoutClientAdvanceAtMs,
        IntSupplier lastLoginRuntimeAdvanceTick,
        IntConsumer setLastLoginRuntimeAdvanceTick,
        IntSupplier lastLogoutRuntimeAdvanceTick,
        IntConsumer setLastLogoutRuntimeAdvanceTick,
        IntConsumer onLoginRuntimeGameTick,
        IntConsumer onLogoutRuntimeGameTick
    ) {
        return new AccountRuntimeTickCoordinator.Host() {
            @Override
            public GameState gameState() {
                return gameState.get();
            }

            @Override
            public boolean isLoginRuntimeActive() {
                return isLoginRuntimeActive.getAsBoolean();
            }

            @Override
            public boolean isLogoutRuntimeActive() {
                return isLogoutRuntimeActive.getAsBoolean();
            }

            @Override
            public long currentTimeMs() {
                return currentTimeMs.getAsLong();
            }

            @Override
            public long loginClientTickAdvanceMinIntervalMs() {
                return loginClientTickAdvanceMinIntervalMs.getAsLong();
            }

            @Override
            public long logoutClientTickAdvanceMinIntervalMs() {
                return logoutClientTickAdvanceMinIntervalMs.getAsLong();
            }

            @Override
            public long lastLoginClientAdvanceAtMs() {
                return lastLoginClientAdvanceAtMs.getAsLong();
            }

            @Override
            public void setLastLoginClientAdvanceAtMs(long value) {
                setLastLoginClientAdvanceAtMs.accept(value);
            }

            @Override
            public long lastLogoutClientAdvanceAtMs() {
                return lastLogoutClientAdvanceAtMs.getAsLong();
            }

            @Override
            public void setLastLogoutClientAdvanceAtMs(long value) {
                setLastLogoutClientAdvanceAtMs.accept(value);
            }

            @Override
            public int lastLoginRuntimeAdvanceTick() {
                return lastLoginRuntimeAdvanceTick.getAsInt();
            }

            @Override
            public void setLastLoginRuntimeAdvanceTick(int value) {
                setLastLoginRuntimeAdvanceTick.accept(value);
            }

            @Override
            public int lastLogoutRuntimeAdvanceTick() {
                return lastLogoutRuntimeAdvanceTick.getAsInt();
            }

            @Override
            public void setLastLogoutRuntimeAdvanceTick(int value) {
                setLastLogoutRuntimeAdvanceTick.accept(value);
            }

            @Override
            public void onLoginRuntimeGameTick(int runtimeTick) {
                onLoginRuntimeGameTick.accept(runtimeTick);
            }

            @Override
            public void onLogoutRuntimeGameTick(int runtimeTick) {
                onLogoutRuntimeGameTick.accept(runtimeTick);
            }
        };
    }

    static AccountRuntimeOrchestrator.Host createAccountRuntimeOrchestratorHost(
        Supplier<GameState> gameState,
        IntSupplier currentExecutorTick,
        ManualMetricsSignalFn hasManualMetricsRuntimeSignalFor,
        Supplier<LogoutProfile> resolveManualMetricsLogoutProfile,
        Supplier<LoginProfile> resolveManualMetricsLoginProfile,
        ManualMetricsEventFn maybeEmitManualMetricsRuntimeGateEvent,
        Function<Object[], JsonObject> details,
        BooleanSupplier isLogoutRuntimeActive,
        BooleanSupplier isLogoutRuntimeSuccessful,
        BooleanSupplier isLogoutRuntimeFailedHardStop,
        Consumer<LogoutProfile> requestLogoutRuntimeStart,
        Runnable requestLogoutRuntimeStop,
        IntConsumer advanceLogoutRuntimeOnObservedTick,
        BooleanSupplier loginBreakRuntimeEnabled,
        BooleanSupplier isLoginRuntimeActive,
        Runnable requestLoginRuntimeStop,
        BiConsumer<LoginProfile, TypingProfile> requestLoginRuntimeStart,
        LongSupplier currentTimeMs,
        LongSupplier loginIdleSuppressStartWindowMs,
        LongConsumer extendSuppressIdleForLoginUntil,
        Runnable suppressIdleMotionForLoginStart,
        Runnable resetLoginSubmitState,
        Runnable notifyBreakRuntimeStopAll,
        Runnable cancelResumePlanner,
        Runnable cancelHumanTyping,
        Consumer<String> stopOperationalRuntimeState,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision
    ) {
        return new AccountRuntimeOrchestrator.Host() {
            @Override
            public GameState gameState() {
                return gameState.get();
            }

            @Override
            public int currentExecutorTick() {
                return currentExecutorTick.getAsInt();
            }

            @Override
            public boolean hasManualMetricsRuntimeSignalFor(String consumer, boolean emitWhenMissing) {
                return hasManualMetricsRuntimeSignalFor.hasSignal(consumer, emitWhenMissing);
            }

            @Override
            public LogoutProfile resolveManualMetricsLogoutProfile() {
                return resolveManualMetricsLogoutProfile.get();
            }

            @Override
            public LoginProfile resolveManualMetricsLoginProfile() {
                return resolveManualMetricsLoginProfile.get();
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
            public boolean isLogoutRuntimeActive() {
                return isLogoutRuntimeActive.getAsBoolean();
            }

            @Override
            public boolean isLogoutRuntimeSuccessful() {
                return isLogoutRuntimeSuccessful.getAsBoolean();
            }

            @Override
            public boolean isLogoutRuntimeFailedHardStop() {
                return isLogoutRuntimeFailedHardStop.getAsBoolean();
            }

            @Override
            public void requestLogoutRuntimeStart(LogoutProfile profile) {
                requestLogoutRuntimeStart.accept(profile);
            }

            @Override
            public void requestLogoutRuntimeStop() {
                requestLogoutRuntimeStop.run();
            }

            @Override
            public void advanceLogoutRuntimeOnObservedTick(int observedTick) {
                advanceLogoutRuntimeOnObservedTick.accept(observedTick);
            }

            @Override
            public boolean loginBreakRuntimeEnabled() {
                return loginBreakRuntimeEnabled.getAsBoolean();
            }

            @Override
            public boolean isLoginRuntimeActive() {
                return isLoginRuntimeActive.getAsBoolean();
            }

            @Override
            public void requestLoginRuntimeStop() {
                requestLoginRuntimeStop.run();
            }

            @Override
            public void requestLoginRuntimeStart(LoginProfile profile, TypingProfile typingProfile) {
                requestLoginRuntimeStart.accept(profile, typingProfile);
            }

            @Override
            public long currentTimeMs() {
                return currentTimeMs.getAsLong();
            }

            @Override
            public long loginIdleSuppressStartWindowMs() {
                return loginIdleSuppressStartWindowMs.getAsLong();
            }

            @Override
            public void extendSuppressIdleForLoginUntil(long untilMs) {
                extendSuppressIdleForLoginUntil.accept(untilMs);
            }

            @Override
            public void suppressIdleMotionForLoginStart() {
                suppressIdleMotionForLoginStart.run();
            }

            @Override
            public void resetLoginSubmitState() {
                resetLoginSubmitState.run();
            }

            @Override
            public void notifyBreakRuntimeStopAll() {
                notifyBreakRuntimeStopAll.run();
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
            public void stopOperationalRuntimeState(String cancelReason) {
                stopOperationalRuntimeState.accept(cancelReason);
            }

            @Override
            public CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject detailsObj) {
                return acceptDecision.apply(reason, detailsObj);
            }
        };
    }
}
