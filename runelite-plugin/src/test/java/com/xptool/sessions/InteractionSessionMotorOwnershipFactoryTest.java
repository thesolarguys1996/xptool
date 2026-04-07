package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteractionSessionMotorOwnershipFactoryTest {
    @Test
    void createMotorOwnershipHostFromDelegatesRoutesAcquireAndRelease() {
        boolean[] acquireOrRenewResult = {false};
        int[] acquireCalls = {0};
        int[] releaseCalls = {0};
        InteractionSessionMotorOwnershipService.Host host =
            InteractionSessionMotorOwnershipFactory.createMotorOwnershipHostFromDelegates(
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

    @Test
    void createMotorOwnershipServiceFromHostDelegatesAcquireAndReleaseLifecycle() {
        TestHost host = new TestHost();
        InteractionSessionMotorOwnershipService service =
            InteractionSessionMotorOwnershipFactory.createMotorOwnershipServiceFromHost(host);

        service.acquireOrRenewInteractionMotorOwnership();
        service.acquireOrRenewInteractionMotorOwnership();
        service.releaseInteractionMotorOwnership();
        service.releaseInteractionMotorOwnership();

        assertEquals(2, host.acquireCalls);
        assertEquals(2, host.releaseCalls);
    }

    private static final class TestHost implements InteractionSessionMotorOwnershipService.Host {
        private int acquireCalls = 0;
        private int releaseCalls = 0;

        @Override
        public boolean acquireOrRenewInteractionMotorOwnership() {
            acquireCalls++;
            return true;
        }

        @Override
        public void releaseInteractionMotorOwnership() {
            releaseCalls++;
        }
    }

}
