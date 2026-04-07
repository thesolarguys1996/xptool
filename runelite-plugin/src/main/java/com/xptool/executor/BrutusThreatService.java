package com.xptool.executor;

import com.xptool.systems.CombatTargetPolicy;
import java.util.Locale;
import java.util.function.Predicate;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

final class BrutusThreatService {
    private final net.runelite.api.Client client;
    private final Predicate<NPC> isAttackableNpc;
    private final String brutusNameToken;

    BrutusThreatService(
        net.runelite.api.Client client,
        Predicate<NPC> isAttackableNpc,
        String brutusNameToken
    ) {
        this.client = client;
        this.isAttackableNpc = isAttackableNpc;
        this.brutusNameToken = safeString(brutusNameToken).trim().toLowerCase(Locale.ROOT);
    }

    boolean isBrutusNpc(NPC npc) {
        if (npc == null) {
            return false;
        }
        NPCComposition comp = npc.getTransformedComposition();
        if (comp == null) {
            comp = npc.getComposition();
        }
        String compName = CombatTargetPolicy.normalizedNpcName(comp);
        if (!compName.isEmpty()) {
            return compName.contains(brutusNameToken);
        }
        return safeString(npc.getName()).trim().toLowerCase(Locale.ROOT).contains(brutusNameToken);
    }

    BrutusTelegraph detectTelegraph(NPC npc) {
        if (npc == null) {
            return BrutusTelegraph.NONE;
        }
        String overhead = safeString(npc.getOverheadText()).trim().toLowerCase(Locale.ROOT);
        if (overhead.contains("snort")) {
            return BrutusTelegraph.SNORT;
        }
        if (overhead.contains("growl")) {
            return BrutusTelegraph.GROWL;
        }
        return BrutusTelegraph.NONE;
    }

    BrutusTelegraph detectNearbyTelegraph(Player local, int maxDistanceTiles) {
        if (local == null) {
            return BrutusTelegraph.NONE;
        }
        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null) {
            return BrutusTelegraph.NONE;
        }
        WorldView view = client.getTopLevelWorldView();
        if (view == null) {
            return BrutusTelegraph.NONE;
        }
        int maxDistance = Math.max(1, maxDistanceTiles);
        for (NPC npc : view.npcs()) {
            if (npc == null || !isBrutusNpc(npc)) {
                continue;
            }
            WorldPoint npcPos = npc.getWorldLocation();
            if (npcPos == null) {
                continue;
            }
            int dist = localPos.distanceTo2D(npcPos);
            if (dist < 0 || dist > maxDistance) {
                continue;
            }
            BrutusTelegraph telegraph = detectTelegraph(npc);
            if (telegraph != BrutusTelegraph.NONE) {
                return telegraph;
            }
        }
        return BrutusTelegraph.NONE;
    }

    boolean isBrutusNpcNearby(Player local, int maxDistance) {
        if (local == null) {
            return false;
        }
        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null) {
            return false;
        }
        WorldView view = client.getTopLevelWorldView();
        if (view == null) {
            return false;
        }
        int leash = Math.max(1, maxDistance);
        for (NPC npc : view.npcs()) {
            if (npc == null || !isBrutusNpc(npc) || !isAttackableNpc.test(npc)) {
                continue;
            }
            WorldPoint npcPos = npc.getWorldLocation();
            if (npcPos == null) {
                continue;
            }
            int dist = localPos.distanceTo2D(npcPos);
            if (dist >= 0 && dist <= leash) {
                return true;
            }
        }
        return false;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}

