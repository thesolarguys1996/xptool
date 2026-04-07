package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InteractionSessionShutdownFactoryTest {
    @Test
    void createShutdownServiceRoutesShutdownLifecycle() {
        int[] settleCalls = {0};
        int[] registrationCalls = {0};
        int[] motorCalls = {0};
        InteractionSessionShutdownService service = InteractionSessionShutdownFactory.createShutdownService(
            () -> settleCalls[0]++,
            () -> registrationCalls[0]++,
            () -> motorCalls[0]++
        );

        service.shutdown();
        service.shutdown();

        assertEquals(2, settleCalls[0]);
        assertEquals(2, registrationCalls[0]);
        assertEquals(2, motorCalls[0]);
    }

    @Test
    void createShutdownServiceFromHostRoutesShutdownLifecycle() {
        TestHost host = new TestHost();
        InteractionSessionShutdownService service = InteractionSessionShutdownFactory.createShutdownServiceFromHost(host);

        service.shutdown();
        service.shutdown();

        assertEquals(2, host.settleCalls);
        assertEquals(2, host.registrationCalls);
        assertEquals(2, host.motorCalls);
    }

    @Test
    void createShutdownHostRoutesLifecycleRunnables() {
        int[] settleCalls = {0};
        int[] registrationCalls = {0};
        int[] motorCalls = {0};
        InteractionSessionShutdownService.Host host = InteractionSessionShutdownFactory.createShutdownHost(
            () -> settleCalls[0]++,
            () -> registrationCalls[0]++,
            () -> motorCalls[0]++
        );

        host.clearPendingPostClickSettle();
        host.clearRegistration();
        host.releaseInteractionMotorOwnership();

        assertEquals(1, settleCalls[0]);
        assertEquals(1, registrationCalls[0]);
        assertEquals(1, motorCalls[0]);
    }

    @Test
    void createShutdownHostFromDelegatesRoutesLifecycleRunnables() {
        int[] settleCalls = {0};
        int[] registrationCalls = {0};
        int[] motorCalls = {0};
        InteractionSessionShutdownService.Host host = InteractionSessionShutdownFactory.createShutdownHostFromDelegates(
            () -> settleCalls[0]++,
            () -> registrationCalls[0]++,
            () -> motorCalls[0]++
        );

        host.clearPendingPostClickSettle();
        host.clearRegistration();
        host.releaseInteractionMotorOwnership();

        assertEquals(1, settleCalls[0]);
        assertEquals(1, registrationCalls[0]);
        assertEquals(1, motorCalls[0]);
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
