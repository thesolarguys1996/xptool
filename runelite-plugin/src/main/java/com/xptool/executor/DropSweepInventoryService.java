package com.xptool.executor;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;

final class DropSweepInventoryService {
    interface InventoryView {
        int slotCount();

        int itemIdAt(int slot);

        int quantityAt(int slot);
    }

    private final int clueScrollItemId;
    private final int scrollBoxItemId;
    private final String clueScrollName;
    private final String scrollBoxName;

    DropSweepInventoryService(
        int clueScrollItemId,
        int scrollBoxItemId,
        String clueScrollName,
        String scrollBoxName
    ) {
        this.clueScrollItemId = clueScrollItemId;
        this.scrollBoxItemId = scrollBoxItemId;
        this.clueScrollName = ExecutorValueParsers.safeString(clueScrollName);
        this.scrollBoxName = ExecutorValueParsers.safeString(scrollBoxName);
    }

    Set<Integer> normalizeItemIds(int itemId, Set<Integer> itemIds) {
        Set<Integer> normalized = new LinkedHashSet<>();
        if (itemIds != null) {
            for (Integer value : itemIds) {
                int parsed = value == null ? -1 : value.intValue();
                if (parsed > 0) {
                    normalized.add(parsed);
                }
            }
        }
        if (itemId > 0) {
            normalized.add(itemId);
        }
        return normalized;
    }

    Optional<Integer> findInventorySlotFrom(
        InventoryView inventoryView,
        int sessionItemId,
        Set<Integer> configuredItemIds,
        int startSlot,
        IntFunction<String> itemNameLookup
    ) {
        if (inventoryView == null || sessionItemId <= 0) {
            return Optional.empty();
        }
        int slotCount = Math.max(0, Math.min(28, inventoryView.slotCount()));
        if (slotCount == 0) {
            return Optional.empty();
        }
        int start = Math.max(0, Math.min(slotCount - 1, startSlot));
        for (int i = 0; i < slotCount; i++) {
            int idx = (start + i) % slotCount;
            int itemId = inventoryView.itemIdAt(idx);
            int quantity = inventoryView.quantityAt(idx);
            if (quantity <= 0) {
                continue;
            }
            if (isDropSweepTargetItem(itemId, sessionItemId, configuredItemIds, itemNameLookup)) {
                return Optional.of(idx);
            }
        }
        return Optional.empty();
    }

    int countDropSweepTargetItems(
        InventoryView inventoryView,
        int sessionItemId,
        Set<Integer> configuredItemIds,
        IntFunction<String> itemNameLookup
    ) {
        if (inventoryView == null || sessionItemId <= 0) {
            return 0;
        }
        int slotCount = Math.max(0, Math.min(28, inventoryView.slotCount()));
        int total = 0;
        for (int idx = 0; idx < slotCount; idx++) {
            int itemId = inventoryView.itemIdAt(idx);
            int quantity = inventoryView.quantityAt(idx);
            if (quantity <= 0) {
                continue;
            }
            if (isDropSweepTargetItem(itemId, sessionItemId, configuredItemIds, itemNameLookup)) {
                total += quantity;
            }
        }
        return total;
    }

    boolean isDropSweepTargetItem(
        int candidateItemId,
        int sessionItemId,
        Set<Integer> configuredItemIds,
        IntFunction<String> itemNameLookup
    ) {
        if (candidateItemId <= 0 || sessionItemId <= 0) {
            return false;
        }
        if (configuredItemIds != null && !configuredItemIds.isEmpty() && configuredItemIds.contains(candidateItemId)) {
            return true;
        }
        if (candidateItemId == sessionItemId) {
            return true;
        }
        return isBeginnerClueOrBoxItem(candidateItemId, itemNameLookup);
    }

    private boolean isBeginnerClueOrBoxItem(int itemId, IntFunction<String> itemNameLookup) {
        if (itemId <= 0) {
            return false;
        }
        if (itemId == clueScrollItemId || itemId == scrollBoxItemId) {
            return true;
        }
        String resolvedName = ExecutorValueParsers.safeString(itemNameLookup.apply(itemId)).trim();
        return clueScrollName.equalsIgnoreCase(resolvedName) || scrollBoxName.equalsIgnoreCase(resolvedName);
    }
}
