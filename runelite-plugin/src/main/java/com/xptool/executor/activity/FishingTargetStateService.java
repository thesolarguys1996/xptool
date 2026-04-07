package com.xptool.executor.activity;

import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

public final class FishingTargetStateService {
    private int lockedNpcIndex = -1;
    private WorldPoint lockedWorldPoint = null;

    public int lockedNpcIndex() {
        return lockedNpcIndex;
    }

    public WorldPoint lockedWorldPoint() {
        return lockedWorldPoint;
    }

    public boolean hasLockedTarget() {
        return lockedNpcIndex >= 0 || lockedWorldPoint != null;
    }

    public void lockTarget(NPC npc) {
        if (npc == null || npc.getWorldLocation() == null) {
            return;
        }
        lockedNpcIndex = npc.getIndex();
        lockedWorldPoint = npc.getWorldLocation();
    }

    public void clearTargetLock() {
        lockedNpcIndex = -1;
        lockedWorldPoint = null;
    }
}
