package com.xptool.executor;

import java.awt.Point;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class IdleActionRepetitionGuard {
    private static final int REPEAT_WINDOW_TICKS = 180;
    private static final double DEFAULT_REPEAT_EXCLUSION_PX = 16.0;
    private static final double PARK_REPEAT_EXCLUSION_PX = 24.0;
    private static final double OFFSCREEN_REPEAT_EXCLUSION_PX = 56.0;

    private final Map<String, ActionSample> lastSampleByReason = new HashMap<>();

    boolean isRepeated(int tick, String reason, Point target) {
        String normalizedReason = normalizeReason(reason);
        if (normalizedReason.isEmpty() || target == null) {
            return false;
        }
        ActionSample sample = lastSampleByReason.get(normalizedReason);
        if (sample == null || sample.target == null) {
            return false;
        }
        int elapsedTicks = elapsedTicksSince(tick, sample.tick);
        if (elapsedTicks < 0 || elapsedTicks > REPEAT_WINDOW_TICKS) {
            return false;
        }
        return pixelDistance(sample.target, target) <= repeatExclusionPxForReason(normalizedReason);
    }

    void recordAction(int tick, String reason, Point target) {
        String normalizedReason = normalizeReason(reason);
        if (normalizedReason.isEmpty() || target == null) {
            return;
        }
        lastSampleByReason.put(normalizedReason, new ActionSample(tick, new Point(target)));
    }

    void reset() {
        lastSampleByReason.clear();
    }

    private static double repeatExclusionPxForReason(String reason) {
        String normalized = normalizeReason(reason);
        if (normalized.contains("offscreen")) {
            return OFFSCREEN_REPEAT_EXCLUSION_PX;
        }
        if (normalized.contains("park")) {
            return PARK_REPEAT_EXCLUSION_PX;
        }
        return DEFAULT_REPEAT_EXCLUSION_PX;
    }

    private static String normalizeReason(String reason) {
        return reason == null ? "" : reason.trim().toLowerCase(Locale.ROOT);
    }

    private static int elapsedTicksSince(int nowTick, int thenTick) {
        long elapsed = (long) nowTick - (long) thenTick;
        if (elapsed <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (elapsed >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) elapsed;
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.POSITIVE_INFINITY;
        }
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.hypot(dx, dy);
    }

    private static final class ActionSample {
        private final int tick;
        private final Point target;

        private ActionSample(int tick, Point target) {
            this.tick = tick;
            this.target = target == null ? null : new Point(target);
        }
    }
}
