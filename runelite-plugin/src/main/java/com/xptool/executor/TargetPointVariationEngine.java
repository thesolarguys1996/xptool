package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

final class TargetPointVariationEngine {
    private static final String WOODCUT_HOVER_KEY_PREFIX = "woodcut_hover:";
    private static final double WOODCUT_HOVER_EXTRA_EXCLUSION_PX = 1.5;
    private static final int WOODCUT_HOVER_MIN_BOUNDS_AREA_FOR_EXPLORATION = 36;
    private static final int WOODCUT_HOVER_MIN_EXPLORATION_ATTEMPTS = 14;

    interface Host {
        boolean isUsableCanvasPoint(Point point);

        int canvasWidth();

        int canvasHeight();
    }

    static final class Config {
        final boolean enabled;
        final int historyPerTarget;
        final int maxTrackedTargets;
        final int maxAttempts;
        final int relaxedAttempts;
        final int exhaustiveScanMaxArea;
        final double repeatExclusionPx;

        Config(
            boolean enabled,
            int historyPerTarget,
            int maxTrackedTargets,
            int maxAttempts,
            int relaxedAttempts,
            int exhaustiveScanMaxArea,
            double repeatExclusionPx
        ) {
            this.enabled = enabled;
            this.historyPerTarget = Math.max(1, historyPerTarget);
            this.maxTrackedTargets = Math.max(1, maxTrackedTargets);
            this.maxAttempts = Math.max(1, maxAttempts);
            this.relaxedAttempts = Math.max(1, relaxedAttempts);
            this.exhaustiveScanMaxArea = Math.max(1, exhaustiveScanMaxArea);
            this.repeatExclusionPx = Math.max(0.0, repeatExclusionPx);
        }
    }

    private final Host host;
    private final Config config;
    private final Map<String, Deque<Point>> recentClickPointsByTargetKey = new HashMap<>();
    private long targetVariationSamples = 0L;
    private long targetVariationFallbacks = 0L;

    TargetPointVariationEngine(Host host, Config config) {
        this.host = host == null
            ? new Host() {
                @Override
                public boolean isUsableCanvasPoint(Point point) {
                    return false;
                }

                @Override
                public int canvasWidth() {
                    return 0;
                }

                @Override
                public int canvasHeight() {
                    return 0;
                }
            }
            : host;
        this.config = config == null
            ? new Config(true, 96, 512, 12, 28, 2048, 2.0)
            : config;
    }

    boolean isEnabled() {
        return config.enabled;
    }

    long sampleCount() {
        return targetVariationSamples;
    }

    long fallbackCount() {
        return targetVariationFallbacks;
    }

    void resetTelemetryCounters() {
        targetVariationSamples = 0L;
        targetVariationFallbacks = 0L;
    }

    Point varyForNpc(NPC npc, Point basePoint, String family, int minRadiusPx, int maxRadiusPx) {
        if (basePoint == null) {
            return null;
        }
        Rectangle bounds = null;
        if (npc != null) {
            try {
                Shape hull = npc.getConvexHull();
                bounds = hull == null ? null : hull.getBounds();
            } catch (Exception ignored) {
                bounds = null;
            }
        }
        Point varied = varyForTarget(keyForNpc(npc, family), basePoint, bounds, minRadiusPx, maxRadiusPx);
        return host.isUsableCanvasPoint(varied) ? varied : null;
    }

    Point varyForTileObject(TileObject targetObject, Point basePoint, String family, int minRadiusPx, int maxRadiusPx) {
        Rectangle bounds = resolveTileObjectClickBounds(targetObject);
        return varyForTileObjectInBounds(targetObject, basePoint, bounds, family, minRadiusPx, maxRadiusPx);
    }

    Point varyForTileObjectInBounds(
        TileObject targetObject,
        Point basePoint,
        Rectangle localBounds,
        String family,
        int minRadiusPx,
        int maxRadiusPx
    ) {
        if (basePoint == null) {
            return null;
        }
        Point varied = varyForTarget(keyForTileObject(targetObject, family), basePoint, localBounds, minRadiusPx, maxRadiusPx);
        return host.isUsableCanvasPoint(varied) ? varied : null;
    }

    Point varyDropSlotPoint(
        long sessionSerial,
        int slot,
        Point anchor,
        Rectangle slotBounds,
        Point previousClickCanvasPoint,
        int minRadiusPx,
        int maxRadiusPx,
        int repeatRetryAttempts
    ) {
        Point varied = varyForTarget(
            "drop_slot_hover:" + sessionSerial + ":" + slot,
            anchor,
            slotBounds,
            minRadiusPx,
            maxRadiusPx
        );
        return ensureNotExactPrevious(
            varied,
            anchor,
            slotBounds,
            previousClickCanvasPoint,
            minRadiusPx,
            maxRadiusPx,
            repeatRetryAttempts
        );
    }

    Point varyForTarget(
        String targetKey,
        Point basePoint,
        Rectangle localBounds,
        int minRadiusPx,
        int maxRadiusPx
    ) {
        if (basePoint == null || !host.isUsableCanvasPoint(basePoint)) {
            return basePoint;
        }
        if (!config.enabled) {
            return basePoint;
        }
        String key = safeString(targetKey).trim();
        if (key.isEmpty()) {
            key = "target:unknown";
        }
        boolean woodcutHoverTarget = key.startsWith(WOODCUT_HOVER_KEY_PREFIX);
        int minRadius = Math.max(0, minRadiusPx);
        int maxRadius = Math.max(minRadius, maxRadiusPx);
        if (maxRadius <= 0) {
            rememberTargetPoint(key, basePoint);
            return basePoint;
        }
        Rectangle variationBounds = resolveVariationBounds(basePoint, localBounds, maxRadius);
        if (variationBounds == null || variationBounds.width <= 0 || variationBounds.height <= 0) {
            rememberTargetPoint(key, basePoint);
            return basePoint;
        }
        Point fallback = clampPointToRectangle(basePoint, variationBounds);
        if (!host.isUsableCanvasPoint(fallback)) {
            fallback = basePoint;
        }
        Deque<Point> recent = recentClickPointsByTargetKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        targetVariationSamples++;
        double repeatExclusionPx = woodcutHoverTarget
            ? (config.repeatExclusionPx + WOODCUT_HOVER_EXTRA_EXCLUSION_PX)
            : config.repeatExclusionPx;
        for (int i = 0; i < config.maxAttempts; i++) {
            int radius = randomIntInclusive(minRadius, maxRadius);
            Point candidate = randomPointNearBaseWithinBounds(fallback, variationBounds, radius);
            if (!host.isUsableCanvasPoint(candidate)) {
                continue;
            }
            if (isNearRecentPoint(candidate, recent, repeatExclusionPx)) {
                continue;
            }
            rememberTargetPoint(key, candidate);
            return candidate;
        }
        int variationArea = Math.max(1, variationBounds.width * variationBounds.height);
        if (woodcutHoverTarget && variationArea >= WOODCUT_HOVER_MIN_BOUNDS_AREA_FOR_EXPLORATION) {
            int explorationAttempts = Math.max(WOODCUT_HOVER_MIN_EXPLORATION_ATTEMPTS, config.maxAttempts);
            Point exploratory = sampleNonRepeatedPointInBounds(
                variationBounds,
                recent,
                repeatExclusionPx,
                explorationAttempts
            );
            if (exploratory != null) {
                rememberTargetPoint(key, exploratory);
                return exploratory;
            }
        }
        for (int i = 0; i < config.relaxedAttempts; i++) {
            int radius = randomIntInclusive(minRadius, maxRadius);
            Point candidate = randomPointNearBaseWithinBounds(fallback, variationBounds, radius);
            if (!host.isUsableCanvasPoint(candidate)) {
                continue;
            }
            if (isExactRecentPoint(candidate, recent)) {
                continue;
            }
            rememberTargetPoint(key, candidate);
            return candidate;
        }
        targetVariationFallbacks++;
        return null;
    }

    private Point ensureNotExactPrevious(
        Point candidate,
        Point anchor,
        Rectangle localBounds,
        Point previousClickCanvasPoint,
        int minRadiusPx,
        int maxRadiusPx,
        int repeatRetryAttempts
    ) {
        if (!host.isUsableCanvasPoint(candidate)) {
            return candidate;
        }
        Point previous = host.isUsableCanvasPoint(previousClickCanvasPoint)
            ? new Point(previousClickCanvasPoint)
            : null;
        if (previous == null || !previous.equals(candidate)) {
            return candidate;
        }
        Point base = host.isUsableCanvasPoint(anchor) ? new Point(anchor) : new Point(candidate);
        Rectangle variationBounds = resolveVariationBounds(base, localBounds, Math.max(0, maxRadiusPx) + 2);
        if (variationBounds == null || variationBounds.width <= 0 || variationBounds.height <= 0) {
            return candidate;
        }
        int attempts = Math.max(0, repeatRetryAttempts);
        for (int i = 0; i < attempts; i++) {
            int radius = randomIntInclusive(minRadiusPx, maxRadiusPx + 2);
            Point retry = randomPointNearBaseWithinBounds(base, variationBounds, radius);
            if (host.isUsableCanvasPoint(retry) && !retry.equals(previous)) {
                return retry;
            }
        }
        int exploratoryAttempts = Math.max(12, attempts * 2);
        for (int i = 0; i < exploratoryAttempts; i++) {
            Point exploratory = randomPointInBounds(variationBounds);
            if (host.isUsableCanvasPoint(exploratory) && !exploratory.equals(previous)) {
                return exploratory;
            }
        }
        return null;
    }

    private Rectangle resolveVariationBounds(Point basePoint, Rectangle localBounds, int maxRadiusPx) {
        Rectangle bounds = localBounds == null
            ? new Rectangle(
                basePoint.x - maxRadiusPx,
                basePoint.y - maxRadiusPx,
                (maxRadiusPx * 2) + 1,
                (maxRadiusPx * 2) + 1
            )
            : new Rectangle(localBounds);
        Rectangle canvasBounds = new Rectangle(
            1,
            1,
            Math.max(1, host.canvasWidth() - 2),
            Math.max(1, host.canvasHeight() - 2)
        );
        Rectangle bounded = bounds.intersection(canvasBounds);
        if (bounded.width <= 0 || bounded.height <= 0) {
            return null;
        }
        return bounded;
    }

    private Point randomPointNearBaseWithinBounds(Point basePoint, Rectangle bounds, int radiusPx) {
        int radius = Math.max(0, radiusPx);
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return basePoint;
        }
        int minX = Math.max(bounds.x, basePoint.x - radius);
        int maxX = Math.min(bounds.x + bounds.width - 1, basePoint.x + radius);
        int minY = Math.max(bounds.y, basePoint.y - radius);
        int maxY = Math.min(bounds.y + bounds.height - 1, basePoint.y + radius);
        if (minX > maxX || minY > maxY) {
            return randomPointInBounds(bounds);
        }
        return new Point(
            randomIntInclusive(minX, maxX),
            randomIntInclusive(minY, maxY)
        );
    }

    private Point sampleNonRepeatedPointInBounds(
        Rectangle bounds,
        Deque<Point> recent,
        double exclusionRadiusPx,
        int attempts
    ) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return null;
        }
        int totalAttempts = Math.max(1, attempts);
        Point bestRelaxed = null;
        for (int i = 0; i < totalAttempts; i++) {
            Point candidate = randomPointInBounds(bounds);
            if (!host.isUsableCanvasPoint(candidate)) {
                continue;
            }
            if (!isNearRecentPoint(candidate, recent, exclusionRadiusPx)) {
                return candidate;
            }
            if (bestRelaxed == null && !isExactRecentPoint(candidate, recent)) {
                bestRelaxed = candidate;
            }
        }
        return bestRelaxed;
    }

    private static Point randomPointInBounds(Rectangle bounds) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return null;
        }
        int minX = bounds.x;
        int maxX = bounds.x + bounds.width - 1;
        int minY = bounds.y;
        int maxY = bounds.y + bounds.height - 1;
        return new Point(
            randomIntInclusive(minX, maxX),
            randomIntInclusive(minY, maxY)
        );
    }

    private boolean isNearRecentPoint(Point candidate, Deque<Point> recent, double exclusionRadiusPx) {
        if (candidate == null || recent == null || recent.isEmpty()) {
            return false;
        }
        double exclusion = Math.max(0.0, exclusionRadiusPx);
        for (Point prior : recent) {
            if (prior == null) {
                continue;
            }
            if (pixelDistance(candidate, prior) <= exclusion) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExactRecentPoint(Point candidate, Deque<Point> recent) {
        if (candidate == null || recent == null || recent.isEmpty()) {
            return false;
        }
        for (Point prior : recent) {
            if (prior != null && prior.x == candidate.x && prior.y == candidate.y) {
                return true;
            }
        }
        return false;
    }

    private void rememberTargetPoint(String targetKey, Point point) {
        if (point == null) {
            return;
        }
        if (recentClickPointsByTargetKey.size() > config.maxTrackedTargets) {
            java.util.Iterator<String> it = recentClickPointsByTargetKey.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        Deque<Point> recent = recentClickPointsByTargetKey.computeIfAbsent(targetKey, k -> new ArrayDeque<>());
        recent.addLast(new Point(point));
        while (recent.size() > config.historyPerTarget) {
            recent.removeFirst();
        }
    }

    private static Rectangle resolveTileObjectClickBounds(TileObject targetObject) {
        if (targetObject == null) {
            return null;
        }
        try {
            Shape clickbox = targetObject.getClickbox();
            Rectangle bounds = clickbox == null ? null : clickbox.getBounds();
            if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                return bounds;
            }
        } catch (Exception ignored) {
            // Fall through.
        }
        return null;
    }

    private static Point clampPointToRectangle(Point point, Rectangle bounds) {
        if (point == null || bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return point;
        }
        int minX = bounds.x;
        int maxX = bounds.x + bounds.width - 1;
        int minY = bounds.y;
        int maxY = bounds.y + bounds.height - 1;
        return new Point(
            Math.max(minX, Math.min(maxX, point.x)),
            Math.max(minY, Math.min(maxY, point.y))
        );
    }

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.POSITIVE_INFINITY;
        }
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.hypot(dx, dy);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String keyForTileObject(TileObject targetObject, String family) {
        if (targetObject == null) {
            return safeString(family) + ":none";
        }
        WorldPoint wp = targetObject.getWorldLocation();
        String wpKey = wp == null
            ? "none"
            : (wp.getX() + ":" + wp.getY() + ":" + wp.getPlane());
        return safeString(family)
            + ":" + targetObject.getId()
            + ":" + wpKey;
    }

    private static String keyForNpc(NPC npc, String family) {
        if (npc == null) {
            return safeString(family) + ":none";
        }
        WorldPoint wp = npc.getWorldLocation();
        String wpKey = wp == null
            ? "none"
            : (wp.getX() + ":" + wp.getY() + ":" + wp.getPlane());
        return safeString(family)
            + ":" + npc.getId()
            + ":" + npc.getIndex()
            + ":" + wpKey;
    }
}
