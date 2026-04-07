package com.xptool.executor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile.MotorGestureMode;
import com.xptool.sessions.idle.IdleSkillContext;
import com.xptool.systems.CommandIngestor;
import com.xptool.systems.TargetSelectionEngine;
import java.awt.Canvas;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToLongFunction;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;

final class ExecutorEngineWiring {
    private ExecutorEngineWiring() {
    }

    @FunctionalInterface
    interface RobotMoveCurve {
        void apply(Robot robot, Point from, Point to, int steps, long stepDelayMs);
    }

    @FunctionalInterface
    interface RobotPointPointAction {
        void apply(Robot robot, Point from, Point to);
    }

    @FunctionalInterface
    interface ScreenTolerancePredicate {
        boolean test(Point point, double tolerancePx);
    }

    @FunctionalInterface
    interface RegionInsetPointResolver {
        Optional<Point> apply(Rectangle region, int insetPx);
    }

    @FunctionalInterface
    interface MotorCanvasMoveExecutor {
        boolean apply(
            Point canvasPoint,
            MotorGestureMode gestureMode,
            boolean allowActivationClick,
            boolean dropSweepMode,
            boolean requireReady
        );
    }

    @FunctionalInterface
    interface RobotPointAction {
        void apply(Robot robot, Point point);
    }

    @FunctionalInterface
    interface PointBoundsRadiusResolver {
        Point apply(Point point, Rectangle bounds, int radiusPx);
    }

    @FunctionalInterface
    interface WidgetSlotPointResolver {
        Optional<Point> apply(Widget widget, int slot);
    }

    @FunctionalInterface
    interface SlotWidgetResolver {
        Widget apply(int slot);
    }

    @FunctionalInterface
    interface WidgetSlotBoundsResolver {
        Optional<Rectangle> apply(Widget widget, int slot);
    }

    @FunctionalInterface
    interface EmitConfigUpdated {
        void emit(String configuredPath, boolean exists);
    }

    @FunctionalInterface
    interface ParseCommandLine {
        Optional<CommandRowParser.ParsedCommandRow> parse(String line, Gson gson);
    }

    static CommandIngestor createCommandIngestor(
        Supplier<String> resolveCommandFilePath,
        IntSupplier maxLinesPerPoll,
        EmitConfigUpdated onConfigPathUpdated,
        Runnable onCommandFileAttached,
        Runnable onCommandFileTruncated,
        ParseCommandLine parseCommandLine,
        Supplier<Gson> gsonSupplier,
        Consumer<CommandRowParser.ParsedCommandRow> onParsedCommandLine,
        Consumer<String> onFailure,
        int ingestPollIntervalMs,
        String threadName
    ) {
        return new CommandIngestor(
            new CommandIngestor.Host() {
                @Override
                public String resolveCommandFilePath() {
                    return resolveCommandFilePath.get();
                }

                @Override
                public int maxLinesPerPoll() {
                    return maxLinesPerPoll.getAsInt();
                }

                @Override
                public void onConfigPathUpdated(String configuredPath, boolean exists) {
                    onConfigPathUpdated.emit(configuredPath, exists);
                }

                @Override
                public void onCommandFileAttached() {
                    onCommandFileAttached.run();
                }

                @Override
                public void onCommandFileTruncated() {
                    onCommandFileTruncated.run();
                }

                @Override
                public void onCommandLine(String line) {
                    parseCommandLine.parse(line, gsonSupplier.get()).ifPresent(onParsedCommandLine);
                }

                @Override
                public void onFailure(String reason) {
                    onFailure.accept(reason);
                }
            },
            ingestPollIntervalMs,
            threadName
        );
    }

    static TargetSelectionEngine createTargetSelectionEngine(
        Supplier<Point> currentCursorCanvasPoint,
        Function<TileObject, Point> primaryCandidateCanvasPoint,
        Function<TileObject, Point> fallbackCandidateCanvasPoint,
        Predicate<Point> isUsableCanvasPoint,
        ToDoubleBiFunction<Point, Point> pixelDistance,
        IntSupplier consecutiveLocalInteractions,
        TargetSelectionEngine.Config config
    ) {
        return new TargetSelectionEngine(
            new TargetSelectionEngine.Host() {
                @Override
                public Point currentCursorCanvasPoint() {
                    return currentCursorCanvasPoint.get();
                }

                @Override
                public Point primaryCandidateCanvasPoint(TileObject candidate) {
                    return primaryCandidateCanvasPoint.apply(candidate);
                }

                @Override
                public Point fallbackCandidateCanvasPoint(TileObject candidate) {
                    return fallbackCandidateCanvasPoint.apply(candidate);
                }

                @Override
                public boolean isUsableCanvasPoint(Point point) {
                    return isUsableCanvasPoint.test(point);
                }

                @Override
                public double pixelDistance(Point from, Point to) {
                    return pixelDistance.applyAsDouble(from, to);
                }

                @Override
                public int consecutiveLocalInteractions() {
                    return consecutiveLocalInteractions.getAsInt();
                }
            },
            config
        );
    }

    static MotorRuntimePort createMotorRuntimePort(
        Supplier<PendingMouseMove> pendingMouseMove,
        Runnable clearPendingMouseMove,
        Predicate<PendingMouseMove> isPendingMouseMoveOwnerValid,
        BooleanSupplier isMotorActionReadyNow,
        Consumer<PendingMouseMove> notePendingMoveAge,
        Predicate<PendingMouseMove> pendingMoveHasExceededCommitTimeout,
        Predicate<PendingMouseMove> pendingMoveTargetInvalidated,
        Consumer<PendingMouseMove> notePendingMoveRemainingDistance,
        BooleanSupplier tryConsumeMouseMutationBudget,
        Function<Point, Point> currentPointerLocationOr,
        MotorRuntimePortAdapter.PendingMoveBlockOrClearObserver notePendingMoveBlocked,
        MotorRuntimePortAdapter.PendingMoveAdvanceObserver notePendingMoveAdvanced,
        MotorRuntimePortAdapter.PendingMoveBlockOrClearObserver notePendingMoveCleared,
        Consumer<Point> noteMouseMutation,
        Runnable noteInteractionActivityNow,
        DoubleSupplier pendingMoveArrivalTolerancePx,
        Supplier<MotorProgram> activeMotorProgram,
        Function<String, String> normalizedMotorOwnerName,
        Predicate<String> isSessionMotorOwner,
        MotorRuntimePortAdapter.RenewSessionMotor renewSessionMotor,
        ToLongFunction<String> motorProgramLeaseMsForOwner,
        BiConsumer<MotorProgram, String> cancelMotorProgram,
        Function<String, String> pushMotorOwnerContext,
        Function<String, String> pushClickTypeContext,
        Consumer<MotorProgram> advanceMotorProgramMove,
        Predicate<MotorProgram> validateMotorProgramMenu,
        BiConsumer<MotorProgram, String> failMotorProgram,
        Consumer<MotorProgram> runMotorProgramClick,
        Consumer<String> popClickTypeContext,
        Consumer<String> popMotorOwnerContext
    ) {
        return MotorRuntimePortAdapter.create(
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
            pendingMoveArrivalTolerancePx,
            activeMotorProgram,
            normalizedMotorOwnerName,
            isSessionMotorOwner,
            renewSessionMotor,
            motorProgramLeaseMsForOwner,
            cancelMotorProgram,
            pushMotorOwnerContext,
            pushClickTypeContext,
            advanceMotorProgramMove,
            validateMotorProgramMenu,
            failMotorProgram,
            runMotorProgramClick,
            popClickTypeContext,
            popMotorOwnerContext
        );
    }

    static IdleCursorTargetPlanner createIdleCursorTargetPlanner(
        Supplier<Optional<Rectangle>> resolveInventoryInteractionRegionCanvas,
        RegionInsetPointResolver randomCanvasPointInRegion,
        Predicate<Point> isUsableCanvasPoint,
        IntSupplier canvasWidth,
        IntSupplier canvasHeight,
        Supplier<Point> currentMouseCanvasPoint,
        Supplier<Optional<Rectangle>> resolveClientCanvasBoundsScreen,
        Supplier<Optional<Rectangle>> resolveClientWindowBoundsScreen,
        Function<Point, Optional<Rectangle>> resolveScreenBoundsForPoint,
        Function<Point, Point> currentPointerLocationOr,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        Supplier<IdleCadenceTuning> activeIdleCadenceTuning,
        BiConsumer<String, JsonObject> emitIdleEvent
    ) {
        return new IdleCursorTargetPlanner(new IdleCursorTargetPlanner.Host() {
            @Override
            public Optional<Rectangle> resolveInventoryInteractionRegionCanvas() {
                return resolveInventoryInteractionRegionCanvas.get();
            }

            @Override
            public Optional<Point> randomCanvasPointInRegion(Rectangle region, int insetPx) {
                return randomCanvasPointInRegion.apply(region, insetPx);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public int canvasWidth() {
                return canvasWidth.getAsInt();
            }

            @Override
            public int canvasHeight() {
                return canvasHeight.getAsInt();
            }

            @Override
            public Point currentMouseCanvasPoint() {
                return currentMouseCanvasPoint.get();
            }

            @Override
            public Optional<Rectangle> resolveClientCanvasBoundsScreen() {
                return resolveClientCanvasBoundsScreen.get();
            }

            @Override
            public Optional<Rectangle> resolveClientWindowBoundsScreen() {
                return resolveClientWindowBoundsScreen.get();
            }

            @Override
            public Optional<Rectangle> resolveScreenBoundsForPoint(Point point) {
                return resolveScreenBoundsForPoint.apply(point);
            }

            @Override
            public Point currentPointerLocationOr(Point fallback) {
                return currentPointerLocationOr.apply(fallback);
            }

            @Override
            public IdleSkillContext resolveIdleSkillContext() {
                return resolveIdleSkillContext == null ? IdleSkillContext.GLOBAL : resolveIdleSkillContext.get();
            }

            @Override
            public IdleCadenceTuning activeIdleCadenceTuning() {
                IdleCadenceTuning tuning = activeIdleCadenceTuning == null ? null : activeIdleCadenceTuning.get();
                return tuning == null ? IdleCadenceTuning.none() : tuning;
            }

            @Override
            public void emitIdleEvent(String reason, JsonObject details) {
                if (emitIdleEvent != null) {
                    emitIdleEvent.accept(reason, details);
                }
            }
        });
    }

    static TargetPointVariationEngine createTargetPointVariationEngine(
        Predicate<Point> isUsableCanvasPoint,
        IntSupplier canvasWidth,
        IntSupplier canvasHeight,
        TargetPointVariationEngine.Config config
    ) {
        return new TargetPointVariationEngine(new TargetPointVariationEngine.Host() {
            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public int canvasWidth() {
                return canvasWidth.getAsInt();
            }

            @Override
            public int canvasHeight() {
                return canvasHeight.getAsInt();
            }
        }, config);
    }

    static InventorySlotPointPlanner createInventorySlotPointPlanner(
        Supplier<Widget> bankInventoryWidget,
        Supplier<Widget> inventoryWidget,
        BooleanSupplier isBankOpen,
        WidgetSlotPointResolver slotCenter,
        SlotWidgetResolver resolveInventorySlotWidget,
        WidgetSlotBoundsResolver slotBoundsByGrid,
        Predicate<Point> isUsableCanvasPoint,
        BiFunction<Point, Rectangle, Point> clampPointToRectangle,
        TargetPointVariationEngine targetPointVariationEngine,
        InventorySlotPointPlanner.Config config
    ) {
        return new InventorySlotPointPlanner(new InventorySlotPointPlanner.Host() {
            @Override
            public Widget bankInventoryWidget() {
                return bankInventoryWidget.get();
            }

            @Override
            public Widget inventoryWidget() {
                return inventoryWidget.get();
            }

            @Override
            public boolean isBankOpen() {
                return isBankOpen.getAsBoolean();
            }

            @Override
            public Optional<Point> slotCenter(Widget container, int slot) {
                return slotCenter.apply(container, slot);
            }

            @Override
            public Widget resolveInventorySlotWidget(int slot) {
                return resolveInventorySlotWidget.apply(slot);
            }

            @Override
            public Optional<Rectangle> slotBoundsByGrid(Widget container, int slot) {
                return slotBoundsByGrid.apply(container, slot);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public Point clampPointToRectangle(Point point, Rectangle bounds) {
                return clampPointToRectangle.apply(point, bounds);
            }
        }, targetPointVariationEngine, config);
    }

    static FocusMenuInteractionController createFocusMenuInteractionController(
        Supplier<Canvas> clientCanvas,
        BooleanSupplier isMenuOpen,
        Supplier<Robot> getOrCreateRobot,
        BooleanSupplier canPerformMotorActionNow,
        PointBoundsRadiusResolver jitterWithinBounds,
        RobotPointAction moveMouseCurve,
        LongConsumer sleepCritical,
        LongConsumer sleepNoCooldown,
        LongConsumer sleepQuietly,
        Runnable noteMotorAction,
        FocusMenuInteractionController.Config config
    ) {
        return new FocusMenuInteractionController(new FocusMenuInteractionController.Host() {
            @Override
            public Canvas clientCanvas() {
                return clientCanvas.get();
            }

            @Override
            public boolean isMenuOpen() {
                return isMenuOpen.getAsBoolean();
            }

            @Override
            public Robot getOrCreateRobot() {
                return getOrCreateRobot.get();
            }

            @Override
            public boolean canPerformMotorActionNow() {
                return canPerformMotorActionNow.getAsBoolean();
            }

            @Override
            public Point jitterWithinBounds(Point base, Rectangle bounds, int radiusPx) {
                return jitterWithinBounds.apply(base, bounds, radiusPx);
            }

            @Override
            public void moveMouseCurve(Robot robot, Point to) {
                moveMouseCurve.apply(robot, to);
            }

            @Override
            public void sleepCritical(long ms) {
                sleepCritical.accept(ms);
            }

            @Override
            public void sleepNoCooldown(long ms) {
                sleepNoCooldown.accept(ms);
            }

            @Override
            public void sleepQuietly(long ms) {
                sleepQuietly.accept(ms);
            }

            @Override
            public void noteMotorAction() {
                noteMotorAction.run();
            }
        }, config);
    }

    static InteractionClickEngine createInteractionClickEngine(
        Predicate<Point> isUsableCanvasPoint,
        BooleanSupplier canPerformMotorActionNow,
        MotorCanvasMoveExecutor motorMoveToCanvasPoint,
        Supplier<Point> currentMouseCanvasPoint,
        BiFunction<Point, Rectangle, Point> clampPointToRectangle,
        Supplier<Optional<Rectangle>> resolveInventoryInteractionRegionCanvas,
        Supplier<Point> lastInteractionAnchorCenterCanvasPoint,
        Supplier<Rectangle> lastInteractionAnchorBoundsCanvas,
        IntSupplier canvasWidth,
        IntSupplier canvasHeight,
        ToDoubleBiFunction<Point, Point> pixelDistance,
        BooleanSupplier isClientCanvasFocused,
        BooleanSupplier allowWindowRefocusForInteraction,
        BooleanSupplier focusClientWindowAndCanvas,
        Function<Point, Optional<Point>> toScreenPoint,
        Supplier<Robot> getOrCreateRobot,
        Consumer<Robot> clickCanvasActivationAnchor,
        Runnable noteInteractionClickSuccess,
        ScreenTolerancePredicate isCursorNearScreenPoint,
        RobotPointAction moveMouseCurve,
        LongConsumer sleepCritical,
        LongConsumer sleepNoCooldown,
        InteractionClickEngine.Config config
    ) {
        return new InteractionClickEngine(new InteractionClickEngine.Host() {
            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public boolean canPerformMotorActionNow() {
                return canPerformMotorActionNow.getAsBoolean();
            }

            @Override
            public boolean motorMoveToCanvasPoint(
                Point canvasPoint,
                MotorGestureMode gestureMode,
                boolean allowActivationClick,
                boolean dropSweepMode,
                boolean requireReady
            ) {
                return motorMoveToCanvasPoint.apply(
                    canvasPoint,
                    gestureMode,
                    allowActivationClick,
                    dropSweepMode,
                    requireReady
                );
            }

            @Override
            public Point currentMouseCanvasPoint() {
                return currentMouseCanvasPoint.get();
            }

            @Override
            public Point clampPointToRectangle(Point point, Rectangle bounds) {
                return clampPointToRectangle.apply(point, bounds);
            }

            @Override
            public Optional<Rectangle> resolveInventoryInteractionRegionCanvas() {
                return resolveInventoryInteractionRegionCanvas.get();
            }

            @Override
            public Point lastInteractionAnchorCenterCanvasPoint() {
                return lastInteractionAnchorCenterCanvasPoint.get();
            }

            @Override
            public Rectangle lastInteractionAnchorBoundsCanvas() {
                return lastInteractionAnchorBoundsCanvas.get();
            }

            @Override
            public int canvasWidth() {
                return canvasWidth.getAsInt();
            }

            @Override
            public int canvasHeight() {
                return canvasHeight.getAsInt();
            }

            @Override
            public double pixelDistance(Point a, Point b) {
                return pixelDistance.applyAsDouble(a, b);
            }

            @Override
            public boolean isClientCanvasFocused() {
                return isClientCanvasFocused.getAsBoolean();
            }

            @Override
            public boolean allowWindowRefocusForInteraction() {
                return allowWindowRefocusForInteraction.getAsBoolean();
            }

            @Override
            public boolean focusClientWindowAndCanvas() {
                return focusClientWindowAndCanvas.getAsBoolean();
            }

            @Override
            public Optional<Point> toScreenPoint(Point canvasPoint) {
                return toScreenPoint.apply(canvasPoint);
            }

            @Override
            public Robot getOrCreateRobot() {
                return getOrCreateRobot.get();
            }

            @Override
            public void clickCanvasActivationAnchor(Robot robot) {
                clickCanvasActivationAnchor.accept(robot);
            }

            @Override
            public void noteInteractionClickSuccess() {
                noteInteractionClickSuccess.run();
            }

            @Override
            public boolean isCursorNearScreenPoint(Point screenPoint, double tolerancePx) {
                return isCursorNearScreenPoint.test(screenPoint, tolerancePx);
            }

            @Override
            public void moveMouseCurve(Robot robot, Point to) {
                moveMouseCurve.apply(robot, to);
            }

            @Override
            public void sleepCritical(long ms) {
                sleepCritical.accept(ms);
            }

            @Override
            public void sleepNoCooldown(long ms) {
                sleepNoCooldown.accept(ms);
            }
        }, config);
    }

    static CameraMotionService createCameraMotionService(
        Supplier<Robot> getOrCreateRobot,
        BooleanSupplier isClientCanvasFocused,
        BiPredicate<Boolean, Boolean> focusClientWindowAndCanvas,
        LongConsumer sleepNoCooldown,
        LongConsumer reserveMotorCooldown,
        Runnable noteMotorAction,
        IntSupplier cameraYaw,
        IntSupplier cameraPitch
    ) {
        return new CameraMotionService(
            new CameraMotionService.Host() {
                @Override
                public Robot getOrCreateRobot() {
                    return getOrCreateRobot.get();
                }

                @Override
                public boolean isClientCanvasFocused() {
                    return isClientCanvasFocused.getAsBoolean();
                }

                @Override
                public boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas) {
                    return focusClientWindowAndCanvas.test(focusWindow, focusCanvas);
                }

                @Override
                public void sleepNoCooldown(long ms) {
                    sleepNoCooldown.accept(ms);
                }

                @Override
                public void reserveMotorCooldown(long ms) {
                    reserveMotorCooldown.accept(ms);
                }

                @Override
                public void noteMotorAction() {
                    noteMotorAction.run();
                }

                @Override
                public int cameraYaw() {
                    return cameraYaw.getAsInt();
                }

                @Override
                public int cameraPitch() {
                    return cameraPitch.getAsInt();
                }
            }
        );
    }

    static MotorProgramLifecycleEngine createMotorProgramLifecycleEngine(
        BooleanSupplier isTopMenuBankOnObject,
        Predicate<TileObject> isTopMenuChopOnTree,
        Predicate<TileObject> isTopMenuMineOnRock,
        BooleanSupplier hasAttackEntryOnNpc,
        LongConsumer reserveMotorCooldown
    ) {
        return new MotorProgramLifecycleEngine(new MotorProgramLifecycleEngine.Host() {
            @Override
            public boolean isTopMenuBankOnObject() {
                return isTopMenuBankOnObject.getAsBoolean();
            }

            @Override
            public boolean isTopMenuChopOnTree(TileObject targetObject) {
                return isTopMenuChopOnTree.test(targetObject);
            }

            @Override
            public boolean isTopMenuMineOnRock(TileObject targetObject) {
                return isTopMenuMineOnRock.test(targetObject);
            }

            @Override
            public boolean hasAttackEntryOnNpc() {
                return hasAttackEntryOnNpc.getAsBoolean();
            }

            @Override
            public void reserveMotorCooldown(long ms) {
                reserveMotorCooldown.accept(ms);
            }
        });
    }

    static MotorCanvasMoveEngine createMotorCanvasMoveEngine(
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
        ScreenTolerancePredicate isCursorNearScreenPoint,
        BooleanSupplier tryConsumeMouseMutationBudget,
        Function<Point, Point> currentPointerLocationOr,
        BiConsumer<Robot, Point> moveMouseCurveTo,
        RobotMoveCurve moveMouseCurve,
        RobotPointPointAction moveMouseCurveIdle,
        BooleanSupplier isIdleMotorOwnerActive,
        Consumer<Point> noteMouseMutation,
        Consumer<Point> updateMotorCursorState,
        Runnable clearPendingMouseMove,
        Runnable noteInteractionActivityNow,
        MotorCanvasMoveEngine.Config config
    ) {
        return new MotorCanvasMoveEngine(
            new MotorCanvasMoveEngine.Host() {
                @Override
                public boolean isUsableCanvasPoint(Point canvasPoint) {
                    return isUsableCanvasPoint.test(canvasPoint);
                }

                @Override
                public boolean canPerformMotorActionNow() {
                    return canPerformMotorActionNow.getAsBoolean();
                }

                @Override
                public boolean focusClientWindowAndCanvas(boolean allowActivationClick) {
                    return focusClientWindowAndCanvas.test(allowActivationClick);
                }

                @Override
                public Optional<Point> toScreenPoint(Point canvasPoint) {
                    return toScreenPoint.apply(canvasPoint);
                }

                @Override
                public Robot getOrCreateRobot() {
                    return getOrCreateRobot.get();
                }

                @Override
                public Rectangle dropSweepRegionScreen() {
                    return dropSweepRegionScreen.get();
                }

                @Override
                public Point dropSweepLastTargetScreen() {
                    return dropSweepLastTargetScreen.get();
                }

                @Override
                public void setDropSweepLastTargetScreen(Point point) {
                    setDropSweepLastTargetScreen.accept(point);
                }

                @Override
                public boolean dropSweepAwaitingFirstCursorSync() {
                    return dropSweepAwaitingFirstCursorSync.getAsBoolean();
                }

                @Override
                public Point motorCursorLocationOr(Point fallback) {
                    return motorCursorLocationOr.apply(fallback);
                }

                @Override
                public boolean isCursorNearScreenPoint(Point screenPoint, double tolerancePx) {
                    return isCursorNearScreenPoint.test(screenPoint, tolerancePx);
                }

                @Override
                public boolean tryConsumeMouseMutationBudget() {
                    return tryConsumeMouseMutationBudget.getAsBoolean();
                }

                @Override
                public Point currentPointerLocationOr(Point fallback) {
                    return currentPointerLocationOr.apply(fallback);
                }

                @Override
                public void moveMouseCurveTo(Robot robot, Point to) {
                    moveMouseCurveTo.accept(robot, to);
                }

                @Override
                public void moveMouseCurve(Robot robot, Point from, Point to, int steps, long stepDelayMs) {
                    moveMouseCurve.apply(robot, from, to, steps, stepDelayMs);
                }

                @Override
                public void moveMouseCurveIdle(Robot robot, Point from, Point to) {
                    moveMouseCurveIdle.apply(robot, from, to);
                }

                @Override
                public boolean isIdleMotorOwnerActive() {
                    return isIdleMotorOwnerActive.getAsBoolean();
                }

                @Override
                public void noteMouseMutation(Point point) {
                    noteMouseMutation.accept(point);
                }

                @Override
                public void updateMotorCursorState(Point point) {
                    updateMotorCursorState.accept(point);
                }

                @Override
                public void clearPendingMouseMove() {
                    clearPendingMouseMove.run();
                }

                @Override
                public void noteInteractionActivityNow() {
                    noteInteractionActivityNow.run();
                }
            },
            config
        );
    }

    static MotorProgramMoveEngine createMotorProgramMoveEngine(
        Function<Point, Optional<Point>> toScreenPoint,
        Supplier<Rectangle> dropSweepRegionScreen,
        Supplier<Point> dropSweepLastTargetScreen,
        Consumer<Point> setDropSweepLastTargetScreen,
        BooleanSupplier dropSweepAwaitingFirstCursorSync,
        Supplier<Point> motorCursorScreenPoint,
        Function<Point, Point> motorCursorLocationOr,
        Supplier<Robot> getOrCreateRobot,
        BiConsumer<MotorProgram, String> failMotorProgram,
        BooleanSupplier tryConsumeMouseMutationBudget,
        Function<Point, Point> currentPointerLocationOr,
        Consumer<Point> noteMouseMutation,
        Consumer<MotorProgram> noteMotorProgramFirstMouseMutation,
        Runnable noteInteractionActivityNow,
        ScreenTolerancePredicate isCursorNearScreenPoint,
        Consumer<Point> updateMotorCursorState,
        BiConsumer<MotorProgram, String> completeMotorProgram,
        Function<String, String> normalizedMotorOwnerName,
        Predicate<String> isWoodcutWorldClickType,
        Predicate<String> isFishingWorldClickType,
        MotorProgramMoveEngine.Config config
    ) {
        return new MotorProgramMoveEngine(
            new MotorProgramMoveEngine.Host() {
                @Override
                public Optional<Point> toScreenPoint(Point canvasPoint) {
                    return toScreenPoint.apply(canvasPoint);
                }

                @Override
                public Rectangle dropSweepRegionScreen() {
                    return dropSweepRegionScreen.get();
                }

                @Override
                public Point dropSweepLastTargetScreen() {
                    return dropSweepLastTargetScreen.get();
                }

                @Override
                public void setDropSweepLastTargetScreen(Point point) {
                    setDropSweepLastTargetScreen.accept(point);
                }

                @Override
                public boolean dropSweepAwaitingFirstCursorSync() {
                    return dropSweepAwaitingFirstCursorSync.getAsBoolean();
                }

                @Override
                public Point motorCursorScreenPoint() {
                    return motorCursorScreenPoint.get();
                }

                @Override
                public Point motorCursorLocationOr(Point fallback) {
                    return motorCursorLocationOr.apply(fallback);
                }

                @Override
                public Robot getOrCreateRobot() {
                    return getOrCreateRobot.get();
                }

                @Override
                public void failMotorProgram(MotorProgram program, String reason) {
                    failMotorProgram.accept(program, reason);
                }

                @Override
                public boolean tryConsumeMouseMutationBudget() {
                    return tryConsumeMouseMutationBudget.getAsBoolean();
                }

                @Override
                public Point currentPointerLocationOr(Point fallback) {
                    return currentPointerLocationOr.apply(fallback);
                }

                @Override
                public void noteMouseMutation(Point point) {
                    noteMouseMutation.accept(point);
                }

                @Override
                public void noteMotorProgramFirstMouseMutation(MotorProgram program) {
                    noteMotorProgramFirstMouseMutation.accept(program);
                }

                @Override
                public void noteInteractionActivityNow() {
                    noteInteractionActivityNow.run();
                }

                @Override
                public boolean isCursorNearScreenPoint(Point point, double tolerancePx) {
                    return isCursorNearScreenPoint.test(point, tolerancePx);
                }

                @Override
                public void updateMotorCursorState(Point point) {
                    updateMotorCursorState.accept(point);
                }

                @Override
                public void completeMotorProgram(MotorProgram program, String reason) {
                    completeMotorProgram.accept(program, reason);
                }

                @Override
                public String normalizedMotorOwnerName(String owner) {
                    return normalizedMotorOwnerName.apply(owner);
                }

                @Override
                public boolean isWoodcutWorldClickType(String clickType) {
                    return isWoodcutWorldClickType.test(clickType);
                }

                @Override
                public boolean isFishingWorldClickType(String clickType) {
                    return isFishingWorldClickType.test(clickType);
                }
            },
            config
        );
    }

    static MotorProgramClickEngine createMotorProgramClickEngine(
        BooleanSupplier tryConsumeMouseMutationBudget,
        Supplier<Robot> getOrCreateRobot,
        BiConsumer<MotorProgram, String> failMotorProgram,
        BiConsumer<MotorProgram, String> completeMotorProgram,
        Consumer<String> noteInteractionClickSuccess,
        MotorProgramClickEngine.Config config
    ) {
        return new MotorProgramClickEngine(
            new MotorProgramClickEngine.Host() {
                @Override
                public boolean tryConsumeMouseMutationBudget() {
                    return tryConsumeMouseMutationBudget.getAsBoolean();
                }

                @Override
                public Robot getOrCreateRobot() {
                    return getOrCreateRobot.get();
                }

                @Override
                public void failMotorProgram(MotorProgram program, String reason) {
                    failMotorProgram.accept(program, reason);
                }

                @Override
                public void completeMotorProgram(MotorProgram program, String reason) {
                    completeMotorProgram.accept(program, reason);
                }

                @Override
                public void noteInteractionClickSuccess(String clickType) {
                    noteInteractionClickSuccess.accept(clickType);
                }
            },
            config
        );
    }
}
