package com.xptool.executor;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedHashSet;
import java.util.Set;

final class DropSweepSessionService {
    private static final int INVENTORY_SLOT_COUNT = 28;

    private boolean sessionActive = false;
    private boolean awaitingFirstCursorSync = false;
    private int sessionItemId = -1;
    private final Set<Integer> sessionItemIds = new LinkedHashSet<>();
    private int nextSlot = 0;
    private int lastDispatchTick = Integer.MIN_VALUE;
    private int lastObservedQuantity = -1;
    private int noProgressStreak = 0;
    private boolean progressCheckPending = false;
    private int dispatchFailStreak = 0;
    private Rectangle regionScreen = null;
    private Point lastTargetScreen = null;
    private long sessionSerial = 0L;
    private long lastSessionEndedAtMs = 0L;

    boolean beginSession(int itemId, Set<Integer> normalizedItemIds, int initialObservedQuantity) {
        Set<Integer> safeItemIds = normalizedItemIds == null ? Set.of() : normalizedItemIds;
        boolean sessionChanged =
            !sessionActive
                || sessionItemId != itemId
                || !sessionItemIds.equals(safeItemIds);
        if (!sessionChanged) {
            return false;
        }

        sessionActive = true;
        sessionItemId = itemId;
        sessionItemIds.clear();
        sessionItemIds.addAll(safeItemIds);
        sessionSerial++;
        nextSlot = 0;
        lastTargetScreen = null;
        awaitingFirstCursorSync = true;
        lastObservedQuantity = initialObservedQuantity;
        noProgressStreak = 0;
        progressCheckPending = false;
        dispatchFailStreak = 0;
        return true;
    }

    boolean endSession(long nowMs) {
        boolean wasActive = sessionActive;
        sessionActive = false;
        sessionItemId = -1;
        sessionItemIds.clear();
        nextSlot = 0;
        regionScreen = null;
        lastTargetScreen = null;
        awaitingFirstCursorSync = false;
        lastObservedQuantity = -1;
        noProgressStreak = 0;
        progressCheckPending = false;
        dispatchFailStreak = 0;
        if (wasActive) {
            lastSessionEndedAtMs = nowMs;
        }
        return wasActive;
    }

    boolean hasActiveSession() {
        return sessionActive && sessionItemId > 0;
    }

    boolean isSessionActive() {
        return sessionActive;
    }

    Set<Integer> activeItemIds() {
        if (!hasActiveSession()) {
            return Set.of();
        }
        if (sessionItemIds.isEmpty()) {
            return Set.of(sessionItemId);
        }
        return Set.copyOf(sessionItemIds);
    }

    Set<Integer> configuredItemIds() {
        if (sessionItemIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(sessionItemIds);
    }

    int itemId() {
        return sessionItemId;
    }

    int nextSlot() {
        return nextSlot;
    }

    void setNextSlot(int slot) {
        nextSlot = Math.floorMod(slot, INVENTORY_SLOT_COUNT);
    }

    int lastDispatchTick() {
        return lastDispatchTick;
    }

    void setLastDispatchTick(int tick) {
        lastDispatchTick = tick;
    }

    int dispatchFailStreak() {
        return dispatchFailStreak;
    }

    boolean awaitingFirstCursorSync() {
        return awaitingFirstCursorSync;
    }

    void setAwaitingFirstCursorSync(boolean awaiting) {
        awaitingFirstCursorSync = awaiting;
    }

    void setProgressCheckPending(boolean pending) {
        progressCheckPending = pending;
    }

    Rectangle regionScreen() {
        return regionScreen == null ? null : new Rectangle(regionScreen);
    }

    void refreshRegionScreen(Rectangle refreshedRegionScreen) {
        if (refreshedRegionScreen == null) {
            return;
        }
        regionScreen = new Rectangle(refreshedRegionScreen);
    }

    Point lastTargetScreen() {
        return lastTargetScreen == null ? null : new Point(lastTargetScreen);
    }

    void setLastTargetScreen(Point point) {
        lastTargetScreen = point == null ? null : new Point(point);
    }

    long sessionSerial() {
        return sessionSerial;
    }

    long lastSessionEndedAtMs() {
        return lastSessionEndedAtMs;
    }

    void clearDispatchFailureStreak() {
        dispatchFailStreak = 0;
    }

    boolean noteDispatchFailure(int failLimit) {
        dispatchFailStreak++;
        return dispatchFailStreak < failLimit;
    }

    boolean updateProgressState(int currentQuantity, int currentTick, int noProgressLimit) {
        if (lastObservedQuantity < 0) {
            lastObservedQuantity = currentQuantity;
            noProgressStreak = 0;
            progressCheckPending = false;
            return true;
        }
        if (!progressCheckPending) {
            lastObservedQuantity = currentQuantity;
            return true;
        }
        if (currentTick <= lastDispatchTick) {
            return true;
        }
        if (currentQuantity < lastObservedQuantity) {
            noProgressStreak = 0;
        } else {
            noProgressStreak++;
        }
        lastObservedQuantity = currentQuantity;
        progressCheckPending = false;
        return noProgressStreak < noProgressLimit;
    }
}
