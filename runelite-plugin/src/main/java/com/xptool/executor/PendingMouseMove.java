package com.xptool.executor;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class PendingMouseMove {
    private static final String OWNER_IDLE = "idle";
    private static final double TERMINAL_ALIGNMENT_TOLERANCE_PX = 1.4;
    private static final int TERMINAL_FINALIZE_JITTER_RADIUS_PX = 1;
    private static final double IDLE_S_CURVE_SPLIT_MIN = 0.30;
    private static final double IDLE_S_CURVE_SPLIT_MAX = 0.72;
    private static final double IDLE_ARC_SWEEP_SPLIT_MIN = 0.58;
    private static final double IDLE_ARC_SWEEP_SPLIT_MAX = 0.84;
    private static final int IDLE_ARC_SWEEP_CHANCE_PERCENT = 42;
    private static final int IDLE_SAME_SIGN_CURVE_CHANCE_PERCENT = 33;
    private static final int IDLE_WIDE_CURVE_CHANCE_PERCENT = 22;
    private static final double IDLE_LONG_MOVE_DISTANCE_PX = 420.0;
    private static final double IDLE_EXTRA_LONG_MOVE_DISTANCE_PX = 860.0;
    private static Point lastTerminalFinalizePoint;
    final Robot robot;
    final Point from;
    final Point to;
    final Point committedTargetCanvasPoint;
    final String owner;
    final Point control;
    final List<Point> scriptedPathScreen;
    final int totalSteps;
    int stepIndex;
    int nextAllowedTick;
    int ticksAlive;
    int lastObservedTick;

    PendingMouseMove(
        Robot robot,
        Point from,
        Point to,
        Point committedTargetCanvasPoint,
        String owner,
        int totalSteps,
        int nextAllowedTick
    ) {
        this.robot = robot;
        this.from = from == null ? to : from;
        this.to = to;
        this.committedTargetCanvasPoint = committedTargetCanvasPoint == null ? null : new Point(committedTargetCanvasPoint);
        this.owner = owner == null ? "" : owner;
        this.control = resolveControlPoint(this.from, to, this.owner);
        List<Point> scripted = resolveScriptedPath(this.from, this.to, this.owner, totalSteps);
        this.scriptedPathScreen = scripted;
        this.totalSteps = scripted == null || scripted.isEmpty()
            ? Math.max(1, totalSteps)
            : scripted.size();
        this.nextAllowedTick = nextAllowedTick;
        this.stepIndex = 0;
        this.ticksAlive = 0;
        this.lastObservedTick = Integer.MIN_VALUE;
    }

    boolean matchesTarget(Point target) {
        return target != null
            && to != null
            && CommandExecutor.pixelDistance(target, to)
                <= ExecutorEngineConfigCatalog.PENDING_MOVE_TARGET_MATCH_TOLERANCE_PX;
    }

    void advanceOneStep() {
        if (robot == null || to == null) {
            stepIndex = totalSteps;
            return;
        }
        if (stepIndex >= totalSteps) {
            Point pointer = currentPointerLocation();
            Point start = pointer == null ? from : pointer;
            if (start != null && CommandExecutor.pixelDistance(start, to) > TERMINAL_ALIGNMENT_TOLERANCE_PX) {
                applyTerminalMicroCorrection(start, to);
            }
            return;
        }
        stepIndex++;
        Point p;
        if (scriptedPathScreen != null && !scriptedPathScreen.isEmpty()) {
            int pathIndex = Math.max(0, Math.min(scriptedPathScreen.size() - 1, stepIndex - 1));
            p = scriptedPathScreen.get(pathIndex);
        } else {
            double t = (double) stepIndex / (double) totalSteps;
            double progress = CommandExecutor.smoothstep(t);
            p = CommandExecutor.quadraticBezier(from, control, to, progress);
        }
        robot.mouseMove(p.x, p.y);
    }

    boolean complete(Point currentPointer, double arrivalTolerancePx) {
        if (to == null) {
            return true;
        }
        if (CommandExecutor.pixelDistance(currentPointer, to) <= Math.max(0.0, arrivalTolerancePx)) {
            return true;
        }
        // Do not mark complete from step count alone; require observed arrival.
        // Otherwise we can report "complete" even when OS cursor motion did not land.
        return false;
    }

    void observeTick(int tick) {
        if (tick == Integer.MIN_VALUE || tick == lastObservedTick) {
            return;
        }
        if (lastObservedTick == Integer.MIN_VALUE || tick > lastObservedTick) {
            ticksAlive++;
            lastObservedTick = tick;
        }
    }

    private static Point resolveControlPoint(Point from, Point to, String owner) {
        if (from == null || to == null) {
            return null;
        }
        if (OWNER_IDLE.equals(owner)) {
            Point control = ExecutorCursorMotion.idleCurveControlPoint(from, to);
            if (control != null) {
                return control;
            }
        }
        return CommandExecutor.curveControlPoint(from, to);
    }

    private static List<Point> resolveScriptedPath(Point from, Point to, String owner, int baseSteps) {
        if (!OWNER_IDLE.equals(owner) || from == null || to == null) {
            return null;
        }
        double distancePx = CommandExecutor.pixelDistance(from, to);
        int total = Math.max(6, baseSteps + randomIntInclusive(0, Math.max(2, baseSteps / 3)));
        boolean arcSweep = distancePx >= 120.0
            && ThreadLocalRandom.current().nextInt(100) < IDLE_ARC_SWEEP_CHANCE_PERCENT;
        double splitT = arcSweep
            ? ThreadLocalRandom.current().nextDouble(IDLE_ARC_SWEEP_SPLIT_MIN, IDLE_ARC_SWEEP_SPLIT_MAX)
            : ThreadLocalRandom.current().nextDouble(IDLE_S_CURVE_SPLIT_MIN, IDLE_S_CURVE_SPLIT_MAX);
        Point split = ExecutorCursorMotion.linearInterpolate(from, to, splitT);
        int sign = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        boolean sameSign = ThreadLocalRandom.current().nextInt(100) < IDLE_SAME_SIGN_CURVE_CHANCE_PERCENT;
        int secondSign = sameSign ? sign : -sign;
        IdleCurveEnvelope envelope = sampleIdleCurveEnvelope(distancePx, arcSweep);
        Point c1 = curveControlPoint(
            from,
            split,
            envelope.leadScale,
            envelope.leadMinPx,
            envelope.leadMaxPx,
            sign
        );
        Point c2 = curveControlPoint(
            split,
            to,
            envelope.settleScale,
            envelope.settleMinPx,
            envelope.settleMaxPx,
            secondSign
        );
        int controlJitterPx = resolveIdleControlJitterRadius(distancePx);
        c1 = jitterControlPoint(c1, controlJitterPx);
        c2 = jitterControlPoint(c2, controlJitterPx);

        int leadPercent = arcSweep
            ? randomIntInclusive(56, 82)
            : randomIntInclusive(44, 68);
        int leadSteps = Math.max(2, (int) Math.round((double) total * ((double) leadPercent / 100.0)));
        int settleSteps = Math.max(2, total - leadSteps);
        List<Point> path = new ArrayList<>(total + 2);
        appendBezierSegmentPoints(path, from, c1, split, leadSteps);
        appendBezierSegmentPoints(path, split, c2, to, settleSteps);
        if (path.isEmpty()) {
            path.add(new Point(to));
        } else {
            Point last = path.get(path.size() - 1);
            if (last.x != to.x || last.y != to.y) {
                path.add(new Point(to));
            }
        }
        return path;
    }

    private static IdleCurveEnvelope sampleIdleCurveEnvelope(double distancePx, boolean arcSweep) {
        double longFactor = clamp01(distancePx / IDLE_LONG_MOVE_DISTANCE_PX);
        double extraLongFactor = clamp01(distancePx / IDLE_EXTRA_LONG_MOVE_DISTANCE_PX);
        double leadScale = lerp(0.14, 0.30, longFactor);
        double settleScale = lerp(0.10, 0.22, longFactor);
        double leadMinPx = lerp(4.0, 8.0, extraLongFactor);
        double settleMinPx = lerp(3.0, 6.0, extraLongFactor);
        double leadMaxPx = lerp(22.0, 130.0, longFactor);
        double settleMaxPx = lerp(16.0, 88.0, longFactor);
        if (arcSweep) {
            leadScale *= 1.16;
            settleScale *= 0.92;
            leadMaxPx *= 1.14;
        }
        if (ThreadLocalRandom.current().nextInt(100) < IDLE_WIDE_CURVE_CHANCE_PERCENT) {
            leadScale *= 1.12;
            settleScale *= 1.08;
            leadMaxPx *= 1.20;
            settleMaxPx *= 1.15;
        }
        leadScale = clampDouble(leadScale, 0.10, 0.46);
        settleScale = clampDouble(settleScale, 0.08, 0.36);
        leadMaxPx = clampDouble(leadMaxPx, 14.0, 180.0);
        settleMaxPx = clampDouble(settleMaxPx, 12.0, 130.0);
        return new IdleCurveEnvelope(
            leadScale,
            leadMinPx,
            leadMaxPx,
            settleScale,
            settleMinPx,
            settleMaxPx
        );
    }

    private static int resolveIdleControlJitterRadius(double distancePx) {
        double factor = clamp01(distancePx / IDLE_EXTRA_LONG_MOVE_DISTANCE_PX);
        int base = (int) Math.round(lerp(3.0, 8.0, factor));
        int min = Math.max(2, base - 1);
        int max = Math.max(min, base + 2);
        return randomIntInclusive(min, max);
    }

    private static void appendBezierSegmentPoints(List<Point> out, Point from, Point control, Point to, int steps) {
        if (out == null || from == null || to == null) {
            return;
        }
        int total = Math.max(1, steps);
        Point c = control == null ? CommandExecutor.curveControlPoint(from, to) : control;
        for (int i = 1; i <= total; i++) {
            double t = (double) i / (double) total;
            double progress = CommandExecutor.smoothstep(t);
            Point p = CommandExecutor.quadraticBezier(from, c, to, progress);
            if (out.isEmpty()) {
                out.add(p);
                continue;
            }
            Point last = out.get(out.size() - 1);
            if (last.x != p.x || last.y != p.y) {
                out.add(p);
            }
        }
    }

    private static Point jitterControlPoint(Point control, int radiusPx) {
        if (control == null || radiusPx <= 0) {
            return control;
        }
        int dx = ThreadLocalRandom.current().nextInt(-radiusPx, radiusPx + 1);
        int dy = ThreadLocalRandom.current().nextInt(-radiusPx, radiusPx + 1);
        return new Point(control.x + dx, control.y + dy);
    }

    private static Point curveControlPoint(
        Point from,
        Point to,
        double curveScale,
        double curveMinPx,
        double curveMaxPx,
        int preferredSign
    ) {
        if (from == null || to == null) {
            return null;
        }
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

    private static Point currentPointerLocation() {
        PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer == null) {
            return null;
        }
        return pointer.getLocation();
    }

    private void applyTerminalMicroCorrection(Point start, Point target) {
        if (robot == null || start == null || target == null) {
            return;
        }
        Point finalTarget = resolveTerminalFinalizeTarget(target);
        if (finalTarget == null) {
            return;
        }
        Point current = new Point(start);
        for (int attempt = 0; attempt < 2; attempt++) {
            int steps = CommandExecutor.pixelDistance(current, finalTarget) >= 3.0 ? 3 : 2;
            Point control = resolveControlPoint(current, finalTarget, owner);
            Point last = current;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / (double) steps;
                Point p = CommandExecutor.quadraticBezier(current, control, finalTarget, CommandExecutor.smoothstep(t));
                if (p.x != last.x || p.y != last.y) {
                    robot.mouseMove(p.x, p.y);
                    last = p;
                }
            }
            Point pointer = currentPointerLocation();
            current = pointer == null ? last : pointer;
            if (CommandExecutor.pixelDistance(current, finalTarget) <= TERMINAL_ALIGNMENT_TOLERANCE_PX) {
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
            int dx = ThreadLocalRandom.current().nextInt(
                -TERMINAL_FINALIZE_JITTER_RADIUS_PX,
                TERMINAL_FINALIZE_JITTER_RADIUS_PX + 1
            );
            int dy = ThreadLocalRandom.current().nextInt(
                -TERMINAL_FINALIZE_JITTER_RADIUS_PX,
                TERMINAL_FINALIZE_JITTER_RADIUS_PX + 1
            );
            Point candidate = new Point(base.x + dx, base.y + dy);
            if (last == null || CommandExecutor.pixelDistance(last, candidate) > 0.0) {
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

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private static double lerp(double a, double b, double t) {
        double clamped = clamp01(t);
        return a + ((b - a) * clamped);
    }

    private static double clamp01(double value) {
        return clampDouble(value, 0.0, 1.0);
    }

    private static double clampDouble(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static final class IdleCurveEnvelope {
        private final double leadScale;
        private final double leadMinPx;
        private final double leadMaxPx;
        private final double settleScale;
        private final double settleMinPx;
        private final double settleMaxPx;

        private IdleCurveEnvelope(
            double leadScale,
            double leadMinPx,
            double leadMaxPx,
            double settleScale,
            double settleMinPx,
            double settleMaxPx
        ) {
            this.leadScale = leadScale;
            this.leadMinPx = leadMinPx;
            this.leadMaxPx = leadMaxPx;
            this.settleScale = settleScale;
            this.settleMinPx = settleMinPx;
            this.settleMaxPx = settleMaxPx;
        }
    }
}
