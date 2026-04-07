package com.xptool.executor;

import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.motion.MotionProfile.MotorGestureMode;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

final class InteractionClickEngine {
    private static final double CLICK_TARGET_REACH_TOLERANCE_PX = 2.0;
    private static final long CLICK_REPEAT_GUARD_WINDOW_MS = 2200L;
    private static final double CLICK_REPEAT_EXCLUSION_PX = 2.8;
    private static final int CLICK_REPEAT_SAMPLE_RADIUS_PX = 7;
    private static final int CLICK_REPEAT_SAMPLE_ATTEMPTS = 26;

    interface Host {
        boolean isUsableCanvasPoint(Point point);

        boolean canPerformMotorActionNow();

        boolean motorMoveToCanvasPoint(
            Point canvasPoint,
            MotorGestureMode gestureMode,
            boolean allowActivationClick,
            boolean dropSweepMode,
            boolean requireReady
        );

        Point currentMouseCanvasPoint();

        Point clampPointToRectangle(Point point, Rectangle bounds);

        Optional<Rectangle> resolveInventoryInteractionRegionCanvas();

        Point lastInteractionAnchorCenterCanvasPoint();

        Rectangle lastInteractionAnchorBoundsCanvas();

        int canvasWidth();

        int canvasHeight();

        double pixelDistance(Point a, Point b);

        boolean isClientCanvasFocused();
        boolean allowWindowRefocusForInteraction();

        boolean focusClientWindowAndCanvas();

        Optional<Point> toScreenPoint(Point canvasPoint);

        Robot getOrCreateRobot();

        void clickCanvasActivationAnchor(Robot robot);

        void noteInteractionClickSuccess();

        boolean isCursorNearScreenPoint(Point screenPoint, double tolerancePx);

        void moveMouseCurve(Robot robot, Point to);

        void sleepCritical(long ms);

        void sleepNoCooldown(long ms);
    }

    static final class Config {
        final boolean visualCursorMotionEnabled;
        final boolean humanizedTimingEnabled;
        final int postClickSettleMaxRadiusPx;
        final long bankMotorReadyWaitMaxMs;
        final double bankMenuRightClickReuseTolerancePx;
        final int microSettleBeforeRightClickMs;
        final int microRightButtonDownMs;
        final int microSettleBeforeClickMs;
        final int microButtonDownMs;
        final int criticalClickJitterPx;

        Config(
            boolean visualCursorMotionEnabled,
            boolean humanizedTimingEnabled,
            int postClickSettleMaxRadiusPx,
            long bankMotorReadyWaitMaxMs,
            double bankMenuRightClickReuseTolerancePx,
            int microSettleBeforeRightClickMs,
            int microRightButtonDownMs,
            int microSettleBeforeClickMs,
            int microButtonDownMs,
            int criticalClickJitterPx
        ) {
            this.visualCursorMotionEnabled = visualCursorMotionEnabled;
            this.humanizedTimingEnabled = humanizedTimingEnabled;
            this.postClickSettleMaxRadiusPx = Math.max(1, postClickSettleMaxRadiusPx);
            this.bankMotorReadyWaitMaxMs = Math.max(0L, bankMotorReadyWaitMaxMs);
            this.bankMenuRightClickReuseTolerancePx = Math.max(0.0, bankMenuRightClickReuseTolerancePx);
            this.microSettleBeforeRightClickMs = Math.max(0, microSettleBeforeRightClickMs);
            this.microRightButtonDownMs = Math.max(0, microRightButtonDownMs);
            this.microSettleBeforeClickMs = Math.max(0, microSettleBeforeClickMs);
            this.microButtonDownMs = Math.max(0, microButtonDownMs);
            this.criticalClickJitterPx = Math.max(0, criticalClickJitterPx);
        }
    }

    private final Host host;
    private final Config config;
    private Point lastClickCanvasPoint = null;
    private long lastClickAtMs = 0L;

    InteractionClickEngine(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    boolean performInteractionPostClickSettleMove(Point anchorCanvasPoint) {
        if (!config.visualCursorMotionEnabled) {
            return false;
        }
        if (anchorCanvasPoint == null || !host.isUsableCanvasPoint(anchorCanvasPoint)) {
            return false;
        }
        if (!host.canPerformMotorActionNow()) {
            return false;
        }
        Point settleTarget = resolvePostClickSettleTarget(anchorCanvasPoint);
        if (settleTarget == null) {
            return false;
        }
        return host.motorMoveToCanvasPoint(
            settleTarget,
            MotorGestureMode.GENERAL,
            true,
            false,
            true
        );
    }

    boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion) {
        return clickCanvasPointWithModifier(canvasPoint, motion, -1);
    }

    boolean clickCanvasPointWithModifier(Point canvasPoint, ClickMotionSettings motion, int modifierKeyCode) {
        if (canvasPoint == null || !host.isUsableCanvasPoint(canvasPoint)) {
            return false;
        }
        if (!host.canPerformMotorActionNow()) {
            return false;
        }
        Point guardedCanvasPoint = resolveRepeatSafeCanvasPoint(canvasPoint);
        if (guardedCanvasPoint == null || !host.isUsableCanvasPoint(guardedCanvasPoint)) {
            return false;
        }
        boolean hadCanvasFocus = host.isClientCanvasFocused();
        if (!hadCanvasFocus) {
            if (!host.allowWindowRefocusForInteraction()) {
                return false;
            }
            if (!host.focusClientWindowAndCanvas()) {
                return false;
            }
        }
        Optional<Point> screen = host.toScreenPoint(guardedCanvasPoint);
        if (screen.isEmpty()) {
            return false;
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        if (!hadCanvasFocus && host.allowWindowRefocusForInteraction()) {
            host.clickCanvasActivationAnchor(robot);
        }
        Point target = applyMotionDrift(screen.get(), motion);
        if (!host.isCursorNearScreenPoint(target, CLICK_TARGET_REACH_TOLERANCE_PX)) {
            host.moveMouseCurve(robot, target);
            host.sleepNoCooldown(1L);
        }
        if (!host.isCursorNearScreenPoint(target, CLICK_TARGET_REACH_TOLERANCE_PX)) {
            host.moveMouseCurve(robot, target);
            host.sleepNoCooldown(1L);
        }
        if (!host.isCursorNearScreenPoint(target, CLICK_TARGET_REACH_TOLERANCE_PX)) {
            return false;
        }
        boolean clicked;
        try {
            if (modifierKeyCode > 0) {
                robot.keyPress(modifierKeyCode);
                host.sleepCritical(6L);
            }
            double microJitterRadiusPx = Math.max(0.6, Math.min(2.4, motion.driftRadiusPx));
            clicked = ExecutorMenuClickSupport.clickAt(
                robot,
                target,
                motion.preClickDelayMs,
                motion.postClickDelayMs,
                microJitterRadiusPx,
                config.microSettleBeforeClickMs,
                config.microButtonDownMs
            );
        } finally {
            if (modifierKeyCode > 0) {
                try {
                    host.sleepCritical(4L);
                    robot.keyRelease(modifierKeyCode);
                } catch (Exception ignored) {
                    // Best-effort modifier release.
                }
            }
        }
        if (clicked) {
            host.noteInteractionClickSuccess();
            noteClickPointDispatched(guardedCanvasPoint);
        }
        return clicked;
    }

    boolean rightClickCanvasPointBank(Point canvasPoint, ClickMotionSettings motion) {
        if (canvasPoint == null || !host.isUsableCanvasPoint(canvasPoint)) {
            return false;
        }
        if (!waitForMotorActionReady(config.bankMotorReadyWaitMaxMs)) {
            return false;
        }
        Point guardedCanvasPoint = resolveRepeatSafeCanvasPoint(canvasPoint);
        if (guardedCanvasPoint == null || !host.isUsableCanvasPoint(guardedCanvasPoint)) {
            return false;
        }
        boolean hadCanvasFocus = host.isClientCanvasFocused();
        if (!hadCanvasFocus) {
            if (!host.allowWindowRefocusForInteraction()) {
                return false;
            }
            if (!host.focusClientWindowAndCanvas()) {
                return false;
            }
        }
        Optional<Point> screen = host.toScreenPoint(guardedCanvasPoint);
        if (screen.isEmpty()) {
            return false;
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        if (!hadCanvasFocus && host.allowWindowRefocusForInteraction()) {
            host.clickCanvasActivationAnchor(robot);
        }
        Point to = applyMotionDrift(screen.get(), motion);
        if (!host.isCursorNearScreenPoint(to, config.bankMenuRightClickReuseTolerancePx)) {
            host.moveMouseCurve(robot, to);
        }
        host.sleepCritical(Math.max(config.microSettleBeforeRightClickMs, motion.preClickDelayMs));
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        host.sleepCritical(Math.max(config.microRightButtonDownMs, motion.postClickDelayMs));
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        host.sleepCritical(1L);
        host.noteInteractionClickSuccess();
        noteClickPointDispatched(guardedCanvasPoint);
        return true;
    }

    boolean clickCanvasPointNoRefocus(Point canvasPoint, int clickSettleMs, int clickDownMs) {
        if (canvasPoint == null || !host.isUsableCanvasPoint(canvasPoint)) {
            return false;
        }
        Point guardedCanvasPoint = resolveRepeatSafeCanvasPoint(canvasPoint);
        if (guardedCanvasPoint == null || !host.isUsableCanvasPoint(guardedCanvasPoint)) {
            return false;
        }
        Optional<Point> screen = host.toScreenPoint(guardedCanvasPoint);
        if (screen.isEmpty()) {
            return false;
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        boolean clicked = ExecutorMenuClickSupport.clickAtCritical(
            robot,
            screen.get(),
            clickSettleMs,
            clickDownMs,
            config.criticalClickJitterPx
        );
        if (clicked) {
            host.noteInteractionClickSuccess();
            noteClickPointDispatched(guardedCanvasPoint);
        }
        return clicked;
    }

    private Point resolveRepeatSafeCanvasPoint(Point requestedPoint) {
        if (requestedPoint == null || !host.isUsableCanvasPoint(requestedPoint)) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (lastClickCanvasPoint == null || (now - lastClickAtMs) > CLICK_REPEAT_GUARD_WINDOW_MS) {
            return requestedPoint;
        }
        double distance = host.pixelDistance(requestedPoint, lastClickCanvasPoint);
        if (distance > CLICK_REPEAT_EXCLUSION_PX) {
            return requestedPoint;
        }
        Rectangle canvasBounds = new Rectangle(
            1,
            1,
            Math.max(1, host.canvasWidth() - 2),
            Math.max(1, host.canvasHeight() - 2)
        );
        Rectangle localBounds = new Rectangle(
            requestedPoint.x - CLICK_REPEAT_SAMPLE_RADIUS_PX,
            requestedPoint.y - CLICK_REPEAT_SAMPLE_RADIUS_PX,
            (CLICK_REPEAT_SAMPLE_RADIUS_PX * 2) + 1,
            (CLICK_REPEAT_SAMPLE_RADIUS_PX * 2) + 1
        ).intersection(canvasBounds);
        if (localBounds.width <= 0 || localBounds.height <= 0) {
            return null;
        }
        Point sampled = RepeatSafeClickPointChooser.randomPointInBoundsAvoiding(
            localBounds,
            host::isUsableCanvasPoint,
            lastClickCanvasPoint,
            CLICK_REPEAT_EXCLUSION_PX,
            0,
            CLICK_REPEAT_SAMPLE_ATTEMPTS,
            requestedPoint
        );
        if (sampled == null || !host.isUsableCanvasPoint(sampled)) {
            return null;
        }
        return sampled;
    }

    private void noteClickPointDispatched(Point canvasPoint) {
        if (canvasPoint == null) {
            return;
        }
        lastClickCanvasPoint = new Point(canvasPoint);
        lastClickAtMs = System.currentTimeMillis();
    }

    private boolean waitForMotorActionReady(long timeoutMs) {
        long budgetMs = Math.max(0L, timeoutMs);
        long deadlineMs = System.currentTimeMillis() + budgetMs;
        while (System.currentTimeMillis() <= deadlineMs) {
            if (host.canPerformMotorActionNow()) {
                return true;
            }
            host.sleepNoCooldown(1L);
        }
        return host.canPerformMotorActionNow();
    }

    private Point resolvePostClickSettleTarget(Point anchorCanvasPoint) {
        Point anchor = new Point(anchorCanvasPoint);
        Rectangle settleBounds = resolvePostClickSettleBounds(anchor);
        Point current = host.currentMouseCanvasPoint();
        if (!host.isUsableCanvasPoint(current)) {
            current = new Point(anchor);
        }
        current = host.clampPointToRectangle(current, settleBounds);

        Point anchorCenter = resolvePostClickAnchorCenter(anchor, settleBounds);
        double currentAnchorDistance = host.pixelDistance(current, anchorCenter);
        int amplitude = samplePostClickSettleAmplitudePx(currentAnchorDistance);
        if (amplitude <= 0) {
            return null;
        }

        Point candidate = inwardPostClickSettleCandidate(current, anchorCenter, amplitude);
        if (candidate == null) {
            return null;
        }
        candidate = host.clampPointToRectangle(candidate, settleBounds);
        candidate = clampPointToAnchorRadius(candidate, anchorCenter, config.postClickSettleMaxRadiusPx);
        if (!host.isUsableCanvasPoint(candidate)) {
            return null;
        }

        double candidateAnchorDistance = host.pixelDistance(candidate, anchorCenter);
        double settleSlack = config.humanizedTimingEnabled
            ? ThreadLocalRandom.current().nextDouble(0.45, 1.16)
            : 0.35;
        double maxAllowedDistance = Math.min(
            config.postClickSettleMaxRadiusPx,
            Math.max(1.4, currentAnchorDistance + settleSlack)
        );
        if (candidateAnchorDistance > maxAllowedDistance) {
            candidate = clampPointToAnchorRadius(candidate, anchorCenter, maxAllowedDistance);
        }
        if (!host.isUsableCanvasPoint(candidate)) {
            return null;
        }
        if (host.pixelDistance(candidate, current) < 0.85) {
            return null;
        }
        return candidate;
    }

    private Point resolvePostClickAnchorCenter(Point clickAnchorPoint, Rectangle settleBounds) {
        Point anchorCenter = host.lastInteractionAnchorCenterCanvasPoint();
        if (host.isUsableCanvasPoint(anchorCenter)) {
            return host.clampPointToRectangle(new Point(anchorCenter), settleBounds);
        }
        Point fallback = host.isUsableCanvasPoint(clickAnchorPoint) ? new Point(clickAnchorPoint) : null;
        if (!host.isUsableCanvasPoint(fallback)) {
            Point current = host.currentMouseCanvasPoint();
            if (host.isUsableCanvasPoint(current)) {
                fallback = new Point(current);
            }
        }
        if (!host.isUsableCanvasPoint(fallback)) {
            fallback = sampleRandomPointInBounds(settleBounds);
        }
        Rectangle anchorBounds = host.lastInteractionAnchorBoundsCanvas();
        if (anchorBounds != null && fallback != null && anchorBounds.contains(fallback)) {
            Rectangle bounded = settleBounds == null ? anchorBounds : anchorBounds.intersection(settleBounds);
            if (bounded.width > 0 && bounded.height > 0) {
                Point sampled = sampleRandomPointInBounds(bounded);
                if (host.isUsableCanvasPoint(sampled)) {
                    return host.clampPointToRectangle(sampled, settleBounds);
                }
            }
        }
        Optional<Rectangle> interactionRegion = host.resolveInventoryInteractionRegionCanvas();
        if (interactionRegion.isPresent() && fallback != null && interactionRegion.get().contains(fallback)) {
            Rectangle bounded = interactionRegion.get();
            if (settleBounds != null) {
                Rectangle intersection = bounded.intersection(settleBounds);
                if (intersection.width > 0 && intersection.height > 0) {
                    bounded = intersection;
                }
            }
            Point sampled = sampleRandomPointInBounds(bounded);
            if (host.isUsableCanvasPoint(sampled)) {
                return host.clampPointToRectangle(sampled, settleBounds);
            }
        }
        return host.isUsableCanvasPoint(fallback) ? host.clampPointToRectangle(fallback, settleBounds) : null;
    }

    private Rectangle resolvePostClickSettleBounds(Point anchorCanvasPoint) {
        Rectangle canvasBounds = new Rectangle(
            1,
            1,
            Math.max(1, host.canvasWidth() - 2),
            Math.max(1, host.canvasHeight() - 2)
        );
        Point anchor = host.isUsableCanvasPoint(anchorCanvasPoint) ? new Point(anchorCanvasPoint) : null;
        if (!host.isUsableCanvasPoint(anchor)) {
            Point current = host.currentMouseCanvasPoint();
            if (host.isUsableCanvasPoint(current)) {
                anchor = new Point(current);
            }
        }
        if (!host.isUsableCanvasPoint(anchor)) {
            Point sampled = sampleRandomPointInBounds(canvasBounds);
            if (host.isUsableCanvasPoint(sampled)) {
                anchor = sampled;
            }
        }
        if (!host.isUsableCanvasPoint(anchor) || anchor == null) {
            return new Rectangle(canvasBounds);
        }
        Point boundedAnchor = new Point(anchor);
        Rectangle localBounds = new Rectangle(
            boundedAnchor.x - config.postClickSettleMaxRadiusPx,
            boundedAnchor.y - config.postClickSettleMaxRadiusPx,
            (config.postClickSettleMaxRadiusPx * 2) + 1,
            (config.postClickSettleMaxRadiusPx * 2) + 1
        );
        Rectangle bounded = localBounds.intersection(canvasBounds);
        if (bounded.width <= 0 || bounded.height <= 0) {
            return new Rectangle(boundedAnchor.x, boundedAnchor.y, 1, 1);
        }
        Rectangle anchorBounds = host.lastInteractionAnchorBoundsCanvas();
        if (anchorBounds != null && anchorBounds.contains(boundedAnchor)) {
            Rectangle intersected = bounded.intersection(anchorBounds);
            if (intersected.width > 0 && intersected.height > 0) {
                return intersected;
            }
        }
        Optional<Rectangle> interactionRegion = host.resolveInventoryInteractionRegionCanvas();
        if (interactionRegion.isPresent() && interactionRegion.get().contains(boundedAnchor)) {
            Rectangle intersected = bounded.intersection(interactionRegion.get());
            if (intersected.width > 0 && intersected.height > 0) {
                return intersected;
            }
        }
        return bounded;
    }

    private static Point sampleRandomPointInBounds(Rectangle bounds) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return null;
        }
        int minX = bounds.x;
        int maxX = bounds.x + Math.max(0, bounds.width - 1);
        int minY = bounds.y;
        int maxY = bounds.y + Math.max(0, bounds.height - 1);
        return new Point(
            ThreadLocalRandom.current().nextInt(minX, maxX + 1),
            ThreadLocalRandom.current().nextInt(minY, maxY + 1)
        );
    }

    private Point inwardPostClickSettleCandidate(Point currentCanvasPoint, Point anchorCenter, int amplitudePx) {
        if (currentCanvasPoint == null || anchorCenter == null) {
            return null;
        }
        double toCenterX = anchorCenter.x - currentCanvasPoint.x;
        double toCenterY = anchorCenter.y - currentCanvasPoint.y;
        double toCenterDistance = Math.hypot(toCenterX, toCenterY);
        if (toCenterDistance < 0.75) {
            return null;
        }
        int amplitude = Math.max(1, Math.min(config.postClickSettleMaxRadiusPx, amplitudePx));
        double ux = toCenterX / toCenterDistance;
        double uy = toCenterY / toCenterDistance;
        double px = -uy;
        double py = ux;
        double lateralScale = config.humanizedTimingEnabled
            ? ThreadLocalRandom.current().nextDouble(-0.24, 0.24)
            : 0.0;
        int lateral = (int) Math.round((double) amplitude * lateralScale);
        int dx = (int) Math.round((ux * amplitude) + (px * lateral));
        int dy = (int) Math.round((uy * amplitude) + (py * lateral));
        if (dx == 0 && dy == 0) {
            if (Math.abs(toCenterX) >= Math.abs(toCenterY)) {
                dx = toCenterX >= 0 ? 1 : -1;
            } else {
                dy = toCenterY >= 0 ? 1 : -1;
            }
        }
        return new Point(currentCanvasPoint.x + dx, currentCanvasPoint.y + dy);
    }

    private int samplePostClickSettleAmplitudePx(double currentAnchorDistancePx) {
        double dist = Math.max(0.0, currentAnchorDistancePx);
        if (dist < 0.95) {
            return 0;
        }
        int amplitudeCap = Math.max(2, Math.min(config.postClickSettleMaxRadiusPx, 5));
        int amplitude = Math.max(1, Math.min(amplitudeCap, (int) Math.round(dist * 0.72)));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (config.humanizedTimingEnabled) {
            if (amplitude <= 1) {
                return amplitude;
            }
            int roll = random.nextInt(100);
            if (roll < 30) {
                amplitude = Math.max(1, amplitude - 2);
            } else if (roll < 78) {
                amplitude = Math.max(1, amplitude - 1);
            }
            int wobble = random.nextInt(-1, 2);
            return Math.max(1, Math.min(config.postClickSettleMaxRadiusPx, amplitude + wobble));
        }
        // Even in non-humanized mode, keep settle amplitude stochastic to avoid deterministic movement cadence.
        int roll = random.nextInt(100);
        if (roll < 45) {
            amplitude = Math.max(1, amplitude - 1);
        }
        int wobble = random.nextInt(0, 2);
        return Math.max(1, Math.min(config.postClickSettleMaxRadiusPx, amplitude + wobble));
    }

    private static Point clampPointToAnchorRadius(Point point, Point anchorCenter, double maxRadiusPx) {
        if (point == null || anchorCenter == null || maxRadiusPx < 0.0) {
            return point;
        }
        double distance = pixelDistance(point, anchorCenter);
        if (distance <= maxRadiusPx) {
            return point;
        }
        if (distance <= 0.0) {
            return new Point(anchorCenter);
        }
        double ratio = maxRadiusPx / distance;
        int x = anchorCenter.x + (int) Math.round((point.x - anchorCenter.x) * ratio);
        int y = anchorCenter.y + (int) Math.round((point.y - anchorCenter.y) * ratio);
        return new Point(x, y);
    }

    private static Point applyMotionDrift(Point target, ClickMotionSettings motion) {
        if (target == null || motion == null) {
            return target;
        }
        double radius = Math.max(0.0, motion.driftRadiusPx);
        if (radius <= 0.0) {
            return target;
        }
        double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
        double distance = ThreadLocalRandom.current().nextDouble(0.0, radius);
        int dx = (int) Math.round(Math.cos(angle) * distance);
        int dy = (int) Math.round(Math.sin(angle) * distance);
        return new Point(target.x + dx, target.y + dy);
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.POSITIVE_INFINITY;
        }
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.hypot(dx, dy);
    }
}
