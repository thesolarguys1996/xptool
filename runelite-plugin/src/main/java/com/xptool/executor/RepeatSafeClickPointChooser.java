package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

final class RepeatSafeClickPointChooser {
    private RepeatSafeClickPointChooser() {
    }

    static Point randomPointInBoundsAvoiding(
        Rectangle bounds,
        Predicate<Point> isUsableCanvasPoint,
        Point avoidPoint,
        double minSeparationPx,
        int insetPx,
        int attempts,
        Point fallback
    ) {
        if (isUsableCanvasPoint == null) {
            return null;
        }
        Rectangle sampledBounds = insetBounds(normalizeBounds(bounds), insetPx);
        if (!hasArea(sampledBounds)) {
            return null;
        }

        int tryCount = Math.max(1, attempts);
        double minDistance = Math.max(0.0, minSeparationPx);
        for (int i = 0; i < tryCount; i++) {
            Point candidate = randomPoint(sampledBounds);
            if (!isUsableCanvasPoint.test(candidate)) {
                continue;
            }
            if (isNear(candidate, avoidPoint, minDistance)) {
                continue;
            }
            return candidate;
        }

        Point fallbackPoint = usableOrNull(fallback, isUsableCanvasPoint);
        int exploratoryAttempts = Math.max(12, tryCount * 2);
        for (int i = 0; i < exploratoryAttempts; i++) {
            Point candidate = fallbackPoint == null
                ? randomPoint(sampledBounds)
                : jitterNearSeed(fallbackPoint, sampledBounds, Math.max(3, insetPx + 2));
            if (!isUsableCanvasPoint.test(candidate)) {
                continue;
            }
            if (isNear(candidate, avoidPoint, minDistance)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static Point randomPoint(Rectangle bounds) {
        int minX = bounds.x;
        int maxX = bounds.x + Math.max(0, bounds.width - 1);
        int minY = bounds.y;
        int maxY = bounds.y + Math.max(0, bounds.height - 1);
        return new Point(
            randomIntInclusive(minX, maxX),
            randomIntInclusive(minY, maxY)
        );
    }

    private static Point jitterNearSeed(Point seed, Rectangle bounds, int minRadiusPx) {
        if (seed == null || !hasArea(bounds)) {
            return null;
        }
        Point clampedSeed = clampToBounds(seed, bounds);
        int radiusCap = Math.max(minRadiusPx, Math.min(bounds.width, bounds.height) / 3);
        int radius = randomIntInclusive(Math.max(1, minRadiusPx), Math.max(1, radiusCap));
        int dx = randomIntInclusive(-radius, radius);
        int dy = randomIntInclusive(-radius, radius);
        return clampToBounds(new Point(clampedSeed.x + dx, clampedSeed.y + dy), bounds);
    }

    private static Point clampToBounds(Point point, Rectangle bounds) {
        if (point == null) {
            return randomPoint(bounds);
        }
        int minX = bounds.x;
        int maxX = bounds.x + Math.max(0, bounds.width - 1);
        int minY = bounds.y;
        int maxY = bounds.y + Math.max(0, bounds.height - 1);
        int x = Math.max(minX, Math.min(maxX, point.x));
        int y = Math.max(minY, Math.min(maxY, point.y));
        return new Point(x, y);
    }

    private static Rectangle normalizeBounds(Rectangle bounds) {
        if (bounds == null) {
            return new Rectangle();
        }
        int width = Math.max(0, bounds.width);
        int height = Math.max(0, bounds.height);
        return new Rectangle(bounds.x, bounds.y, width, height);
    }

    private static Rectangle insetBounds(Rectangle bounds, int insetPx) {
        if (!hasArea(bounds)) {
            return new Rectangle();
        }
        int inset = Math.max(0, insetPx);
        int x = bounds.x + inset;
        int y = bounds.y + inset;
        int width = bounds.width - (inset * 2);
        int height = bounds.height - (inset * 2);
        if (width <= 0 || height <= 0) {
            return new Rectangle(bounds);
        }
        return new Rectangle(x, y, width, height);
    }

    private static boolean hasArea(Rectangle bounds) {
        return bounds != null && bounds.width > 0 && bounds.height > 0;
    }

    private static Point usableOrNull(Point point, Predicate<Point> isUsableCanvasPoint) {
        if (point == null || isUsableCanvasPoint == null) {
            return null;
        }
        return isUsableCanvasPoint.test(point) ? new Point(point) : null;
    }

    private static boolean isNear(Point a, Point b, double radiusPx) {
        if (a == null || b == null) {
            return false;
        }
        if (radiusPx <= 0.0) {
            return samePoint(a, b);
        }
        return pixelDistance(a, b) <= radiusPx;
    }

    private static boolean samePoint(Point a, Point b) {
        if (a == null || b == null) {
            return false;
        }
        return a.x == b.x && a.y == b.y;
    }

    private static double pixelDistance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.POSITIVE_INFINITY;
        }
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.hypot(dx, dy);
    }

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }
}
