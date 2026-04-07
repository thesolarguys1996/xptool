package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdleSuppressionReleasePolicyTest {
    @Test
    void returnsFalseWhenNoIdleOwnedStateWasSuppressed() {
        assertFalse(IdleSuppressionReleasePolicy.shouldReleaseIdleOwnership(false, false, false));
    }

    @Test
    void returnsTrueWhenIdleMotorOwnerWasActive() {
        assertTrue(IdleSuppressionReleasePolicy.shouldReleaseIdleOwnership(true, false, false));
    }

    @Test
    void returnsTrueWhenIdlePendingMoveWasCleared() {
        assertTrue(IdleSuppressionReleasePolicy.shouldReleaseIdleOwnership(false, true, false));
    }

    @Test
    void returnsTrueWhenIdleProgramWasCancelled() {
        assertTrue(IdleSuppressionReleasePolicy.shouldReleaseIdleOwnership(false, false, true));
    }

    @Test
    void returnsTrueWhenAllSuppressionSignalsArePresent() {
        assertTrue(IdleSuppressionReleasePolicy.shouldReleaseIdleOwnership(true, true, true));
    }
}
