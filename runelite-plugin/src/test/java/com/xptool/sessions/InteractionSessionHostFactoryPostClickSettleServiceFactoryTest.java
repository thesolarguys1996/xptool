package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xptool.executor.InteractionClickEvent;
import java.awt.Point;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryPostClickSettleServiceFactoryTest {
    private static final String FISHING_WORLD_INTERACTION_CLICK_TYPE = "fishing_world_interaction";

    @Test
    void createPostClickSettleServiceFromHostSchedulesAndExecutesSettle() {
        TestHost host = new TestHost();
        InteractionPostClickSettleService service = InteractionSessionHostFactory.createPostClickSettleServiceFromHost(host);
        host.nowMs = 10_000L;
        host.enqueuePercentRolls(99, 12, 99);
        host.enqueueLongRolls(80L);

        service.onInteractionClickEvent(settleEligibleEvent(7L, host.nowMs, 22L, new Point(50, 60), new Point(49, 61)));

        assertTrue(service.hasPendingPostClickSettle());
        assertFalse(service.shouldAcquireMotorForPendingSettle());

        host.nowMs = 10_080L;
        assertTrue(service.shouldAcquireMotorForPendingSettle());
        service.tryRunPostClickSettle();

        assertEquals(1, host.performCalls);
        assertEquals(new Point(50, 60), host.lastSettleAnchor);
        assertFalse(service.hasPendingPostClickSettle());
    }

    private static InteractionClickEvent settleEligibleEvent(
        long clickSerial,
        long clickedAtMs,
        long motorActionSerial,
        Point anchorCanvasPoint,
        Point clickCanvasPoint
    ) {
        return new InteractionClickEvent(
            clickSerial,
            1,
            clickedAtMs,
            "interaction",
            FISHING_WORLD_INTERACTION_CLICK_TYPE,
            clickCanvasPoint,
            anchorCanvasPoint,
            null,
            motorActionSerial
        );
    }

    private static final class TestHost implements InteractionPostClickSettleService.Host {
        long nowMs = 0L;
        boolean hasOtherSession = false;
        boolean hasPendingCommandRows = false;
        long currentMotorActionSerial = 0L;
        boolean performReturn = true;
        int performCalls = 0;
        Point lastSettleAnchor = null;
        private final ArrayDeque<Integer> percentRolls = new ArrayDeque<>();
        private final ArrayDeque<Long> longRolls = new ArrayDeque<>();

        void enqueuePercentRolls(int... rolls) {
            for (int roll : rolls) {
                percentRolls.addLast(roll);
            }
        }

        void enqueueLongRolls(long... rolls) {
            for (long roll : rolls) {
                longRolls.addLast(roll);
            }
        }

        @Override
        public boolean hasActiveSessionOtherThanInteraction() {
            return hasOtherSession;
        }

        @Override
        public boolean hasPendingCommandRows() {
            return hasPendingCommandRows;
        }

        @Override
        public long currentMotorActionSerial() {
            return currentMotorActionSerial;
        }

        @Override
        public boolean performInteractionPostClickSettleMove(Point settleAnchor) {
            performCalls++;
            lastSettleAnchor = settleAnchor == null ? null : new Point(settleAnchor);
            return performReturn;
        }

        @Override
        public long nowMs() {
            return nowMs;
        }

        @Override
        public int randomPercentRoll() {
            return percentRolls.isEmpty() ? 99 : percentRolls.removeFirst();
        }

        @Override
        public long randomLongInclusive(long minInclusive, long maxInclusive) {
            long value = longRolls.isEmpty() ? minInclusive : longRolls.removeFirst();
            if (value < minInclusive) {
                return minInclusive;
            }
            if (value > maxInclusive) {
                return maxInclusive;
            }
            return value;
        }
    }
}
