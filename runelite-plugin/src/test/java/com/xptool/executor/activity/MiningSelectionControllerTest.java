package com.xptool.executor.activity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningSelectionControllerTest {
    @Test
    void toggleSelectedTargetAddsAndThenRemovesTarget() {
        WorldPoint target = new WorldPoint(3200, 3200, 0);
        SelectionTargetWorldPointLookup lookup = (sceneX, sceneY, worldViewId) ->
            sceneX == 10 && sceneY == 20 ? Optional.of(target) : Optional.empty();
        MiningSelectionService selectionService = new MiningSelectionService(WorldPoint::equals);
        MiningTargetStateService targetStateService = new MiningTargetStateService();
        AtomicInteger clearTargetLockCalls = new AtomicInteger();
        AtomicInteger clearHoverCalls = new AtomicInteger();
        Runnable clearTargetLock = () -> {
            clearTargetLockCalls.incrementAndGet();
            targetStateService.clearTargetLock();
        };
        Runnable clearHoverPoint = clearHoverCalls::incrementAndGet;
        MiningSelectionController controller = new MiningSelectionController(
            lookup,
            selectionService,
            targetStateService,
            WorldPoint::equals,
            clearTargetLock,
            clearHoverPoint
        );

        boolean selected = controller.toggleSelectedTarget(10, 20, 0);
        boolean deselected = controller.toggleSelectedTarget(10, 20, 0);

        assertTrue(selected);
        assertFalse(deselected);
        assertEquals(1, clearTargetLockCalls.get());
        assertEquals(1, clearHoverCalls.get());
        assertEquals(0, selectionService.size());
        assertNull(targetStateService.lockedWorldPoint());
        assertNull(targetStateService.preferredSelectedWorldPoint());
        assertFalse(controller.isSelectedTarget(10, 20, 0));
    }

    @Test
    void isSelectedTargetReturnsFalseWhenTargetCannotBeResolved() {
        SelectionTargetWorldPointLookup lookup = (sceneX, sceneY, worldViewId) -> Optional.empty();
        MiningSelectionController controller = new MiningSelectionController(
            lookup,
            new MiningSelectionService(WorldPoint::equals),
            new MiningTargetStateService(),
            WorldPoint::equals,
            () -> { },
            () -> { }
        );

        assertFalse(controller.isSelectedTarget(0, 0, 0));
    }
}
