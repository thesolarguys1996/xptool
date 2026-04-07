package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodcuttingTargetStateServiceTest {
    @Test
    void lockSelectedTargetSetsPreferredAndLockedPoints() {
        WoodcuttingTargetStateService service = new WoodcuttingTargetStateService();
        WorldPoint target = new WorldPoint(3200, 3200, 0);

        service.lockSelectedTarget(target);

        assertEquals(target, service.lockedWorldPoint());
        assertEquals(target, service.preferredSelectedWorldPoint());
        assertTrue(service.hasLockedTarget());
    }

    @Test
    void clearSelectionContextResetsLockAndPreferredPoints() {
        WoodcuttingTargetStateService service = new WoodcuttingTargetStateService();
        service.lockSelectedTarget(new WorldPoint(3200, 3200, 0));

        service.clearSelectionContext();

        assertFalse(service.hasLockedTarget());
        assertNull(service.lockedWorldPoint());
        assertNull(service.preferredSelectedWorldPoint());
    }
}
