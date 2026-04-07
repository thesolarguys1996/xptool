package com.xptool.systems;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.runelite.api.TileObject;

public final class TargetSelectionEngine {
    @FunctionalInterface
    public interface WorldDistanceProvider {
        int worldDistance(TileObject candidate);
    }

    public interface Host {
        Point currentCursorCanvasPoint();

        Point primaryCandidateCanvasPoint(TileObject candidate);

        Point fallbackCandidateCanvasPoint(TileObject candidate);

        boolean isUsableCanvasPoint(Point point);

        double pixelDistance(Point from, Point to);

        int consecutiveLocalInteractions();
    }

    public static final class Config {
        private final double playerDistanceWeight;
        private final double cursorDistanceWeight;
        private final int cursorBiasWorldWindowTiles;
        private final int localityDecayMinReuseStreak;
        private final int localityDecayStreakCap;
        private final double localityDecayExpK;
        private final double localityDecayApplyBaseChance;
        private final double localityDecayApplyChanceStep;
        private final double localityDecayApplyMaxChance;
        private final double localityDecayMinWeightMultiplier;

        public Config(
            double playerDistanceWeight,
            double cursorDistanceWeight,
            int cursorBiasWorldWindowTiles,
            int localityDecayMinReuseStreak,
            int localityDecayStreakCap,
            double localityDecayExpK,
            double localityDecayApplyBaseChance,
            double localityDecayApplyChanceStep,
            double localityDecayApplyMaxChance,
            double localityDecayMinWeightMultiplier
        ) {
            this.playerDistanceWeight = playerDistanceWeight;
            this.cursorDistanceWeight = cursorDistanceWeight;
            this.cursorBiasWorldWindowTiles = Math.max(0, cursorBiasWorldWindowTiles);
            this.localityDecayMinReuseStreak = Math.max(0, localityDecayMinReuseStreak);
            this.localityDecayStreakCap = Math.max(1, localityDecayStreakCap);
            this.localityDecayExpK = localityDecayExpK;
            this.localityDecayApplyBaseChance = localityDecayApplyBaseChance;
            this.localityDecayApplyChanceStep = localityDecayApplyChanceStep;
            this.localityDecayApplyMaxChance = localityDecayApplyMaxChance;
            this.localityDecayMinWeightMultiplier = localityDecayMinWeightMultiplier;
        }
    }

    private final Host host;
    private final Config config;

    public TargetSelectionEngine(Host host, Config config) {
        this.host = host;
        this.config = config;
    }

    public Optional<TileObject> selectBestCursorAwareTarget(
        Iterable<TileObject> candidates,
        WorldDistanceProvider worldDistanceProvider
    ) {
        if (host == null || config == null || candidates == null || worldDistanceProvider == null) {
            return Optional.empty();
        }

        List<TileDistanceCandidate> viable = new ArrayList<>();
        int minWorldDistance = Integer.MAX_VALUE;
        for (TileObject candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            int worldDistance = worldDistanceProvider.worldDistance(candidate);
            if (worldDistance < 0) {
                continue;
            }
            viable.add(new TileDistanceCandidate(candidate, worldDistance));
            minWorldDistance = Math.min(minWorldDistance, worldDistance);
        }
        if (viable.isEmpty()) {
            return Optional.empty();
        }

        Point cursorCanvasPoint = currentCursorCanvasPointForSelection();
        boolean hasCursorCanvasPoint = cursorCanvasPoint != null;
        double effectiveCursorWeight = effectiveCursorDistanceWeightForSelection();
        int worldDistanceWindowMax = hasCursorCanvasPoint
            ? minWorldDistance + config.cursorBiasWorldWindowTiles
            : Integer.MAX_VALUE;

        TileObject best = null;
        int bestWorldDistance = Integer.MAX_VALUE;
        double bestScore = Double.POSITIVE_INFINITY;
        for (TileDistanceCandidate candidate : viable) {
            if (hasCursorCanvasPoint && candidate.worldDistance > worldDistanceWindowMax) {
                continue;
            }
            double score = scoreTileTarget(
                candidate.tileObject,
                candidate.worldDistance,
                cursorCanvasPoint,
                effectiveCursorWeight
            );
            if (score < bestScore) {
                bestScore = score;
                bestWorldDistance = candidate.worldDistance;
                best = candidate.tileObject;
                continue;
            }
            if (Double.compare(score, bestScore) == 0 && candidate.worldDistance < bestWorldDistance) {
                bestWorldDistance = candidate.worldDistance;
                best = candidate.tileObject;
            }
        }

        if (best != null) {
            return Optional.of(best);
        }

        TileObject nearest = null;
        int nearestWorldDistance = Integer.MAX_VALUE;
        for (TileDistanceCandidate candidate : viable) {
            if (candidate.worldDistance < nearestWorldDistance) {
                nearestWorldDistance = candidate.worldDistance;
                nearest = candidate.tileObject;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private Point currentCursorCanvasPointForSelection() {
        Point cursorCanvas = host.currentCursorCanvasPoint();
        if (!host.isUsableCanvasPoint(cursorCanvas)) {
            return null;
        }
        return cursorCanvas;
    }

    private double effectiveCursorDistanceWeightForSelection() {
        int consecutiveLocalInteractions = host.consecutiveLocalInteractions();
        if (consecutiveLocalInteractions < config.localityDecayMinReuseStreak) {
            return config.cursorDistanceWeight;
        }
        int decayDepth = Math.min(
            config.localityDecayStreakCap,
            consecutiveLocalInteractions - config.localityDecayMinReuseStreak + 1
        );
        double applyChance = Math.min(
            config.localityDecayApplyMaxChance,
            config.localityDecayApplyBaseChance + (config.localityDecayApplyChanceStep * (double) decayDepth)
        );
        if (ThreadLocalRandom.current().nextDouble() >= applyChance) {
            return config.cursorDistanceWeight;
        }
        double decayMultiplier = Math.max(
            config.localityDecayMinWeightMultiplier,
            Math.exp(-config.localityDecayExpK * (double) decayDepth)
        );
        return config.cursorDistanceWeight * decayMultiplier;
    }

    private double scoreTileTarget(
        TileObject candidate,
        int worldDistance,
        Point cursorCanvasPoint,
        double cursorDistanceWeight
    ) {
        double score = config.playerDistanceWeight * Math.max(0, worldDistance);
        if (cursorCanvasPoint == null) {
            return score;
        }
        Point candidateCanvasPoint = resolveCandidateCanvasPointForSelection(candidate);
        if (!host.isUsableCanvasPoint(candidateCanvasPoint)) {
            return score;
        }
        double cursorDistancePx = host.pixelDistance(cursorCanvasPoint, candidateCanvasPoint);
        return score + (Math.max(0.0, cursorDistanceWeight) * cursorDistancePx);
    }

    private Point resolveCandidateCanvasPointForSelection(TileObject candidate) {
        if (candidate == null) {
            return null;
        }
        Point canvasPoint = host.primaryCandidateCanvasPoint(candidate);
        if (!host.isUsableCanvasPoint(canvasPoint)) {
            canvasPoint = host.fallbackCandidateCanvasPoint(candidate);
        }
        if (!host.isUsableCanvasPoint(canvasPoint)) {
            return null;
        }
        return canvasPoint;
    }

    private static final class TileDistanceCandidate {
        private final TileObject tileObject;
        private final int worldDistance;

        private TileDistanceCandidate(TileObject tileObject, int worldDistance) {
            this.tileObject = tileObject;
            this.worldDistance = worldDistance;
        }
    }
}
