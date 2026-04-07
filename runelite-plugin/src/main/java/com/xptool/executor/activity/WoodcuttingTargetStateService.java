package com.xptool.executor.activity;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class WoodcuttingTargetStateService {
    private WorldPoint lockedWorldPoint = null;
    private WorldPoint preferredSelectedWorldPoint = null;

    public WorldPoint lockedWorldPoint() {
        return lockedWorldPoint;
    }

    public WorldPoint preferredSelectedWorldPoint() {
        return preferredSelectedWorldPoint;
    }

    public boolean hasLockedTarget() {
        return lockedWorldPoint != null;
    }

    public boolean lockTarget(TileObject targetObject) {
        WorldPoint targetWorldPoint = targetObject == null ? null : targetObject.getWorldLocation();
        if (targetWorldPoint == null) {
            return false;
        }
        boolean changed = !targetWorldPoint.equals(lockedWorldPoint);
        lockedWorldPoint = targetWorldPoint;
        return changed;
    }

    public void lockSelectedTarget(WorldPoint targetWorldPoint) {
        if (targetWorldPoint == null) {
            return;
        }
        preferredSelectedWorldPoint = targetWorldPoint;
        lockedWorldPoint = targetWorldPoint;
    }

    public void clearTargetLock() {
        lockedWorldPoint = null;
    }

    public void setPreferredSelectedWorldPoint(WorldPoint worldPoint) {
        preferredSelectedWorldPoint = worldPoint;
    }

    public void clearPreferredSelectedWorldPoint() {
        preferredSelectedWorldPoint = null;
    }

    public void clearSelectionContext() {
        clearTargetLock();
        clearPreferredSelectedWorldPoint();
    }
}
