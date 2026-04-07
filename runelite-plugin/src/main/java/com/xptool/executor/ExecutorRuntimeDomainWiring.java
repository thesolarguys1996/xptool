package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Consumer;
import net.runelite.api.NPC;
import net.runelite.api.Player;

final class ExecutorRuntimeDomainWiring {
    @FunctionalInterface
    interface IntPredicate {
        boolean test(int value);
    }

    @FunctionalInterface
    interface IntBooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    interface IntSetConsumer {
        void accept(int itemId, Set<Integer> itemIds);
    }

    @FunctionalInterface
    interface IntIntOptionalResolver {
        Optional<Integer> resolve(int itemId, int startSlot);
    }

    @FunctionalInterface
    interface IntIntPointPredicate {
        boolean test(int a, int b, Point c);
    }

    private ExecutorRuntimeDomainWiring() {
    }

    static DropRuntime.Host createDropRuntimeHost(
        IntSupplier currentExecutorTick,
        IntSupplier currentPlayerAnimation,
        IntPredicate isAnimationActive,
        BooleanSupplier isDropSweepSessionActive,
        IntSupplier dropSweepItemId,
        IntSupplier dropSweepNextSlot,
        IntSupplier dropSweepLastDispatchTick,
        IntSupplier dropSweepDispatchFailStreak,
        BooleanSupplier dropSweepAwaitingFirstCursorSync,
        IntConsumer setDropSweepNextSlot,
        IntConsumer setDropSweepLastDispatchTick,
        IntBooleanSetter setDropSweepAwaitingFirstCursorSync,
        IntBooleanSetter setDropSweepProgressCheckPending,
        IntSetConsumer beginDropSweepSession,
        Runnable endDropSweepSession,
        IntPredicate updateDropSweepProgressState,
        BooleanSupplier noteDropSweepDispatchFailure,
        Runnable noteDropSweepDispatchSuccess,
        IntIntOptionalResolver findInventorySlotFrom,
        IntFunction<Optional<Point>> resolveInventorySlotPoint,
        IntFunction<Optional<Point>> resolveInventorySlotBasePoint,
        Supplier<Optional<Point>> centerOfDropSweepRegionCanvas,
        Predicate<Point> isCursorNearDropTarget,
        Function<Point, MotorHandle> scheduleDropMoveGesture,
        BooleanSupplier acquireOrRenewDropMotorOwner,
        BooleanSupplier isLoggedInAndBankClosed,
        IntIntPointPredicate dispatchInventoryDropAction,
        Runnable applyDropPerceptionDelay,
        Runnable incrementClicksDispatched,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        Consumer<String> onDropCadenceProfileSelected,
        Consumer<IdleCadenceTuning> onIdleCadenceTuningSelected,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        Function<IdleSkillContext, FishingIdleMode> resolveFishingIdleMode,
        Function<Object[], JsonObject> details,
        BiConsumer<String, JsonObject> emitDropDebug,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision
    ) {
        return new DropRuntime.Host() {
            @Override
            public int currentExecutorTick() {
                return currentExecutorTick.getAsInt();
            }

            @Override
            public int currentPlayerAnimation() {
                return currentPlayerAnimation.getAsInt();
            }

            @Override
            public boolean isAnimationActive(int animation) {
                return isAnimationActive.test(animation);
            }

            @Override
            public boolean isDropSweepSessionActive() {
                return isDropSweepSessionActive.getAsBoolean();
            }

            @Override
            public int dropSweepItemId() {
                return dropSweepItemId.getAsInt();
            }

            @Override
            public int dropSweepNextSlot() {
                return dropSweepNextSlot.getAsInt();
            }

            @Override
            public int dropSweepLastDispatchTick() {
                return dropSweepLastDispatchTick.getAsInt();
            }

            @Override
            public int dropSweepDispatchFailStreak() {
                return dropSweepDispatchFailStreak.getAsInt();
            }

            @Override
            public boolean dropSweepAwaitingFirstCursorSync() {
                return dropSweepAwaitingFirstCursorSync.getAsBoolean();
            }

            @Override
            public void setDropSweepNextSlot(int slot) {
                setDropSweepNextSlot.accept(slot);
            }

            @Override
            public void setDropSweepLastDispatchTick(int tick) {
                setDropSweepLastDispatchTick.accept(tick);
            }

            @Override
            public void setDropSweepAwaitingFirstCursorSync(boolean awaiting) {
                setDropSweepAwaitingFirstCursorSync.set(awaiting);
            }

            @Override
            public void setDropSweepProgressCheckPending(boolean pending) {
                setDropSweepProgressCheckPending.set(pending);
            }

            @Override
            public void beginDropSweepSession(int itemId, Set<Integer> itemIds) {
                beginDropSweepSession.accept(itemId, itemIds);
            }

            @Override
            public void endDropSweepSession() {
                endDropSweepSession.run();
            }

            @Override
            public boolean updateDropSweepProgressState(int itemId) {
                return updateDropSweepProgressState.test(itemId);
            }

            @Override
            public boolean noteDropSweepDispatchFailure() {
                return noteDropSweepDispatchFailure.getAsBoolean();
            }

            @Override
            public void noteDropSweepDispatchSuccess() {
                noteDropSweepDispatchSuccess.run();
            }

            @Override
            public Optional<Integer> findInventorySlotFrom(int itemId, int startSlot) {
                return findInventorySlotFrom.resolve(itemId, startSlot);
            }

            @Override
            public Optional<Point> resolveInventorySlotPoint(int slot) {
                return resolveInventorySlotPoint.apply(slot);
            }

            @Override
            public Optional<Point> resolveInventorySlotBasePoint(int slot) {
                return resolveInventorySlotBasePoint.apply(slot);
            }

            @Override
            public Optional<Point> centerOfDropSweepRegionCanvas() {
                return centerOfDropSweepRegionCanvas.get();
            }

            @Override
            public boolean isCursorNearDropTarget(Point canvasPoint) {
                return isCursorNearDropTarget.test(canvasPoint);
            }

            @Override
            public MotorHandle scheduleDropMoveGesture(Point canvasPoint) {
                return scheduleDropMoveGesture.apply(canvasPoint);
            }

            @Override
            public boolean acquireOrRenewDropMotorOwner() {
                return acquireOrRenewDropMotorOwner.getAsBoolean();
            }

            @Override
            public boolean isLoggedInAndBankClosed() {
                return isLoggedInAndBankClosed.getAsBoolean();
            }

            @Override
            public boolean dispatchInventoryDropAction(int slot, int expectedItemId, Point preparedCanvasPoint) {
                return dispatchInventoryDropAction.test(slot, expectedItemId, preparedCanvasPoint);
            }

            @Override
            public void applyDropPerceptionDelay() {
                applyDropPerceptionDelay.run();
            }

            @Override
            public void incrementClicksDispatched() {
                incrementClicksDispatched.run();
            }

            @Override
            public FatigueSnapshot fatigueSnapshot() {
                return fatigueSnapshot.get();
            }

            @Override
            public void onDropCadenceProfileSelected(String profileKey) {
                onDropCadenceProfileSelected.accept(profileKey);
            }

            @Override
            public void onIdleCadenceTuningSelected(IdleCadenceTuning tuning) {
                onIdleCadenceTuningSelected.accept(tuning);
            }

            @Override
            public IdleSkillContext resolveIdleSkillContext() {
                return resolveIdleSkillContext.get();
            }

            @Override
            public FishingIdleMode resolveFishingIdleMode(IdleSkillContext context) {
                return resolveFishingIdleMode.apply(context);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public void emitDropDebug(String reason, JsonObject detailsJson) {
                emitDropDebug.accept(reason, detailsJson);
            }

            @Override
            public CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public CommandExecutor.CommandDecision rejectDecision(String reason) {
                return rejectDecision.apply(reason);
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
        return ExecutorIdleHostFactory.createIdleRuntimeHost(
            hasActiveSession,
            hasActiveSessionOtherThan,
            activeSessionName,
            hasActiveDropSweepSession,
            releaseIdleMotorOwnership,
            isIdleInterActionWindowOpen,
            resolveIdleSkillContext,
            isIdleActionWindowOpen,
            isIdleCameraWindowOpen,
            idleWindowGateSnapshot,
            isIdleAnimationActiveNow,
            isIdleInteractionDelaySatisfied,
            isIdleCameraInteractionDelaySatisfied,
            lastInteractionClickSerial,
            isCursorOutsideClientWindow,
            acquireOrRenewIdleMotorOwnership,
            canPerformIdleMotorActionNow,
            performIdleCameraMicroAdjust,
            resolveIdleHoverTargetCanvasPoint,
            performIdleCursorMove,
            resolveIdleDriftTargetCanvasPoint,
            resolveIdleOffscreenTargetScreenPoint,
            performIdleOffscreenCursorMove,
            resolveIdleParkingTargetCanvasPoint,
            resolveFishingIdleMode,
            resolveActivityIdlePolicy,
            activeIdleCadenceTuning,
            fatigueSnapshot,
            isFishingOffscreenIdleSuppressed,
            fishingOffscreenIdleSuppressionRemainingMs,
            isFishingInventoryFullAfkActive,
            fishingInventoryFullAfkRemainingMs,
            emitIdleEvent
        );
    }

    static RandomEventDismissRuntime.Host createRandomEventDismissRuntimeHost(
        BooleanSupplier isRuntimeEnabled,
        BooleanSupplier isRuntimeArmed,
        BooleanSupplier isLoggedIn,
        BooleanSupplier isBankOpen,
        Predicate<String> hasActiveSessionOtherThan,
        BooleanSupplier hasActiveInteractionMotorProgram,
        BooleanSupplier acquireOrRenewInteractionMotorOwnership,
        Runnable releaseInteractionMotorOwnership,
        Supplier<Player> localPlayer,
        Supplier<Iterable<NPC>> npcs,
        Function<NPC, Point> resolveVariedNpcClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearTarget,
        Predicate<Point> selectDismissMenuOptionAt,
        LongBinaryOperator randomBetween,
        LongSupplier randomEventPreAttemptCooldownMinMs,
        LongSupplier randomEventPreAttemptCooldownMaxMs,
        LongSupplier randomEventSuccessCooldownMinMs,
        LongSupplier randomEventSuccessCooldownMaxMs,
        LongSupplier randomEventFailureRetryCooldownMinMs,
        LongSupplier randomEventFailureRetryCooldownMaxMs,
        LongSupplier randomEventCursorReadyHoldMs,
        Function<Object[], JsonObject> details,
        BiConsumer<String, JsonObject> emitRandomEventEvent
    ) {
        return new RandomEventDismissRuntime.Host() {
            @Override
            public boolean isRuntimeEnabled() {
                return isRuntimeEnabled.getAsBoolean();
            }

            @Override
            public boolean isRuntimeArmed() {
                return isRuntimeArmed.getAsBoolean();
            }

            @Override
            public boolean isLoggedIn() {
                return isLoggedIn.getAsBoolean();
            }

            @Override
            public boolean isBankOpen() {
                return isBankOpen.getAsBoolean();
            }

            @Override
            public boolean hasActiveSessionOtherThan(String sessionName) {
                return hasActiveSessionOtherThan.test(sessionName);
            }

            @Override
            public boolean hasActiveInteractionMotorProgram() {
                return hasActiveInteractionMotorProgram.getAsBoolean();
            }

            @Override
            public boolean acquireOrRenewInteractionMotorOwnership() {
                return acquireOrRenewInteractionMotorOwnership.getAsBoolean();
            }

            @Override
            public void releaseInteractionMotorOwnership() {
                releaseInteractionMotorOwnership.run();
            }

            @Override
            public Player localPlayer() {
                return localPlayer.get();
            }

            @Override
            public Iterable<NPC> npcs() {
                return npcs.get();
            }

            @Override
            public Point resolveVariedNpcClickPoint(NPC npc) {
                return resolveVariedNpcClickPoint.apply(npc);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public boolean moveInteractionCursorToCanvasPoint(Point canvasPoint) {
                return moveInteractionCursorToCanvasPoint.test(canvasPoint);
            }

            @Override
            public boolean isCursorNearTarget(Point canvasPoint) {
                return isCursorNearTarget.test(canvasPoint);
            }

            @Override
            public boolean selectDismissMenuOptionAt(Point canvasPoint) {
                return selectDismissMenuOptionAt.test(canvasPoint);
            }

            @Override
            public long randomBetween(long minInclusive, long maxInclusive) {
                return randomBetween.applyAsLong(minInclusive, maxInclusive);
            }

            @Override
            public long randomEventPreAttemptCooldownMinMs() {
                return randomEventPreAttemptCooldownMinMs.getAsLong();
            }

            @Override
            public long randomEventPreAttemptCooldownMaxMs() {
                return randomEventPreAttemptCooldownMaxMs.getAsLong();
            }

            @Override
            public long randomEventSuccessCooldownMinMs() {
                return randomEventSuccessCooldownMinMs.getAsLong();
            }

            @Override
            public long randomEventSuccessCooldownMaxMs() {
                return randomEventSuccessCooldownMaxMs.getAsLong();
            }

            @Override
            public long randomEventFailureRetryCooldownMinMs() {
                return randomEventFailureRetryCooldownMinMs.getAsLong();
            }

            @Override
            public long randomEventFailureRetryCooldownMaxMs() {
                return randomEventFailureRetryCooldownMaxMs.getAsLong();
            }

            @Override
            public long randomEventCursorReadyHoldMs() {
                return randomEventCursorReadyHoldMs.getAsLong();
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public void emitRandomEventEvent(String reason, JsonObject detailsJson) {
                emitRandomEventEvent.accept(reason, detailsJson);
            }
        };
    }
}
