package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryMotorOwnershipHostTest {
    @Test
    void createMotorOwnershipHostFromDelegatesRoutesAcquireAndRelease() {
        boolean[] acquireOrRenewResult = {false};
        int[] acquireCalls = {0};
        int[] releaseCalls = {0};
        InteractionSessionMotorOwnershipService.Host host = InteractionSessionHostFactory.createMotorOwnershipHostFromDelegates(
            () -> {
                acquireCalls[0]++;
                return acquireOrRenewResult[0];
            },
            () -> releaseCalls[0]++
        );

        acquireOrRenewResult[0] = true;
        assertTrue(host.acquireOrRenewInteractionMotorOwnership());
        acquireOrRenewResult[0] = false;
        assertFalse(host.acquireOrRenewInteractionMotorOwnership());
        host.releaseInteractionMotorOwnership();
        host.releaseInteractionMotorOwnership();

        assertEquals(2, acquireCalls[0]);
        assertEquals(2, releaseCalls[0]);
    }
}
