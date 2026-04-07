package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;

final class ExecutorGameplayRuntimeInputFactory {
    private ExecutorGameplayRuntimeInputFactory() {
    }

    static ExecutorGameplayRuntimeInputs.RuntimeInputs create(
        IntSupplier currentExecutorTick,
        IntSupplier currentPlayerAnimation,
        ExecutorRuntimeDomainWiring.IntPredicate isAnimationActive,
        BooleanSupplier isDropSweepSessionActive,
        IntSupplier dropSweepItemId,
        IntSupplier dropSweepNextSlot,
        IntSupplier dropSweepLastDispatchTick,
        IntSupplier dropSweepDispatchFailStreak,
        BooleanSupplier dropSweepAwaitingFirstCursorSync,
        IntConsumer setDropSweepNextSlot,
        IntConsumer setDropSweepLastDispatchTick,
        ExecutorRuntimeDomainWiring.IntBooleanSetter setDropSweepAwaitingFirstCursorSync,
        ExecutorRuntimeDomainWiring.IntBooleanSetter setDropSweepProgressCheckPending,
        ExecutorRuntimeDomainWiring.IntSetConsumer beginDropSweepSession,
        Runnable endDropSweepSession,
        ExecutorRuntimeDomainWiring.IntPredicate updateDropSweepProgressState,
        BooleanSupplier noteDropSweepDispatchFailure,
        Runnable noteDropSweepDispatchSuccess,
        ExecutorRuntimeDomainWiring.IntIntOptionalResolver findInventorySlotFrom,
        IntFunction<Optional<Point>> resolveInventorySlotPoint,
        IntFunction<Optional<Point>> resolveInventorySlotBasePoint,
        Supplier<Optional<Point>> centerOfDropSweepRegionCanvas,
        Predicate<Point> isCursorNearDropTarget,
        Function<Point, MotorHandle> scheduleDropMoveGesture,
        BooleanSupplier acquireOrRenewDropMotorOwner,
        BooleanSupplier isLoggedInAndBankClosed,
        ExecutorRuntimeDomainWiring.IntIntPointPredicate dispatchInventoryDropAction,
        Runnable applyDropPerceptionDelay,
        Runnable incrementClicksDispatched,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        Consumer<String> onDropCadenceProfileSelected,
        Consumer<IdleCadenceTuning> onIdleCadenceTuningSelected,
        Function<Object[], JsonObject> details,
        BiConsumer<String, JsonObject> emitDropDebug,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        BooleanSupplier hasActiveDropSweepSession,
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
        BiConsumer<String, JsonObject> emitIdleEvent,
        BooleanSupplier isRandomEventRuntimeEnabled,
        BooleanSupplier isRandomEventRuntimeArmed,
        BooleanSupplier isLoggedIn,
        BooleanSupplier isBankOpen,
        BooleanSupplier hasActiveInteractionMotorProgram,
        BooleanSupplier acquireOrRenewInteractionMotorOwnership,
        Runnable releaseInteractionMotorOwnership,
        Supplier<Player> localPlayer,
        Supplier<Iterable<NPC>> npcs,
        Function<NPC, Point> resolveVariedNpcClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearRandomEventTarget,
        Predicate<Point> selectRandomEventDismissMenuOptionAt,
        LongBinaryOperator randomBetween,
        LongSupplier randomEventPreAttemptCooldownMinMs,
        LongSupplier randomEventPreAttemptCooldownMaxMs,
        LongSupplier randomEventSuccessCooldownMinMs,
        LongSupplier randomEventSuccessCooldownMaxMs,
        LongSupplier randomEventFailureRetryCooldownMinMs,
        LongSupplier randomEventFailureRetryCooldownMaxMs,
        LongSupplier randomEventCursorReadyHoldMs,
        BiConsumer<String, JsonObject> emitRandomEventEvent,
        BooleanSupplier isTopMenuBankOnObject,
        Predicate<TileObject> isTopMenuChopOnTree,
        Predicate<TileObject> isTopMenuMineOnRock,
        BooleanSupplier hasAttackEntryOnNpc,
        LongConsumer reserveMotorCooldown
    ) {
        ExecutorGameplayRuntimeInputs.RuntimeInputs gameplayRuntimeInputs = new ExecutorGameplayRuntimeInputs.RuntimeInputs();
        gameplayRuntimeInputs.currentExecutorTick = currentExecutorTick;
        gameplayRuntimeInputs.currentPlayerAnimation = currentPlayerAnimation;
        gameplayRuntimeInputs.isAnimationActive = isAnimationActive;
        gameplayRuntimeInputs.isDropSweepSessionActive = isDropSweepSessionActive;
        gameplayRuntimeInputs.dropSweepItemId = dropSweepItemId;
        gameplayRuntimeInputs.dropSweepNextSlot = dropSweepNextSlot;
        gameplayRuntimeInputs.dropSweepLastDispatchTick = dropSweepLastDispatchTick;
        gameplayRuntimeInputs.dropSweepDispatchFailStreak = dropSweepDispatchFailStreak;
        gameplayRuntimeInputs.dropSweepAwaitingFirstCursorSync = dropSweepAwaitingFirstCursorSync;
        gameplayRuntimeInputs.setDropSweepNextSlot = setDropSweepNextSlot;
        gameplayRuntimeInputs.setDropSweepLastDispatchTick = setDropSweepLastDispatchTick;
        gameplayRuntimeInputs.setDropSweepAwaitingFirstCursorSync = setDropSweepAwaitingFirstCursorSync;
        gameplayRuntimeInputs.setDropSweepProgressCheckPending = setDropSweepProgressCheckPending;
        gameplayRuntimeInputs.beginDropSweepSession = beginDropSweepSession;
        gameplayRuntimeInputs.endDropSweepSession = endDropSweepSession;
        gameplayRuntimeInputs.updateDropSweepProgressState = updateDropSweepProgressState;
        gameplayRuntimeInputs.noteDropSweepDispatchFailure = noteDropSweepDispatchFailure;
        gameplayRuntimeInputs.noteDropSweepDispatchSuccess = noteDropSweepDispatchSuccess;
        gameplayRuntimeInputs.findInventorySlotFrom = findInventorySlotFrom;
        gameplayRuntimeInputs.resolveInventorySlotPoint = resolveInventorySlotPoint;
        gameplayRuntimeInputs.resolveInventorySlotBasePoint = resolveInventorySlotBasePoint;
        gameplayRuntimeInputs.centerOfDropSweepRegionCanvas = centerOfDropSweepRegionCanvas;
        gameplayRuntimeInputs.isCursorNearDropTarget = isCursorNearDropTarget;
        gameplayRuntimeInputs.scheduleDropMoveGesture = scheduleDropMoveGesture;
        gameplayRuntimeInputs.acquireOrRenewDropMotorOwner = acquireOrRenewDropMotorOwner;
        gameplayRuntimeInputs.isLoggedInAndBankClosed = isLoggedInAndBankClosed;
        gameplayRuntimeInputs.dispatchInventoryDropAction = dispatchInventoryDropAction;
        gameplayRuntimeInputs.applyDropPerceptionDelay = applyDropPerceptionDelay;
        gameplayRuntimeInputs.incrementClicksDispatched = incrementClicksDispatched;
        gameplayRuntimeInputs.fatigueSnapshot = fatigueSnapshot;
        gameplayRuntimeInputs.onDropCadenceProfileSelected = onDropCadenceProfileSelected;
        gameplayRuntimeInputs.onIdleCadenceTuningSelected = onIdleCadenceTuningSelected;
        gameplayRuntimeInputs.details = details;
        gameplayRuntimeInputs.emitDropDebug = emitDropDebug;
        gameplayRuntimeInputs.acceptDecision = acceptDecision;
        gameplayRuntimeInputs.rejectDecision = rejectDecision;
        gameplayRuntimeInputs.hasActiveDropSweepSession = hasActiveDropSweepSession;
        gameplayRuntimeInputs.isIdleInterActionWindowOpen = isIdleInterActionWindowOpen;
        gameplayRuntimeInputs.resolveIdleSkillContext = resolveIdleSkillContext;
        gameplayRuntimeInputs.isIdleActionWindowOpen = isIdleActionWindowOpen;
        gameplayRuntimeInputs.isIdleCameraWindowOpen = isIdleCameraWindowOpen;
        gameplayRuntimeInputs.idleWindowGateSnapshot = idleWindowGateSnapshot;
        gameplayRuntimeInputs.isIdleAnimationActiveNow = isIdleAnimationActiveNow;
        gameplayRuntimeInputs.isIdleInteractionDelaySatisfied = isIdleInteractionDelaySatisfied;
        gameplayRuntimeInputs.isIdleCameraInteractionDelaySatisfied = isIdleCameraInteractionDelaySatisfied;
        gameplayRuntimeInputs.lastInteractionClickSerial = lastInteractionClickSerial;
        gameplayRuntimeInputs.isCursorOutsideClientWindow = isCursorOutsideClientWindow;
        gameplayRuntimeInputs.acquireOrRenewIdleMotorOwnership = acquireOrRenewIdleMotorOwnership;
        gameplayRuntimeInputs.canPerformIdleMotorActionNow = canPerformIdleMotorActionNow;
        gameplayRuntimeInputs.performIdleCameraMicroAdjust = performIdleCameraMicroAdjust;
        gameplayRuntimeInputs.resolveIdleHoverTargetCanvasPoint = resolveIdleHoverTargetCanvasPoint;
        gameplayRuntimeInputs.performIdleCursorMove = performIdleCursorMove;
        gameplayRuntimeInputs.resolveIdleDriftTargetCanvasPoint = resolveIdleDriftTargetCanvasPoint;
        gameplayRuntimeInputs.resolveIdleOffscreenTargetScreenPoint = resolveIdleOffscreenTargetScreenPoint;
        gameplayRuntimeInputs.performIdleOffscreenCursorMove = performIdleOffscreenCursorMove;
        gameplayRuntimeInputs.resolveIdleParkingTargetCanvasPoint = resolveIdleParkingTargetCanvasPoint;
        gameplayRuntimeInputs.resolveFishingIdleMode = resolveFishingIdleMode;
        gameplayRuntimeInputs.resolveActivityIdlePolicy = resolveActivityIdlePolicy;
        gameplayRuntimeInputs.activeIdleCadenceTuning = activeIdleCadenceTuning;
        gameplayRuntimeInputs.emitIdleEvent = emitIdleEvent;
        gameplayRuntimeInputs.isRandomEventRuntimeEnabled = isRandomEventRuntimeEnabled;
        gameplayRuntimeInputs.isRandomEventRuntimeArmed = isRandomEventRuntimeArmed;
        gameplayRuntimeInputs.isLoggedIn = isLoggedIn;
        gameplayRuntimeInputs.isBankOpen = isBankOpen;
        gameplayRuntimeInputs.hasActiveInteractionMotorProgram = hasActiveInteractionMotorProgram;
        gameplayRuntimeInputs.acquireOrRenewInteractionMotorOwnership = acquireOrRenewInteractionMotorOwnership;
        gameplayRuntimeInputs.releaseInteractionMotorOwnership = releaseInteractionMotorOwnership;
        gameplayRuntimeInputs.localPlayer = localPlayer;
        gameplayRuntimeInputs.npcs = npcs;
        gameplayRuntimeInputs.resolveVariedNpcClickPoint = resolveVariedNpcClickPoint;
        gameplayRuntimeInputs.isUsableCanvasPoint = isUsableCanvasPoint;
        gameplayRuntimeInputs.moveInteractionCursorToCanvasPoint = moveInteractionCursorToCanvasPoint;
        gameplayRuntimeInputs.isCursorNearRandomEventTarget = isCursorNearRandomEventTarget;
        gameplayRuntimeInputs.selectRandomEventDismissMenuOptionAt = selectRandomEventDismissMenuOptionAt;
        gameplayRuntimeInputs.randomBetween = randomBetween;
        gameplayRuntimeInputs.randomEventPreAttemptCooldownMinMs = randomEventPreAttemptCooldownMinMs;
        gameplayRuntimeInputs.randomEventPreAttemptCooldownMaxMs = randomEventPreAttemptCooldownMaxMs;
        gameplayRuntimeInputs.randomEventSuccessCooldownMinMs = randomEventSuccessCooldownMinMs;
        gameplayRuntimeInputs.randomEventSuccessCooldownMaxMs = randomEventSuccessCooldownMaxMs;
        gameplayRuntimeInputs.randomEventFailureRetryCooldownMinMs = randomEventFailureRetryCooldownMinMs;
        gameplayRuntimeInputs.randomEventFailureRetryCooldownMaxMs = randomEventFailureRetryCooldownMaxMs;
        gameplayRuntimeInputs.randomEventCursorReadyHoldMs = randomEventCursorReadyHoldMs;
        gameplayRuntimeInputs.emitRandomEventEvent = emitRandomEventEvent;
        gameplayRuntimeInputs.isTopMenuBankOnObject = isTopMenuBankOnObject;
        gameplayRuntimeInputs.isTopMenuChopOnTree = isTopMenuChopOnTree;
        gameplayRuntimeInputs.isTopMenuMineOnRock = isTopMenuMineOnRock;
        gameplayRuntimeInputs.hasAttackEntryOnNpc = hasAttackEntryOnNpc;
        gameplayRuntimeInputs.reserveMotorCooldown = reserveMotorCooldown;
        return gameplayRuntimeInputs;
    }
}
