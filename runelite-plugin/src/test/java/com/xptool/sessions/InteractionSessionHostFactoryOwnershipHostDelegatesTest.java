package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryOwnershipHostDelegatesTest {
    @Test
    void createOwnershipHostFromDelegatesRoutesAllCallbacks() {
        boolean[] shouldOwn = {true};
        boolean[] hasPendingSettle = {true};
        boolean[] settleReadyForMotor = {false};
        boolean[] hasActiveProgram = {false};
        boolean[] hasActiveOtherSession = {true};
        boolean[] acquireOrRenewResult = {true};
        int[] clearPendingCalls = {0};
        int[] clearRegistrationCalls = {0};
        int[] releaseMotorCalls = {0};
        int[] ensureRegisteredCalls = {0};
        int[] tryRunSettleCalls = {0};

        InteractionSessionOwnershipService.Host host = InteractionSessionHostFactory.createOwnershipHostFromDelegates(
            () -> shouldOwn[0],
            () -> hasPendingSettle[0],
            () -> settleReadyForMotor[0],
            () -> hasActiveProgram[0],
            () -> clearPendingCalls[0]++,
            () -> clearRegistrationCalls[0]++,
            () -> releaseMotorCalls[0]++,
            () -> ensureRegisteredCalls[0]++,
            () -> hasActiveOtherSession[0],
            () -> acquireOrRenewResult[0],
            () -> tryRunSettleCalls[0]++
        );

        assertTrue(host.shouldOwnForInteraction());
        assertTrue(host.hasPendingSettle());
        assertFalse(host.settleReadyForMotor());
        assertFalse(host.hasActiveInteractionMotorProgram());
        assertTrue(host.hasActiveSessionOtherThanInteraction());
        assertTrue(host.acquireOrRenewInteractionMotorOwnership());

        host.clearPendingSettle();
        host.clearRegistration();
        host.releaseInteractionMotorOwnership();
        host.ensureRegistered();
        host.tryRunPostClickSettle();

        assertEquals(1, clearPendingCalls[0]);
        assertEquals(1, clearRegistrationCalls[0]);
        assertEquals(1, releaseMotorCalls[0]);
        assertEquals(1, ensureRegisteredCalls[0]);
        assertEquals(1, tryRunSettleCalls[0]);
    }
}
