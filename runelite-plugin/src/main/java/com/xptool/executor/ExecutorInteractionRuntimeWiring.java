package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.IdleSkillContext;
import com.xptool.systems.TargetSelectionEngine;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.InterfaceID;

final class ExecutorInteractionRuntimeWiring {
    private ExecutorInteractionRuntimeWiring() {
    }

    static ExecutorInteractionRuntimeBundle createBundle(
        Client client,
        ClientScreenBoundsResolver clientScreenBoundsResolver,
        InventorySlotInteractionController.Host inventorySlotInteractionControllerHost,
        BankMenuInteractionController.Host bankMenuInteractionControllerHost,
        IdleOffscreenMoveEngine.Host idleOffscreenMoveHost,
        Supplier<Point> currentMouseCanvasPoint,
        Function<TileObject, Point> primaryTileObjectClickPoint,
        Function<TileObject, Point> fallbackTileObjectCanvasPoint,
        Predicate<Point> isUsableCanvasPoint,
        ToDoubleBiFunction<Point, Point> pixelDistance,
        IntSupplier consecutiveLocalInteractions,
        BooleanSupplier isBankOpen,
        BiFunction<Point, Rectangle, Point> clampPointToRectangle,
        BooleanSupplier canPerformMotorActionNow,
        ExecutorEngineWiring.MotorCanvasMoveExecutor motorMoveToCanvasPoint,
        Supplier<Optional<Rectangle>> resolveInventoryInteractionRegionCanvas,
        Supplier<Point> lastInteractionAnchorCenterCanvasPoint,
        Supplier<Rectangle> lastInteractionAnchorBoundsCanvas,
        BooleanSupplier isClientCanvasFocused,
        BooleanSupplier allowWindowRefocusForInteraction,
        BooleanSupplier focusClientWindowAndCanvas,
        Function<Point, Optional<Point>> toScreenPoint,
        Supplier<Robot> getOrCreateRobot,
        Consumer<Robot> clickCanvasActivationAnchor,
        Runnable noteInteractionClickSuccess,
        ExecutorEngineWiring.ScreenTolerancePredicate isCursorNearScreenPoint,
        ExecutorEngineWiring.RobotPointAction moveMouseCurve,
        ExecutorEngineWiring.RegionInsetPointResolver randomCanvasPointInRegion,
        Function<Point, Point> currentPointerLocationOr,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        ExecutorEngineWiring.PointBoundsRadiusResolver jitterWithinBounds,
        LongConsumer sleepCritical,
        LongConsumer sleepNoCooldown,
        LongConsumer sleepQuietly,
        Runnable noteMotorAction,
        BiPredicate<Boolean, Boolean> focusClientWindowAndCanvasDetailed,
        LongConsumer reserveMotorCooldown,
        Supplier<IdleCadenceTuning> activeIdleCadenceTuning,
        boolean visualCursorMotionEnabled,
        boolean humanizedTimingEnabled,
        BiConsumer<String, JsonObject> emitIdleEvent
    ) {
        TargetSelectionEngine targetSelectionEngine = ExecutorEngineWiring.createTargetSelectionEngine(
            currentMouseCanvasPoint,
            primaryTileObjectClickPoint,
            fallbackTileObjectCanvasPoint,
            isUsableCanvasPoint,
            pixelDistance,
            consecutiveLocalInteractions,
            ExecutorEngineConfigCatalog.TARGET_SELECTION_CONFIG
        );

        TargetPointVariationEngine targetPointVariationEngine =
            ExecutorEngineWiring.createTargetPointVariationEngine(
                isUsableCanvasPoint,
                client::getCanvasWidth,
                client::getCanvasHeight,
                ExecutorEngineConfigCatalog.TARGET_POINT_VARIATION_CONFIG
            );

        InventorySlotInteractionController inventorySlotInteractionController =
            new InventorySlotInteractionController(
                inventorySlotInteractionControllerHost,
                ExecutorEngineConfigCatalog.INVENTORY_SLOT_INTERACTION_CONFIG
            );

        InventorySlotPointPlanner inventorySlotPointPlanner = ExecutorEngineWiring.createInventorySlotPointPlanner(
            () -> client.getWidget(InterfaceID.Bankside.ITEMS),
            () -> client.getWidget(InterfaceID.Inventory.ITEMS),
            isBankOpen,
            inventorySlotInteractionController::slotCenter,
            inventorySlotInteractionController::resolveInventorySlotWidget,
            inventorySlotInteractionController::slotBoundsByGrid,
            isUsableCanvasPoint,
            clampPointToRectangle,
            targetPointVariationEngine,
            ExecutorEngineConfigCatalog.INVENTORY_SLOT_POINT_PLANNER_CONFIG
        );

        FocusMenuInteractionController focusMenuInteractionController =
            ExecutorEngineWiring.createFocusMenuInteractionController(
                client::getCanvas,
                client::isMenuOpen,
                getOrCreateRobot,
                canPerformMotorActionNow,
                jitterWithinBounds,
                moveMouseCurve,
                sleepCritical,
                sleepNoCooldown,
                sleepQuietly,
                noteMotorAction,
                ExecutorEngineConfigCatalog.FOCUS_MENU_INTERACTION_CONFIG
            );

        InteractionClickEngine interactionClickEngine = ExecutorEngineWiring.createInteractionClickEngine(
            isUsableCanvasPoint,
            canPerformMotorActionNow,
            motorMoveToCanvasPoint,
            currentMouseCanvasPoint,
            clampPointToRectangle,
            resolveInventoryInteractionRegionCanvas,
            lastInteractionAnchorCenterCanvasPoint,
            lastInteractionAnchorBoundsCanvas,
            client::getCanvasWidth,
            client::getCanvasHeight,
            pixelDistance,
            isClientCanvasFocused,
            allowWindowRefocusForInteraction,
            focusClientWindowAndCanvas,
            toScreenPoint,
            getOrCreateRobot,
            clickCanvasActivationAnchor,
            noteInteractionClickSuccess,
            isCursorNearScreenPoint,
            moveMouseCurve,
            sleepCritical,
            sleepNoCooldown,
            ExecutorEngineConfigCatalog.createInteractionClickEngineConfig(
                visualCursorMotionEnabled,
                humanizedTimingEnabled
            )
        );

        BankMenuInteractionController bankMenuInteractionController = new BankMenuInteractionController(
            bankMenuInteractionControllerHost,
            ExecutorEngineConfigCatalog.BANK_MENU_INTERACTION_CONFIG
        );

        IdleCursorTargetPlanner idleCursorTargetPlanner = ExecutorEngineWiring.createIdleCursorTargetPlanner(
            resolveInventoryInteractionRegionCanvas,
            randomCanvasPointInRegion,
            isUsableCanvasPoint,
            client::getCanvasWidth,
            client::getCanvasHeight,
            currentMouseCanvasPoint,
            clientScreenBoundsResolver::resolveClientCanvasBoundsScreen,
            clientScreenBoundsResolver::resolveClientWindowBoundsScreen,
            clientScreenBoundsResolver::resolveScreenBoundsForPoint,
            currentPointerLocationOr,
            resolveIdleSkillContext,
            activeIdleCadenceTuning,
            emitIdleEvent
        );

        IdleOffscreenMoveEngine idleOffscreenMoveEngine = new IdleOffscreenMoveEngine(
            idleOffscreenMoveHost,
            ExecutorEngineConfigCatalog.createIdleOffscreenMoveConfig(humanizedTimingEnabled)
        );

        CameraMotionService cameraMotionService = ExecutorEngineWiring.createCameraMotionService(
            getOrCreateRobot,
            isClientCanvasFocused,
            focusClientWindowAndCanvasDetailed,
            sleepNoCooldown,
            reserveMotorCooldown,
            noteMotorAction,
            client::getCameraYaw,
            client::getCameraPitch
        );

        return new ExecutorInteractionRuntimeBundle(
            targetSelectionEngine,
            targetPointVariationEngine,
            inventorySlotInteractionController,
            inventorySlotPointPlanner,
            focusMenuInteractionController,
            interactionClickEngine,
            bankMenuInteractionController,
            idleCursorTargetPlanner,
            idleOffscreenMoveEngine,
            cameraMotionService
        );
    }
}
