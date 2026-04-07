package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryPostClickSettleHostTest {
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

        InteractionPostClickSettleService.Host host = InteractionSessionHostFactory.createPostClickSettleHostFromDelegates(
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
}
