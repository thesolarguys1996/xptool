package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

final class DropPointSequencePlanner {
    private static final int RECENT_USED_POINTS_LIMIT = 768;

    interface Host {
        boolean isUsableCanvasPoint(Point point);

        Point clampPointToRectangle(Point point, Rectangle bounds);
    }

    static final class Config {
        final int dropSlotTargetJitterMinPx;
        final int dropSlotTargetJitterMaxPx;
        final int dropSlotRepeatRetryAttempts;
        final int plannedPointsPerSlot;
        final int planBuildAttemptsPerPoint;
        final int planFallbackAttempts;
        final double minInterClickDistancePx;

        Config(
            int dropSlotTargetJitterMinPx,
            int dropSlotTargetJitterMaxPx,
            int dropSlotRepeatRetryAttempts,
            int plannedPointsPerSlot,
            int planBuildAttemptsPerPoint,
            int planFallbackAttempts,
            double minInterClickDistancePx
        ) {
            this.dropSlotTargetJitterMinPx = Math.max(0, dropSlotTargetJitterMinPx);
            this.dropSlotTargetJitterMaxPx = Math.max(this.dropSlotTargetJitterMinPx, dropSlotTargetJitterMaxPx);
            this.dropSlotRepeatRetryAttempts = Math.max(0, dropSlotRepeatRetryAttempts);
            this.plannedPointsPerSlot = Math.max(1, plannedPointsPerSlot);
            this.planBuildAttemptsPerPoint = Math.max(1, planBuildAttemptsPerPoint);
            this.planFallbackAttempts = Math.max(1, planFallbackAttempts);
            this.minInterClickDistancePx = Math.max(0.0, minInterClickDistancePx);
        }
    }

    private final Host host;
    private final TargetPointVariationEngine targetPointVariationEngine;
    private final Config config;
    private final Map<Integer, ArrayDeque<Point>> plannedPointsBySlot = new HashMap<>();
    private final Set<Long> recentUsedPoints = new HashSet<>();
    private final ArrayDeque<Long> recentUsedPointOrder = new ArrayDeque<>();
    private long activeSessionSerial = Long.MIN_VALUE;
    private int dropRepeatBlockedCount = 0;

    DropPointSequencePlanner(
        Host host,
        TargetPointVariationEngine targetPointVariationEngine,
        Config config
    ) {
        this.host = host;
        this.targetPointVariationEngine = targetPointVariationEngine;
        this.config = config;
    }

    void beginSession(long sessionSerial) {
        if (sessionSerial == activeSessionSerial) {
            return;
        }
        activeSessionSerial = sessionSerial;
        plannedPointsBySlot.clear();
    }

    void endSession() {
        activeSessionSerial = Long.MIN_VALUE;
        plannedPointsBySlot.clear();
    }

    void resetUsedPointHistory() {
        recentUsedPoints.clear();
        recentUsedPointOrder.clear();
    }

    void notePointDispatched(long sessionSerial, Point point) {
        if (point == null || !host.isUsableCanvasPoint(point)) {
            return;
        }
        beginSession(sessionSerial);
        rememberUsedPoint(encodePoint(point));
    }

    Optional<Point> nextPoint(
        long sessionSerial,
        int slot,
        Point anchor,
        Rectangle slotBounds,
        Point previousClickCanvasPoint
    ) {
        if (slot < 0 || slot >= 28 || !host.isUsableCanvasPoint(anchor)) {
            return Optional.empty();
        }
        beginSession(sessionSerial);

        Point normalizedAnchor = normalizeAnchor(anchor, slotBounds);
        Point previous = host.isUsableCanvasPoint(previousClickCanvasPoint)
            ? new Point(previousClickCanvasPoint)
            : null;
        ProximityRejectionTracker tracker = new ProximityRejectionTracker();

        ArrayDeque<Point> plan = plannedPointsBySlot.computeIfAbsent(
            slot,
            ignored -> buildPlan(sessionSerial, slot, normalizedAnchor, slotBounds, previous, tracker)
        );
        Point planned = nextPlannedPoint(plan, previous, tracker);
        if (planned != null) {
            return Optional.of(planned);
        }

        plan.clear();
        plan.addAll(buildPlan(sessionSerial, slot, normalizedAnchor, slotBounds, previous, tracker));
        planned = nextPlannedPoint(plan, previous, tracker);
        if (planned == null) {
            if (tracker.proximityRejected) {
                noteDropRepeatBlocked();
            }
            return Optional.empty();
        }
        return Optional.of(planned);
    }

    int consumeDropRepeatBlockedCount() {
        int blocked = dropRepeatBlockedCount;
        dropRepeatBlockedCount = 0;
        return blocked;
    }

    private ArrayDeque<Point> buildPlan(
        long sessionSerial,
        int slot,
        Point anchor,
        Rectangle slotBounds,
        Point previousClickCanvasPoint,
        ProximityRejectionTracker tracker
    ) {
        ArrayDeque<Point> out = new ArrayDeque<>();
        Point prior = previousClickCanvasPoint == null ? null : new Point(previousClickCanvasPoint);
        for (int i = 0; i < config.plannedPointsPerSlot; i++) {
            Point candidate = resolvePlannedPoint(sessionSerial, slot, anchor, slotBounds, prior, out, tracker);
            if (candidate == null) {
                break;
            }
            out.addLast(candidate);
            prior = candidate;
        }
        if (out.isEmpty() && isFarEnough(anchor, previousClickCanvasPoint) && host.isUsableCanvasPoint(anchor)) {
            if (isUnused(anchor)) {
                out.addLast(new Point(anchor));
            } else {
                tracker.proximityRejected = true;
            }
        }
        return out;
    }

    private Point resolvePlannedPoint(
        long sessionSerial,
        int slot,
        Point anchor,
        Rectangle slotBounds,
        Point previousReference,
        ArrayDeque<Point> alreadyPlanned,
        ProximityRejectionTracker tracker
    ) {
        for (int attempt = 0; attempt < config.planBuildAttemptsPerPoint; attempt++) {
            Point varied = targetPointVariationEngine.varyDropSlotPoint(
                sessionSerial,
                slot,
                anchor,
                slotBounds,
                previousReference,
                config.dropSlotTargetJitterMinPx,
                config.dropSlotTargetJitterMaxPx,
                config.dropSlotRepeatRetryAttempts
            );
            Point candidate = host.isUsableCanvasPoint(varied) ? new Point(varied) : new Point(anchor);
            if (!host.isUsableCanvasPoint(candidate)) {
                continue;
            }
            if (!isFarEnough(candidate, previousReference)) {
                tracker.proximityRejected = true;
                continue;
            }
            if (!isFarEnoughFromPlan(candidate, alreadyPlanned)) {
                tracker.proximityRejected = true;
                continue;
            }
            if (!isUnused(candidate)) {
                tracker.proximityRejected = true;
                continue;
            }
            return candidate;
        }
        return resolveFallbackPoint(anchor, slotBounds, previousReference, alreadyPlanned, tracker);
    }

    private Point resolveFallbackPoint(
        Point anchor,
        Rectangle slotBounds,
        Point previousReference,
        ArrayDeque<Point> alreadyPlanned,
        ProximityRejectionTracker tracker
    ) {
        for (int attempt = 0; attempt < config.planFallbackAttempts; attempt++) {
            Point candidate = slotBounds == null
                ? randomPointNear(anchor, config.dropSlotTargetJitterMaxPx + 3)
                : randomPointInBounds(slotBounds);
            if (!host.isUsableCanvasPoint(candidate)) {
                continue;
            }
            if (!isFarEnough(candidate, previousReference)) {
                tracker.proximityRejected = true;
                continue;
            }
            if (!isFarEnoughFromPlan(candidate, alreadyPlanned)) {
                tracker.proximityRejected = true;
                continue;
            }
            if (!isUnused(candidate)) {
                tracker.proximityRejected = true;
                continue;
            }
            return candidate;
        }
        if (!isFarEnough(anchor, previousReference)) {
            tracker.proximityRejected = true;
            return null;
        }
        if (!isFarEnoughFromPlan(anchor, alreadyPlanned)) {
            tracker.proximityRejected = true;
            return null;
        }
        if (!isUnused(anchor)) {
            tracker.proximityRejected = true;
            return null;
        }
        return host.isUsableCanvasPoint(anchor) ? new Point(anchor) : null;
    }

    private Point nextPlannedPoint(
        ArrayDeque<Point> plan,
        Point previousClickCanvasPoint,
        ProximityRejectionTracker tracker
    ) {
        while (plan != null && !plan.isEmpty()) {
            Point candidate = plan.removeFirst();
            if (!host.isUsableCanvasPoint(candidate)) {
                continue;
            }
            if (!isFarEnough(candidate, previousClickCanvasPoint)) {
                tracker.proximityRejected = true;
                continue;
            }
            if (!isUnused(candidate)) {
                tracker.proximityRejected = true;
                continue;
            }
            return new Point(candidate);
        }
        return null;
    }

    private Point normalizeAnchor(Point anchor, Rectangle bounds) {
        if (bounds == null) {
            return new Point(anchor);
        }
        return host.clampPointToRectangle(new Point(anchor), bounds);
    }

    private boolean isFarEnoughFromPlan(Point candidate, ArrayDeque<Point> planned) {
        if (candidate == null || planned == null || planned.isEmpty()) {
            return true;
        }
        for (Point prior : planned) {
            if (!isFarEnough(candidate, prior)) {
                return false;
            }
        }
        return true;
    }

    private boolean isFarEnough(Point left, Point right) {
        if (left == null || right == null) {
            return true;
        }
        return pixelDistance(left, right) >= config.minInterClickDistancePx;
    }

    private static double pixelDistance(Point a, Point b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.hypot(dx, dy);
    }

    private static Point randomPointNear(Point anchor, int radiusPx) {
        int radius = Math.max(0, radiusPx);
        int x = anchor.x + randomIntInclusive(-radius, radius);
        int y = anchor.y + randomIntInclusive(-radius, radius);
        return new Point(x, y);
    }

    private static Point randomPointInBounds(Rectangle bounds) {
        int minX = bounds.x;
        int maxX = bounds.x + bounds.width - 1;
        int minY = bounds.y;
        int maxY = bounds.y + bounds.height - 1;
        return new Point(
            randomIntInclusive(minX, maxX),
            randomIntInclusive(minY, maxY)
        );
    }

    private static int randomIntInclusive(int min, int max) {
        if (min >= max) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private boolean isUnused(Point point) {
        if (point == null) {
            return false;
        }
        return !recentUsedPoints.contains(encodePoint(point));
    }

    private static long encodePoint(Point point) {
        long x = (long) point.x;
        long y = (long) point.y & 0xffffffffL;
        return (x << 32) ^ y;
    }

    private void rememberUsedPoint(long encodedPoint) {
        if (recentUsedPoints.add(encodedPoint)) {
            recentUsedPointOrder.addLast(encodedPoint);
        }
        while (recentUsedPointOrder.size() > RECENT_USED_POINTS_LIMIT) {
            Long oldest = recentUsedPointOrder.removeFirst();
            if (oldest != null) {
                recentUsedPoints.remove(oldest);
            }
        }
    }

    private void noteDropRepeatBlocked() {
        if (dropRepeatBlockedCount < Integer.MAX_VALUE) {
            dropRepeatBlockedCount++;
        }
    }

    private static final class ProximityRejectionTracker {
        private boolean proximityRejected = false;
    }
}
