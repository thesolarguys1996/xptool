package com.xptool.executor;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.util.concurrent.ThreadLocalRandom;

final class ExecutorCursorMotion {
    private static final int CURSOR_MOVE_MIN_STEPS = 12;
    private static final int CURSOR_MOVE_MAX_STEPS = 34;
    private static final int CURSOR_MOVE_BASE_STEP_DELAY_MS = 8;
    private static final int IDLE_DIRECT_MOVE_MIN_STEPS = 8;
    private static final int IDLE_DIRECT_MOVE_MAX_STEPS = 24;
    private static final long IDLE_BLOCKING_STEP_DELAY_MAX_MS = Math.max(
        0L,
        Math.min(2L, Long.getLong("xptool.idleBlockingStepDelayMaxMs", 1L))
    );

    private static final double CURVE_SCALE_DEFAULT = 0.20;
    private static final double CURVE_MIN_PX_DEFAULT = 6.0;
    private static final double CURVE_MAX_PX_DEFAULT = 36.0;
    private static final double CURVE_SCALE_IDLE = 0.11;
    private static final double CURVE_MIN_PX_IDLE = 2.0;
    private static final double CURVE_MAX_PX_IDLE = 14.0;
    private static final double CURVE_SCALE_SETTLE = 0.09;
    private static final double CURVE_MIN_PX_SETTLE = 1.0;
    private static final double CURVE_MAX_PX_SETTLE = 8.0;

    private static final int IDLE_S_CURVE_CHANCE_PERCENT = 44;
    private static final int IDLE_S_CURVE_MIN_DISTANCE_PX = 62;
    private static final int IDLE_CONTROL_JITTER_PX = 3;
    private static final int IDLE_MICRO_CORRECTION_CHANCE_PERCENT = 34;
    private static final int IDLE_MICRO_CORRECTION_MIN_DISTANCE_PX = 38;
    private static final int IDLE_MICRO_CORRECTION_MAX_RADIUS_PX = 3;

    private static final int OVERSHOOT_MIN_DISTANCE_PX = 320;
    private static final int OVERSHOOT_MAX_PX = 2;
    private static final double OVERSHOOT_APPLY_CHANCE = 0.42;
    private static final double TERMINAL_ALIGNMENT_TOLERANCE_PX = 1.4;
    private static final int TERMINAL_FINALIZE_JITTER_RADIUS_PX = 1;
    private static Point lastTerminalFinalizePoint;

    private static final boolean HUMAN_OVERSHOOT_ENABLED = true;

    private ExecutorCursorMotion() {
    }

    static void smoothMouseMove(Robot robot, Point from, Point to, int durationMs) {
        if (robot == null || from == null || to == null) {
            return;
        }
        int ms = Math.max(12, Math.min(72, durationMs));
        int steps = Math.max(2, Math.min(4, ms / 20));
        moveMouseCurve(robot, from, to, Math.max(3, steps + 2), Math.max(0L, ms / Math.max(1, steps)));
    }

    static void moveMouseCurve(Robot robot, Point to) {
        if (robot == null || to == null) {
            return;
        }
        PointerInfo pointer = MouseInfo.getPointerInfo();
        Point from = pointer == null ? null : pointer.getLocation();
        if (from == null) {
            from = fallbackStartNearTarget(to);
        }
        int steps = stepsForDistance(from, to);
        long stepDelay = stepDelayForDistance(from, to);
        moveMouseCurve(robot, from, to, steps, stepDelay);
    }

    static void moveMouseCurve(Robot robot, Point from, Point to, int steps, long stepDelayMs) {
        moveMouseCurve(
            robot,
            from,
            to,
            steps,
            stepDelayMs,
            true,
            CURVE_SCALE_DEFAULT,
            CURVE_MIN_PX_DEFAULT,
            CURVE_MAX_PX_DEFAULT
        );
    }

    static void moveMouseCurve(
        Robot robot,
        Point from,
        Point to,
        int steps,
        long stepDelayMs,
        boolean allowOvershoot,
        double curveScale,
        double curveMinPx,
        double curveMaxPx
    ) {
        if (robot == null || to == null) {
            return;
        }
        Point start = from == null ? to : from;
        if (start.equals(to)) {
            return;
        }
        Point overshoot = maybeOvershootTarget(start, to, allowOvershoot);
        if (overshoot != null && (overshoot.x != to.x || overshoot.y != to.y)) {
            int leadSteps = Math.max(2, (int) Math.round(Math.max(2, steps) * 0.58));
            int settleSteps = Math.max(3, (int) Math.round(Math.max(2, steps) * 0.78));
            moveMouseSegment(robot, start, overshoot, leadSteps, stepDelayMs, curveScale, curveMinPx, curveMaxPx);
            long settleDelay = stepDelayMs <= 0L ? 0L : Math.max(1L, stepDelayMs + 1L);
            moveMouseSegment(
                robot,
                overshoot,
                to,
                settleSteps,
                settleDelay,
                CURVE_SCALE_SETTLE,
                CURVE_MIN_PX_SETTLE,
                CURVE_MAX_PX_SETTLE
            );
            return;
        }
        moveMouseSegment(robot, start, to, Math.max(2, steps), stepDelayMs, curveScale, curveMinPx, curveMaxPx);
    }

    static void moveMouseCurveIdle(Robot robot, Point from, Point to, boolean humanizedTimingEnabled) {
        if (robot == null || to == null) {
            return;
        }
        Point start = from == null ? to : from;
        if (start.equals(to)) {
            return;
        }
        int baseSteps = stepsForDistance(start, to);
        int scaledBaseSteps = (int) Math.round((double) baseSteps * (humanizedTimingEnabled ? 0.72 : 0.66));
        int extraSteps = humanizedTimingEnabled ? randomIntInclusive(0, 3) : 1;
        int steps = Math.max(
            IDLE_DIRECT_MOVE_MIN_STEPS,
            Math.min(IDLE_DIRECT_MOVE_MAX_STEPS, scaledBaseSteps + extraSteps)
        );
        long stepDelay = 0L;
        if (IDLE_BLOCKING_STEP_DELAY_MAX_MS > 0L) {
            stepDelay = humanizedTimingEnabled
                ? randomBetween(0L, IDLE_BLOCKING_STEP_DELAY_MAX_MS)
                : Math.min(1L, IDLE_BLOCKING_STEP_DELAY_MAX_MS);
        }
        boolean useSCurve = humanizedTimingEnabled
            && pixelDistance(start, to) >= IDLE_S_CURVE_MIN_DISTANCE_PX
            && ThreadLocalRandom.current().nextInt(100) < IDLE_S_CURVE_CHANCE_PERCENT;
        if (useSCurve) {
            moveMouseSCurve(
                robot,
                start,
                to,
                steps,
                stepDelay,
                CURVE_SCALE_IDLE,
                CURVE_MIN_PX_IDLE,
                CURVE_MAX_PX_IDLE,
                humanizedTimingEnabled
            );
        } else {
            Point control = humanizedTimingEnabled
                ? idleCurveControlPoint(start, to)
                : curveControlPoint(start, to, CURVE_SCALE_IDLE, CURVE_MIN_PX_IDLE, CURVE_MAX_PX_IDLE, 0);
            moveMouseSegment(robot, start, to, steps, stepDelay, control);
        }
        maybeApplyIdleMicroCorrection(robot, start, to, stepDelay, humanizedTimingEnabled);
    }

    static Point idleCurveControlPoint(Point from, Point to) {
        if (from == null || to == null) {
            return null;
        }
        int sign = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        Point control = curveControlPoint(from, to, CURVE_SCALE_IDLE, CURVE_MIN_PX_IDLE, CURVE_MAX_PX_IDLE, sign);
        return jitterControlPoint(control, resolveIdleControlJitterRadius(from, to));
    }

    static int stepsForDistance(Point from, Point to) {
        double dist = pixelDistance(from, to);
        int steps = (int) Math.round(6.0 + (dist / 14.0));
        return Math.max(CURSOR_MOVE_MIN_STEPS, Math.min(CURSOR_MOVE_MAX_STEPS, steps));
    }

    static long stepDelayForDistance(Point from, Point to) {
        double dist = pixelDistance(from, to);
        long extra = (long) Math.min(4.0, dist / 120.0);
        return Math.max(1L, CURSOR_MOVE_BASE_STEP_DELAY_MS + extra);
    }

    static Point curveControlPoint(Point from, Point to) {
        return curveControlPoint(from, to, CURVE_SCALE_DEFAULT, CURVE_MIN_PX_DEFAULT, CURVE_MAX_PX_DEFAULT);
    }

    static Point curveControlPoint(
        Point from,
        Point to,
        double curveScale,
        double curveMinPx,
        double curveMaxPx
    ) {
        return curveControlPoint(from, to, curveScale, curveMinPx, curveMaxPx, 0);
    }

    static Point quadraticBezier(Point p0, Point p1, Point p2, double t) {
        double u = 1.0 - t;
        double x = (u * u * p0.x) + (2.0 * u * t * p1.x) + (t * t * p2.x);
        double y = (u * u * p0.y) + (2.0 * u * t * p1.y) + (t * t * p2.y);
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    static Point linearInterpolate(Point from, Point to, double t) {
        if (from == null) {
            return to;
        }
        if (to == null) {
            return from;
        }
        double clamped = Math.max(0.0, Math.min(1.0, t));
        double x = from.x + ((to.x - from.x) * clamped);
        double y = from.y + ((to.y - from.y) * clamped);
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    static double smoothstep(double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        return clamped * clamped * (3.0 - (2.0 * clamped));
    }

    static int computeMoveDurationMs(Point from, Point to, int configuredMs) {
        int base = Math.max(30, Math.min(220, configuredMs));
        if (from == null || to == null) {
            return base;
        }
        double distance = pixelDistance(from, to);
        if (distance < 8.0) {
            return 24;
        }
        if (distance < 28.0) {
            return Math.min(base, 48);
        }
        if (distance < 90.0) {
            return Math.min(base, 86);
        }
        return Math.min(base, 150);
    }

    static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        long dx = (long) b.x - a.x;
        long dy = (long) b.y - a.y;
        return Math.sqrt((double) (dx * dx) + (double) (dy * dy));
    }

    static Point withMicroJitter(Point base, int radiusPx) {
        if (base == null || radiusPx <= 0) {
            return base;
        }
        long n = System.nanoTime();
        long seed = n ^ (((long) base.x) << 21) ^ (((long) base.y) << 7);
        int span = (radiusPx * 2) + 1;
        int jx = (int) Math.floorMod(seed, span) - radiusPx;
        int jy = (int) Math.floorMod(seed >>> 9, span) - radiusPx;
        return new Point(base.x + jx, base.y + jy);
    }

    static Point jitterWithinBounds(Point base, Rectangle bounds, int radiusPx) {
        Point jittered = withMicroJitter(base, radiusPx);
        if (jittered == null || bounds == null) {
            return jittered;
        }
        int minX = bounds.x;
        int minY = bounds.y;
        int maxX = bounds.x + Math.max(0, bounds.width - 1);
        int maxY = bounds.y + Math.max(0, bounds.height - 1);
        int clampedX = Math.max(minX, Math.min(maxX, jittered.x));
        int clampedY = Math.max(minY, Math.min(maxY, jittered.y));
        return new Point(clampedX, clampedY);
    }

    private static void moveMouseSCurve(
        Robot robot,
        Point from,
        Point to,
        int steps,
        long stepDelayMs,
        double curveScale,
        double curveMinPx,
        double curveMaxPx,
        boolean humanizedTimingEnabled
    ) {
        if (robot == null || from == null || to == null) {
            return;
        }
        int total = Math.max(4, steps);
        double splitT = humanizedTimingEnabled
            ? ThreadLocalRandom.current().nextDouble(0.44, 0.58)
            : 0.5;
        Point split = linearInterpolate(from, to, splitT);
        int sign = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        Point c1 = curveControlPoint(from, split, curveScale, curveMinPx, curveMaxPx, sign);
        Point c2 = curveControlPoint(split, to, curveScale, curveMinPx, curveMaxPx, -sign);
        if (humanizedTimingEnabled) {
            c1 = jitterControlPoint(c1, IDLE_CONTROL_JITTER_PX);
            c2 = jitterControlPoint(c2, IDLE_CONTROL_JITTER_PX);
        }
        int leadSteps = Math.max(2, (int) Math.round(total * 0.52));
        int settleSteps = Math.max(2, total - leadSteps);
        moveMouseSegment(robot, from, split, leadSteps, stepDelayMs, c1);
        moveMouseSegment(robot, split, to, settleSteps, stepDelayMs, c2);
    }

    private static void maybeApplyIdleMicroCorrection(
        Robot robot,
        Point from,
        Point to,
        long stepDelayMs,
        boolean humanizedTimingEnabled
    ) {
        if (robot == null || from == null || to == null || !humanizedTimingEnabled) {
            return;
        }
        if (pixelDistance(from, to) < IDLE_MICRO_CORRECTION_MIN_DISTANCE_PX) {
            return;
        }
        if (ThreadLocalRandom.current().nextInt(100) >= IDLE_MICRO_CORRECTION_CHANCE_PERCENT) {
            return;
        }
        int radius = randomIntInclusive(1, IDLE_MICRO_CORRECTION_MAX_RADIUS_PX);
        int dx = randomIntInclusive(-radius, radius);
        int dy = randomIntInclusive(-radius, radius);
        if (dx == 0 && dy == 0) {
            return;
        }
        Point correction = new Point(to.x + dx, to.y + dy);
        long correctionDelayMs = Math.max(1L, Math.min(2L, stepDelayMs));
        moveMouseSegment(
            robot,
            to,
            correction,
            2,
            correctionDelayMs,
            CURVE_SCALE_SETTLE,
            CURVE_MIN_PX_SETTLE,
            CURVE_MAX_PX_SETTLE
        );
        moveMouseSegment(
            robot,
            correction,
            to,
            2,
            correctionDelayMs,
            CURVE_SCALE_SETTLE,
            CURVE_MIN_PX_SETTLE,
            CURVE_MAX_PX_SETTLE
        );
    }

    private static Point jitterControlPoint(Point controlPoint, int radiusPx) {
        if (controlPoint == null || radiusPx <= 0) {
            return controlPoint;
        }
        int dx = randomIntInclusive(-radiusPx, radiusPx);
        int dy = randomIntInclusive(-radiusPx, radiusPx);
        return new Point(controlPoint.x + dx, controlPoint.y + dy);
    }

    private static int resolveIdleControlJitterRadius(Point from, Point to) {
        double distance = pixelDistance(from, to);
        if (distance >= 220.0) {
            return 4;
        }
        if (distance >= 120.0) {
            return 3;
        }
        if (distance >= 56.0) {
            return 2;
        }
        return 1;
    }

    private static void moveMouseSegment(
        Robot robot,
        Point from,
        Point to,
        int steps,
        long stepDelayMs,
        double curveScale,
        double curveMinPx,
        double curveMaxPx
    ) {
        Point control = curveControlPoint(from, to, curveScale, curveMinPx, curveMaxPx);
        moveMouseSegment(robot, from, to, steps, stepDelayMs, control);
    }

    private static void moveMouseSegment(
        Robot robot,
        Point from,
        Point to,
        int steps,
        long stepDelayMs,
        Point control
    ) {
        if (robot == null || from == null || to == null) {
            return;
        }
        int total = Math.max(2, steps);
        Point controlPoint = control == null ? curveControlPoint(from, to) : control;
        Point last = from;
        for (int i = 1; i <= total; i++) {
            double t = (double) i / (double) total;
            double progress = smoothstep(t);
            Point p = quadraticBezier(from, controlPoint, to, progress);
            if (p.x != last.x || p.y != last.y) {
                robot.mouseMove(p.x, p.y);
                last = p;
            }
            if (stepDelayMs > 0L) {
                sleepMotionStep(stepDelayMs);
            }
        }
        if (pixelDistance(last, to) > TERMINAL_ALIGNMENT_TOLERANCE_PX) {
            applyTerminalMicroCorrection(robot, last, to, stepDelayMs);
        }
    }

    private static void sleepMotionStep(long ms) {
        if (ms <= 0L) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        MotorActionGate.reserveGlobalCooldownOnly(ms);
    }

    private static Point curveControlPoint(
        Point from,
        Point to,
        double curveScale,
        double curveMinPx,
        double curveMaxPx,
        int preferredSign
    ) {
        double midX = (from.x + to.x) / 2.0;
        double midY = (from.y + to.y) / 2.0;
        double dx = (double) to.x - from.x;
        double dy = (double) to.y - from.y;
        double len = Math.max(1.0, Math.hypot(dx, dy));
        double nx = -dy / len;
        double ny = dx / len;
        double curve = Math.max(curveMinPx, Math.min(curveMaxPx, len * curveScale));
        int sign;
        if (preferredSign > 0) {
            sign = 1;
        } else if (preferredSign < 0) {
            sign = -1;
        } else {
            sign = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        }
        int cx = (int) Math.round(midX + (nx * curve * sign));
        int cy = (int) Math.round(midY + (ny * curve * sign));
        return new Point(cx, cy);
    }

    private static Point maybeOvershootTarget(Point from, Point to, boolean allowOvershoot) {
        if (!allowOvershoot || !HUMAN_OVERSHOOT_ENABLED || from == null || to == null) {
            return to;
        }
        double dx = (double) to.x - from.x;
        double dy = (double) to.y - from.y;
        double len = Math.hypot(dx, dy);
        if (len < OVERSHOOT_MIN_DISTANCE_PX) {
            return to;
        }
        if (ThreadLocalRandom.current().nextDouble() > OVERSHOOT_APPLY_CHANCE) {
            return to;
        }
        double ux = dx / Math.max(1.0, len);
        double uy = dy / Math.max(1.0, len);
        int overshoot = Math.max(1, Math.min(OVERSHOOT_MAX_PX, (int) Math.round(len / 220.0)));
        return new Point(
            (int) Math.round(to.x + (ux * overshoot)),
            (int) Math.round(to.y + (uy * overshoot))
        );
    }

    private static long randomBetween(long minInclusive, long maxInclusive) {
        long min = Math.min(minInclusive, maxInclusive);
        long max = Math.max(minInclusive, maxInclusive);
        if (min == max) {
            return min;
        }
        return ThreadLocalRandom.current().nextLong(min, max + 1L);
    }

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int min = Math.min(minInclusive, maxInclusive);
        int max = Math.max(minInclusive, maxInclusive);
        if (min == max) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static Point fallbackStartNearTarget(Point target) {
        if (target == null) {
            return null;
        }
        int radius = randomIntInclusive(22, 54);
        int dx = randomIntInclusive(-radius, radius);
        int dy = randomIntInclusive(-radius, radius);
        if (dx == 0 && dy == 0) {
            dx = 1;
        }
        return new Point(target.x + dx, target.y + dy);
    }

    private static void applyTerminalMicroCorrection(Robot robot, Point from, Point to, long stepDelayMs) {
        if (robot == null || from == null || to == null) {
            return;
        }
        Point finalTarget = resolveTerminalFinalizeTarget(to);
        if (finalTarget == null) {
            return;
        }
        if (pixelDistance(from, finalTarget) <= TERMINAL_ALIGNMENT_TOLERANCE_PX) {
            return;
        }
        Point current = new Point(from);
        long correctionStepDelayMs = Math.max(0L, Math.min(1L, stepDelayMs));
        for (int attempt = 0; attempt < 2; attempt++) {
            int steps = pixelDistance(current, finalTarget) >= 3.0 ? 3 : 2;
            Point control = curveControlPoint(
                current,
                finalTarget,
                CURVE_SCALE_SETTLE,
                CURVE_MIN_PX_SETTLE,
                CURVE_MAX_PX_SETTLE
            );
            control = jitterControlPoint(control, 1);
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / (double) steps;
                Point p = quadraticBezier(current, control, finalTarget, smoothstep(t));
                robot.mouseMove(p.x, p.y);
                if (correctionStepDelayMs > 0L) {
                    sleepMotionStep(correctionStepDelayMs);
                }
            }
            Point pointer = MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
            current = pointer == null ? new Point(finalTarget) : pointer;
            if (pixelDistance(current, finalTarget) <= TERMINAL_ALIGNMENT_TOLERANCE_PX) {
                return;
            }
        }
    }

    private static Point resolveTerminalFinalizeTarget(Point target) {
        if (target == null) {
            return null;
        }
        Point base = new Point(target);
        Point last = lastTerminalFinalizePoint == null ? null : new Point(lastTerminalFinalizePoint);
        Point selected = null;
        for (int i = 0; i < 10; i++) {
            int dx = randomIntInclusive(-TERMINAL_FINALIZE_JITTER_RADIUS_PX, TERMINAL_FINALIZE_JITTER_RADIUS_PX);
            int dy = randomIntInclusive(-TERMINAL_FINALIZE_JITTER_RADIUS_PX, TERMINAL_FINALIZE_JITTER_RADIUS_PX);
            Point candidate = new Point(base.x + dx, base.y + dy);
            if (last == null || pixelDistance(last, candidate) > 0.0) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            return null;
        }
        lastTerminalFinalizePoint = new Point(selected);
        return selected;
    }
}
