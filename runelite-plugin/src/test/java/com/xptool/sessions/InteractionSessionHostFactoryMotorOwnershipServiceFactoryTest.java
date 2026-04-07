package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryMotorOwnershipServiceFactoryTest {
    @Test
    void createMotorOwnershipServiceFromHostDelegatesAcquireAndReleaseLifecycle() {
        TestHost host = new TestHost();
        InteractionSessionMotorOwnershipService service = InteractionSessionHostFactory.createMotorOwnershipServiceFromHost(host);

        host.acquireOrRenewResult = true;
        assertTrue(service.acquireOrRenewInteractionMotorOwnership());
        host.acquireOrRenewResult = false;
        assertFalse(service.acquireOrRenewInteractionMotorOwnership());
        service.releaseInteractionMotorOwnership();
        service.releaseInteractionMotorOwnership();

        assertEquals(2, host.acquireCalls);
        assertEquals(2, host.releaseCalls);
    }

    private static final class TestHost implements InteractionSessionMotorOwnershipService.Host {
        private boolean acquireOrRenewResult = false;
        private int acquireCalls = 0;
        private int releaseCalls = 0;

        @Override
        public boolean acquireOrRenewInteractionMotorOwnership() {
            acquireCalls++;
            return acquireOrRenewResult;
        }

        @Override
        public void releaseInteractionMotorOwnership() {
            releaseCalls++;
        }
    }
}
