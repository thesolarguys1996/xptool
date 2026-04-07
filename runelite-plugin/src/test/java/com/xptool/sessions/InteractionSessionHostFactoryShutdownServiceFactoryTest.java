package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryShutdownServiceFactoryTest {
    @Test
    void createShutdownServiceDelegatesAllLifecycleRunnables() {
        int[] clearPendingPostClickSettleCalls = {0};
        int[] clearRegistrationCalls = {0};
        int[] releaseInteractionMotorOwnershipCalls = {0};
        InteractionSessionShutdownService service = InteractionSessionHostFactory.createShutdownService(
            () -> clearPendingPostClickSettleCalls[0]++,
            () -> clearRegistrationCalls[0]++,
            () -> releaseInteractionMotorOwnershipCalls[0]++
        );

        service.shutdown();
        service.shutdown();

        assertEquals(2, clearPendingPostClickSettleCalls[0]);
        assertEquals(2, clearRegistrationCalls[0]);
        assertEquals(2, releaseInteractionMotorOwnershipCalls[0]);
    }
}
