package com.xptool.systems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class WoodcuttingTargetResolver {
    private static final int TARGET_SELECTION_TOP_N = 5;
    private static final int TARGET_SELECTION_TOP_K_MIN = 2;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_PERCENT = 62;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_JITTER_MIN_PERCENT = -24;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_JITTER_MAX_PERCENT = 18;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MIN_PERCENT = 34;
    private static final int TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MAX_PERCENT = 89;
    private static final double TARGET_SELECTION_RANK_EXPONENT_MIN = 1.45;
    private static final double TARGET_SELECTION_RANK_EXPONENT_MAX = 2.05;
    private static final double TARGET_SELECTION_RANK_MULTIPLIER_MIN = 1.9;
    private static final double TARGET_SELECTION_RANK_MULTIPLIER_MAX = 3.3;
    private static final double TARGET_SELECTION_DISTANCE_MULTIPLIER_MIN = 0.58;
    private static final double TARGET_SELECTION_DISTANCE_MULTIPLIER_MAX = 1.12;
    private static final double TARGET_SELECTION_WEIGHT_JITTER_MIN = 0.66;
    private static final double TARGET_SELECTION_WEIGHT_JITTER_MAX = 1.42;
    private static final double TARGET_SELECTION_RECENT_NEAREST_WEIGHT_MULTIPLIER_MIN = 0.72;
    private static final double TARGET_SELECTION_RECENT_NEAREST_WEIGHT_MULTIPLIER_MAX = 0.90;
    private static final double TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER_MIN = 0.52;
    private static final double TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER_MAX = 0.80;
    private static final int RECENT_TARGET_HISTORY_SIZE = 10;

    @FunctionalInterface
    public interface WorldDistanceProvider {
        int worldDistance(TileObject candidate);
    }

    public interface Host {
        WorldPoint localPlayerWorldPoint();

        WorldPoint lockedWoodcutWorldPoint();

        WorldPoint preferredSelectedWoodcutWorldPoint();

        int selectedWoodcutTargetCount();

        Iterable<TileObject> cachedTreeObjects();

        Iterable<TileObject> cachedNormalTreeObjects();

        Iterable<TileObject> cachedOakTreeObjects();

        Iterable<TileObject> cachedWillowTreeObjects();

        boolean hasSelectedTreeTargetNear(WorldPoint worldPoint);

        boolean worldPointsMatch(WorldPoint a, WorldPoint b);

        Optional<TileObject> selectBestCursorAwareTarget(
            Iterable<TileObject> candidates,
            WorldDistanceProvider worldDistanceProvider
        );
    }

    private final Host host;
    private final long[] recentTargetHistory = new long[RECENT_TARGET_HISTORY_SIZE];
    private int recentTargetHistoryWriteIndex = 0;
    private long lastSelectedTargetKey = Long.MIN_VALUE;
    private int sameTargetSelectionStreak = 0;

    public WoodcuttingTargetResolver(Host host) {
        this.host = host;
        initializeRecentTargetHistory();
    }

    public Optional<TileObject> resolveLockedSelectedTreeTarget() {
        WorldPoint locked = host.lockedWoodcutWorldPoint();
        if (locked == null || !host.hasSelectedTreeTargetNear(locked)) {
            return Optional.empty();
        }
        return findTreeObjectBySelectedWorldPoint(locked);
    }

    public Optional<TileObject> resolvePreferredSelectedTreeTarget() {
        WorldPoint preferred = host.preferredSelectedWoodcutWorldPoint();
        if (preferred == null || !host.hasSelectedTreeTargetNear(preferred)) {
            return Optional.empty();
        }
        return findTreeObjectBySelectedWorldPoint(preferred);
    }

    public Optional<TileObject> resolveNearestSelectedTreeTarget() {
        if (host.selectedWoodcutTargetCount() <= 0) {
            return Optional.empty();
        }
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        return resolveRandomizedNearestTarget(
            host.cachedTreeObjects(),
            candidate -> {
                if (candidate == null || candidate.getWorldLocation() == null) {
                    return -1;
                }
                if (!host.hasSelectedTreeTargetNear(candidate.getWorldLocation())) {
                    return -1;
                }
                return localPos.distanceTo(candidate.getWorldLocation());
            }
        );
    }

    public Optional<TileObject> resolveLockedNormalTreeTarget() {
        return resolveLockedTargetIn(host.cachedNormalTreeObjects());
    }

    public Optional<TileObject> resolveNearestNormalTreeTarget(int preferredWorldX, int preferredWorldY, int maxDistance) {
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        boolean hasPreferred = preferredWorldX > 0 && preferredWorldY > 0;
        return resolveRandomizedNearestTarget(
            host.cachedNormalTreeObjects(),
            candidate -> worldDistanceByPreferenceOrLocal(
                candidate,
                hasPreferred,
                preferredWorldX,
                preferredWorldY,
                maxDistance,
                localPos
            )
        );
    }

    public Optional<TileObject> resolveLockedOakTreeTarget() {
        return resolveLockedTargetIn(host.cachedOakTreeObjects());
    }

    public Optional<TileObject> resolveNearestOakTreeTarget(int preferredWorldX, int preferredWorldY, int maxDistance) {
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        boolean hasPreferred = preferredWorldX > 0 && preferredWorldY > 0;
        return resolveRandomizedNearestTarget(
            host.cachedOakTreeObjects(),
            candidate -> worldDistanceByPreferenceOrLocal(
                candidate,
                hasPreferred,
                preferredWorldX,
                preferredWorldY,
                maxDistance,
                localPos
            )
        );
    }

    public Optional<TileObject> resolveLockedWillowTreeTarget() {
        return resolveLockedTargetIn(host.cachedWillowTreeObjects());
    }

    public Optional<TileObject> resolveNearestWillowTreeTarget(int preferredWorldX, int preferredWorldY, int maxDistance) {
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        boolean hasPreferred = preferredWorldX > 0 && preferredWorldY > 0;
        return resolveRandomizedNearestTarget(
            host.cachedWillowTreeObjects(),
            candidate -> worldDistanceByPreferenceOrLocal(
                candidate,
                hasPreferred,
                preferredWorldX,
                preferredWorldY,
                maxDistance,
                localPos
            )
        );
    }

    public Optional<TileObject> resolveNearestTreeTargetInArea(int targetWorldX, int targetWorldY, int maxDistance) {
        if (targetWorldX <= 0 || targetWorldY <= 0 || maxDistance <= 0) {
            return Optional.empty();
        }
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        return resolveRandomizedNearestTarget(
            host.cachedTreeObjects(),
            candidate -> worldDistanceByPreferenceOrLocal(
                candidate,
                true,
                targetWorldX,
                targetWorldY,
                maxDistance,
                localPos
            )
        );
    }

    private Optional<TileObject> findTreeObjectBySelectedWorldPoint(WorldPoint selectedWorldPoint) {
        if (selectedWorldPoint == null) {
            return Optional.empty();
        }
        for (TileObject candidate : host.cachedTreeObjects()) {
            if (candidate == null || candidate.getWorldLocation() == null) {
                continue;
            }
            if (host.worldPointsMatch(selectedWorldPoint, candidate.getWorldLocation())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Optional<TileObject> resolveLockedTargetIn(Iterable<TileObject> candidates) {
        WorldPoint locked = host.lockedWoodcutWorldPoint();
        if (locked == null) {
            return Optional.empty();
        }
        for (TileObject candidate : candidates) {
            if (candidate == null || candidate.getWorldLocation() == null) {
                continue;
            }
            if (locked.equals(candidate.getWorldLocation())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static int worldDistanceByPreferenceOrLocal(
        TileObject candidate,
        boolean hasPreferred,
        int preferredWorldX,
        int preferredWorldY,
        int maxDistance,
        WorldPoint localPos
    ) {
        if (candidate == null || candidate.getWorldLocation() == null) {
            return -1;
        }
        WorldPoint world = candidate.getWorldLocation();
        if (hasPreferred) {
            int dx = Math.abs(world.getX() - preferredWorldX);
            int dy = Math.abs(world.getY() - preferredWorldY);
            int chebyshev = Math.max(dx, dy);
            if (maxDistance > 0 && chebyshev > maxDistance) {
                return -1;
            }
            return dx + dy;
        }
        if (localPos == null) {
            return -1;
        }
        return localPos.distanceTo(world);
    }

    private Optional<TileObject> resolveRandomizedNearestTarget(
        Iterable<TileObject> candidates,
        WorldDistanceProvider worldDistanceProvider
    ) {
        List<TargetCandidate> nearestCandidates = topNCandidates(candidates, worldDistanceProvider, TARGET_SELECTION_TOP_N);
        if (nearestCandidates.isEmpty()) {
            return Optional.empty();
        }
        int poolLimit = sampleSelectionPoolLimit(nearestCandidates.size());
        List<TargetCandidate> pool = nearestCandidates.subList(0, Math.min(poolLimit, nearestCandidates.size()));
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        SelectionTuning tuning = sampleSelectionTuning();
        TargetCandidate nearestCandidate = pool.get(0);
        int nearestPriorityChancePercent = tuning.nearestPriorityChancePercent;
        if (nearestCandidate != null && nearestCandidate.worldPointKey == lastSelectedTargetKey) {
            int streakPenalty = 14 + Math.min(22, Math.max(0, sameTargetSelectionStreak - 1) * 4);
            nearestPriorityChancePercent = clampInt(
                nearestPriorityChancePercent - streakPenalty,
                TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MIN_PERCENT / 2,
                TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MAX_PERCENT
            );
        }
        List<WeightedTargetCandidate> weightedCandidates = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            TargetCandidate candidate = pool.get(i);
            if (candidate == null) {
                continue;
            }
            boolean recentCandidate = isRecentlyUsedTarget(candidate.worldPointKey);
            double weight = selectionWeight(i, candidate.worldDistance, recentCandidate, tuning);
            if (candidate.worldPointKey == lastSelectedTargetKey) {
                double repeatPenalty = i == 0
                    ? randomBetweenInclusive(0.36, 0.62)
                    : randomBetweenInclusive(0.64, 0.86);
                weight *= repeatPenalty;
            }
            if (weight <= 0.0) {
                continue;
            }
            weightedCandidates.add(new WeightedTargetCandidate(candidate, weight));
        }

        TargetCandidate selectedCandidate = null;
        if (ThreadLocalRandom.current().nextInt(100) < nearestPriorityChancePercent) {
            selectedCandidate = nearestCandidate;
        } else {
            WeightedTargetCandidate weightedSelection = selectWeightedCandidate(weightedCandidates);
            if (weightedSelection != null) {
                selectedCandidate = weightedSelection.candidate;
            }
        }
        if (selectedCandidate == null) {
            selectedCandidate = nearestCandidate;
        }
        if (selectedCandidate == null || selectedCandidate.tileObject == null) {
            return Optional.empty();
        }
        noteRecentTarget(selectedCandidate.worldPointKey);
        return Optional.of(selectedCandidate.tileObject);
    }

    private static List<TargetCandidate> topNCandidates(
        Iterable<TileObject> candidates,
        WorldDistanceProvider worldDistanceProvider,
        int maxCount
    ) {
        List<TargetCandidate> out = new ArrayList<>();
        if (candidates == null || worldDistanceProvider == null || maxCount <= 0) {
            return out;
        }
        for (TileObject candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            WorldPoint worldPoint = candidate.getWorldLocation();
            int worldDistance = worldDistanceProvider.worldDistance(candidate);
            if (worldDistance < 0) {
                continue;
            }
            long worldPointKey = encodeWorldPointKey(worldPoint);
            if (containsEquivalentTarget(out, worldPointKey)) {
                continue;
            }
            out.add(new TargetCandidate(candidate, worldDistance, worldPointKey));
        }
        out.sort(
            Comparator
                .comparingInt((TargetCandidate candidate) -> candidate.worldDistance)
                .thenComparingLong(candidate -> candidate.worldPointKey)
        );
        if (out.size() <= maxCount) {
            return out;
        }
        return new ArrayList<>(out.subList(0, maxCount));
    }

    private static boolean containsEquivalentTarget(List<TargetCandidate> candidates, long worldPointKey) {
        if (candidates == null || candidates.isEmpty() || worldPointKey == Long.MIN_VALUE) {
            return false;
        }
        for (TargetCandidate candidate : candidates) {
            if (candidate != null && candidate.worldPointKey == worldPointKey) {
                return true;
            }
        }
        return false;
    }

    private static int sampleSelectionPoolLimit(int candidateCount) {
        int upperBound = Math.max(1, Math.min(TARGET_SELECTION_TOP_N, candidateCount));
        int lowerBound = Math.min(TARGET_SELECTION_TOP_K_MIN, upperBound);
        if (upperBound <= lowerBound) {
            return upperBound;
        }
        return ThreadLocalRandom.current().nextInt(lowerBound, upperBound + 1);
    }

    private static SelectionTuning sampleSelectionTuning() {
        int nearestChanceJitter = (int) randomBetweenInclusive(
            TARGET_SELECTION_NEAREST_PRIORITY_JITTER_MIN_PERCENT,
            TARGET_SELECTION_NEAREST_PRIORITY_JITTER_MAX_PERCENT
        );
        int nearestPriorityChancePercent = clampInt(
            TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_PERCENT + nearestChanceJitter,
            TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MIN_PERCENT,
            TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_MAX_PERCENT
        );
        double rankExponent = randomBetweenInclusive(
            TARGET_SELECTION_RANK_EXPONENT_MIN,
            TARGET_SELECTION_RANK_EXPONENT_MAX
        );
        double rankMultiplier = randomBetweenInclusive(
            TARGET_SELECTION_RANK_MULTIPLIER_MIN,
            TARGET_SELECTION_RANK_MULTIPLIER_MAX
        );
        double distanceMultiplier = randomBetweenInclusive(
            TARGET_SELECTION_DISTANCE_MULTIPLIER_MIN,
            TARGET_SELECTION_DISTANCE_MULTIPLIER_MAX
        );
        double weightJitter = randomBetweenInclusive(
            TARGET_SELECTION_WEIGHT_JITTER_MIN,
            TARGET_SELECTION_WEIGHT_JITTER_MAX
        );
        double recentNearestMultiplier = randomBetweenInclusive(
            TARGET_SELECTION_RECENT_NEAREST_WEIGHT_MULTIPLIER_MIN,
            TARGET_SELECTION_RECENT_NEAREST_WEIGHT_MULTIPLIER_MAX
        );
        double recentMultiplier = randomBetweenInclusive(
            TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER_MIN,
            TARGET_SELECTION_RECENT_WEIGHT_MULTIPLIER_MAX
        );
        return new SelectionTuning(
            nearestPriorityChancePercent,
            rankExponent,
            rankMultiplier,
            distanceMultiplier,
            weightJitter,
            recentNearestMultiplier,
            recentMultiplier
        );
    }

    private static double selectionWeight(
        int rank,
        int worldDistance,
        boolean recentCandidate,
        SelectionTuning tuning
    ) {
        SelectionTuning effectiveTuning = tuning == null ? SelectionTuning.defaults() : tuning;
        int boundedRank = Math.max(0, rank);
        double rankWeight =
            1.0 / Math.pow(1.0 + (double) boundedRank, Math.max(1.1, effectiveTuning.rankExponent));
        double distanceWeight = worldDistance < 0
            ? 0.0
            : (1.0 / (1.0 + (double) worldDistance));
        double weight =
            (rankWeight * Math.max(0.1, effectiveTuning.rankMultiplier))
                + (distanceWeight * Math.max(0.1, effectiveTuning.distanceMultiplier));
        if (recentCandidate) {
            weight *= boundedRank == 0
                ? effectiveTuning.recentNearestMultiplier
                : effectiveTuning.recentMultiplier;
        }
        weight *= Math.max(0.20, effectiveTuning.weightJitter);
        return Math.max(0.0, weight);
    }

    private static WeightedTargetCandidate selectWeightedCandidate(List<WeightedTargetCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        double totalWeight = 0.0;
        WeightedTargetCandidate fallback = null;
        for (WeightedTargetCandidate candidate : candidates) {
            if (candidate == null || candidate.candidate == null || candidate.weight <= 0.0) {
                continue;
            }
            totalWeight += candidate.weight;
            fallback = candidate;
        }
        if (fallback == null || totalWeight <= 0.0) {
            return fallback;
        }
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulativeWeight = 0.0;
        for (WeightedTargetCandidate candidate : candidates) {
            if (candidate == null || candidate.candidate == null || candidate.weight <= 0.0) {
                continue;
            }
            cumulativeWeight += candidate.weight;
            if (roll < cumulativeWeight) {
                return candidate;
            }
        }
        return fallback;
    }

    private static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static double randomBetweenInclusive(double min, double max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, Math.nextUp(max));
    }

    private void initializeRecentTargetHistory() {
        for (int i = 0; i < recentTargetHistory.length; i++) {
            recentTargetHistory[i] = Long.MIN_VALUE;
        }
    }

    private boolean isRecentlyUsedTarget(long key) {
        if (key == Long.MIN_VALUE) {
            return false;
        }
        for (long recentKey : recentTargetHistory) {
            if (recentKey == key) {
                return true;
            }
        }
        return false;
    }

    private void noteRecentTarget(long key) {
        if (key == Long.MIN_VALUE || recentTargetHistory.length <= 0) {
            return;
        }
        if (key == lastSelectedTargetKey) {
            sameTargetSelectionStreak = Math.min(12, sameTargetSelectionStreak + 1);
        } else {
            sameTargetSelectionStreak = 1;
            lastSelectedTargetKey = key;
        }
        recentTargetHistory[recentTargetHistoryWriteIndex] = key;
        recentTargetHistoryWriteIndex = (recentTargetHistoryWriteIndex + 1) % recentTargetHistory.length;
    }

    private static long encodeWorldPointKey(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return Long.MIN_VALUE;
        }
        long plane = ((long) worldPoint.getPlane() & 0x3FL) << 58;
        long x = ((long) worldPoint.getX() & 0x1FFFFFFFL) << 29;
        long y = ((long) worldPoint.getY() & 0x1FFFFFFFL);
        return plane | x | y;
    }

    private static final class SelectionTuning {
        private final int nearestPriorityChancePercent;
        private final double rankExponent;
        private final double rankMultiplier;
        private final double distanceMultiplier;
        private final double weightJitter;
        private final double recentNearestMultiplier;
        private final double recentMultiplier;

        private SelectionTuning(
            int nearestPriorityChancePercent,
            double rankExponent,
            double rankMultiplier,
            double distanceMultiplier,
            double weightJitter,
            double recentNearestMultiplier,
            double recentMultiplier
        ) {
            this.nearestPriorityChancePercent = nearestPriorityChancePercent;
            this.rankExponent = rankExponent;
            this.rankMultiplier = rankMultiplier;
            this.distanceMultiplier = distanceMultiplier;
            this.weightJitter = weightJitter;
            this.recentNearestMultiplier = recentNearestMultiplier;
            this.recentMultiplier = recentMultiplier;
        }

        private static SelectionTuning defaults() {
            return new SelectionTuning(
                TARGET_SELECTION_NEAREST_PRIORITY_CHANCE_PERCENT,
                1.75,
                2.5,
                0.84,
                1.0,
                0.81,
                0.66
            );
        }
    }

    private static final class TargetCandidate {
        private final TileObject tileObject;
        private final int worldDistance;
        private final long worldPointKey;

        private TargetCandidate(TileObject tileObject, int worldDistance, long worldPointKey) {
            this.tileObject = tileObject;
            this.worldDistance = worldDistance;
            this.worldPointKey = worldPointKey;
        }
    }

    private static final class WeightedTargetCandidate {
        private final TargetCandidate candidate;
        private final double weight;

        private WeightedTargetCandidate(TargetCandidate candidate, double weight) {
            this.candidate = candidate;
            this.weight = weight;
        }
    }
}
