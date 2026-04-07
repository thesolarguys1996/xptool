package com.xptool.executor;

import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

final class BrutusDodgeProgressTracker {
    interface Host {
        WorldView topLevelWorldView();

        boolean isBrutusPathWalkable(WorldView view, WorldPoint from, WorldPoint to);

        void suppressBrutusDodgeTile(WorldPoint worldPoint, long durationMs);
    }

    private final Host host;
    private WorldPoint lastDodgeTargetWorldPoint = null;
    private WorldPoint lastDodgeStartWorldPoint = null;

    BrutusDodgeProgressTracker(Host host) {
        this.host = host;
    }

    void setLastDodgeStartWorldPoint(WorldPoint worldPoint) {
        lastDodgeStartWorldPoint = worldPoint;
    }

    void setLastDodgeTargetWorldPoint(WorldPoint worldPoint) {
        lastDodgeTargetWorldPoint = worldPoint;
    }

    WorldPoint lastDodgeTargetWorldPoint() {
        return lastDodgeTargetWorldPoint;
    }

    boolean isDodgeProgressActive(long lastDodgeAtMs, long now, long stuckTimeoutMs) {
        if (lastDodgeTargetWorldPoint == null
            || lastDodgeStartWorldPoint == null
            || lastDodgeAtMs <= 0L) {
            return false;
        }
        long elapsedMs = now - lastDodgeAtMs;
        return elapsedMs >= 0L && elapsedMs <= stuckTimeoutMs;
    }

    void updateDodgeProgressState(
        Player local,
        long now,
        long lastDodgeAtMs,
        long progressCheckMs,
        long stuckTimeoutMs,
        long tileSuppressMs
    ) {
        if (local == null
            || lastDodgeTargetWorldPoint == null
            || lastDodgeStartWorldPoint == null
            || lastDodgeAtMs <= 0L) {
            return;
        }
        if ((now - lastDodgeAtMs) < progressCheckMs) {
            return;
        }
        WorldPoint current = local.getWorldLocation();
        if (current == null) {
            return;
        }
        if (current.equals(lastDodgeTargetWorldPoint)) {
            clearDodgeProgressState();
            return;
        }
        WorldView view = host.topLevelWorldView();
        int startDist = lastDodgeStartWorldPoint.distanceTo2D(lastDodgeTargetWorldPoint);
        int currentDist = current.distanceTo2D(lastDodgeTargetWorldPoint);
        boolean madeProgress = startDist >= 0 && currentDist >= 0 && currentDist < startDist;
        boolean remainingPathWalkable = view != null
            && host.isBrutusPathWalkable(view, current, lastDodgeTargetWorldPoint);
        if (current.equals(lastDodgeStartWorldPoint) || !remainingPathWalkable) {
            host.suppressBrutusDodgeTile(lastDodgeTargetWorldPoint, tileSuppressMs);
            clearDodgeProgressState();
            return;
        }
        long elapsedMs = now - lastDodgeAtMs;
        if (!madeProgress) {
            host.suppressBrutusDodgeTile(lastDodgeTargetWorldPoint, tileSuppressMs);
            clearDodgeProgressState();
            return;
        }
        if (elapsedMs < stuckTimeoutMs) {
            return;
        }
        if (currentDist > 1) {
            host.suppressBrutusDodgeTile(lastDodgeTargetWorldPoint, tileSuppressMs);
        }
        clearDodgeProgressState();
    }

    private void clearDodgeProgressState() {
        lastDodgeStartWorldPoint = null;
        lastDodgeTargetWorldPoint = null;
    }
}
