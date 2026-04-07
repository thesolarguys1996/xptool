package com.xptool.executor;

import java.util.Optional;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;

final class BrutusEncounterResolver {
    interface Host {
        boolean isBrutusNpc(NPC npc);

        boolean npcMatchesPreferredTarget(
            NPC npc,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint
        );

        boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance);

        boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance);

        Optional<NPC> resolveNearestCombatTarget(
            Player local,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance,
            int maxChaseDistance,
            boolean requireAttackable
        );
    }

    private final Host host;

    BrutusEncounterResolver(Host host) {
        this.host = host;
    }

    Optional<NPC> resolveBrutusEncounterNpc(
        Player local,
        int preferredNpcId,
        Set<Integer> preferredNpcIds,
        String preferredNpcNameHint,
        int targetWorldX,
        int targetWorldY,
        int targetMaxDistance,
        int maxChaseDistance
    ) {
        if (local == null) {
            return Optional.empty();
        }
        Actor interacting = local.getInteracting();
        if (interacting instanceof NPC) {
            NPC engagedNpc = (NPC) interacting;
            if (host.isBrutusNpc(engagedNpc)
                && host.npcMatchesPreferredTarget(engagedNpc, preferredNpcId, preferredNpcIds, preferredNpcNameHint)
                && host.isNpcWithinCombatArea(engagedNpc, targetWorldX, targetWorldY, targetMaxDistance)
                && host.isNpcWithinCombatChaseDistance(local, engagedNpc, maxChaseDistance)) {
                return Optional.of(engagedNpc);
            }
        }
        Optional<NPC> nearest = host.resolveNearestCombatTarget(
            local,
            preferredNpcId,
            preferredNpcIds,
            preferredNpcNameHint,
            targetWorldX,
            targetWorldY,
            targetMaxDistance,
            maxChaseDistance,
            true
        );
        if (nearest.isPresent() && host.isBrutusNpc(nearest.get())) {
            return nearest;
        }
        return Optional.empty();
    }
}
