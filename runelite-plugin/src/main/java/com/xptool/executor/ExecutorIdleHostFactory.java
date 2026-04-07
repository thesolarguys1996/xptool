package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.awt.Robot;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.GameState;

final class ExecutorIdleHostFactory {
    @FunctionalInterface
    interface RemainingFutureMsFn {
        long remainingFutureMs(long untilMs, long nowMs);
    }

    @FunctionalInterface
    interface EmitFn {
        void emit(String status, CommandRow row, String reason, JsonObject details, String eventType);
    }

    @FunctionalInterface
    interface CursorNearScreenPoint {
        boolean test(Point point, double tolerancePx);
    }

    private ExecutorIdleHostFactory() {
    }

    static IdleOffscreenMoveEngine.Host createIdleOffscreenMoveHost(
        Supplier<PendingMouseMove> pendingMouseMove,
        java.util.function.Consumer<PendingMouseMove> setPendingMouseMove,
        Runnable clearPendingMouseMove,
        Predicate<PendingMouseMove> isPendingMouseMoveOwnerValid,
        Function<Point, Point> currentPointerLocationOr,
        Supplier<Robot> getOrCreateRobot,
        IntSupplier currentExecutorTick,
        CursorNearScreenPoint isCursorNearScreenPoint,
        BiConsumer<String, JsonObject> emitIdleEvent
    ) {
        return new IdleOffscreenMoveEngine.Host() {
            @Override
            public PendingMouseMove pendingMouseMove() {
                return pendingMouseMove.get();
            }

            @Override
            public void setPendingMouseMove(PendingMouseMove move) {
                setPendingMouseMove.accept(move);
            }

            @Override
            public void clearPendingMouseMove() {
                clearPendingMouseMove.run();
            }

            @Override
            public boolean isPendingMouseMoveOwnerValid(PendingMouseMove move) {
                return isPendingMouseMoveOwnerValid.test(move);
            }

            @Override
            public Point currentPointerLocationOr(Point fallback) {
                return currentPointerLocationOr.apply(fallback);
            }

            @Override
            public Robot getOrCreateRobot() {
                return getOrCreateRobot.get();
            }

            @Override
            public int currentExecutorTick() {
                return currentExecutorTick.getAsInt();
            }

            @Override
            public boolean isCursorNearScreenPoint(Point point, double tolerancePx) {
                return isCursorNearScreenPoint.test(point, tolerancePx);
            }

            @Override
            public void emitIdleEvent(String reason, JsonObject details) {
                emitIdleEvent.accept(reason, details);
            }
        };
    }

    static IdleGateTelemetryService.Host createIdleGateTelemetryHost(
        BooleanSupplier isIdleRuntimeEnabled,
        BooleanSupplier isIdleRuntimeArmedForCurrentContext,
        Function<IdleSkillContext, Boolean> isIdleRuntimeArmedForContext,
        BooleanSupplier isClientWindowForegroundEligible,
        BooleanSupplier isLogoutRuntimeActive,
        BooleanSupplier shouldSuppressIdleForLogin,
        Supplier<GameState> gameState,
        BooleanSupplier isBankOpen,
        BooleanSupplier shouldSuppressIdleForTraversalOrBank,
        BooleanSupplier isMouseMovePending,
        BooleanSupplier isIdleMenuBlockActive,
        BooleanSupplier isMenuOpen,
        BooleanSupplier shouldOwnInteractionSession,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        BooleanSupplier hasPendingCommandRows,
        BooleanSupplier isIdleAnimationActiveNow,
        Predicate<String> hasActiveMotorProgramForOwner,
        BooleanSupplier isClientCanvasFocused,
        Function<IdleSkillContext, FishingIdleMode> resolveFishingIdleMode,
        BooleanSupplier fishingIdleModeOverrideEnabled,
        Supplier<IdleArmingService> idleArmingService,
        BooleanSupplier strictForegroundWindowGating,
        LongSupplier suppressIdleForLoginUntilMs,
        BooleanSupplier isLoginRuntimeActive,
        BooleanSupplier isPrimaryLoginSubmitPromptVisible,
        BooleanSupplier isSecondaryLoginSubmitPromptVisible,
        LongSupplier currentTimeMs,
        RemainingFutureMsFn remainingFutureMs,
        Supplier<IdleTraversalBankSuppressionGate> idleTraversalBankSuppressionGate,
        EmitFn emit
    ) {
        return new IdleGateTelemetryService.Host() {
            @Override
            public boolean isIdleRuntimeEnabled() {
                return isIdleRuntimeEnabled.getAsBoolean();
            }

            @Override
            public boolean isIdleRuntimeArmedForCurrentContext() {
                return isIdleRuntimeArmedForCurrentContext.getAsBoolean();
            }

            @Override
            public boolean isIdleRuntimeArmedForContext(IdleSkillContext context) {
                return Boolean.TRUE.equals(isIdleRuntimeArmedForContext.apply(context));
            }

            @Override
            public boolean isClientWindowForegroundEligible() {
                return isClientWindowForegroundEligible.getAsBoolean();
            }

            @Override
            public boolean isLogoutRuntimeActive() {
                return isLogoutRuntimeActive.getAsBoolean();
            }

            @Override
            public boolean shouldSuppressIdleForLogin() {
                return shouldSuppressIdleForLogin.getAsBoolean();
            }

            @Override
            public GameState gameState() {
                return gameState.get();
            }

            @Override
            public boolean isBankOpen() {
                return isBankOpen.getAsBoolean();
            }

            @Override
            public boolean shouldSuppressIdleForTraversalOrBank() {
                return shouldSuppressIdleForTraversalOrBank.getAsBoolean();
            }

            @Override
            public boolean isMouseMovePending() {
                return isMouseMovePending.getAsBoolean();
            }

            @Override
            public boolean isIdleMenuBlockActive() {
                return isIdleMenuBlockActive.getAsBoolean();
            }

            @Override
            public boolean isMenuOpen() {
                return isMenuOpen.getAsBoolean();
            }

            @Override
            public boolean shouldOwnInteractionSession() {
                return shouldOwnInteractionSession.getAsBoolean();
            }

            @Override
            public IdleSkillContext resolveIdleSkillContext() {
                return resolveIdleSkillContext.get();
            }

            @Override
            public boolean hasPendingCommandRows() {
                return hasPendingCommandRows.getAsBoolean();
            }

            @Override
            public boolean isIdleAnimationActiveNow() {
                return isIdleAnimationActiveNow.getAsBoolean();
            }

            @Override
            public boolean hasActiveMotorProgramForOwner(String owner) {
                return hasActiveMotorProgramForOwner.test(owner);
            }

            @Override
            public boolean isClientCanvasFocused() {
                return isClientCanvasFocused.getAsBoolean();
            }

            @Override
            public FishingIdleMode resolveFishingIdleMode(IdleSkillContext context) {
                return resolveFishingIdleMode.apply(context);
            }

            @Override
            public boolean fishingIdleModeOverrideEnabled() {
                return fishingIdleModeOverrideEnabled.getAsBoolean();
            }

            @Override
            public IdleArmingService idleArmingService() {
                return idleArmingService.get();
            }

            @Override
            public boolean strictForegroundWindowGating() {
                return strictForegroundWindowGating.getAsBoolean();
            }

            @Override
            public long suppressIdleForLoginUntilMs() {
                return suppressIdleForLoginUntilMs.getAsLong();
            }

            @Override
            public boolean isLoginRuntimeActive() {
                return isLoginRuntimeActive.getAsBoolean();
            }

            @Override
            public boolean isPrimaryLoginSubmitPromptVisible() {
                return isPrimaryLoginSubmitPromptVisible.getAsBoolean();
            }

            @Override
            public boolean isSecondaryLoginSubmitPromptVisible() {
                return isSecondaryLoginSubmitPromptVisible.getAsBoolean();
            }

            @Override
            public long currentTimeMs() {
                return currentTimeMs.getAsLong();
            }

            @Override
            public long remainingFutureMs(long untilMs, long nowMs) {
                return remainingFutureMs.remainingFutureMs(untilMs, nowMs);
            }

            @Override
            public IdleTraversalBankSuppressionGate idleTraversalBankSuppressionGate() {
                return idleTraversalBankSuppressionGate.get();
            }

            @Override
            public void emit(String status, CommandRow row, String reason, JsonObject detailsObj, String eventType) {
                emit.emit(status, row, reason, detailsObj, eventType);
            }
        };
    }

    static IdleSuppressionService.Host createIdleSuppressionHost(
        BooleanSupplier hasActiveSession,
        Predicate<String> hasActiveSessionOtherThan,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        Supplier<PendingMouseMove> pendingMouseMove,
        BooleanSupplier hasActiveDropSweepSession,
        Predicate<PendingMouseMove> isIdleOwnedOffscreenPendingMove,
        Predicate<String> hasActiveMotorProgramForOwner,
        BooleanSupplier isBankOpen,
        BooleanSupplier hasPendingCommandRows,
        BooleanSupplier isIdleMotorOwnerActive,
        Runnable releasePendingIdleCameraDrag,
        Function<String, String> normalizedMotorOwnerName,
        Runnable clearPendingMouseMove,
        Supplier<MotorProgram> activeMotorProgram,
        BiConsumer<MotorProgram, String> cancelMotorProgram,
        Runnable releaseIdleMotorOwnershipAfterSuppression
    ) {
        return new IdleSuppressionService.Host() {
            @Override
            public boolean hasActiveSession() {
                return hasActiveSession.getAsBoolean();
            }

            @Override
            public boolean hasActiveSessionOtherThan(String owner) {
                return hasActiveSessionOtherThan.test(owner);
            }

            @Override
            public IdleSkillContext resolveIdleSkillContext() {
                return resolveIdleSkillContext.get();
            }

            @Override
            public PendingMouseMove pendingMouseMove() {
                return pendingMouseMove.get();
            }

            @Override
            public boolean hasActiveDropSweepSession() {
                return hasActiveDropSweepSession.getAsBoolean();
            }

            @Override
            public boolean isIdleOwnedOffscreenPendingMove(PendingMouseMove pending) {
                return isIdleOwnedOffscreenPendingMove.test(pending);
            }

            @Override
            public boolean hasActiveMotorProgramForOwner(String owner) {
                return hasActiveMotorProgramForOwner.test(owner);
            }

            @Override
            public boolean isBankOpen() {
                return isBankOpen.getAsBoolean();
            }

            @Override
            public boolean hasPendingCommandRows() {
                return hasPendingCommandRows.getAsBoolean();
            }

            @Override
            public boolean isIdleMotorOwnerActive() {
                return isIdleMotorOwnerActive.getAsBoolean();
            }

            @Override
            public void releasePendingIdleCameraDrag() {
                releasePendingIdleCameraDrag.run();
            }

            @Override
            public String normalizedMotorOwnerName(String owner) {
                return normalizedMotorOwnerName.apply(owner);
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
            public void releaseIdleMotorOwnershipAfterSuppression() {
                releaseIdleMotorOwnershipAfterSuppression.run();
            }
        };
    }

    static IdleRuntime.Host createIdleRuntimeHost(
        BooleanSupplier hasActiveSession,
        Predicate<String> hasActiveSessionOtherThan,
        Supplier<Optional<String>> activeSessionName,
        BooleanSupplier hasActiveDropSweepSession,
        Runnable releaseIdleMotorOwnership,
        BooleanSupplier isIdleInterActionWindowOpen,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        BooleanSupplier isIdleActionWindowOpen,
        BooleanSupplier isIdleCameraWindowOpen,
        Supplier<JsonObject> idleWindowGateSnapshot,
        BooleanSupplier isIdleAnimationActiveNow,
        BooleanSupplier isIdleInteractionDelaySatisfied,
        BooleanSupplier isIdleCameraInteractionDelaySatisfied,
        Supplier<Long> lastInteractionClickSerial,
        BooleanSupplier isCursorOutsideClientWindow,
        BooleanSupplier acquireOrRenewIdleMotorOwnership,
        BooleanSupplier canPerformIdleMotorActionNow,
        BooleanSupplier performIdleCameraMicroAdjust,
        Supplier<Optional<Point>> resolveIdleHoverTargetCanvasPoint,
        Predicate<Point> performIdleCursorMove,
        Supplier<Optional<Point>> resolveIdleDriftTargetCanvasPoint,
        Supplier<Optional<Point>> resolveIdleOffscreenTargetScreenPoint,
        Predicate<Point> performIdleOffscreenCursorMove,
        Supplier<Optional<Point>> resolveIdleParkingTargetCanvasPoint,
        Function<IdleSkillContext, FishingIdleMode> resolveFishingIdleMode,
        Function<IdleSkillContext, ActivityIdlePolicy> resolveActivityIdlePolicy,
        Supplier<IdleCadenceTuning> activeIdleCadenceTuning,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        BooleanSupplier isFishingOffscreenIdleSuppressed,
        LongSupplier fishingOffscreenIdleSuppressionRemainingMs,
        BooleanSupplier isFishingInventoryFullAfkActive,
        LongSupplier fishingInventoryFullAfkRemainingMs,
        BiConsumer<String, JsonObject> emitIdleEvent
    ) {
        return new IdleRuntime.Host() {
            @Override
            public boolean hasActiveSession() {
                return hasActiveSession.getAsBoolean();
            }

            @Override
            public boolean hasActiveSessionOtherThan(String sessionName) {
                return hasActiveSessionOtherThan.test(sessionName);
            }

            @Override
            public Optional<String> activeSessionName() {
                return activeSessionName.get();
            }

            @Override
            public boolean hasActiveDropSweepSession() {
                return hasActiveDropSweepSession.getAsBoolean();
            }

            @Override
            public void releaseIdleMotorOwnership() {
                releaseIdleMotorOwnership.run();
            }

            @Override
            public boolean isIdleInterActionWindowOpen() {
                return isIdleInterActionWindowOpen.getAsBoolean();
            }

            @Override
            public IdleSkillContext resolveIdleSkillContext() {
                return resolveIdleSkillContext.get();
            }

            @Override
            public boolean isIdleActionWindowOpen() {
                return isIdleActionWindowOpen.getAsBoolean();
            }

            @Override
            public boolean isIdleCameraWindowOpen() {
                return isIdleCameraWindowOpen.getAsBoolean();
            }

            @Override
            public JsonObject idleWindowGateSnapshot() {
                return idleWindowGateSnapshot.get();
            }

            @Override
            public boolean isIdleAnimationActiveNow() {
                return isIdleAnimationActiveNow.getAsBoolean();
            }

            @Override
            public boolean isIdleInteractionDelaySatisfied() {
                return isIdleInteractionDelaySatisfied.getAsBoolean();
            }

            @Override
            public boolean isIdleCameraInteractionDelaySatisfied() {
                return isIdleCameraInteractionDelaySatisfied.getAsBoolean();
            }

            @Override
            public long lastInteractionClickSerial() {
                Long value = lastInteractionClickSerial.get();
                return value == null ? 0L : value.longValue();
            }

            @Override
            public boolean isCursorOutsideClientWindow() {
                return isCursorOutsideClientWindow.getAsBoolean();
            }

            @Override
            public boolean acquireOrRenewIdleMotorOwnership() {
                return acquireOrRenewIdleMotorOwnership.getAsBoolean();
            }

            @Override
            public boolean canPerformIdleMotorActionNow() {
                return canPerformIdleMotorActionNow.getAsBoolean();
            }

            @Override
            public boolean performIdleCameraMicroAdjust() {
                return performIdleCameraMicroAdjust.getAsBoolean();
            }

            @Override
            public Optional<Point> resolveIdleHoverTargetCanvasPoint() {
                return resolveIdleHoverTargetCanvasPoint.get();
            }

            @Override
            public boolean performIdleCursorMove(Point canvasTarget) {
                return performIdleCursorMove.test(canvasTarget);
            }

            @Override
            public Optional<Point> resolveIdleDriftTargetCanvasPoint() {
                return resolveIdleDriftTargetCanvasPoint.get();
            }

            @Override
            public Optional<Point> resolveIdleOffscreenTargetScreenPoint() {
                return resolveIdleOffscreenTargetScreenPoint.get();
            }

            @Override
            public boolean performIdleOffscreenCursorMove(Point screenTarget) {
                return performIdleOffscreenCursorMove.test(screenTarget);
            }

            @Override
            public Optional<Point> resolveIdleParkingTargetCanvasPoint() {
                return resolveIdleParkingTargetCanvasPoint.get();
            }

            @Override
            public FishingIdleMode resolveFishingIdleMode(IdleSkillContext context) {
                return resolveFishingIdleMode.apply(context);
            }

            @Override
            public ActivityIdlePolicy resolveActivityIdlePolicy(IdleSkillContext context) {
                return resolveActivityIdlePolicy.apply(context);
            }

            @Override
            public IdleCadenceTuning activeIdleCadenceTuning() {
                IdleCadenceTuning tuning = activeIdleCadenceTuning.get();
                return tuning == null ? IdleCadenceTuning.none() : tuning;
            }

            @Override
            public FatigueSnapshot fatigueSnapshot() {
                return fatigueSnapshot.get();
            }

            @Override
            public boolean isFishingOffscreenIdleSuppressed() {
                return isFishingOffscreenIdleSuppressed.getAsBoolean();
            }

            @Override
            public long fishingOffscreenIdleSuppressionRemainingMs() {
                return fishingOffscreenIdleSuppressionRemainingMs.getAsLong();
            }

            @Override
            public boolean isFishingInventoryFullAfkActive() {
                return isFishingInventoryFullAfkActive.getAsBoolean();
            }

            @Override
            public long fishingInventoryFullAfkRemainingMs() {
                return fishingInventoryFullAfkRemainingMs.getAsLong();
            }

            @Override
            public void emitIdleEvent(String reason, JsonObject details) {
                emitIdleEvent.accept(reason, details);
            }
        };
    }
}
