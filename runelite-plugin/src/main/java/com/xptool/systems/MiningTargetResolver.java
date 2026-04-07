package com.xptool.systems;

import java.util.Optional;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class MiningTargetResolver {
    @FunctionalInterface
    public interface WorldDistanceProvider {
        int worldDistance(TileObject candidate);
    }

    public interface Host {
        int selectedMiningTargetCount();

        WorldPoint localPlayerWorldPoint();

        Iterable<TileObject> cachedRockObjects();

        boolean isRockObjectCandidate(TileObject candidate);

        boolean isMiningRockSuppressed(WorldPoint worldPoint);

        boolean hasSelectedRockTargetNear(WorldPoint worldPoint);

        Optional<TileObject> selectBestCursorAwareTarget(
            Iterable<TileObject> candidates,
            WorldDistanceProvider worldDistanceProvider
        );
    }

    private final Host host;

    public MiningTargetResolver(Host host) {
        this.host = host;
    }

    public Optional<TileObject> resolveNearestSelectedRockTarget() {
        if (host.selectedMiningTargetCount() <= 0) {
            return Optional.empty();
        }
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        return host.selectBestCursorAwareTarget(
            host.cachedRockObjects(),
            candidate -> worldDistanceForSelectedCandidate(localPos, candidate)
        );
    }

    public Optional<TileObject> resolveNearestSelectedRockTargetExcluding(WorldPoint excludedWorldPoint) {
        if (host.selectedMiningTargetCount() <= 0 || excludedWorldPoint == null) {
            return Optional.empty();
        }
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        return host.selectBestCursorAwareTarget(
            host.cachedRockObjects(),
            candidate -> {
                WorldPoint world = candidate == null ? null : candidate.getWorldLocation();
                if (world != null && world.equals(excludedWorldPoint)) {
                    return -1;
                }
                return worldDistanceForSelectedCandidate(localPos, candidate);
            }
        );
    }

    public Optional<TileObject> resolveLockedRockTarget(WorldPoint lockedMiningWorldPoint, int lockedMiningObjectId) {
        if (lockedMiningWorldPoint == null || host.isMiningRockSuppressed(lockedMiningWorldPoint)) {
            return Optional.empty();
        }
        for (TileObject candidate : host.cachedRockObjects()) {
            if (candidate == null || candidate.getWorldLocation() == null) {
                continue;
            }
            if (!host.isRockObjectCandidate(candidate)) {
                continue;
            }
            if (lockedMiningObjectId > 0 && candidate.getId() != lockedMiningObjectId) {
                continue;
            }
            if (lockedMiningWorldPoint.equals(candidate.getWorldLocation())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public Optional<TileObject> resolveNearestRockTarget() {
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        return host.selectBestCursorAwareTarget(
            host.cachedRockObjects(),
            candidate -> worldDistanceForGeneralCandidate(localPos, candidate)
        );
    }

    public Optional<TileObject> resolveNearestRockTargetExcluding(WorldPoint excludedWorldPoint) {
        if (excludedWorldPoint == null) {
            return resolveNearestRockTarget();
        }
        WorldPoint localPos = host.localPlayerWorldPoint();
        if (localPos == null) {
            return Optional.empty();
        }
        return host.selectBestCursorAwareTarget(
            host.cachedRockObjects(),
            candidate -> {
                WorldPoint world = candidate == null ? null : candidate.getWorldLocation();
                if (world != null && world.equals(excludedWorldPoint)) {
                    return -1;
                }
                return worldDistanceForGeneralCandidate(localPos, candidate);
            }
        );
    }

    private int worldDistanceForSelectedCandidate(WorldPoint localPos, TileObject candidate) {
        int distance = worldDistanceForGeneralCandidate(localPos, candidate);
        if (distance < 0) {
            return -1;
        }
        WorldPoint world = candidate == null ? null : candidate.getWorldLocation();
        if (world == null || !host.hasSelectedRockTargetNear(world)) {
            return -1;
        }
        return distance;
    }

    private int worldDistanceForGeneralCandidate(WorldPoint localPos, TileObject candidate) {
        if (localPos == null || candidate == null || candidate.getWorldLocation() == null) {
            return -1;
        }
        if (!host.isRockObjectCandidate(candidate)) {
            return -1;
        }
        WorldPoint world = candidate.getWorldLocation();
        if (host.isMiningRockSuppressed(world)) {
            return -1;
        }
        return localPos.distanceTo(world);
    }
}
