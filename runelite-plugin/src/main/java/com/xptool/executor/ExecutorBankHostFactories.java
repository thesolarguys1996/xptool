package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.awt.Robot;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;

final class ExecutorBankHostFactories {
    private ExecutorBankHostFactories() {
    }

    static Supplier<BankCommandService.Host> createBankCommandHostSupplier(
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BooleanSupplier isBankOpen,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        Function<JsonObject, Optional<TileObject>> resolveOpenBankTarget,
        Function<TileObject, Point> resolveBankObjectClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        BiConsumer<TileObject, Point> rememberInteractionAnchorForTileObject,
        ExecutorBankDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        BiFunction<ClickMotionSettings, TileObject, MotorProfile> buildBankMoveAndClickProfile,
        Runnable incrementClicksDispatched,
        IntFunction<Optional<Integer>> findVisibleBankItemSlot,
        IntFunction<Optional<Integer>> findBankItemSlot,
        IntFunction<Widget> resolveBankItemSlotWidget,
        Supplier<Client> clientSupplier,
        ExecutorBankDomainWiring.SlotCenterResolver slotCenter,
        ExecutorBankDomainWiring.PrepareBankWidgetHover prepareBankWidgetHover,
        ExecutorBankDomainWiring.WidgetOpChooser chooseWidgetOpByKeywordPriority,
        BooleanSupplier tryConsumeWorkBudget,
        BooleanSupplier humanizedBankWidgetActionsEnabled,
        LongSupplier bankMotorReadyWaitMaxMs,
        LongPredicate waitForMotorActionReady,
        ExecutorBankDomainWiring.HumanizedWidgetAction tryHumanizedBankWidgetAction,
        Predicate<String> typeWithdrawQuantity,
        Function<Widget, String> summarizeWidgetActions,
        IntFunction<Optional<Integer>> findInventorySlot,
        IntFunction<Optional<Point>> resolveInventorySlotPoint,
        IntFunction<Widget> resolveInventorySlotWidget,
        Function<JsonElement, Set<Integer>> parseExcludeItemIds,
        Function<Set<Integer>, Optional<Integer>> findFirstInventoryItemNotIn,
        BiConsumer<JsonObject, JsonObject> copyMotionFields,
        Supplier<Robot> getOrCreateRobot,
        LongConsumer sleepQuietly,
        LongBinaryOperator randomBetween,
        LongSupplier bankSearchKeyMinDelayMs,
        LongSupplier bankSearchKeyMaxDelayMs,
        ExecutorBankDomainWiring.RobotCharTyper typeBankSearchChar,
        BooleanSupplier isBankPinPromptVisible,
        Function<Widget, Optional<Point>> centerOfWidget,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint,
        Supplier<ClickMotionSettings> genericInteractClickSettings
    ) {
        return () -> ExecutorBankDomainWiring.createBankCommandHost(
            acceptDecision,
            rejectDecision,
            details,
            safeString,
            isBankOpen,
            resolveClickMotion,
            resolveOpenBankTarget,
            resolveBankObjectClickPoint,
            isUsableCanvasPoint,
            rememberInteractionAnchorForTileObject,
            scheduleMotorGesture,
            buildBankMoveAndClickProfile,
            incrementClicksDispatched,
            findVisibleBankItemSlot,
            findBankItemSlot,
            resolveBankItemSlotWidget,
            clientSupplier,
            slotCenter,
            prepareBankWidgetHover,
            chooseWidgetOpByKeywordPriority,
            tryConsumeWorkBudget,
            humanizedBankWidgetActionsEnabled,
            bankMotorReadyWaitMaxMs,
            waitForMotorActionReady,
            tryHumanizedBankWidgetAction,
            typeWithdrawQuantity,
            summarizeWidgetActions,
            findInventorySlot,
            resolveInventorySlotPoint,
            resolveInventorySlotWidget,
            parseExcludeItemIds,
            findFirstInventoryItemNotIn,
            copyMotionFields,
            getOrCreateRobot,
            sleepQuietly,
            randomBetween,
            bankSearchKeyMinDelayMs,
            bankSearchKeyMaxDelayMs,
            typeBankSearchChar,
            isBankPinPromptVisible,
            centerOfWidget,
            clickCanvasPoint,
            genericInteractClickSettings
        );
    }
}
