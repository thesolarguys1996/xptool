package com.xptool.executor;

import java.awt.Point;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

final class WoodcutPreClickSettlePlanner {
    private static final int PRECLICK_SETTLE_ENABLE_CHANCE_PERCENT = 86;
    private static final int PRECLICK_SETTLE_SHORT_MOVE_CHANCE_PERCENT = 68;
    private static final double PRECLICK_SETTLE_SHORT_MOVE_DISTANCE_PX = 44.0;
    private static final int PRECLICK_SETTLE_OFFSET_MIN_PX = 2;
    private static final int PRECLICK_SETTLE_OFFSET_MAX_PX = 9;
    private static final int PRECLICK_SETTLE_ANCHOR_MAX_ATTEMPTS = 8;
    private static final int PRECLICK_CORRECTION_STEPS_MIN = 4;
    private static final int PRECLICK_CORRECTION_STEPS_MAX = 7;
    private static final int PRECLICK_CORRECTION_STEPS_INITIAL_MAX = 9;

    Optional<Point> resolveApproachAnchor(Point from, Point clickTarget, boolean humanizedTimingEnabled) {
        if (!humanizedTimingEnabled || from == null || clickTarget == null) {
            return Optional.empty();
        }
        double distance = pixelDistance(from, clickTarget);
        int chancePercent = distance <= PRECLICK_SETTLE_SHORT_MOVE_DISTANCE_PX
            ? PRECLICK_SETTLE_SHORT_MOVE_CHANCE_PERCENT
            : PRECLICK_SETTLE_ENABLE_CHANCE_PERCENT;
        if (!rollChance(chancePercent)) {
            return Optional.empty();
        }
        for (int i = 0; i < PRECLICK_SETTLE_ANCHOR_MAX_ATTEMPTS; i++) {
            int radius = randomIntInclusive(PRECLICK_SETTLE_OFFSET_MIN_PX, PRECLICK_SETTLE_OFFSET_MAX_PX);
            double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
            int dx = (int) Math.round(Math.cos(angle) * radius);
            int dy = (int) Math.round(Math.sin(angle) * radius);
            if (dx == 0 && dy == 0) {
                continue;
            }
            Point anchor = new Point(clickTarget.x + dx, clickTarget.y + dy);
            if (pixelDistance(anchor, clickTarget) < PRECLICK_SETTLE_OFFSET_MIN_PX * 0.85) {
                continue;
            }
            return Optional.of(anchor);
        }
        return Optional.empty();
    }

    int resolveCorrectionSteps(boolean initialApproach, boolean humanizedTimingEnabled) {
        if (!humanizedTimingEnabled) {
            return PRECLICK_CORRECTION_STEPS_MIN;
        }
        int max = initialApproach
            ? PRECLICK_CORRECTION_STEPS_INITIAL_MAX
            : PRECLICK_CORRECTION_STEPS_MAX;
        return randomIntInclusive(PRECLICK_CORRECTION_STEPS_MIN, max);
    }

    private static boolean rollChance(int percent) {
        int bounded = Math.max(0, Math.min(100, percent));
        if (bounded <= 0) {
            return false;
        }
        if (bounded >= 100) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(100) < bounded;
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return 0.0;
        }
        double dx = (double) a.x - b.x;
        double dy = (double) a.y - b.y;
        return Math.hypot(dx, dy);
    }

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }
}
