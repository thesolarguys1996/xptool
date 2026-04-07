package com.xptool.executor;

import net.runelite.api.coords.WorldPoint;

final class BrutusCombatArea {
    private BrutusCombatArea() {
    }

    static boolean isWorldPointWithinTargetArea(
        WorldPoint worldPoint,
        int targetWorldX,
        int targetWorldY,
        int targetMaxDistance
    ) {
        if (worldPoint == null) {
            return false;
        }
        if (targetWorldX <= 0 || targetWorldY <= 0) {
            return true;
        }
        int distance = Math.max(
            Math.abs(worldPoint.getX() - targetWorldX),
            Math.abs(worldPoint.getY() - targetWorldY)
        );
        return distance <= Math.max(1, targetMaxDistance);
    }
}
