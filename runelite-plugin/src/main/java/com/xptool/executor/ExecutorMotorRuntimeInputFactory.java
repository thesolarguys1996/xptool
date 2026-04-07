package com.xptool.executor;

import com.xptool.sessions.SessionManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

final class ExecutorMotorRuntimeInputFactory {
    private ExecutorMotorRuntimeInputFactory() {
    }

    static ExecutorMotorRuntimeInputs.Inputs create(
        SessionManager sessionManager,
        Predicate<Point> isUsableCanvasPoint,
        BooleanSupplier canPerformMotorActionNow,
        Predicate<Boolean> focusClientWindowAndCanvas,
        Function<Point, Optional<Point>> toScreenPoint,
        Supplier<Robot> getOrCreateRobot,
        Supplier<Rectangle> dropSweepRegionScreen,
        Supplier<Point> dropSweepLastTargetScreen,
        Consumer<Point> setDropSweepLastTargetScreen,
        BooleanSupplier dropSweepAwaitingFirstCursorSync,
        Function<Point, Point> motorCursorLocationOr,
        ExecutorEngineWiring.ScreenTolerancePredicate isCursorNearScreenPoint,
        BooleanSupplier tryConsumeMouseMutationBudget,
        Function<Point, Point> currentPointerLocationOr,
        BiConsumer<Robot, Point> moveMouseCurveTo,
        ExecutorEngineWiring.RobotMoveCurve moveMouseCurve,
        ExecutorEngineWiring.RobotPointPointAction moveMouseCurveIdle,
        BooleanSupplier isIdleMotorOwnerActive,
        Consumer<Point> noteMouseMutation,
        Consumer<MotorProgram> noteMotorProgramFirstMouseMutation,
        Consumer<Point> updateMotorCursorState,
        Runnable clearPendingMouseMove,
        Runnable noteInteractionActivityNow,
        Supplier<Point> motorCursorScreenPoint,
        BiConsumer<MotorProgram, String> failMotorProgram,
        BiConsumer<MotorProgram, String> completeMotorProgram,
        Function<String, String> normalizedMotorOwnerName,
        Predicate<String> isWoodcutWorldClickType,
        Predicate<String> isFishingWorldClickType,
        Consumer<String> noteInteractionClickSuccess,
        Supplier<PendingMouseMove> pendingMouseMove,
        Predicate<PendingMouseMove> isPendingMouseMoveOwnerValid,
        BooleanSupplier isMotorActionReadyNow,
        Consumer<PendingMouseMove> notePendingMoveAge,
        Predicate<PendingMouseMove> pendingMoveHasExceededCommitTimeout,
        Predicate<PendingMouseMove> pendingMoveTargetInvalidated,
        Consumer<PendingMouseMove> notePendingMoveRemainingDistance,
        MotorRuntimePortAdapter.PendingMoveBlockOrClearObserver notePendingMoveBlocked,
        MotorRuntimePortAdapter.PendingMoveAdvanceObserver notePendingMoveAdvanced,
        MotorRuntimePortAdapter.PendingMoveBlockOrClearObserver notePendingMoveCleared,
        Supplier<MotorProgram> activeMotorProgram,
        ToLongFunction<String> motorProgramLeaseMsForOwner,
        BiConsumer<MotorProgram, String> cancelMotorProgram,
        Function<String, String> pushMotorOwnerContext,
        Function<String, String> pushClickTypeContext,
        Consumer<MotorProgram> advanceMotorProgramMove,
        Predicate<MotorProgram> validateMotorProgramMenu,
        Consumer<String> popClickTypeContext,
        Consumer<String> popMotorOwnerContext,
        boolean humanizedTimingEnabled
    ) {
        ExecutorMotorRuntimeInputs.Inputs motorRuntimeInputs = new ExecutorMotorRuntimeInputs.Inputs();
        motorRuntimeInputs.sessionManager = sessionManager;
        motorRuntimeInputs.isUsableCanvasPoint = isUsableCanvasPoint;
        motorRuntimeInputs.canPerformMotorActionNow = canPerformMotorActionNow;
        motorRuntimeInputs.focusClientWindowAndCanvas = focusClientWindowAndCanvas;
        motorRuntimeInputs.toScreenPoint = toScreenPoint;
        motorRuntimeInputs.getOrCreateRobot = getOrCreateRobot;
        motorRuntimeInputs.dropSweepRegionScreen = dropSweepRegionScreen;
        motorRuntimeInputs.dropSweepLastTargetScreen = dropSweepLastTargetScreen;
        motorRuntimeInputs.setDropSweepLastTargetScreen = setDropSweepLastTargetScreen;
        motorRuntimeInputs.dropSweepAwaitingFirstCursorSync = dropSweepAwaitingFirstCursorSync;
        motorRuntimeInputs.motorCursorLocationOr = motorCursorLocationOr;
        motorRuntimeInputs.isCursorNearScreenPoint = isCursorNearScreenPoint;
        motorRuntimeInputs.tryConsumeMouseMutationBudget = tryConsumeMouseMutationBudget;
        motorRuntimeInputs.currentPointerLocationOr = currentPointerLocationOr;
        motorRuntimeInputs.moveMouseCurveTo = moveMouseCurveTo;
        motorRuntimeInputs.moveMouseCurve = moveMouseCurve;
        motorRuntimeInputs.moveMouseCurveIdle = moveMouseCurveIdle;
        motorRuntimeInputs.isIdleMotorOwnerActive = isIdleMotorOwnerActive;
        motorRuntimeInputs.noteMouseMutation = noteMouseMutation;
        motorRuntimeInputs.noteMotorProgramFirstMouseMutation = noteMotorProgramFirstMouseMutation;
        motorRuntimeInputs.updateMotorCursorState = updateMotorCursorState;
        motorRuntimeInputs.clearPendingMouseMove = clearPendingMouseMove;
        motorRuntimeInputs.noteInteractionActivityNow = noteInteractionActivityNow;
        motorRuntimeInputs.motorCursorScreenPoint = motorCursorScreenPoint;
        motorRuntimeInputs.failMotorProgram = failMotorProgram;
        motorRuntimeInputs.completeMotorProgram = completeMotorProgram;
        motorRuntimeInputs.normalizedMotorOwnerName = normalizedMotorOwnerName;
        motorRuntimeInputs.isWoodcutWorldClickType = isWoodcutWorldClickType;
        motorRuntimeInputs.isFishingWorldClickType = isFishingWorldClickType;
        motorRuntimeInputs.noteInteractionClickSuccess = noteInteractionClickSuccess;
        motorRuntimeInputs.pendingMouseMove = pendingMouseMove;
        motorRuntimeInputs.isPendingMouseMoveOwnerValid = isPendingMouseMoveOwnerValid;
        motorRuntimeInputs.isMotorActionReadyNow = isMotorActionReadyNow;
        motorRuntimeInputs.notePendingMoveAge = notePendingMoveAge;
        motorRuntimeInputs.pendingMoveHasExceededCommitTimeout = pendingMoveHasExceededCommitTimeout;
        motorRuntimeInputs.pendingMoveTargetInvalidated = pendingMoveTargetInvalidated;
        motorRuntimeInputs.notePendingMoveRemainingDistance = notePendingMoveRemainingDistance;
        motorRuntimeInputs.notePendingMoveBlocked = notePendingMoveBlocked;
        motorRuntimeInputs.notePendingMoveAdvanced = notePendingMoveAdvanced;
        motorRuntimeInputs.notePendingMoveCleared = notePendingMoveCleared;
        motorRuntimeInputs.activeMotorProgram = activeMotorProgram;
        motorRuntimeInputs.motorProgramLeaseMsForOwner = motorProgramLeaseMsForOwner;
        motorRuntimeInputs.cancelMotorProgram = cancelMotorProgram;
        motorRuntimeInputs.pushMotorOwnerContext = pushMotorOwnerContext;
        motorRuntimeInputs.pushClickTypeContext = pushClickTypeContext;
        motorRuntimeInputs.advanceMotorProgramMove = advanceMotorProgramMove;
        motorRuntimeInputs.validateMotorProgramMenu = validateMotorProgramMenu;
        motorRuntimeInputs.popClickTypeContext = popClickTypeContext;
        motorRuntimeInputs.popMotorOwnerContext = popMotorOwnerContext;
        motorRuntimeInputs.humanizedTimingEnabled = humanizedTimingEnabled;
        return motorRuntimeInputs;
    }
}
