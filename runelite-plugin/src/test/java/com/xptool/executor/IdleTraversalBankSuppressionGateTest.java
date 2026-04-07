package com.xptool.executor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdleTraversalBankSuppressionGateTest {
    @Test
    void suppressesWhenBankOpen() {
        IdleTraversalBankSuppressionGate gate = new IdleTraversalBankSuppressionGate();
        IdleTraversalBankSuppressionGate.DirectState state = new IdleTraversalBankSuppressionGate.DirectState(
            true,
            false,
            false,
            false,
            false,
            false
        );

        assertTrue(gate.isSuppressedNow(state));
    }

    @Test
    void suppressesWhenBankMotorProgramActive() {
        IdleTraversalBankSuppressionGate gate = new IdleTraversalBankSuppressionGate();
        IdleTraversalBankSuppressionGate.DirectState state = new IdleTraversalBankSuppressionGate.DirectState(
            false,
            true,
            false,
            false,
            false,
            false
        );

        assertTrue(gate.isSuppressedNow(state));
    }

    @Test
    void suppressesWhenInteractionMotorActiveAndCommandsPending() {
        IdleTraversalBankSuppressionGate gate = new IdleTraversalBankSuppressionGate();
        IdleTraversalBankSuppressionGate.DirectState state = new IdleTraversalBankSuppressionGate.DirectState(
            false,
            false,
            true,
            true,
            false,
            false
        );

        assertTrue(gate.isSuppressedNow(state));
    }

    @Test
    void doesNotSuppressWhenInteractionMotorActiveWithoutPendingCommands() {
        IdleTraversalBankSuppressionGate gate = new IdleTraversalBankSuppressionGate();
        IdleTraversalBankSuppressionGate.DirectState state = new IdleTraversalBankSuppressionGate.DirectState(
            false,
            false,
            true,
            false,
            false,
            false
        );

        assertFalse(gate.isSuppressedNow(state));
    }

    @Test
    void doesNotSuppressWhenInteractionOnlySessionOwnsRuntime() {
        IdleTraversalBankSuppressionGate gate = new IdleTraversalBankSuppressionGate();
        IdleTraversalBankSuppressionGate.DirectState state = new IdleTraversalBankSuppressionGate.DirectState(
            false,
            false,
            false,
            false,
            true,
            true
        );

        assertFalse(gate.isSuppressedNow(state));
    }

    @Test
    void legacyTimedSuppressionApisRemainInactive() {
        IdleTraversalBankSuppressionGate gate = new IdleTraversalBankSuppressionGate();

        assertFalse(gate.isSuppressedNow());
        assertFalse(gate.isFishingHardSuppressedNow());
    }
}
