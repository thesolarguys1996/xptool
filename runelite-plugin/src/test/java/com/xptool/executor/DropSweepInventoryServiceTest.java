package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DropSweepInventoryServiceTest {
    @Test
    void normalizeItemIdsIncludesPrimaryItemAndFiltersInvalidValues() {
        DropSweepInventoryService service = new DropSweepInventoryService(
            23182,
            24361,
            "Clue scroll (beginner)",
            "Scroll box (beginner)"
        );

        Set<Integer> normalized = service.normalizeItemIds(1511, Set.of(1513, -1, 0, 1511));
        assertEquals(Set.of(1511, 1513), normalized);
    }

    @Test
    void findInventorySlotFromRespectsStartSlotAndTargetPolicy() {
        DropSweepInventoryService service = new DropSweepInventoryService(
            23182,
            24361,
            "Clue scroll (beginner)",
            "Scroll box (beginner)"
        );
        DropSweepInventoryService.InventoryView inventoryView = inventory(
            new int[] { 1511, 1513, 1511, -1 },
            new int[] { 1, 1, 1, 0 }
        );

        Optional<Integer> slot = service.findInventorySlotFrom(
            inventoryView,
            1511,
            Set.of(1511),
            1,
            itemId -> ""
        );

        assertTrue(slot.isPresent());
        assertEquals(2, slot.get().intValue());
    }

    @Test
    void countDropSweepTargetItemsIncludesAliasItemsByName() {
        DropSweepInventoryService service = new DropSweepInventoryService(
            23182,
            24361,
            "Clue scroll (beginner)",
            "Scroll box (beginner)"
        );
        DropSweepInventoryService.InventoryView inventoryView = inventory(
            new int[] { 1511, 50000, 1511 },
            new int[] { 3, 2, 1 }
        );

        int quantity = service.countDropSweepTargetItems(
            inventoryView,
            1511,
            Set.of(1511),
            itemId -> itemId == 50000 ? "Scroll box (beginner)" : ""
        );
        assertEquals(6, quantity);

        assertFalse(service.isDropSweepTargetItem(40000, 1511, Set.of(1511), itemId -> ""));
    }

    private static DropSweepInventoryService.InventoryView inventory(int[] ids, int[] quantities) {
        return new DropSweepInventoryService.InventoryView() {
            @Override
            public int slotCount() {
                return Math.min(ids.length, quantities.length);
            }

            @Override
            public int itemIdAt(int slot) {
                return ids[slot];
            }

            @Override
            public int quantityAt(int slot) {
                return quantities[slot];
            }
        };
    }
}
