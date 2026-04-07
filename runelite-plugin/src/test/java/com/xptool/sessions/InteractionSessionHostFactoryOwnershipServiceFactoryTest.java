package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryOwnershipServiceFactoryTest {
    @Test
    void createOwnershipServiceFromHostRoutesOnGameTickOwnershipLifecycle() {
        TestHost host = new TestHost();
        host.shouldOwnForInteraction = false;
        host.hasPendingSettle = false;
        host.hasActiveInteractionMotorProgram = false;
        InteractionSessionOwnershipService service = InteractionSessionHostFactory.createOwnershipServiceFromHost(host);

        service.onGameTick();

        assertEquals(1, host.clearPendingSettleCalls);
        assertEquals(1, host.clearRegistrationCalls);
        assertEquals(1, host.releaseInteractionMotorOwnershipCalls);
        assertEquals(0, host.ensureRegisteredCalls);
        assertEquals(0, host.tryRunPostClickSettleCalls);
    }

    private static final class TestHost implements InteractionSessionOwnershipService.Host {
        private boolean shouldOwnForInteraction;
        private boolean hasPendingSettle;
        private boolean settleReadyForMotor;
        private boolean hasActiveInteractionMotorProgram;
        private boolean hasActiveSessionOtherThanInteraction;
        private boolean acquireOrRenewInteractionMotorOwnership;
        private int clearPendingSettleCalls;
        private int clearRegistrationCalls;
        private int releaseInteractionMotorOwnershipCalls;
        private int ensureRegisteredCalls;
        private int tryRunPostClickSettleCalls;

        @Override
        public boolean shouldOwnForInteraction() {
            return shouldOwnForInteraction;
        }

        @Override
        public boolean hasPendingSettle() {
            return hasPendingSettle;
        }

        @Override
        public boolean settleReadyForMotor() {
            return settleReadyForMotor;
        }

        @Override
        public boolean hasActiveInteractionMotorProgram() {
            return hasActiveInteractionMotorProgram;
        }

        @Override
        public void clearPendingSettle() {
            clearPendingSettleCalls++;
            hasPendingSettle = false;
        }

        @Override
        public void clearRegistration() {
            clearRegistrationCalls++;
        }

        @Override
        public void releaseInteractionMotorOwnership() {
            releaseInteractionMotorOwnershipCalls++;
        }

        @Override
        public void ensureRegistered() {
            ensureRegisteredCalls++;
        }

        @Override
        public boolean hasActiveSessionOtherThanInteraction() {
            return hasActiveSessionOtherThanInteraction;
        }

        @Override
        public boolean acquireOrRenewInteractionMotorOwnership() {
            return acquireOrRenewInteractionMotorOwnership;
        }

        @Override
        public void tryRunPostClickSettle() {
            tryRunPostClickSettleCalls++;
        }
    }
}
