package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryShutdownServiceHostDecompositionTest {
    @Test
    void createShutdownServiceFromHostRoutesShutdownLifecycle() {
        TestHost host = new TestHost();
        InteractionSessionShutdownService service = InteractionSessionHostFactory.createShutdownServiceFromHost(host);

        service.shutdown();
        service.shutdown();

        assertEquals(2, host.settleCalls);
        assertEquals(2, host.registrationCalls);
        assertEquals(2, host.motorCalls);
    }

    private static final class TestHost implements InteractionSessionShutdownService.Host {
        private int settleCalls = 0;
        private int registrationCalls = 0;
        private int motorCalls = 0;

        @Override
        public void clearPendingPostClickSettle() {
            settleCalls++;
        }

        @Override
        public void clearRegistration() {
            registrationCalls++;
        }

        @Override
        public void releaseInteractionMotorOwnership() {
            motorCalls++;
        }
    }
}
