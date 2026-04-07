package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningTargetStateServiceTest {
    @Test
    void lockSelectedTargetSetsPreferredAndLockedPointsAndResetsObjectId() {
        MiningTargetStateService service = new MiningTargetStateService();
        WorldPoint target = new WorldPoint(3200, 3200, 0);

        service.lockSelectedTarget(target);

        assertEquals(target, service.lockedWorldPoint());
        assertEquals(target, service.preferredSelectedWorldPoint());
        assertEquals(-1, service.lockedObjectId());
        assertTrue(service.hasLockedTarget());
    }

    @Test
    void clearSelectionContextResetsLockAndPreferredPoints() {
        MiningTargetStateService service = new MiningTargetStateService();
        service.lockSelectedTarget(new WorldPoint(3200, 3200, 0));

        service.clearSelectionContext();

        assertFalse(service.hasLockedTarget());
        assertNull(service.lockedWorldPoint());
        assertNull(service.preferredSelectedWorldPoint());
        assertEquals(-1, service.lockedObjectId());
    }
}
