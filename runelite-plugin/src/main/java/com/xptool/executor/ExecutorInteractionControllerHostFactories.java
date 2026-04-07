package com.xptool.executor;

import com.xptool.motion.MotionProfile;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.Supplier;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.Widget;

final class ExecutorInteractionControllerHostFactories {
    @FunctionalInterface
    interface SlotPointVariation {
        Point vary(int slot, Point basePoint, Rectangle slotBounds);
    }

    @FunctionalInterface
    interface ClickCanvasPointNoRefocus {
        boolean execute(Point canvasPoint, int clickSettleMs, int clickDownMs);
    }

    @FunctionalInterface
    interface ContextMenuOptionEmitter {
        void emit(String option, String target, String matchedKeyword, int row, int menuX, int menuY);
    }

    private ExecutorInteractionControllerHostFactories() {
    }

    static InventorySlotInteractionController.Host createInventorySlotInteractionControllerHost(
        Supplier<Widget> banksideItemsWidget,
        Supplier<Widget> inventoryItemsWidget,
        Supplier<Widget> bankmainItemsWidget,
        BooleanSupplier isBankOpen,
        BooleanSupplier canPerformMotorActionNow,
        Supplier<Robot> getOrCreateRobot,
        IntFunction<Optional<Point>> resolveInventorySlotPoint,
        Function<Point, Optional<Point>> toScreenPoint,
        SlotPointVariation varyInventorySlotPoint,
        Runnable noteInteractionClickSuccess
    ) {
        return new InventorySlotInteractionController.Host() {
            @Override
            public Widget bankInventoryWidget() {
                return banksideItemsWidget.get();
            }

            @Override
            public Widget inventoryWidget() {
                return inventoryItemsWidget.get();
            }

            @Override
            public Widget bankMainItemsWidget() {
                return bankmainItemsWidget.get();
            }

            @Override
            public boolean isBankOpen() {
                return isBankOpen.getAsBoolean();
            }

            @Override
            public boolean canPerformMotorActionNow() {
                return canPerformMotorActionNow.getAsBoolean();
            }

            @Override
            public Robot getOrCreateRobot() {
                return getOrCreateRobot.get();
            }

            @Override
            public Optional<Point> resolveInventorySlotPoint(int slot) {
                return resolveInventorySlotPoint.apply(slot);
            }

            @Override
            public Optional<Point> toScreenPoint(Point canvasPoint) {
                return toScreenPoint.apply(canvasPoint);
            }

            @Override
            public Point varyInventorySlotPoint(int slot, Point basePoint, Rectangle slotBounds) {
                return varyInventorySlotPoint.vary(slot, basePoint, slotBounds);
            }

            @Override
            public void noteInteractionClickSuccess() {
                noteInteractionClickSuccess.run();
            }
        };
    }

    static BankMenuInteractionController.Host createBankMenuInteractionControllerHost(
        BiFunction<Point, MotionProfile.ClickMotionSettings, Boolean> rightClickCanvasPointBank,
        LongPredicate waitForMenuOpen,
        Supplier<MenuEntry[]> menuEntries,
        IntSupplier menuX,
        IntSupplier menuY,
        IntSupplier menuWidth,
        ClickCanvasPointNoRefocus clickCanvasPointNoRefocus,
        LongConsumer sleepCritical,
        BooleanSupplier isMenuOpen,
        Supplier<MotionProfile.ClickMotionSettings> menuInteractionClickSettings,
        ContextMenuOptionEmitter emitContextMenuSelection
    ) {
        return new BankMenuInteractionController.Host() {
            @Override
            public boolean rightClickCanvasPointBank(Point canvasPoint, MotionProfile.ClickMotionSettings motion) {
                return rightClickCanvasPointBank.apply(canvasPoint, motion);
            }

            @Override
            public boolean waitForMenuOpen(long timeoutMs) {
                return waitForMenuOpen.test(timeoutMs);
            }

            @Override
            public MenuEntry[] menuEntries() {
                return menuEntries.get();
            }

            @Override
            public int menuX() {
                return menuX.getAsInt();
            }

            @Override
            public int menuY() {
                return menuY.getAsInt();
            }

            @Override
            public int menuWidth() {
                return menuWidth.getAsInt();
            }

            @Override
            public boolean clickCanvasPointNoRefocus(Point canvasPoint, int clickSettleMs, int clickDownMs) {
                return clickCanvasPointNoRefocus.execute(canvasPoint, clickSettleMs, clickDownMs);
            }

            @Override
            public void sleepCritical(long ms) {
                sleepCritical.accept(ms);
            }

            @Override
            public boolean isMenuOpen() {
                return isMenuOpen.getAsBoolean();
            }

            @Override
            public MotionProfile.ClickMotionSettings defaultMenuInteractionMotion() {
                return menuInteractionClickSettings.get();
            }

            @Override
            public void emitContextMenuOptionClicked(
                String option,
                String target,
                String matchedKeyword,
                int row,
                int menuXValue,
                int menuYValue
            ) {
                emitContextMenuSelection.emit(option, target, matchedKeyword, row, menuXValue, menuYValue);
            }
        };
    }
}
