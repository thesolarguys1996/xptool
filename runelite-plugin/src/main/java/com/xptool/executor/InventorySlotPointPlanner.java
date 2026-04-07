package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;
import net.runelite.api.widgets.Widget;

final class InventorySlotPointPlanner {
    private static final int DROP_SEQUENCE_POINTS_PER_SLOT = 3;
    private static final int DROP_SEQUENCE_BUILD_ATTEMPTS_PER_POINT = 12;
    private static final int DROP_SEQUENCE_FALLBACK_ATTEMPTS = 28;
    private static final double DROP_SEQUENCE_MIN_INTER_CLICK_DISTANCE_PX = 5.0;

    interface Host {
        Widget bankInventoryWidget();

        Widget inventoryWidget();

        boolean isBankOpen();

        Optional<Point> slotCenter(Widget container, int slot);

        Widget resolveInventorySlotWidget(int slot);

        Optional<Rectangle> slotBoundsByGrid(Widget container, int slot);

        boolean isUsableCanvasPoint(Point point);

        Point clampPointToRectangle(Point point, Rectangle bounds);
    }

    static final class Config {
        final int dropSlotTargetJitterMinPx;
        final int dropSlotTargetJitterMaxPx;
        final int dropSlotRepeatRetryAttempts;

        Config(
            int dropSlotTargetJitterMinPx,
            int dropSlotTargetJitterMaxPx,
            int dropSlotRepeatRetryAttempts
        ) {
            this.dropSlotTargetJitterMinPx = dropSlotTargetJitterMinPx;
            this.dropSlotTargetJitterMaxPx = Math.max(dropSlotTargetJitterMinPx, dropSlotTargetJitterMaxPx);
            this.dropSlotRepeatRetryAttempts = Math.max(0, dropSlotRepeatRetryAttempts);
        }
    }

    private final Host host;
    private final DropPointSequencePlanner dropPointSequencePlanner;

    InventorySlotPointPlanner(
        Host host,
        TargetPointVariationEngine targetPointVariationEngine,
        Config config
    ) {
        this.host = host;
        this.dropPointSequencePlanner = new DropPointSequencePlanner(
            new DropPointSequencePlanner.Host() {
                @Override
                public boolean isUsableCanvasPoint(Point point) {
                    return InventorySlotPointPlanner.this.host.isUsableCanvasPoint(point);
                }

                @Override
                public Point clampPointToRectangle(Point point, Rectangle bounds) {
                    return InventorySlotPointPlanner.this.host.clampPointToRectangle(point, bounds);
                }
            },
            targetPointVariationEngine,
            new DropPointSequencePlanner.Config(
                config.dropSlotTargetJitterMinPx,
                config.dropSlotTargetJitterMaxPx,
                config.dropSlotRepeatRetryAttempts,
                DROP_SEQUENCE_POINTS_PER_SLOT,
                DROP_SEQUENCE_BUILD_ATTEMPTS_PER_POINT,
                DROP_SEQUENCE_FALLBACK_ATTEMPTS,
                DROP_SEQUENCE_MIN_INTER_CLICK_DISTANCE_PX
            )
        );
    }

    Optional<Point> resolveInventorySlotPoint(
        int slot,
        boolean dropSweepSessionActive,
        long dropSweepSessionSerial,
        Point lastInteractionClickCanvasPoint
    ) {
        Optional<Point> basePoint = resolveInventorySlotBasePoint(slot);
        if (basePoint.isEmpty()) {
            return Optional.empty();
        }
        if (!dropSweepSessionActive) {
            return basePoint;
        }
        return resolveDropSweepSlotPoint(slot, basePoint.get(), dropSweepSessionSerial, lastInteractionClickCanvasPoint);
    }

    void beginDropSweepSession(long dropSweepSessionSerial) {
        dropPointSequencePlanner.beginSession(dropSweepSessionSerial);
    }

    void endDropSweepSession() {
        dropPointSequencePlanner.endSession();
    }

    void noteDropPointDispatched(long dropSweepSessionSerial, Point canvasPoint) {
        dropPointSequencePlanner.notePointDispatched(dropSweepSessionSerial, canvasPoint);
    }

    void resetDropPointHistory() {
        dropPointSequencePlanner.resetUsedPointHistory();
    }

    int consumeDropRepeatBlockedCount() {
        return dropPointSequencePlanner.consumeDropRepeatBlockedCount();
    }

    private Optional<Point> resolveInventorySlotBasePoint(int slot) {
        Widget bankInv = host.bankInventoryWidget();
        if (host.isBankOpen() && bankInv != null && !bankInv.isHidden()) {
            Optional<Point> p = host.slotCenter(bankInv, slot);
            if (p.isPresent()) {
                return p;
            }
        }
        Widget inv = host.inventoryWidget();
        if (inv == null || inv.isHidden()) {
            return Optional.empty();
        }
        return host.slotCenter(inv, slot);
    }

    private Optional<Point> resolveDropSweepSlotPoint(
        int slot,
        Point baseCanvasPoint,
        long dropSweepSessionSerial,
        Point lastInteractionClickCanvasPoint
    ) {
        if (slot < 0 || slot >= 28 || !host.isUsableCanvasPoint(baseCanvasPoint)) {
            return Optional.empty();
        }
        Point previousClickCanvasPoint = host.isUsableCanvasPoint(lastInteractionClickCanvasPoint)
            ? new Point(lastInteractionClickCanvasPoint)
            : null;
        Rectangle slotBounds = resolveInventorySlotBounds(slot);
        Point anchor = slotBounds == null ? new Point(baseCanvasPoint) : host.clampPointToRectangle(baseCanvasPoint, slotBounds);
        Optional<Point> planned = dropPointSequencePlanner.nextPoint(
            dropSweepSessionSerial,
            slot,
            anchor,
            slotBounds,
            previousClickCanvasPoint
        );
        return planned.filter(host::isUsableCanvasPoint).map(Point::new);
    }

    private Rectangle resolveInventorySlotBounds(int slot) {
        Widget slotWidget = host.resolveInventorySlotWidget(slot);
        if (slotWidget != null && slotWidget.getBounds() != null) {
            Rectangle bounds = slotWidget.getBounds();
            if (bounds.width > 0 && bounds.height > 0) {
                return new Rectangle(bounds);
            }
        }
        Widget container = null;
        Widget bankInv = host.bankInventoryWidget();
        if (host.isBankOpen() && bankInv != null && !bankInv.isHidden()) {
            container = bankInv;
        }
        if (container == null) {
            Widget inv = host.inventoryWidget();
            if (inv != null && !inv.isHidden()) {
                container = inv;
            }
        }
        if (container == null) {
            return null;
        }
        Optional<Rectangle> gridBounds = host.slotBoundsByGrid(container, slot);
        return gridBounds.map(Rectangle::new).orElse(null);
    }

}
