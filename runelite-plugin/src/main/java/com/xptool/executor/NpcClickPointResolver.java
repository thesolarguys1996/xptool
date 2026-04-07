package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.runelite.api.NPC;

final class NpcClickPointResolver {
    private static final int SMALL_HULL_RANDOM_SAMPLE_ATTEMPTS = 12;
    private static final int PREFERRED_POINT_RANDOM_RETRY_ATTEMPTS = 8;
    private final Predicate<Point> isCanvasPointUsable;
    private final double upperFallbackYRatio;
    private final double topFallbackYRatio;
    private final int smallHullMaxWidthPx;
    private final int smallHullMaxHeightPx;
    private final int hullCandidateSearchRadiusPx;

    NpcClickPointResolver(
        Predicate<Point> isCanvasPointUsable,
        double upperFallbackYRatio,
        double topFallbackYRatio,
        int smallHullMaxWidthPx,
        int smallHullMaxHeightPx,
        int hullCandidateSearchRadiusPx
    ) {
        this.isCanvasPointUsable = isCanvasPointUsable == null ? p -> false : isCanvasPointUsable;
        this.upperFallbackYRatio = upperFallbackYRatio;
        this.topFallbackYRatio = topFallbackYRatio;
        this.smallHullMaxWidthPx = smallHullMaxWidthPx;
        this.smallHullMaxHeightPx = smallHullMaxHeightPx;
        this.hullCandidateSearchRadiusPx = Math.max(1, hullCandidateSearchRadiusPx);
    }

    Point resolve(NPC npc) {
        if (npc == null) {
            return null;
        }
        try {
            Shape hull = npc.getConvexHull();
            if (hull == null) {
                return null;
            }
            Rectangle bounds = hull.getBounds();
            if (bounds != null && bounds.width > 0 && bounds.height > 0 && isSmallHull(bounds)) {
                Point smallHullPoint = resolveSmallHullPoint(hull, bounds);
                if (smallHullPoint != null) {
                    return smallHullPoint;
                }
            }
            Point sampled = samplePointInsideShape(hull, 24);
            if (sampled != null) {
                return sampled;
            }
            if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                Point upper = new Point(
                    (int) Math.round(bounds.getCenterX()),
                    bounds.y + (int) Math.round(bounds.height * upperFallbackYRatio)
                );
                Point upperResolved = resolvePointInsideShape(hull, upper, hullCandidateSearchRadiusPx);
                if (upperResolved != null) {
                    return upperResolved;
                }
                Point top = new Point(
                    (int) Math.round(bounds.getCenterX()),
                    bounds.y + (int) Math.round(bounds.height * topFallbackYRatio)
                );
                Point topResolved = resolvePointInsideShape(hull, top, hullCandidateSearchRadiusPx);
                if (topResolved != null) {
                    return topResolved;
                }
                Point center = new Point(
                    (int) Math.round(bounds.getCenterX()),
                    (int) Math.round(bounds.getCenterY())
                );
                Point centerResolved = resolvePointInsideShape(hull, center, hullCandidateSearchRadiusPx);
                if (centerResolved != null) {
                    return centerResolved;
                }
            }
        } catch (Exception ignored) {
            // Fallback below.
        }
        return null;
    }

    private Point samplePointInsideShape(Shape shape, int maxAttempts) {
        if (shape == null) {
            return null;
        }
        Rectangle b = shape.getBounds();
        if (b == null || b.width <= 1 || b.height <= 1) {
            return null;
        }
        int attempts = Math.max(6, maxAttempts);
        int insetX = Math.max(1, (int) Math.round(b.width * 0.18));
        int insetY = Math.max(1, (int) Math.round(b.height * 0.18));
        int innerMinX = b.x + insetX;
        int innerMaxX = b.x + b.width - insetX;
        int innerMinY = b.y + insetY;
        int innerMaxY = b.y + b.height - insetY;
        if (innerMinX < innerMaxX && innerMinY < innerMaxY) {
            int innerAttempts = Math.max(4, attempts / 3);
            for (int i = 0; i < innerAttempts; i++) {
                int x = ThreadLocalRandom.current().nextInt(innerMinX, innerMaxX);
                int y = ThreadLocalRandom.current().nextInt(innerMinY, innerMaxY);
                if (!shape.contains(x, y)) {
                    continue;
                }
                Point candidate = new Point(x, y);
                if (isCanvasPointUsable.test(candidate)) {
                    return candidate;
                }
            }
        }
        for (int i = 0; i < attempts; i++) {
            int x = ThreadLocalRandom.current().nextInt(b.x, b.x + b.width);
            int y = ThreadLocalRandom.current().nextInt(b.y, b.y + b.height);
            if (!shape.contains(x, y)) {
                continue;
            }
            Point candidate = new Point(x, y);
            if (isCanvasPointUsable.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSmallHull(Rectangle bounds) {
        if (bounds == null) {
            return false;
        }
        return bounds.width <= smallHullMaxWidthPx && bounds.height <= smallHullMaxHeightPx;
    }

    private Point resolveSmallHullPoint(Shape hull, Rectangle bounds) {
        if (hull == null || bounds == null) {
            return null;
        }
        Point sampled = sampleSmallHullPoint(hull, bounds, SMALL_HULL_RANDOM_SAMPLE_ATTEMPTS);
        if (sampled != null) {
            return sampled;
        }
        Point upperCenter = new Point(
            (int) Math.round(bounds.getCenterX()),
            bounds.y + (int) Math.round(bounds.height * upperFallbackYRatio)
        );
        Point topCenter = new Point(
            (int) Math.round(bounds.getCenterX()),
            bounds.y + (int) Math.round(bounds.height * topFallbackYRatio)
        );
        Point center = new Point(
            (int) Math.round(bounds.getCenterX()),
            (int) Math.round(bounds.getCenterY())
        );
        Point[] preferredPoints = new Point[] {upperCenter, center, topCenter};
        int start = ThreadLocalRandom.current().nextInt(preferredPoints.length);
        for (int offset = 0; offset < preferredPoints.length; offset++) {
            Point preferred = preferredPoints[(start + offset) % preferredPoints.length];
            Point resolved = resolvePointInsideShape(hull, preferred, hullCandidateSearchRadiusPx);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private Point sampleSmallHullPoint(Shape hull, Rectangle bounds, int attempts) {
        if (hull == null || bounds == null || bounds.width <= 1 || bounds.height <= 1) {
            return null;
        }
        int minX = bounds.x;
        int maxX = bounds.x + bounds.width - 1;
        int minY = bounds.y + (int) Math.round(bounds.height * Math.max(0.0, topFallbackYRatio));
        int maxY = bounds.y + (int) Math.round(bounds.height * Math.min(1.0, upperFallbackYRatio + 0.26));
        minY = Math.max(bounds.y, Math.min(bounds.y + bounds.height - 1, minY));
        maxY = Math.max(minY, Math.min(bounds.y + bounds.height - 1, maxY));
        int count = Math.max(4, attempts);
        for (int i = 0; i < count; i++) {
            int x = randomIntInclusive(minX, maxX);
            int y = randomIntInclusive(minY, maxY);
            if (!hull.contains(x, y)) {
                continue;
            }
            Point candidate = new Point(x, y);
            if (isCanvasPointUsable.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Point resolvePointInsideShape(Shape shape, Point preferred, int maxRadius) {
        if (shape == null || preferred == null) {
            return null;
        }
        int radius = Math.max(1, maxRadius);
        int attempts = Math.max(PREFERRED_POINT_RANDOM_RETRY_ATTEMPTS, radius * 2);
        for (int i = 0; i < attempts; i++) {
            int dx = randomIntInclusive(-radius, radius);
            int dy = randomIntInclusive(-radius, radius);
            int x = preferred.x + dx;
            int y = preferred.y + dy;
            if (!shape.contains(x, y)) {
                continue;
            }
            Point candidate = new Point(x, y);
            if (isCanvasPointUsable.test(candidate)) {
                return candidate;
            }
        }
        return null;
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
