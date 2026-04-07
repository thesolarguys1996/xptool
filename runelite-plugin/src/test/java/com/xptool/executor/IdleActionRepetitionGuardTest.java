package com.xptool.executor;

import java.awt.Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdleActionRepetitionGuardTest {
    @Test
    void blocksNearbyRepeatForSameReasonWithinWindow() {
        IdleActionRepetitionGuard guard = new IdleActionRepetitionGuard();
        guard.recordAction(100, "idle_hover_move", new Point(320, 240));

        assertTrue(guard.isRepeated(120, "idle_hover_move", new Point(328, 246)));
    }

    @Test
    void allowsFarTargetForSameReasonWithinWindow() {
        IdleActionRepetitionGuard guard = new IdleActionRepetitionGuard();
        guard.recordAction(100, "idle_hover_move", new Point(320, 240));

        assertFalse(guard.isRepeated(120, "idle_hover_move", new Point(390, 300)));
    }

    @Test
    void allowsRepeatOutsideWindow() {
        IdleActionRepetitionGuard guard = new IdleActionRepetitionGuard();
        guard.recordAction(100, "idle_drift_move", new Point(220, 180));

        assertFalse(guard.isRepeated(320, "idle_drift_move", new Point(226, 185)));
    }

    @Test
    void usesWiderExclusionForOffscreenTargets() {
        IdleActionRepetitionGuard guard = new IdleActionRepetitionGuard();
        guard.recordAction(100, "idle_fishing_offscreen_park_move", new Point(1500, 900));

        assertTrue(guard.isRepeated(130, "idle_fishing_offscreen_park_move", new Point(1540, 925)));
    }

    @Test
    void resetClearsRepeatMemory() {
        IdleActionRepetitionGuard guard = new IdleActionRepetitionGuard();
        guard.recordAction(100, "idle_hand_park_move", new Point(45, 55));
        guard.reset();

        assertFalse(guard.isRepeated(120, "idle_hand_park_move", new Point(48, 60)));
    }
}
