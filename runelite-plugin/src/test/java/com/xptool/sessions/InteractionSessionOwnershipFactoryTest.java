package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteractionSessionOwnershipFactoryTest {
    @Test
    void createOwnershipServiceFromHostRoutesOnGameTickOwnershipLifecycle() {
        TestOwnershipHost host = new TestOwnershipHost();
        host.shouldOwnForInteraction = false;
        host.hasPendingSettle = false;
        host.hasActiveInteractionMotorProgram = false;
        InteractionSessionOwnershipService service = InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(host);

        service.onGameTick();

        assertEquals(1, host.clearPendingSettleCalls);
        assertEquals(1, host.clearRegistrationCalls);
        assertEquals(1, host.releaseInteractionMotorOwnershipCalls);
        assertEquals(0, host.ensureRegisteredCalls);
        assertEquals(0, host.tryRunPostClickSettleCalls);
    }

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

        InteractionSessionOwnershipService.Host host = InteractionSessionOwnershipFactory.createOwnershipHostFromDelegates(
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

    private static final class TestOwnershipHost implements InteractionSessionOwnershipService.Host {
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
