package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryShutdownHostTest {
    @Test
    void createShutdownHostDelegatesAllLifecycleRunnables() {
        int[] settleCalls = {0};
        int[] registrationCalls = {0};
        int[] motorCalls = {0};

        InteractionSessionShutdownService.Host host = InteractionSessionHostFactory.createShutdownHost(
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
    void createShutdownHostFromDelegatesDelegatesAllLifecycleRunnables() {
        int[] settleCalls = {0};
        int[] registrationCalls = {0};
        int[] motorCalls = {0};

        InteractionSessionShutdownService.Host host = InteractionSessionHostFactory.createShutdownHostFromDelegates(
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
}
