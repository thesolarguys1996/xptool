package com.xptool.executor;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

final class BrutusDodgeCandidateBuilder {
    List<WorldPoint> computeCandidates(
        WorldPoint localPos,
        WorldPoint brutusPos,
        BrutusTelegraph telegraph
    ) {
        List<WorldPoint> out = new ArrayList<>();
        if (localPos == null || brutusPos == null || telegraph == BrutusTelegraph.NONE) {
            return out;
        }
        int dx = Integer.compare(localPos.getX(), brutusPos.getX());
        int dy = Integer.compare(localPos.getY(), brutusPos.getY());
        if (dx == 0 && dy == 0) {
            return out;
        }
        int leftX = -dy;
        int leftY = dx;
        int rightX = dy;
        int rightY = -dx;
        if (telegraph == BrutusTelegraph.GROWL) {
            addCandidate(
                out,
                new WorldPoint(
                    localPos.getX() + (leftX * 3),
                    localPos.getY() + (leftY * 3),
                    localPos.getPlane()
                )
            );
            addCandidate(
                out,
                new WorldPoint(
                    localPos.getX() + (rightX * 3),
                    localPos.getY() + (rightY * 3),
                    localPos.getPlane()
                )
            );
            addCandidate(
                out,
                new WorldPoint(
                    localPos.getX() + (leftX * 4),
                    localPos.getY() + (leftY * 4),
                    localPos.getPlane()
                )
            );
            addCandidate(
                out,
                new WorldPoint(
                    localPos.getX() + (rightX * 4),
                    localPos.getY() + (rightY * 4),
                    localPos.getPlane()
                )
            );
        }
        addCandidate(
            out,
            new WorldPoint(
                localPos.getX() + (leftX * 2),
                localPos.getY() + (leftY * 2),
                localPos.getPlane()
            )
        );
        addCandidate(
            out,
            new WorldPoint(
                localPos.getX() + (rightX * 2),
                localPos.getY() + (rightY * 2),
                localPos.getPlane()
            )
        );
        if (telegraph == BrutusTelegraph.SNORT) {
            addCandidate(
                out,
                new WorldPoint(localPos.getX() + dx, localPos.getY() + dy, localPos.getPlane())
            );
        }
        return out;
    }

    private static void addCandidate(List<WorldPoint> out, WorldPoint candidate) {
        if (out == null || candidate == null) {
            return;
        }
        if (!out.contains(candidate)) {
            out.add(candidate);
        }
    }
}

