package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodcuttingSelectionServiceTest {
    @Test
    void removeTargetsNearUsesConfiguredMatcher() {
        WoodcuttingSelectionService service = new WoodcuttingSelectionService(WoodcuttingSelectionServiceTest::withinOneTile);
        service.add(new WorldPoint(3200, 3200, 0));
        service.add(new WorldPoint(3201, 3200, 0));
        service.add(new WorldPoint(3205, 3205, 0));

        service.removeTargetsNear(new WorldPoint(3200, 3201, 0));

        assertEquals(1, service.size());
        assertTrue(service.hasTargetNear(new WorldPoint(3205, 3205, 0)));
    }

    @Test
    void latestSelectedWorldPointTracksInsertionOrder() {
        WoodcuttingSelectionService service = new WoodcuttingSelectionService(WorldPoint::equals);
        WorldPoint first = new WorldPoint(3200, 3200, 0);
        WorldPoint second = new WorldPoint(3201, 3201, 0);
        service.add(first);
        service.add(second);

        assertEquals(second, service.latestSelectedWorldPoint());
        assertFalse(service.isEmpty());
    }

    private static boolean withinOneTile(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return false;
        }
        int distance = a.distanceTo(b);
        return distance >= 0 && distance <= 1;
    }
}
