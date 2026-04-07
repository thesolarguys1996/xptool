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

final class ExecutorBankDomainWiring {
    @FunctionalInterface
    interface MotorGestureScheduler {
        MotorHandle schedule(CanvasPoint point, MotorGestureType type, MotorProfile profile);
    }

    @FunctionalInterface
    interface PrepareBankWidgetHover {
        Optional<CommandExecutor.CommandDecision> prepare(
            boolean inventorySlot,
            int slot,
            Point targetPoint,
            MotionProfile motionProfile,
            String reasonPrefix
        );
    }

    @FunctionalInterface
    interface WidgetOpChooser {
        int choose(Widget widget, String... preferredKeywords);
    }

    @FunctionalInterface
    interface HumanizedWidgetAction {
        boolean perform(Point targetPoint, String... optionKeywords);
    }

    @FunctionalInterface
    interface SlotCenterResolver {
        Optional<Point> resolve(Widget container, int slot);
    }

    @FunctionalInterface
    interface RobotCharTyper {
        void type(Robot robot, char ch);
    }

    private ExecutorBankDomainWiring() {
    }

    static BankCommandService.Host createBankCommandHost(
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
        MotorGestureScheduler scheduleMotorGesture,
        BiFunction<ClickMotionSettings, TileObject, MotorProfile> buildBankMoveAndClickProfile,
        Runnable incrementClicksDispatched,
        IntFunction<Optional<Integer>> findVisibleBankItemSlot,
        IntFunction<Optional<Integer>> findBankItemSlot,
        IntFunction<Widget> resolveBankItemSlotWidget,
        Supplier<Client> clientSupplier,
        SlotCenterResolver slotCenter,
        PrepareBankWidgetHover prepareBankWidgetHover,
        WidgetOpChooser chooseWidgetOpByKeywordPriority,
        BooleanSupplier tryConsumeWorkBudget,
        BooleanSupplier humanizedBankWidgetActionsEnabled,
        LongSupplier bankMotorReadyWaitMaxMs,
        LongPredicate waitForMotorActionReady,
        HumanizedWidgetAction tryHumanizedBankWidgetAction,
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
        RobotCharTyper typeBankSearchChar,
        BooleanSupplier isBankPinPromptVisible,
        Function<Widget, Optional<Point>> centerOfWidget,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint,
        Supplier<ClickMotionSettings> genericInteractClickSettings
    ) {
        return new BankCommandService.Host() {
            @Override
            public CommandExecutor.CommandDecision accept(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public CommandExecutor.CommandDecision reject(String reason) {
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
            public boolean isBankOpen() {
                return isBankOpen.getAsBoolean();
            }

            @Override
            public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
                return resolveClickMotion.apply(payload, motionProfile);
            }

            @Override
            public Optional<TileObject> resolveOpenBankTarget(JsonObject payload) {
                return resolveOpenBankTarget.apply(payload);
            }

            @Override
            public Point resolveBankObjectClickPoint(TileObject targetObject) {
                return resolveBankObjectClickPoint.apply(targetObject);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public void rememberInteractionAnchorForTileObject(TileObject targetObject, Point point) {
                rememberInteractionAnchorForTileObject.accept(targetObject, point);
            }

            @Override
            public MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile) {
                return scheduleMotorGesture.schedule(point, type, profile);
            }

            @Override
            public MotorProfile buildBankMoveAndClickProfile(ClickMotionSettings motion, TileObject targetObject) {
                return buildBankMoveAndClickProfile.apply(motion, targetObject);
            }

            @Override
            public void incrementClicksDispatched() {
                incrementClicksDispatched.run();
            }

            @Override
            public Optional<Integer> findVisibleBankItemSlot(int itemId) {
                return findVisibleBankItemSlot.apply(itemId);
            }

            @Override
            public Optional<Integer> findBankItemSlot(int itemId) {
                return findBankItemSlot.apply(itemId);
            }

            @Override
            public Widget resolveBankItemSlotWidget(int slot) {
                return resolveBankItemSlotWidget.apply(slot);
            }

            @Override
            public Client client() {
                return clientSupplier.get();
            }

            @Override
            public Optional<Point> slotCenter(Widget container, int slot) {
                return slotCenter.resolve(container, slot);
            }

            @Override
            public Optional<CommandExecutor.CommandDecision> prepareBankWidgetHover(
                boolean inventorySlot,
                int slot,
                Point targetPoint,
                MotionProfile motionProfile,
                String reasonPrefix
            ) {
                return prepareBankWidgetHover.prepare(inventorySlot, slot, targetPoint, motionProfile, reasonPrefix);
            }

            @Override
            public int chooseWidgetOpByKeywordPriority(Widget widget, String... preferredKeywords) {
                return chooseWidgetOpByKeywordPriority.choose(widget, preferredKeywords);
            }

            @Override
            public boolean tryConsumeWorkBudget() {
                return tryConsumeWorkBudget.getAsBoolean();
            }

            @Override
            public boolean humanizedBankWidgetActionsEnabled() {
                return humanizedBankWidgetActionsEnabled.getAsBoolean();
            }

            @Override
            public long bankMotorReadyWaitMaxMs() {
                return bankMotorReadyWaitMaxMs.getAsLong();
            }

            @Override
            public boolean waitForMotorActionReady(long maxWaitMs) {
                return waitForMotorActionReady.test(maxWaitMs);
            }

            @Override
            public boolean tryHumanizedBankWidgetAction(Point targetPoint, String... optionKeywords) {
                return tryHumanizedBankWidgetAction.perform(targetPoint, optionKeywords);
            }

            @Override
            public boolean typeWithdrawQuantity(String quantityRaw) {
                return typeWithdrawQuantity.test(quantityRaw);
            }

            @Override
            public String summarizeWidgetActions(Widget widget) {
                return summarizeWidgetActions.apply(widget);
            }

            @Override
            public Optional<Integer> findInventorySlot(int itemId) {
                return findInventorySlot.apply(itemId);
            }

            @Override
            public Optional<Point> resolveInventorySlotPoint(int slot) {
                return resolveInventorySlotPoint.apply(slot);
            }

            @Override
            public Widget resolveInventorySlotWidget(int slot) {
                return resolveInventorySlotWidget.apply(slot);
            }

            @Override
            public Set<Integer> parseExcludeItemIds(JsonElement element) {
                return parseExcludeItemIds.apply(element);
            }

            @Override
            public Optional<Integer> findFirstInventoryItemNotIn(Set<Integer> excludeItemIds) {
                return findFirstInventoryItemNotIn.apply(excludeItemIds);
            }

            @Override
            public void copyMotionFields(JsonObject source, JsonObject destination) {
                copyMotionFields.accept(source, destination);
            }

            @Override
            public Robot getOrCreateRobot() {
                return getOrCreateRobot.get();
            }

            @Override
            public void sleepQuietly(long ms) {
                sleepQuietly.accept(ms);
            }

            @Override
            public long randomBetween(long minInclusive, long maxInclusive) {
                return randomBetween.applyAsLong(minInclusive, maxInclusive);
            }

            @Override
            public long bankSearchKeyMinDelayMs() {
                return bankSearchKeyMinDelayMs.getAsLong();
            }

            @Override
            public long bankSearchKeyMaxDelayMs() {
                return bankSearchKeyMaxDelayMs.getAsLong();
            }

            @Override
            public void typeBankSearchChar(Robot robot, char ch) {
                typeBankSearchChar.type(robot, ch);
            }

            @Override
            public boolean isBankPinPromptVisible() {
                return isBankPinPromptVisible.getAsBoolean();
            }

            @Override
            public Optional<Point> centerOfWidget(Widget widget) {
                return centerOfWidget.apply(widget);
            }

            @Override
            public boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion) {
                return clickCanvasPoint.apply(canvasPoint, motion);
            }

            @Override
            public ClickMotionSettings genericInteractClickSettings() {
                return genericInteractClickSettings.get();
            }
        };
    }
}
