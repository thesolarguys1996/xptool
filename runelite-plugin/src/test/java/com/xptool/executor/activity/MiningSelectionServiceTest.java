package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningSelectionServiceTest {
    @Test
    void removeTargetsNearUsesExactMatcher() {
        MiningSelectionService service = new MiningSelectionService(WorldPoint::equals);
        WorldPoint first = new WorldPoint(3200, 3200, 0);
        WorldPoint second = new WorldPoint(3200, 3201, 0);
        service.add(first);
        service.add(second);

        service.removeTargetsNear(first);

        assertEquals(1, service.size());
        assertFalse(service.hasTargetNear(first));
        assertTrue(service.hasTargetNear(second));
    }

    @Test
    void latestSelectedWorldPointTracksInsertionOrder() {
        MiningSelectionService service = new MiningSelectionService(WorldPoint::equals);
        WorldPoint first = new WorldPoint(3200, 3200, 0);
        WorldPoint second = new WorldPoint(3201, 3201, 0);
        service.add(first);
        service.add(second);

        assertEquals(second, service.latestSelectedWorldPoint());
    }
}
