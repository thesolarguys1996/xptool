package com.xptool.systems;

import java.util.Locale;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public final class CombatTargetPolicy {
    public interface Host {
        Player localPlayer();

        boolean isBrutusNpc(NPC npc);
    }

    private final Host host;

    public CombatTargetPolicy(Host host) {
        this.host = host;
    }

    public boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance) {
        if (local == null || npc == null) {
            return false;
        }
        WorldPoint localPos = local.getWorldLocation();
        WorldPoint npcPos = npc.getWorldLocation();
        if (localPos == null || npcPos == null) {
            return false;
        }
        int leash = Math.max(1, maxChaseDistance);
        int dist = localPos.distanceTo(npcPos);
        return dist >= 0 && dist <= leash;
    }

    public boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance) {
        if (targetWorldX <= 0 || targetWorldY <= 0) {
            return true;
        }
        if (npc == null || npc.getWorldLocation() == null) {
            return false;
        }
        WorldPoint wp = npc.getWorldLocation();
        int maxDistance = Math.max(1, targetMaxDistance);
        int deltaX = Math.abs(wp.getX() - targetWorldX);
        int deltaY = Math.abs(wp.getY() - targetWorldY);
        return deltaX <= maxDistance && deltaY <= maxDistance;
    }

    public boolean npcMatchesPreferredTarget(
        NPC npc,
        int preferredNpcId,
        Set<Integer> preferredNpcIds,
        String preferredNpcNameHint
    ) {
        if (npc == null) {
            return false;
        }
        NPCComposition comp = npc.getTransformedComposition();
        if (comp == null) {
            comp = npc.getComposition();
        }
        if (preferredNpcIds != null && !preferredNpcIds.isEmpty()) {
            if (preferredNpcIds.contains(npc.getId())) {
                return true;
            }
            if (comp != null && preferredNpcIds.contains(comp.getId())) {
                return true;
            }
            if (preferredNpcNameHint == null || preferredNpcNameHint.isEmpty()) {
                return false;
            }
            return preferredNpcNameHint.equals(normalizedNpcName(comp));
        }

        if (preferredNpcId <= 0) {
            return true;
        }
        if (npc.getId() == preferredNpcId) {
            return true;
        }
        if (comp != null && comp.getId() == preferredNpcId) {
            return true;
        }
        if (preferredNpcNameHint == null || preferredNpcNameHint.isEmpty()) {
            return false;
        }
        return preferredNpcNameHint.equals(normalizedNpcName(comp));
    }

    public boolean isAttackableNpc(NPC npc) {
        if (npc == null) {
            return false;
        }
        NPCComposition comp = npc.getTransformedComposition();
        if (comp == null) {
            comp = npc.getComposition();
        }
        if (comp == null || !comp.isInteractible()) {
            return false;
        }
        if (!hasAttackAction(comp.getActions())) {
            return false;
        }
        if (npc.getWorldLocation() == null) {
            return false;
        }
        int healthRatio = npc.getHealthRatio();
        if (healthRatio == 0) {
            return false;
        }
        if (host != null && host.isBrutusNpc(npc)) {
            return true;
        }
        Actor npcTarget = npc.getInteracting();
        Player local = host == null ? null : host.localPlayer();
        boolean targetsLocal = local != null && npcTarget == local;
        if (npcTarget != null && !targetsLocal) {
            return false;
        }
        if (healthRatio > 0 && !targetsLocal) {
            return false;
        }
        return true;
    }

    public static String normalizedNpcName(NPCComposition comp) {
        return comp == null ? "" : safeString(comp.getName()).trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasAttackAction(String[] actions) {
        if (actions == null || actions.length == 0) {
            return false;
        }
        for (String action : actions) {
            if ("Attack".equalsIgnoreCase(safeString(action).trim())) {
                return true;
            }
        }
        return false;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
