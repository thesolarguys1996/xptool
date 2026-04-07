package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xptool.executor.InteractionClickEvent;
import java.awt.Point;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

class InteractionPostClickSettleFactoryTest {
    private static final String FISHING_WORLD_INTERACTION_CLICK_TYPE = "fishing_world_interaction";

    @Test
    void createPostClickSettleServiceFromHostSchedulesAndExecutesSettle() {
        TestHost host = new TestHost();
        InteractionPostClickSettleService service = InteractionPostClickSettleFactory.createPostClickSettleServiceFromHost(host);
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

    @Test
    void createPostClickSettleHostFromDelegatesRoutesAllCallbacks() {
        boolean[] hasActiveSessionOtherThanInteraction = {false};
        boolean[] hasPendingCommandRows = {true};
        long[] currentMotorActionSerial = {11L};
        boolean[] settleMoveResult = {false};
        Point[] settleAnchor = {null};
        int[] settleMoveCalls = {0};
        long[] nowMs = {33_000L};
        int[] randomPercentRoll = {17};
        int[] randomPercentRollCalls = {0};
        long[] randomMinInclusive = {Long.MIN_VALUE};
        long[] randomMaxInclusive = {Long.MIN_VALUE};
        long[] randomLongResult = {81L};
        int[] randomLongCalls = {0};

        InteractionPostClickSettleService.Host host = InteractionPostClickSettleFactory.createPostClickSettleHostFromDelegates(
            () -> hasActiveSessionOtherThanInteraction[0],
            () -> hasPendingCommandRows[0],
            () -> currentMotorActionSerial[0],
            point -> {
                settleMoveCalls[0]++;
                settleAnchor[0] = point;
                return settleMoveResult[0];
            },
            () -> nowMs[0],
            () -> {
                randomPercentRollCalls[0]++;
                return randomPercentRoll[0];
            },
            (minInclusive, maxInclusive) -> {
                randomLongCalls[0]++;
                randomMinInclusive[0] = minInclusive;
                randomMaxInclusive[0] = maxInclusive;
                return randomLongResult[0];
            }
        );

        assertFalse(host.hasActiveSessionOtherThanInteraction());
        hasActiveSessionOtherThanInteraction[0] = true;
        assertTrue(host.hasActiveSessionOtherThanInteraction());

        assertTrue(host.hasPendingCommandRows());
        hasPendingCommandRows[0] = false;
        assertFalse(host.hasPendingCommandRows());

        assertEquals(11L, host.currentMotorActionSerial());
        currentMotorActionSerial[0] = 42L;
        assertEquals(42L, host.currentMotorActionSerial());

        Point candidate = new Point(20, 40);
        assertFalse(host.performInteractionPostClickSettleMove(candidate));
        settleMoveResult[0] = true;
        assertTrue(host.performInteractionPostClickSettleMove(candidate));
        assertEquals(2, settleMoveCalls[0]);
        assertEquals(candidate, settleAnchor[0]);

        assertEquals(33_000L, host.nowMs());
        nowMs[0] = 34_000L;
        assertEquals(34_000L, host.nowMs());

        assertEquals(17, host.randomPercentRoll());
        randomPercentRoll[0] = 99;
        assertEquals(99, host.randomPercentRoll());
        assertEquals(2, randomPercentRollCalls[0]);

        assertEquals(81L, host.randomLongInclusive(12L, 24L));
        randomLongResult[0] = 125L;
        assertEquals(125L, host.randomLongInclusive(20L, 80L));
        assertEquals(2, randomLongCalls[0]);
        assertEquals(20L, randomMinInclusive[0]);
        assertEquals(80L, randomMaxInclusive[0]);
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
