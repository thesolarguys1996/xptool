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

final class ExecutorMotorRuntimeWiring {
    private ExecutorMotorRuntimeWiring() {
    }

    static ExecutorMotorRuntimeBundle createBundle(ExecutorMotorRuntimeInputs.Inputs inputs) {
        return createBundle(
            inputs.sessionManager,
            inputs.isUsableCanvasPoint,
            inputs.canPerformMotorActionNow,
            inputs.focusClientWindowAndCanvas,
            inputs.toScreenPoint,
            inputs.getOrCreateRobot,
            inputs.dropSweepRegionScreen,
            inputs.dropSweepLastTargetScreen,
            inputs.setDropSweepLastTargetScreen,
            inputs.dropSweepAwaitingFirstCursorSync,
            inputs.motorCursorLocationOr,
            inputs.isCursorNearScreenPoint,
            inputs.tryConsumeMouseMutationBudget,
            inputs.currentPointerLocationOr,
            inputs.moveMouseCurveTo,
            inputs.moveMouseCurve,
            inputs.moveMouseCurveIdle,
            inputs.isIdleMotorOwnerActive,
            inputs.noteMouseMutation,
            inputs.noteMotorProgramFirstMouseMutation,
            inputs.updateMotorCursorState,
            inputs.clearPendingMouseMove,
            inputs.noteInteractionActivityNow,
            inputs.motorCursorScreenPoint,
            inputs.failMotorProgram,
            inputs.completeMotorProgram,
            inputs.normalizedMotorOwnerName,
            inputs.isWoodcutWorldClickType,
            inputs.isFishingWorldClickType,
            inputs.noteInteractionClickSuccess,
            inputs.pendingMouseMove,
            inputs.isPendingMouseMoveOwnerValid,
            inputs.isMotorActionReadyNow,
            inputs.notePendingMoveAge,
            inputs.pendingMoveHasExceededCommitTimeout,
            inputs.pendingMoveTargetInvalidated,
            inputs.notePendingMoveRemainingDistance,
            inputs.notePendingMoveBlocked,
            inputs.notePendingMoveAdvanced,
            inputs.notePendingMoveCleared,
            inputs.activeMotorProgram,
            inputs.motorProgramLeaseMsForOwner,
            inputs.cancelMotorProgram,
            inputs.pushMotorOwnerContext,
            inputs.pushClickTypeContext,
            inputs.advanceMotorProgramMove,
            inputs.validateMotorProgramMenu,
            inputs.popClickTypeContext,
            inputs.popMotorOwnerContext,
            inputs.humanizedTimingEnabled
        );
    }

    static ExecutorMotorRuntimeBundle createBundle(
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
        MotorCanvasMoveEngine motorCanvasMoveEngine = ExecutorEngineWiring.createMotorCanvasMoveEngine(
            isUsableCanvasPoint,
            canPerformMotorActionNow,
            focusClientWindowAndCanvas,
            toScreenPoint,
            getOrCreateRobot,
            dropSweepRegionScreen,
            dropSweepLastTargetScreen,
            setDropSweepLastTargetScreen,
            dropSweepAwaitingFirstCursorSync,
            motorCursorLocationOr,
            isCursorNearScreenPoint,
            tryConsumeMouseMutationBudget,
            currentPointerLocationOr,
            moveMouseCurveTo,
            moveMouseCurve,
            moveMouseCurveIdle,
            isIdleMotorOwnerActive,
            noteMouseMutation,
            updateMotorCursorState,
            clearPendingMouseMove,
            noteInteractionActivityNow,
            ExecutorEngineConfigCatalog.createMotorCanvasMoveEngineConfig(humanizedTimingEnabled)
        );

        MotorProgramMoveEngine motorProgramMoveEngine = ExecutorEngineWiring.createMotorProgramMoveEngine(
            toScreenPoint,
            dropSweepRegionScreen,
            dropSweepLastTargetScreen,
            setDropSweepLastTargetScreen,
            dropSweepAwaitingFirstCursorSync,
            motorCursorScreenPoint,
            motorCursorLocationOr,
            getOrCreateRobot,
            failMotorProgram,
            tryConsumeMouseMutationBudget,
            currentPointerLocationOr,
            noteMouseMutation,
            noteMotorProgramFirstMouseMutation,
            noteInteractionActivityNow,
            isCursorNearScreenPoint,
            updateMotorCursorState,
            completeMotorProgram,
            normalizedMotorOwnerName,
            isWoodcutWorldClickType,
            isFishingWorldClickType,
            ExecutorEngineConfigCatalog.createMotorProgramMoveEngineConfig(humanizedTimingEnabled)
        );

        MotorProgramClickEngine motorProgramClickEngine = ExecutorEngineWiring.createMotorProgramClickEngine(
            tryConsumeMouseMutationBudget,
            getOrCreateRobot,
            failMotorProgram,
            completeMotorProgram,
            noteInteractionClickSuccess,
            ExecutorEngineConfigCatalog.createMotorProgramClickEngineConfig(humanizedTimingEnabled)
        );

        MotorRuntimePort motorRuntimePort = ExecutorEngineWiring.createMotorRuntimePort(
            pendingMouseMove,
            clearPendingMouseMove,
            isPendingMouseMoveOwnerValid,
            isMotorActionReadyNow,
            notePendingMoveAge,
            pendingMoveHasExceededCommitTimeout,
            pendingMoveTargetInvalidated,
            notePendingMoveRemainingDistance,
            tryConsumeMouseMutationBudget,
            currentPointerLocationOr,
            notePendingMoveBlocked,
            notePendingMoveAdvanced,
            notePendingMoveCleared,
            noteMouseMutation,
            noteInteractionActivityNow,
            () -> ExecutorEngineConfigCatalog.PENDING_MOVE_ARRIVAL_TOLERANCE_PX,
            activeMotorProgram,
            normalizedMotorOwnerName,
            sessionManager::isMotorOwner,
            sessionManager::renewMotor,
            motorProgramLeaseMsForOwner,
            cancelMotorProgram,
            pushMotorOwnerContext,
            pushClickTypeContext,
            advanceMotorProgramMove,
            validateMotorProgramMenu,
            failMotorProgram,
            motorProgramClickEngine::runMotorProgramClick,
            popClickTypeContext,
            popMotorOwnerContext
        );

        return new ExecutorMotorRuntimeBundle(
            motorCanvasMoveEngine,
            motorProgramMoveEngine,
            new MotorRuntime(motorRuntimePort)
        );
    }
}
