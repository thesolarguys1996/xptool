package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InteractionSessionShutdownServiceTest {
    @Test
    void shutdownDelegatesAllLifecycleStepsInOrder() {
        TestHost host = new TestHost();
        InteractionSessionShutdownService service = new InteractionSessionShutdownService(host);

        service.shutdown();

        assertEquals(1, host.clearPendingPostClickSettleCalls);
        assertEquals(1, host.clearRegistrationCalls);
        assertEquals(1, host.releaseInteractionMotorOwnershipCalls);
        assertEquals("settle,registration,motor", host.callOrder);
    }

    private static final class TestHost implements InteractionSessionShutdownService.Host {
        private int clearPendingPostClickSettleCalls;
        private int clearRegistrationCalls;
        private int releaseInteractionMotorOwnershipCalls;
        private String callOrder = "";

        @Override
        public void clearPendingPostClickSettle() {
            clearPendingPostClickSettleCalls++;
            append("settle");
        }

        @Override
        public void clearRegistration() {
            clearRegistrationCalls++;
            append("registration");
        }

        @Override
        public void releaseInteractionMotorOwnership() {
            releaseInteractionMotorOwnershipCalls++;
            append("motor");
        }

        private void append(String value) {
            if (callOrder.isEmpty()) {
                callOrder = value;
            } else {
                callOrder = callOrder + "," + value;
            }
        }
    }
}
