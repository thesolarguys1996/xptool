package com.xptool.executor;

import java.awt.Point;
import java.awt.Polygon;
import java.util.Objects;
import java.util.function.Predicate;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

final class BrutusNavigation {
    private final Client client;
    private final Predicate<Point> isCombatCanvasPointUsable;

    BrutusNavigation(Client client, Predicate<Point> isCombatCanvasPointUsable) {
        this.client = client;
        this.isCombatCanvasPointUsable = isCombatCanvasPointUsable == null ? p -> false : isCombatCanvasPointUsable;
    }

    boolean isFenceRiskTile(WorldView view, WorldPoint candidate, int escapeExits, int minEscapeExitsStrict) {
        if (view == null || candidate == null) {
            return true;
        }
        if (escapeExits < minEscapeExitsStrict) {
            return true;
        }
        boolean eastOpen = isStepWalkable(
            view,
            candidate,
            new WorldPoint(candidate.getX() + 1, candidate.getY(), candidate.getPlane())
        );
        boolean westOpen = isStepWalkable(
            view,
            candidate,
            new WorldPoint(candidate.getX() - 1, candidate.getY(), candidate.getPlane())
        );
        boolean northOpen = isStepWalkable(
            view,
            candidate,
            new WorldPoint(candidate.getX(), candidate.getY() + 1, candidate.getPlane())
        );
        boolean southOpen = isStepWalkable(
            view,
            candidate,
            new WorldPoint(candidate.getX(), candidate.getY() - 1, candidate.getPlane())
        );
        // Avoid edge/corridor tiles that tend to pin movement against fences.
        return !(eastOpen && westOpen) && !(northOpen && southOpen);
    }

    Point resolveWorldTileClickPoint(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return null;
        }
        Client safeClient = Objects.requireNonNull(client);
        WorldView view = safeClient.getTopLevelWorldView();
        if (view == null) {
            return null;
        }
        LocalPoint localPoint = LocalPoint.fromWorld(view, worldPoint);
        if (localPoint == null) {
            return null;
        }
        Point direct = CommandExecutor.toAwtPoint(Perspective.localToCanvas(safeClient, localPoint, view.getPlane()));
        if (isCombatCanvasPointUsable.test(direct)) {
            return direct;
        }
        Polygon poly = Perspective.getCanvasTilePoly(safeClient, localPoint);
        if (poly != null && poly.npoints > 0) {
            long sumX = 0L;
            long sumY = 0L;
            for (int i = 0; i < poly.npoints; i++) {
                sumX += poly.xpoints[i];
                sumY += poly.ypoints[i];
            }
            Point center = new Point(
                (int) Math.round((double) sumX / (double) poly.npoints),
                (int) Math.round((double) sumY / (double) poly.npoints)
            );
            if (isCombatCanvasPointUsable.test(center)) {
                return center;
            }
        }
        return null;
    }

    Point resolveWorldTileMinimapClickPoint(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return null;
        }
        Client safeClient = Objects.requireNonNull(client);
        WorldView view = safeClient.getTopLevelWorldView();
        if (view == null) {
            return null;
        }
        LocalPoint localPoint = LocalPoint.fromWorld(view, worldPoint);
        if (localPoint == null) {
            return null;
        }
        Point minimapPoint = CommandExecutor.toAwtPoint(Perspective.localToMinimap(safeClient, localPoint));
        if (isCombatCanvasPointUsable.test(minimapPoint)) {
            return minimapPoint;
        }
        return null;
    }

    WorldPoint resolveNearestWalkableWorldPoint(WorldPoint localWorldPoint, WorldPoint desiredWorldPoint, int maxRadiusTiles) {
        if (desiredWorldPoint == null) {
            return null;
        }
        Client safeClient = Objects.requireNonNull(client);
        WorldView view = safeClient.getTopLevelWorldView();
        if (view == null) {
            return desiredWorldPoint;
        }
        int radius = Math.max(0, maxRadiusTiles);
        if (isWorldPointWalkable(view, desiredWorldPoint)) {
            return desiredWorldPoint;
        }

        WorldPoint best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) {
                        continue;
                    }
                    WorldPoint candidate = new WorldPoint(
                        desiredWorldPoint.getX() + dx,
                        desiredWorldPoint.getY() + dy,
                        desiredWorldPoint.getPlane()
                    );
                    if (!isWorldPointWalkable(view, candidate)) {
                        continue;
                    }
                    int toDesired = chebyshevDistance(candidate, desiredWorldPoint);
                    int toLocal = localWorldPoint == null ? 0 : chebyshevDistance(candidate, localWorldPoint);
                    int score = (toDesired * 100) + toLocal;
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    boolean isWorldPointWalkable(WorldView view, WorldPoint worldPoint) {
        int flags = collisionFlagsForWorldPoint(view, worldPoint);
        if (flags == Integer.MAX_VALUE) {
            return false;
        }
        return (flags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
    }

    boolean isPathWalkable(WorldView view, WorldPoint from, WorldPoint to) {
        if (view == null || from == null || to == null) {
            return false;
        }
        if (from.getPlane() != to.getPlane()) {
            return false;
        }
        int x = from.getX();
        int y = from.getY();
        int steps = 0;
        while (x != to.getX() || y != to.getY()) {
            if (++steps > 8) {
                return false;
            }
            int nextX = x + Integer.compare(to.getX(), x);
            int nextY = y + Integer.compare(to.getY(), y);
            WorldPoint stepFrom = new WorldPoint(x, y, from.getPlane());
            WorldPoint stepTo = new WorldPoint(nextX, nextY, from.getPlane());
            if (!isStepWalkable(view, stepFrom, stepTo)) {
                return false;
            }
            x = nextX;
            y = nextY;
        }
        return true;
    }

    boolean isStepWalkable(WorldView view, WorldPoint from, WorldPoint to) {
        if (view == null || from == null || to == null) {
            return false;
        }
        if (from.getPlane() != to.getPlane()) {
            return false;
        }
        int dx = Integer.compare(to.getX(), from.getX());
        int dy = Integer.compare(to.getY(), from.getY());
        if (dx == 0 && dy == 0) {
            return true;
        }
        if (Math.abs(to.getX() - from.getX()) > 1 || Math.abs(to.getY() - from.getY()) > 1) {
            return false;
        }
        if (!isWorldPointWalkable(view, from) || !isWorldPointWalkable(view, to)) {
            return false;
        }
        int fromFlags = collisionFlagsForWorldPoint(view, from);
        int toFlags = collisionFlagsForWorldPoint(view, to);
        if (fromFlags == Integer.MAX_VALUE || toFlags == Integer.MAX_VALUE) {
            return false;
        }

        if (dx > 0 && ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0
            || (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0)) {
            return false;
        }
        if (dx < 0 && ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0
            || (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0)) {
            return false;
        }
        if (dy > 0 && ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0
            || (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0)) {
            return false;
        }
        if (dy < 0 && ((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0
            || (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0)) {
            return false;
        }

        if (dx > 0 && dy > 0
            && (((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST) != 0)
            || ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST) != 0))) {
            return false;
        }
        if (dx > 0 && dy < 0
            && (((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST) != 0)
            || ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST) != 0))) {
            return false;
        }
        if (dx < 0 && dy > 0
            && (((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST) != 0)
            || ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST) != 0))) {
            return false;
        }
        if (dx < 0 && dy < 0
            && (((fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST) != 0)
            || ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST) != 0))) {
            return false;
        }

        if (dx != 0 && dy != 0) {
            WorldPoint sideA = new WorldPoint(from.getX() + dx, from.getY(), from.getPlane());
            WorldPoint sideB = new WorldPoint(from.getX(), from.getY() + dy, from.getPlane());
            if (!isWorldPointWalkable(view, sideA) || !isWorldPointWalkable(view, sideB)) {
                return false;
            }
        }
        return true;
    }

    int countEscapeExits(WorldView view, WorldPoint center) {
        if (view == null || center == null) {
            return 0;
        }
        int exits = 0;
        int[][] dirs = new int[][] {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
        };
        for (int[] dir : dirs) {
            WorldPoint neighbor = new WorldPoint(
                center.getX() + dir[0],
                center.getY() + dir[1],
                center.getPlane()
            );
            if (isStepWalkable(view, center, neighbor)) {
                exits++;
            }
        }
        return exits;
    }

    static int countBlockedCardinalEdges(int flags) {
        int blocked = 0;
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0) {
            blocked++;
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0) {
            blocked++;
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) {
            blocked++;
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) {
            blocked++;
        }
        return blocked;
    }

    private static int chebyshevDistance(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return Integer.MAX_VALUE;
        }
        if (a.getPlane() != b.getPlane()) {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    int collisionFlagsForWorldPoint(WorldView view, WorldPoint worldPoint) {
        if (view == null || worldPoint == null) {
            return Integer.MAX_VALUE;
        }
        if (worldPoint.getPlane() < 0) {
            return Integer.MAX_VALUE;
        }
        int sceneX = worldPoint.getX() - view.getBaseX();
        int sceneY = worldPoint.getY() - view.getBaseY();
        if (sceneX < 0 || sceneY < 0 || sceneX >= view.getSizeX() || sceneY >= view.getSizeY()) {
            return Integer.MAX_VALUE;
        }
        CollisionData[] collisionMaps = view.getCollisionMaps();
        if (collisionMaps == null || collisionMaps.length == 0) {
            return 0;
        }
        int plane = worldPoint.getPlane();
        if (plane < 0 || plane >= collisionMaps.length) {
            return Integer.MAX_VALUE;
        }
        CollisionData data = collisionMaps[plane];
        if (data == null || data.getFlags() == null) {
            return 0;
        }
        int[][] flags = data.getFlags();
        if (sceneX >= flags.length || flags[sceneX] == null || sceneY >= flags[sceneX].length) {
            return Integer.MAX_VALUE;
        }
        return flags[sceneX][sceneY];
    }
}
