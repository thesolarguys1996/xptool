package com.xptool.systems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

public final class FishingTargetResolver {
    private static final int LOCKED_TARGET_WORLDPOINT_MATCH_RADIUS_TILES = 1;

    public interface Host {
        int lockedFishingNpcIndex();

        WorldPoint lockedFishingWorldPoint();

        WorldView topLevelWorldView();

        boolean isFishingSpotNpcCandidate(NPC npc);

        boolean worldPointsExactMatch(WorldPoint a, WorldPoint b);

        void clearFishingTargetLock();
    }

    private final Host host;

    public FishingTargetResolver(Host host) {
        this.host = host;
    }

    public Optional<NPC> resolveLockedFishingTarget(Set<Integer> preferredNpcIds) {
        if (host.lockedFishingNpcIndex() < 0 || host.lockedFishingWorldPoint() == null) {
            return Optional.empty();
        }
        WorldView view = host.topLevelWorldView();
        if (view == null) {
            clearLockedTargetContext();
            return Optional.empty();
        }
        for (NPC npc : view.npcs()) {
            if (npc == null || npc.getIndex() != host.lockedFishingNpcIndex()) {
                continue;
            }
            WorldPoint npcWorldPoint = npc.getWorldLocation();
            if (npcWorldPoint == null) {
                clearLockedTargetContext();
                return Optional.empty();
            }
            if (!host.isFishingSpotNpcCandidate(npc)) {
                clearLockedTargetContext();
                return Optional.empty();
            }
            if (preferredNpcIds != null && !preferredNpcIds.isEmpty() && !preferredNpcIds.contains(npc.getId())) {
                clearLockedTargetContext();
                return Optional.empty();
            }
            if (!worldPointsNear(
                host.lockedFishingWorldPoint(),
                npcWorldPoint,
                LOCKED_TARGET_WORLDPOINT_MATCH_RADIUS_TILES
            )) {
                clearLockedTargetContext();
                return Optional.empty();
            }
            return Optional.of(npc);
        }
        clearLockedTargetContext();
        return Optional.empty();
    }

    public Optional<NPC> resolveNearestFishingTarget(Player local, Set<Integer> preferredNpcIds) {
        List<NPC> nearestTargets = resolveNearestFishingTargets(local, preferredNpcIds, 1);
        if (nearestTargets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(nearestTargets.get(0));
    }

    public List<NPC> resolveNearestFishingTargets(Player local, Set<Integer> preferredNpcIds, int maxTargets) {
        if (maxTargets <= 0) {
            return List.of();
        }
        WorldView view = host.topLevelWorldView();
        if (view == null) {
            return List.of();
        }
        WorldPoint localPos = local == null ? null : local.getWorldLocation();
        if (localPos == null) {
            return List.of();
        }
        List<NpcDistance> candidates = new ArrayList<>();
        for (NPC npc : view.npcs()) {
            if (!host.isFishingSpotNpcCandidate(npc)) {
                continue;
            }
            if (preferredNpcIds != null && !preferredNpcIds.isEmpty() && !preferredNpcIds.contains(npc.getId())) {
                continue;
            }
            WorldPoint npcWorld = npc.getWorldLocation();
            if (npcWorld == null) {
                continue;
            }
            int dist = localPos.distanceTo(npcWorld);
            if (dist < 0) {
                continue;
            }
            candidates.add(new NpcDistance(npc, dist));
        }
        candidates.sort(
            Comparator
                .comparingInt((NpcDistance entry) -> entry.distance)
                .thenComparingInt(entry -> entry.npc.getIndex())
        );
        int limit = Math.min(maxTargets, candidates.size());
        List<NPC> nearest = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            nearest.add(candidates.get(i).npc);
        }
        return nearest;
    }

    private void clearLockedTargetContext() {
        host.clearFishingTargetLock();
    }

    private static boolean worldPointsNear(WorldPoint a, WorldPoint b, int radiusTiles) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getPlane() != b.getPlane()) {
            return false;
        }
        int dist = a.distanceTo(b);
        int radius = Math.max(0, radiusTiles);
        return dist >= 0 && dist <= radius;
    }

    private static final class NpcDistance {
        private final NPC npc;
        private final int distance;

        private NpcDistance(NPC npc, int distance) {
            this.npc = npc;
            this.distance = distance;
        }
    }
}
