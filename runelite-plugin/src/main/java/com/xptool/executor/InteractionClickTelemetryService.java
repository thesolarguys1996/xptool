package com.xptool.executor;

import com.google.gson.JsonObject;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;

final class InteractionClickTelemetryService {
    interface Host {
        boolean isUsableCanvasPoint(Point point);

        Point currentMouseCanvasPoint();

        int canvasWidth();

        int canvasHeight();

        String normalizedMotorOwnerName(String owner);

        String activeMotorOwnerContext();

        boolean isSettleEligibleClickType(String clickType);

        boolean targetVariationEnabled();

        int currentExecutorTick();

        long motorActionSerial();

        void noteInteractionActivityNow();

        void noteMotorAction();

        void emitInteractionClickTelemetry(JsonObject telemetry);

        void onSettleEligibleInteractionClick(InteractionClickEvent clickEvent);
    }

    private static final String SESSION_DROP_SWEEP = ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP;

    private final Host host;
    private final boolean clickTelemetryEnabled;

    private long interactionClickSerial = 0L;
    private long lastInteractionClickAtMs = 0L;
    private Point lastInteractionClickCanvasPoint = null;
    private Point lastInteractionAnchorCenterCanvasPoint = null;
    private Rectangle lastInteractionAnchorBoundsCanvas = null;

    InteractionClickTelemetryService(Host host, boolean clickTelemetryEnabled) {
        this.host = host;
        this.clickTelemetryEnabled = clickTelemetryEnabled;
    }

    void noteInteractionClickSuccess(String clickType) {
        host.noteInteractionActivityNow();
        host.noteMotorAction();
        Point canvasPoint = host.currentMouseCanvasPoint();
        if (!host.isUsableCanvasPoint(canvasPoint)) {
            return;
        }
        Point previousClickCanvasPoint = host.isUsableCanvasPoint(lastInteractionClickCanvasPoint)
            ? new Point(lastInteractionClickCanvasPoint)
            : null;
        interactionClickSerial++;
        long clickAtMs = System.currentTimeMillis();
        lastInteractionClickAtMs = clickAtMs;
        lastInteractionClickCanvasPoint = new Point(canvasPoint);
        if (!host.isUsableCanvasPoint(lastInteractionAnchorCenterCanvasPoint)) {
            lastInteractionAnchorCenterCanvasPoint = new Point(canvasPoint);
        }
        if (lastInteractionAnchorBoundsCanvas == null || !lastInteractionAnchorBoundsCanvas.contains(canvasPoint)) {
            Rectangle fallbackBounds = new Rectangle(canvasPoint.x - 3, canvasPoint.y - 3, 7, 7);
            Rectangle canvasBounds = canvasBounds();
            Rectangle bounded = fallbackBounds.intersection(canvasBounds);
            if (bounded.width > 0 && bounded.height > 0) {
                lastInteractionAnchorBoundsCanvas = bounded;
            }
        }
        emitInteractionClickTelemetry(clickType, canvasPoint, previousClickCanvasPoint, clickAtMs);
        if (!host.isSettleEligibleClickType(clickType)) {
            return;
        }
        host.onSettleEligibleInteractionClick(
            new InteractionClickEvent(
                interactionClickSerial,
                host.currentExecutorTick(),
                clickAtMs,
                host.normalizedMotorOwnerName(host.activeMotorOwnerContext()),
                safeString(clickType),
                new Point(lastInteractionClickCanvasPoint),
                host.isUsableCanvasPoint(lastInteractionAnchorCenterCanvasPoint)
                    ? new Point(lastInteractionAnchorCenterCanvasPoint)
                    : null,
                lastInteractionAnchorBoundsCanvas == null ? null : new Rectangle(lastInteractionAnchorBoundsCanvas),
                host.motorActionSerial()
            )
        );
    }

    void rememberInteractionAnchor(Point anchorCenterCanvasPoint, Rectangle anchorBoundsCanvas) {
        Rectangle canvasBounds = canvasBounds();
        if (anchorBoundsCanvas != null) {
            Rectangle bounded = anchorBoundsCanvas.intersection(canvasBounds);
            if (bounded.width > 0 && bounded.height > 0) {
                lastInteractionAnchorBoundsCanvas = bounded;
            }
        }
        Point center = anchorCenterCanvasPoint == null ? null : new Point(anchorCenterCanvasPoint);
        if (center != null && host.isUsableCanvasPoint(center)) {
            if (lastInteractionAnchorBoundsCanvas != null) {
                center = clampPointToRectangle(center, lastInteractionAnchorBoundsCanvas);
            }
            lastInteractionAnchorCenterCanvasPoint = center;
            return;
        }
        if (lastInteractionAnchorBoundsCanvas != null) {
            lastInteractionAnchorCenterCanvasPoint = new Point(
                (int) Math.round(lastInteractionAnchorBoundsCanvas.getCenterX()),
                (int) Math.round(lastInteractionAnchorBoundsCanvas.getCenterY())
            );
        }
    }

    long interactionClickSerial() {
        return interactionClickSerial;
    }

    Optional<Point> lastInteractionClickCanvasPoint() {
        if (!host.isUsableCanvasPoint(lastInteractionClickCanvasPoint)) {
            return Optional.empty();
        }
        return Optional.of(new Point(lastInteractionClickCanvasPoint));
    }

    boolean isInteractionClickFresh(long maxAgeMs) {
        if (maxAgeMs < 0L || lastInteractionClickAtMs <= 0L) {
            return false;
        }
        return (System.currentTimeMillis() - lastInteractionClickAtMs) <= maxAgeMs;
    }

    Point lastInteractionClickCanvasPointOrNull() {
        return host.isUsableCanvasPoint(lastInteractionClickCanvasPoint)
            ? new Point(lastInteractionClickCanvasPoint)
            : null;
    }

    Point lastInteractionAnchorCenterCanvasPointOrNull() {
        return host.isUsableCanvasPoint(lastInteractionAnchorCenterCanvasPoint)
            ? new Point(lastInteractionAnchorCenterCanvasPoint)
            : null;
    }

    Rectangle lastInteractionAnchorBoundsCanvasOrNull() {
        return lastInteractionAnchorBoundsCanvas == null ? null : new Rectangle(lastInteractionAnchorBoundsCanvas);
    }

    private void emitInteractionClickTelemetry(
        String clickType,
        Point clickCanvasPoint,
        Point previousClickCanvasPoint,
        long clickAtMs
    ) {
        if (!clickTelemetryEnabled || !host.isUsableCanvasPoint(clickCanvasPoint)) {
            return;
        }
        boolean repeatedExactPixel =
            previousClickCanvasPoint != null && previousClickCanvasPoint.equals(clickCanvasPoint);
        String motorOwner = host.normalizedMotorOwnerName(host.activeMotorOwnerContext());
        if (SESSION_DROP_SWEEP.equals(motorOwner)
            && (interactionClickSerial % 3L) != 0L
            && !repeatedExactPixel) {
            return;
        }
        double previousDistancePx = previousClickCanvasPoint == null
            ? -1.0
            : Math.round(pixelDistance(previousClickCanvasPoint, clickCanvasPoint) * 100.0) / 100.0;
        JsonObject telemetry = ExecutorValueParsers.details(
            "clickSerial", interactionClickSerial,
            "executorTick", host.currentExecutorTick(),
            "clickAtMs", clickAtMs,
            "clickType", safeString(clickType),
            "motorOwner", motorOwner,
            "targetVariationEnabled", host.targetVariationEnabled(),
            "canvasX", clickCanvasPoint.x,
            "canvasY", clickCanvasPoint.y,
            "repeatedExactPixelFromPrevious", repeatedExactPixel,
            "distanceFromPreviousPx", previousDistancePx
        );
        if (previousClickCanvasPoint != null) {
            telemetry.addProperty("previousCanvasX", previousClickCanvasPoint.x);
            telemetry.addProperty("previousCanvasY", previousClickCanvasPoint.y);
        }
        host.emitInteractionClickTelemetry(telemetry);
    }

    private Rectangle canvasBounds() {
        return new Rectangle(
            1,
            1,
            Math.max(1, host.canvasWidth() - 2),
            Math.max(1, host.canvasHeight() - 2)
        );
    }

    private static Point clampPointToRectangle(Point point, Rectangle bounds) {
        if (point == null || bounds == null) {
            return point;
        }
        int minX = bounds.x;
        int maxX = bounds.x + Math.max(0, bounds.width - 1);
        int minY = bounds.y;
        int maxY = bounds.y + Math.max(0, bounds.height - 1);
        return new Point(
            Math.max(minX, Math.min(maxX, point.x)),
            Math.max(minY, Math.min(maxY, point.y))
        );
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.POSITIVE_INFINITY;
        }
        double dx = (double) a.x - b.x;
        double dy = (double) a.y - b.y;
        return Math.hypot(dx, dy);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
