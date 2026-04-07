package com.xptool.activities.woodcutting;

import com.xptool.core.runtime.FatigueSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodcutNoticeDelayControllerTest {
    @Test
    void defersWhenRecentDispatchExistsEvenWithoutAnimationSignal() {
        WoodcutNoticeDelayController controller = new WoodcutNoticeDelayController();

        WoodcutNoticeDelayController.Decision decision = controller.maybeDeferAfterAnimationEnd(
            10_000L,
            9_000L,
            FatigueSnapshot.neutral()
        );

        assertTrue(decision.defer);
        assertTrue(decision.newlyArmed);
        assertTrue(decision.waitMsRemaining > 0L);
    }

    @Test
    void consumesDispatchFallbackOnlyOncePerDispatchAttempt() {
        WoodcutNoticeDelayController controller = new WoodcutNoticeDelayController();
        FatigueSnapshot fatigue = FatigueSnapshot.neutral();

        WoodcutNoticeDelayController.Decision first = controller.maybeDeferAfterAnimationEnd(10_000L, 9_000L, fatigue);
        assertTrue(first.defer);

        // Advance past the dispatch-signal window so the same dispatch cannot re-arm delay.
        WoodcutNoticeDelayController.Decision second = controller.maybeDeferAfterAnimationEnd(50_000L, 9_000L, fatigue);
        assertFalse(second.newlyArmed);

        boolean proceeded = !second.defer;
        long now = 50_000L;
        for (int i = 0; i < 8 && !proceeded; i++) {
            now += 300L;
            WoodcutNoticeDelayController.Decision next = controller.maybeDeferAfterAnimationEnd(now, 9_000L, fatigue);
            assertFalse(next.newlyArmed);
            proceeded = !next.defer;
        }
        assertTrue(proceeded);
    }

    @Test
    void stillDefersFromObservedAnimationSignal() {
        WoodcutNoticeDelayController controller = new WoodcutNoticeDelayController();
        controller.noteAnimationActive(12_000L);

        WoodcutNoticeDelayController.Decision decision = controller.maybeDeferAfterAnimationEnd(
            12_100L,
            0L,
            FatigueSnapshot.neutral()
        );

        assertTrue(decision.defer);
        assertTrue(decision.waitMsRemaining > 0L);
    }

    @Test
    void eventuallyReleasesDuringLongRepeatedDefers() {
        WoodcutNoticeDelayController controller = new WoodcutNoticeDelayController();
        FatigueSnapshot fatigue = FatigueSnapshot.neutral();

        WoodcutNoticeDelayController.Decision first = controller.maybeDeferAfterAnimationEnd(10_000L, 9_000L, fatigue);
        assertTrue(first.defer);

        boolean released = false;
        for (int i = 0; i < 12; i++) {
            WoodcutNoticeDelayController.Decision next = controller.maybeDeferAfterAnimationEnd(10_050L, 9_000L, fatigue);
            if (!next.defer) {
                released = true;
                break;
            }
        }
        assertTrue(released);
    }
}
