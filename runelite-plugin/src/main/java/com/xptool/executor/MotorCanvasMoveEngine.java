package com.xptool.executor;

import com.xptool.motion.MotionProfile.MotorGestureMode;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

final class MotorCanvasMoveEngine {
    interface Host {
        boolean isUsableCanvasPoint(Point canvasPoint);

        boolean canPerformMotorActionNow();

        boolean focusClientWindowAndCanvas(boolean allowActivationClick);

        Optional<Point> toScreenPoint(Point canvasPoint);

        Robot getOrCreateRobot();

        Rectangle dropSweepRegionScreen();

        Point dropSweepLastTargetScreen();

        void setDropSweepLastTargetScreen(Point point);

        boolean dropSweepAwaitingFirstCursorSync();

        Point motorCursorLocationOr(Point fallback);

        boolean isCursorNearScreenPoint(Point screenPoint, double tolerancePx);

        boolean tryConsumeMouseMutationBudget();

        Point currentPointerLocationOr(Point fallback);

        void moveMouseCurveTo(Robot robot, Point to);

        void moveMouseCurve(Robot robot, Point from, Point to, int steps, long stepDelayMs);

        void moveMouseCurveIdle(Robot robot, Point from, Point to);

        boolean isIdleMotorOwnerActive();

        void noteMouseMutation(Point point);

        void updateMotorCursorState(Point point);

        void clearPendingMouseMove();

        void noteInteractionActivityNow();
    }

    static final class Config {
        final double moveTargetTolerancePx;
        final int cursorMotorMinSteps;
        final int cursorMotorMaxSteps;
        final int cursorMotorGeneralStepDelayMs;
        final int cursorMotorDropStepDelayMs;
        final int cursorMoveBaseStepDelayMs;
        final boolean humanizedTimingEnabled;

        Config(
            double moveTargetTolerancePx,
            int cursorMotorMinSteps,
            int cursorMotorMaxSteps,
            int cursorMotorGeneralStepDelayMs,
            int cursorMotorDropStepDelayMs,
            int cursorMoveBaseStepDelayMs,
            boolean humanizedTimingEnabled
        ) {
            this.moveTargetTolerancePx = moveTargetTolerancePx;
            this.cursorMotorMinSteps = Math.max(1, cursorMotorMinSteps);
            this.cursorMotorMaxSteps = Math.max(this.cursorMotorMinSteps, cursorMotorMaxSteps);
            this.cursorMotorGeneralStepDelayMs = Math.max(1, cursorMotorGeneralStepDelayMs);
            this.cursorMotorDropStepDelayMs = Math.max(1, cursorMotorDropStepDelayMs);
            this.cursorMoveBaseStepDelayMs = Math.max(1, cursorMoveBaseStepDelayMs);
            this.humanizedTimingEnabled = humanizedTimingEnabled;
        }
    }

    private final Host host;
    private final Config config;

    MotorCanvasMoveEngine(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    boolean motorMoveToCanvasPoint(
        Point canvasPoint,
        MotorGestureMode mode,
        boolean enforceMutationBudget,
        boolean allowActivationClick,
        boolean recordInteraction
    ) {
        if (canvasPoint == null || !host.isUsableCanvasPoint(canvasPoint)) {
            return false;
        }
        if (!host.canPerformMotorActionNow()) {
            return false;
        }
        if (!host.focusClientWindowAndCanvas(allowActivationClick)) {
            return false;
        }
        Optional<Point> screen = host.toScreenPoint(canvasPoint);
        if (screen.isEmpty()) {
            return false;
        }
        Robot robot = host.getOrCreateRobot();
        if (robot == null) {
            return false;
        }

        Rectangle dropSweepRegionScreen = host.dropSweepRegionScreen();
        Point target = new Point(screen.get());
        if (mode == MotorGestureMode.DROP_SWEEP && dropSweepRegionScreen != null) {
            target = clampScreenPointToRegion(target, dropSweepRegionScreen);
        }
        Point from = host.motorCursorLocationOr(target);
        if (mode == MotorGestureMode.DROP_SWEEP && host.dropSweepLastTargetScreen() != null) {
            from = new Point(host.dropSweepLastTargetScreen());
        }
        if (mode == MotorGestureMode.DROP_SWEEP && dropSweepRegionScreen != null && !host.dropSweepAwaitingFirstCursorSync()) {
            from = clampScreenPointToRegion(from, dropSweepRegionScreen);
        }
        if (host.isCursorNearScreenPoint(target, config.moveTargetTolerancePx)) {
            host.clearPendingMouseMove();
            host.updateMotorCursorState(target);
            return true;
        }
        if (enforceMutationBudget && !host.tryConsumeMouseMutationBudget()) {
            return false;
        }
        moveMouseCurveMotor(robot, from, target, mode);
        Point after = host.currentPointerLocationOr(target);
        if (mode == MotorGestureMode.DROP_SWEEP && dropSweepRegionScreen != null) {
            Point clampedAfter = clampScreenPointToRegion(after, dropSweepRegionScreen);
            if (after == null || after.x != clampedAfter.x || after.y != clampedAfter.y) {
                host.moveMouseCurveTo(robot, clampedAfter);
            }
            after = clampedAfter;
            host.setDropSweepLastTargetScreen(new Point(target));
        }
        host.noteMouseMutation(after);
        host.updateMotorCursorState(after);
        host.clearPendingMouseMove();
        if (recordInteraction) {
            host.noteInteractionActivityNow();
        }
        return true;
    }

    private void moveMouseCurveMotor(Robot robot, Point from, Point to, MotorGestureMode mode) {
        if (robot == null || to == null) {
            return;
        }
        Point start = from == null ? to : from;
        if (start.equals(to)) {
            return;
        }

        if (mode == MotorGestureMode.GENERAL && host.isIdleMotorOwnerActive()) {
            host.moveMouseCurveIdle(robot, start, to);
            return;
        }

        int steps = Math.max(
            config.cursorMotorMinSteps,
            Math.min(
                config.cursorMotorMaxSteps,
                (int) Math.round(pixelDistance(start, to) / 13.0) + 6
            )
        );

        int stepDelay = mode == MotorGestureMode.DROP_SWEEP
            ? resolveDropSweepStepDelayMs(start, to)
            : config.cursorMotorGeneralStepDelayMs;
        host.moveMouseCurve(robot, start, to, steps, Math.max(1L, (long) stepDelay));
    }

    private int resolveDropSweepStepDelayMs(Point from, Point to) {
        long base = Math.max(1L, stepDelayForDistance(from, to));
        if (host.dropSweepAwaitingFirstCursorSync()) {
            long lowFirst = Math.max(2L, base - 2L);
            long highFirst = Math.max(lowFirst, base + 1L);
            return (int) randomBetween(lowFirst, highFirst);
        }
        if (!config.humanizedTimingEnabled) {
            return (int) Math.max(base, (long) config.cursorMotorDropStepDelayMs);
        }
        long low = Math.max(3L, Math.min(base, (long) config.cursorMotorDropStepDelayMs));
        long high = Math.max(low, base + 2L);
        return (int) randomBetween(low, high);
    }

    private long stepDelayForDistance(Point from, Point to) {
        double dist = pixelDistance(from, to);
        long extra = (long) Math.min(4.0, dist / 120.0);
        return Math.max(1L, config.cursorMoveBaseStepDelayMs + extra);
    }

    private static long randomBetween(long minInclusive, long maxInclusive) {
        long lo = Math.min(minInclusive, maxInclusive);
        long hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        if (hi == Long.MAX_VALUE) {
            long sampled = ThreadLocalRandom.current().nextLong(lo, hi);
            return ThreadLocalRandom.current().nextBoolean() ? sampled : hi;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static Point clampScreenPointToRegion(Point point, Rectangle region) {
        if (point == null || region == null) {
            return point;
        }
        int x = Math.max(region.x, Math.min(region.x + Math.max(0, region.width - 1), point.x));
        int y = Math.max(region.y, Math.min(region.y + Math.max(0, region.height - 1), point.y));
        return new Point(x, y);
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return 0.0;
        }
        double dx = (double) a.x - b.x;
        double dy = (double) a.y - b.y;
        return Math.hypot(dx, dy);
    }
}
