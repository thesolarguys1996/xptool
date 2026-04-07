package com.xptool.executor;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.Optional;
import net.runelite.api.widgets.Widget;

final class InventorySlotInteractionController {
    interface Host {
        Widget bankInventoryWidget();

        Widget inventoryWidget();

        Widget bankMainItemsWidget();

        boolean isBankOpen();

        boolean canPerformMotorActionNow();

        Robot getOrCreateRobot();

        Optional<Point> resolveInventorySlotPoint(int slot);

        Optional<Point> toScreenPoint(Point canvasPoint);

        Point varyInventorySlotPoint(int slot, Point basePoint, Rectangle slotBounds);

        void noteInteractionClickSuccess();
    }

    static final class Config {
        final int defaultCursorMoveDurationMs;
        final int quickMoveDurationThresholdMs;
        final long clickSettleMs;
        final long clickDownMs;
        final int microSettleBeforeClickMs;
        final int microButtonDownMs;

        Config(
            int defaultCursorMoveDurationMs,
            int quickMoveDurationThresholdMs,
            long clickSettleMs,
            long clickDownMs,
            int microSettleBeforeClickMs,
            int microButtonDownMs
        ) {
            this.defaultCursorMoveDurationMs = Math.max(0, defaultCursorMoveDurationMs);
            this.quickMoveDurationThresholdMs = Math.max(0, quickMoveDurationThresholdMs);
            this.clickSettleMs = Math.max(0L, clickSettleMs);
            this.clickDownMs = Math.max(0L, clickDownMs);
            this.microSettleBeforeClickMs = Math.max(0, microSettleBeforeClickMs);
            this.microButtonDownMs = Math.max(0, microButtonDownMs);
        }
    }

    private final Host host;
    private final Config config;

    InventorySlotInteractionController(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    boolean clickInventorySlot(int slot) {
        if (!host.canPerformMotorActionNow()) {
            return false;
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        Point slotScreenTarget = moveCursorToInventorySlot(slot, robot);
        if (slotScreenTarget == null) {
            return false;
        }
        boolean clicked = ExecutorMenuClickSupport.clickAt(
            robot,
            slotScreenTarget,
            config.clickSettleMs,
            config.clickDownMs,
            1.2,
            config.microSettleBeforeClickMs,
            config.microButtonDownMs
        );
        if (clicked) {
            host.noteInteractionClickSuccess();
        }
        return clicked;
    }

    Point moveCursorToInventorySlot(int slot, Robot robot) {
        if (robot == null) {
            return null;
        }
        Optional<Point> slotCanvas = Optional.empty();
        Widget slotWidget = resolveInventorySlotWidget(slot);
        if (slotWidget != null) {
            slotCanvas = centerOfWidget(slotWidget);
        }
        if (slotCanvas.isEmpty()) {
            slotCanvas = host.resolveInventorySlotPoint(slot);
        }
        if (slotCanvas.isEmpty()) {
            return null;
        }
        Rectangle slotBounds = slotWidget == null ? null : slotWidget.getBounds();
        Point slotTarget = host.varyInventorySlotPoint(slot, slotCanvas.get(), slotBounds);
        Optional<Point> slotScreen = host.toScreenPoint(slotTarget);
        if (slotScreen.isEmpty()) {
            return null;
        }
        PointerInfo pointer = MouseInfo.getPointerInfo();
        Point from = pointer == null ? slotScreen.get() : pointer.getLocation();
        Point to = slotScreen.get();
        int moveDurationMs = ExecutorCursorMotion.computeMoveDurationMs(from, to, config.defaultCursorMoveDurationMs);
        boolean humanizedTiming = moveDurationMs > config.quickMoveDurationThresholdMs;
        // Use low-blocking humanized movement so eat clicks stay responsive while preserving curved/S-like motion.
        ExecutorCursorMotion.moveMouseCurveIdle(robot, from, to, humanizedTiming);
        return to;
    }

    Widget resolveInventorySlotWidget(int slot) {
        Widget bankInv = host.bankInventoryWidget();
        if (host.isBankOpen()) {
            Widget bankSlot = resolveContainerSlotWidget(bankInv, slot);
            if (bankSlot != null) {
                return bankSlot;
            }
        }
        return resolveContainerSlotWidget(host.inventoryWidget(), slot);
    }

    Widget resolveBankItemSlotWidget(int slot) {
        return resolveContainerSlotWidget(host.bankMainItemsWidget(), slot);
    }

    Optional<Point> slotCenter(Widget container, int slot) {
        if (container == null || container.isHidden() || slot < 0) {
            return Optional.empty();
        }

        Widget[] dynamicChildren = container.getDynamicChildren();
        if (dynamicChildren != null && slot < dynamicChildren.length) {
            Optional<Point> p = centerOfWidget(dynamicChildren[slot]);
            if (p.isPresent()) {
                return p;
            }
        }

        Widget[] children = container.getChildren();
        if (children != null && slot < children.length) {
            Optional<Point> p = centerOfWidget(children[slot]);
            if (p.isPresent()) {
                return p;
            }
        }
        return slotCenterByGrid(container, slot);
    }

    Optional<Rectangle> slotBoundsByGrid(Widget container, int slot) {
        if (container == null || container.isHidden() || slot < 0 || slot >= 28) {
            return Optional.empty();
        }
        Rectangle b = container.getBounds();
        if (b == null || b.width <= 0 || b.height <= 0) {
            return Optional.empty();
        }
        int cols = 4;
        int rows = 7;
        int col = slot % cols;
        int row = slot / cols;
        if (row >= rows) {
            return Optional.empty();
        }
        double cellW = (double) b.width / (double) cols;
        double cellH = (double) b.height / (double) rows;
        int left = (int) Math.round(b.x + (col * cellW));
        int top = (int) Math.round(b.y + (row * cellH));
        int right = (int) Math.round(b.x + ((col + 1) * cellW));
        int bottom = (int) Math.round(b.y + ((row + 1) * cellH));
        int width = Math.max(1, right - left);
        int height = Math.max(1, bottom - top);
        return Optional.of(new Rectangle(left, top, width, height));
    }

    Optional<Point> centerOfWidget(Widget widget) {
        if (widget == null || widget.isHidden() || widget.getBounds() == null) {
            return Optional.empty();
        }
        Rectangle b = widget.getBounds();
        if (b.width <= 0 || b.height <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Point((int) b.getCenterX(), (int) b.getCenterY()));
    }

    private Widget resolveContainerSlotWidget(Widget container, int slot) {
        if (container == null || container.isHidden() || slot < 0) {
            return null;
        }

        Widget directChild = null;
        try {
            directChild = container.getChild(slot);
        } catch (Exception ignored) {
            // Fall back to child arrays below.
        }
        if (isUsableSlotWidget(directChild)) {
            return directChild;
        }

        Widget[] dynamicChildren = container.getDynamicChildren();
        if (dynamicChildren != null && slot < dynamicChildren.length) {
            Widget dynamicChild = dynamicChildren[slot];
            if (isUsableSlotWidget(dynamicChild)) {
                return dynamicChild;
            }
        }

        Widget[] children = container.getChildren();
        if (children != null && slot < children.length) {
            Widget child = children[slot];
            if (isUsableSlotWidget(child)) {
                return child;
            }
        }
        return null;
    }

    private Optional<Point> slotCenterByGrid(Widget container, int slot) {
        if (container == null || container.isHidden() || slot < 0 || slot >= 28) {
            return Optional.empty();
        }
        Rectangle b = container.getBounds();
        if (b == null || b.width <= 0 || b.height <= 0) {
            return Optional.empty();
        }
        int cols = 4;
        int rows = 7;
        int col = slot % cols;
        int row = slot / cols;
        if (row >= rows) {
            return Optional.empty();
        }
        double cellW = (double) b.width / (double) cols;
        double cellH = (double) b.height / (double) rows;
        int x = (int) Math.round(b.x + (col * cellW) + (cellW / 2.0));
        int y = (int) Math.round(b.y + (row * cellH) + (cellH / 2.0));
        return Optional.of(new Point(x, y));
    }

    private static boolean isUsableSlotWidget(Widget widget) {
        return widget != null && !widget.isHidden() && widget.getId() > 0;
    }
}
