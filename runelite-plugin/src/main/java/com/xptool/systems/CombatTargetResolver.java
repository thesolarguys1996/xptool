package com.xptool.systems;

import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

public final class CombatTargetResolver {
    private static final long RECENT_TARGET_REPEAT_GUARD_MS = 2600L;

    public interface Host {
        WorldView topLevelWorldView();

        boolean isCombatNpcSuppressed(int npcIndex);

        boolean npcMatchesPreferredTarget(
            NPC npc,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint
        );

        boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance);

        boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance);

        boolean isBrutusNpc(NPC npc);

        boolean isAttackableNpc(NPC npc);
    }

    private final Host host;
    private int lastChosenNpcIndex = -1;
    private long lastChosenAtMs = 0L;

    public CombatTargetResolver(Host host) {
        this.host = host;
    }

    public Optional<NPC> resolveNearestCombatTarget(
        Player local,
        int preferredNpcId,
        Set<Integer> preferredNpcIds,
        String preferredNpcNameHint,
        int targetWorldX,
        int targetWorldY,
        int targetMaxDistance,
        int maxChaseDistance,
        boolean brutusOnly
    ) {
        WorldView view = host.topLevelWorldView();
        if (view == null) {
            return Optional.empty();
        }
        WorldPoint localPos = local == null ? null : local.getWorldLocation();
        if (localPos == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        List<TargetCandidate> candidates = new ArrayList<>();
        for (NPC npc : view.npcs()) {
            if (npc == null) {
                continue;
            }
            if (host.isCombatNpcSuppressed(npc.getIndex())) {
                continue;
            }
            if (!host.npcMatchesPreferredTarget(npc, preferredNpcId, preferredNpcIds, preferredNpcNameHint)) {
                continue;
            }
            if (!host.isNpcWithinCombatArea(npc, targetWorldX, targetWorldY, targetMaxDistance)) {
                continue;
            }
            if (!host.isNpcWithinCombatChaseDistance(local, npc, maxChaseDistance)) {
                continue;
            }
            if (brutusOnly && !host.isBrutusNpc(npc)) {
                continue;
            }
            if (!host.isAttackableNpc(npc)) {
                continue;
            }
            WorldPoint world = npc.getWorldLocation();
            if (world == null) {
                continue;
            }
            int dist = localPos.distanceTo(world);
            if (dist < 0) {
                continue;
            }
            int score = scoreCandidate(dist, npc.getIndex(), now, false);
            candidates.add(new TargetCandidate(npc, dist, score));
        }
        return chooseCandidate(candidates, now);
    }

    public Optional<NPC> resolveNpcTargetingLocal(
        Player local,
        int targetWorldX,
        int targetWorldY,
        int targetMaxDistance,
        int maxChaseDistance,
        boolean brutusOnly
    ) {
        if (local == null) {
            return Optional.empty();
        }
        WorldView view = host.topLevelWorldView();
        if (view == null) {
            return Optional.empty();
        }
        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        List<TargetCandidate> candidates = new ArrayList<>();
        for (NPC npc : view.npcs()) {
            if (npc == null) {
                continue;
            }
            if (npc.getInteracting() != local) {
                continue;
            }
            if (brutusOnly && !host.isBrutusNpc(npc)) {
                continue;
            }
            if (!host.isNpcWithinCombatArea(npc, targetWorldX, targetWorldY, targetMaxDistance)) {
                continue;
            }
            if (!host.isNpcWithinCombatChaseDistance(local, npc, maxChaseDistance)) {
                continue;
            }
            if (!host.isAttackableNpc(npc)) {
                continue;
            }
            WorldPoint world = npc.getWorldLocation();
            if (world == null) {
                continue;
            }
            int dist = localPos.distanceTo(world);
            if (dist < 0) {
                continue;
            }
            int score = scoreCandidate(dist, npc.getIndex(), now, true);
            candidates.add(new TargetCandidate(npc, dist, score));
        }
        return chooseCandidate(candidates, now);
    }

    private int scoreCandidate(int distance, int npcIndex, long now, boolean inboundAttacker) {
        int distanceScore = Math.max(0, 360 - (distance * 36));
        int inboundBonus = inboundAttacker ? 90 : 0;
        int repeatPenalty = 0;
        if (npcIndex >= 0
            && npcIndex == lastChosenNpcIndex
            && (now - lastChosenAtMs) < RECENT_TARGET_REPEAT_GUARD_MS) {
            repeatPenalty = 170;
        }
        int jitter = ThreadLocalRandom.current().nextInt(-22, 31);
        return distanceScore + inboundBonus + jitter - repeatPenalty;
    }

    private Optional<NPC> chooseCandidate(List<TargetCandidate> candidates, long now) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() == 1) {
            TargetCandidate single = candidates.get(0);
            noteChosen(single.npc, now);
            return Optional.ofNullable(single.npc);
        }
        // Prefer the nearest available target deterministically.
        // Availability filtering (suppression, area, chase distance, attackable, preferred target)
        // is already handled before candidates are created.
        candidates.sort(
            Comparator
                .comparingInt((TargetCandidate c) -> c.distance)
                .thenComparing(Comparator.comparingInt((TargetCandidate c) -> c.score).reversed())
                .thenComparingInt(c -> c.npc == null ? Integer.MAX_VALUE : c.npc.getIndex())
        );
        TargetCandidate nearest = candidates.get(0);
        noteChosen(nearest.npc, now);
        return Optional.ofNullable(nearest.npc);
    }

    private void noteChosen(NPC npc, long now) {
        if (npc == null || npc.getIndex() < 0) {
            return;
        }
        lastChosenNpcIndex = npc.getIndex();
        lastChosenAtMs = now;
    }

    private static final class TargetCandidate {
        private final NPC npc;
        private final int distance;
        private final int score;

        private TargetCandidate(NPC npc, int distance, int score) {
            this.npc = npc;
            this.distance = distance;
            this.score = score;
        }
    }
}
