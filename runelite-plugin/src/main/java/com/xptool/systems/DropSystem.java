package com.xptool.systems;

import com.xptool.models.Snapshot;
import com.xptool.models.Snapshot.InventoryItem;
import com.xptool.motor.BaseMotorEngine;
import java.awt.Point;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DropSystem implements RuntimeSystem {
    private final BaseMotorEngine motorEngine;
    private final Set<Integer> dropItemIds;

    public DropSystem(BaseMotorEngine motorEngine, Set<Integer> dropItemIds) {
        this.motorEngine = motorEngine;
        this.dropItemIds = dropItemIds == null ? Set.of() : new LinkedHashSet<>(dropItemIds);
    }

    @Override
    public String name() {
        return "DropSystem";
    }

    @Override
    public boolean shouldRun(Snapshot snapshot) {
        return snapshot != null
            && snapshot.loggedIn
            && snapshot.isInventoryFull()
            && hasDropCandidate(snapshot);
    }

    @Override
    public boolean run(Snapshot snapshot) {
        if (!shouldRun(snapshot)) {
            return false;
        }
        int slot = firstDropCandidateSlot(snapshot);
        if (slot < 0) {
            return false;
        }
        Point slotPoint = approximateInventorySlotCenter(
            slot,
            motorEngine.canvasWidth(),
            motorEngine.canvasHeight()
        );
        if (slotPoint == null) {
            return false;
        }
        return motorEngine.moveMouse(slotPoint.x, slotPoint.y);
    }

    public boolean hasDropCandidate(Snapshot snapshot) {
        if (snapshot == null || dropItemIds.isEmpty()) {
            return false;
        }
        for (InventoryItem item : snapshot.inventory) {
            if (item == null || item.itemId <= 0 || item.quantity <= 0) {
                continue;
            }
            if (dropItemIds.contains(item.itemId)) {
                return true;
            }
        }
        return false;
    }

    private int firstDropCandidateSlot(Snapshot snapshot) {
        if (snapshot == null || dropItemIds.isEmpty()) {
            return -1;
        }
        int bestSlot = Integer.MAX_VALUE;
        for (InventoryItem item : snapshot.inventory) {
            if (item == null || item.itemId <= 0 || item.quantity <= 0) {
                continue;
            }
            if (dropItemIds.contains(item.itemId) && item.slot >= 0) {
                bestSlot = Math.min(bestSlot, item.slot);
            }
        }
        return bestSlot == Integer.MAX_VALUE ? -1 : bestSlot;
    }

    private static Point approximateInventorySlotCenter(int slot, int canvasWidth, int canvasHeight) {
        if (slot < 0 || slot >= 28 || canvasWidth <= 0 || canvasHeight <= 0) {
            return null;
        }
        int col = slot % 4;
        int row = slot / 4;
        int panelLeft = Math.max(0, canvasWidth - 246);
        int panelTop = Math.max(0, canvasHeight - 341);
        int x = panelLeft + 24 + (col * 42);
        int y = panelTop + 26 + (row * 36);
        if (x < 1 || y < 1) {
            return null;
        }
        return new Point(x, y);
    }
}

