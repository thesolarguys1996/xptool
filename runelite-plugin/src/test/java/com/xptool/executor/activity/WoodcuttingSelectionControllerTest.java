package com.xptool.executor.activity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodcuttingSelectionControllerTest {
    @Test
    void toggleSelectedTargetAddsAndThenRemovesTarget() {
        WorldPoint target = new WorldPoint(3200, 3200, 0);
        SelectionTargetWorldPointLookup lookup = (sceneX, sceneY, worldViewId) ->
            sceneX == 10 && sceneY == 20 ? Optional.of(target) : Optional.empty();
        BiPredicate<WorldPoint, WorldPoint> matcher = WoodcuttingSelectionControllerTest::withinOneTile;
        WoodcuttingSelectionService selectionService = new WoodcuttingSelectionService(matcher);
        WoodcuttingTargetStateService targetStateService = new WoodcuttingTargetStateService();
        AtomicInteger clearTargetLockCalls = new AtomicInteger();
        Runnable clearTargetLock = () -> {
            clearTargetLockCalls.incrementAndGet();
            targetStateService.clearTargetLock();
        };
        WoodcuttingSelectionController controller = new WoodcuttingSelectionController(
            lookup,
            selectionService,
            targetStateService,
            matcher,
            clearTargetLock
        );

        boolean selected = controller.toggleSelectedTarget(10, 20, 0);
        boolean deselected = controller.toggleSelectedTarget(10, 20, 0);

        assertTrue(selected);
        assertFalse(deselected);
        assertEquals(1, clearTargetLockCalls.get());
        assertEquals(0, selectionService.size());
        assertNull(targetStateService.lockedWorldPoint());
        assertNull(targetStateService.preferredSelectedWorldPoint());
        assertFalse(controller.isSelectedTarget(10, 20, 0));
    }

    @Test
    void isSelectedTargetReturnsFalseWhenTargetCannotBeResolved() {
        SelectionTargetWorldPointLookup lookup = (sceneX, sceneY, worldViewId) -> Optional.empty();
        WoodcuttingSelectionController controller = new WoodcuttingSelectionController(
            lookup,
            new WoodcuttingSelectionService(WoodcuttingSelectionControllerTest::withinOneTile),
            new WoodcuttingTargetStateService(),
            WoodcuttingSelectionControllerTest::withinOneTile,
            () -> { }
        );

        assertFalse(controller.isSelectedTarget(0, 0, 0));
    }

    private static boolean withinOneTile(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return false;
        }
        int distance = a.distanceTo(b);
        return distance >= 0 && distance <= 1;
    }
}
