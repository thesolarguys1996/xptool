package com.xptool.executor;

import java.util.List;
import java.util.Optional;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

final class BrutusDodgeTileSelector {
    interface Host {
        WorldView topLevelWorldView();

        boolean isWorldPointWithinCombatArea(
            WorldPoint worldPoint,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance
        );

        boolean isBrutusDodgeTileSuppressed(WorldPoint worldPoint, long now);

        boolean isWorldPointWalkable(WorldView view, WorldPoint worldPoint);

        boolean isBrutusPathWalkable(WorldView view, WorldPoint from, WorldPoint to);

        int countBrutusEscapeExits(WorldView view, WorldPoint center);

        int combatBrutusMinEscapeExits();

        boolean isBrutusFenceRiskTile(WorldView view, WorldPoint candidate, int escapeExits);

        boolean hasCombatBoundary();

        int combatBoundaryCenterX();

        int combatBoundaryCenterY();

        WorldPoint brutusLastDodgeTargetWorldPoint();
    }

    private final Host host;

    BrutusDodgeTileSelector(Host host) {
        this.host = host;
    }

    Optional<WorldPoint> selectBestTile(
        List<WorldPoint> candidates,
        WorldPoint localPos,
        WorldPoint brutusPos,
        BrutusTelegraph telegraph,
        int targetWorldX,
        int targetWorldY,
        int targetMaxDistance,
        long now
    ) {
        if (candidates == null || candidates.isEmpty() || localPos == null || brutusPos == null) {
            return Optional.empty();
        }
        WorldView view = host.topLevelWorldView();
        if (view == null) {
            return Optional.empty();
        }

        WorldPoint best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < candidates.size(); i++) {
            WorldPoint candidate = candidates.get(i);
            if (candidate == null) {
                continue;
            }
            if (candidate.getPlane() != localPos.getPlane()) {
                continue;
            }
            if (!host.isWorldPointWithinCombatArea(candidate, targetWorldX, targetWorldY, targetMaxDistance)) {
                continue;
            }
            if (host.isBrutusDodgeTileSuppressed(candidate, now)) {
                continue;
            }
            if (!host.isWorldPointWalkable(view, candidate)) {
                continue;
            }
            if (!host.isBrutusPathWalkable(view, localPos, candidate)) {
                continue;
            }
            int escapeExits = host.countBrutusEscapeExits(view, candidate);
            if (escapeExits < host.combatBrutusMinEscapeExits()) {
                continue;
            }
            if (host.isBrutusFenceRiskTile(view, candidate, escapeExits)) {
                continue;
            }

            int moveDist = localPos.distanceTo2D(candidate);
            int brutusDist = brutusPos.distanceTo2D(candidate);
            int boundaryDist = host.hasCombatBoundary()
                ? Math.max(
                    Math.abs(candidate.getX() - host.combatBoundaryCenterX()),
                    Math.abs(candidate.getY() - host.combatBoundaryCenterY())
                )
                : 0;
            int score;
            if (telegraph == BrutusTelegraph.GROWL) {
                score =
                    (escapeExits * 120)
                    - (moveDist * 12)
                    + (brutusDist * 46)
                    - (boundaryDist * 12)
                    - (i * 8);
            } else {
                score =
                    (escapeExits * 120)
                    - (moveDist * 36)
                    + (brutusDist * 20)
                    - (boundaryDist * 14)
                    - (i * 12);
            }
            if (host.brutusLastDodgeTargetWorldPoint() != null && host.brutusLastDodgeTargetWorldPoint().equals(candidate)) {
                score -= 140;
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }
}

