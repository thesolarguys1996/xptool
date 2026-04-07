package com.xptool.executor.activity;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class MiningTargetStateService {
    private WorldPoint lockedWorldPoint = null;
    private int lockedObjectId = -1;
    private WorldPoint preferredSelectedWorldPoint = null;

    public WorldPoint lockedWorldPoint() {
        return lockedWorldPoint;
    }

    public int lockedObjectId() {
        return lockedObjectId;
    }

    public WorldPoint preferredSelectedWorldPoint() {
        return preferredSelectedWorldPoint;
    }

    public boolean hasLockedTarget() {
        return lockedWorldPoint != null;
    }

    public boolean lockTarget(TileObject targetObject) {
        if (targetObject == null) {
            return false;
        }
        WorldPoint targetWorldPoint = targetObject.getWorldLocation();
        int targetObjectId = targetObject.getId();
        boolean changed = !targetWorldPoint.equals(lockedWorldPoint) || targetObjectId != lockedObjectId;
        lockedWorldPoint = targetWorldPoint;
        lockedObjectId = targetObjectId;
        return changed;
    }

    public void lockSelectedTarget(WorldPoint targetWorldPoint) {
        if (targetWorldPoint == null) {
            return;
        }
        preferredSelectedWorldPoint = targetWorldPoint;
        lockedWorldPoint = targetWorldPoint;
        lockedObjectId = -1;
    }

    public void clearTargetLock() {
        lockedWorldPoint = null;
        lockedObjectId = -1;
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
