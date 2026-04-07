package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

final class LoginSubmitTargetPlanner {
    interface Host {
        int canvasWidth();
        int canvasHeight();
        boolean isUsableCanvasPoint(Point point);
    }

    static final class Config {
        final double regionLeftRatio;
        final double regionTopRatio;
        final double regionWidthRatio;
        final double regionHeightRatio;
        final int regionInsetPx;
        final int maxAttempts;
        final int historySize;
        final double repeatExclusionPx;

        Config(
            double regionLeftRatio,
            double regionTopRatio,
            double regionWidthRatio,
            double regionHeightRatio,
            int regionInsetPx,
            int maxAttempts,
            int historySize,
            double repeatExclusionPx
        ) {
            this.regionLeftRatio = regionLeftRatio;
            this.regionTopRatio = regionTopRatio;
            this.regionWidthRatio = regionWidthRatio;
            this.regionHeightRatio = regionHeightRatio;
            this.regionInsetPx = Math.max(0, regionInsetPx);
            this.maxAttempts = Math.max(1, maxAttempts);
            this.historySize = Math.max(1, historySize);
            this.repeatExclusionPx = Math.max(0.0, repeatExclusionPx);
        }
    }

    static final class Plan {
        final Point canvasPoint;
        final Rectangle canvasRegion;
        final int samplingAttempts;

        Plan(Point canvasPoint, Rectangle canvasRegion, int samplingAttempts) {
            this.canvasPoint = canvasPoint == null ? null : new Point(canvasPoint);
            this.canvasRegion = canvasRegion == null ? null : new Rectangle(canvasRegion);
            this.samplingAttempts = Math.max(1, samplingAttempts);
        }
    }

    private final Host host;
    private final Config config;
    private final ArrayDeque<Point> recentPoints = new ArrayDeque<>();

    LoginSubmitTargetPlanner(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    void reset() {
        recentPoints.clear();
    }

    Optional<Plan> planNext() {
        Rectangle region = resolveCanvasRegion();
        if (region == null) {
            return Optional.empty();
        }

        Point chosen = null;
        int attemptsUsed = 0;
        for (int i = 0; i < config.maxAttempts; i++) {
            attemptsUsed = i + 1;
            Point sample = samplePoint(region);
            if (sample == null || !host.isUsableCanvasPoint(sample)) {
                continue;
            }
            if (!violatesRecentRepeat(sample)) {
                chosen = sample;
                break;
            }
            if (chosen == null) {
                chosen = sample;
            }
        }
        if (chosen == null || !host.isUsableCanvasPoint(chosen)) {
            return Optional.empty();
        }

        remember(chosen);
        return Optional.of(new Plan(chosen, region, attemptsUsed));
    }

    private Rectangle resolveCanvasRegion() {
        int width = host.canvasWidth();
        int height = host.canvasHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        int left = (int) Math.round(width * config.regionLeftRatio) + config.regionInsetPx;
        int top = (int) Math.round(height * config.regionTopRatio) + config.regionInsetPx;
        int right = (int) Math.round(width * (config.regionLeftRatio + config.regionWidthRatio)) - config.regionInsetPx;
        int bottom = (int) Math.round(height * (config.regionTopRatio + config.regionHeightRatio)) - config.regionInsetPx;
        if (right <= left || bottom <= top) {
            return null;
        }

        left = clamp(left, 1, Math.max(1, width - 2));
        top = clamp(top, 1, Math.max(1, height - 2));
        right = clamp(right, left + 1, Math.max(left + 1, width - 1));
        bottom = clamp(bottom, top + 1, Math.max(top + 1, height - 1));
        int regionWidth = right - left;
        int regionHeight = bottom - top;
        if (regionWidth <= 1 || regionHeight <= 1) {
            return null;
        }
        return new Rectangle(left, top, regionWidth, regionHeight);
    }

    private Point samplePoint(Rectangle region) {
        if (region == null || region.width <= 1 || region.height <= 1) {
            return null;
        }
        int minX = region.x;
        int minY = region.y;
        int maxX = region.x + region.width - 1;
        int maxY = region.y + region.height - 1;
        if (maxX < minX || maxY < minY) {
            return null;
        }
        int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
        int y = ThreadLocalRandom.current().nextInt(minY, maxY + 1);
        return new Point(x, y);
    }

    private boolean violatesRecentRepeat(Point candidate) {
        if (candidate == null || recentPoints.isEmpty()) {
            return false;
        }
        for (Point point : recentPoints) {
            if (point == null) {
                continue;
            }
            if (candidate.equals(point)) {
                return true;
            }
            if (distance(candidate, point) < config.repeatExclusionPx) {
                return true;
            }
        }
        return false;
    }

    private void remember(Point point) {
        if (point == null) {
            return;
        }
        recentPoints.addLast(new Point(point));
        while (recentPoints.size() > config.historySize) {
            recentPoints.pollFirst();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double distance(Point a, Point b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.hypot(dx, dy);
    }
}
