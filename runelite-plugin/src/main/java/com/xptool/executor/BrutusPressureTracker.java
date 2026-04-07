package com.xptool.executor;

final class BrutusPressureTracker {
    private int lastNoSafeTileTick = Integer.MIN_VALUE;
    private int noSafeTileStreak = 0;

    void noteNoSafeTile(int currentTick) {
        if (currentTick == lastNoSafeTileTick) {
            return;
        }
        int tickDelta = currentTick - lastNoSafeTileTick;
        if (lastNoSafeTileTick != Integer.MIN_VALUE && tickDelta >= 0 && tickDelta <= 2) {
            noSafeTileStreak++;
        } else {
            noSafeTileStreak = 1;
        }
        lastNoSafeTileTick = currentTick;
    }

    void clearNoSafeTileState() {
        lastNoSafeTileTick = Integer.MIN_VALUE;
        noSafeTileStreak = 0;
    }

    boolean isNoSafeTilePressureActive(int currentTick, int recoveryWindowTicks) {
        if (noSafeTileStreak <= 0 || lastNoSafeTileTick == Integer.MIN_VALUE) {
            return false;
        }
        int tickDelta = currentTick - lastNoSafeTileTick;
        return tickDelta >= 0 && tickDelta <= recoveryWindowTicks;
    }

    int lastNoSafeTileTick() {
        return lastNoSafeTileTick;
    }

    int noSafeTileStreak() {
        return noSafeTileStreak;
    }
}
