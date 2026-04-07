package com.xptool.executor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingNoticeDelayControllerTest {
    @Test
    void defersWhenRecentDispatchExistsEvenWithoutAnimationSignal() {
        FishingNoticeDelayController controller = new FishingNoticeDelayController();

        FishingNoticeDelayController.Decision decision = controller.maybeDeferAfterAnimationEnd(
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
        FishingNoticeDelayController controller = new FishingNoticeDelayController();
        FatigueSnapshot fatigue = FatigueSnapshot.neutral();

        FishingNoticeDelayController.Decision first = controller.maybeDeferAfterAnimationEnd(10_000L, 9_000L, fatigue);
        assertTrue(first.defer);

        FishingNoticeDelayController.Decision second = controller.maybeDeferAfterAnimationEnd(50_000L, 9_000L, fatigue);
        assertFalse(second.newlyArmed);

        boolean proceeded = !second.defer;
        long now = 50_000L;
        for (int i = 0; i < 8 && !proceeded; i++) {
            now += 300L;
            FishingNoticeDelayController.Decision next = controller.maybeDeferAfterAnimationEnd(now, 9_000L, fatigue);
            assertFalse(next.newlyArmed);
            proceeded = !next.defer;
        }
        assertTrue(proceeded);
    }

    @Test
    void stillDefersFromObservedAnimationSignal() {
        FishingNoticeDelayController controller = new FishingNoticeDelayController();
        controller.noteAnimationActive(12_000L);

        FishingNoticeDelayController.Decision decision = controller.maybeDeferAfterAnimationEnd(
            12_100L,
            11_900L,
            FatigueSnapshot.neutral()
        );

        assertTrue(decision.defer);
        assertTrue(decision.waitMsRemaining > 0L);
    }

    @Test
    void eventuallyReleasesDuringLongRepeatedDefers() {
        FishingNoticeDelayController controller = new FishingNoticeDelayController();
        FatigueSnapshot fatigue = FatigueSnapshot.neutral();

        FishingNoticeDelayController.Decision first = controller.maybeDeferAfterAnimationEnd(10_000L, 9_000L, fatigue);
        assertTrue(first.defer);

        boolean released = false;
        for (int i = 0; i < 12; i++) {
            FishingNoticeDelayController.Decision next = controller.maybeDeferAfterAnimationEnd(10_050L, 9_000L, fatigue);
            if (!next.defer) {
                released = true;
                break;
            }
        }
        assertTrue(released);
    }
}
