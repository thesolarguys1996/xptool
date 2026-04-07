package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xptool.sessions.idle.IdleSkillContext;
import org.junit.jupiter.api.Test;

class IdleSuppressionServiceTest {
    @Test
    void preservesWoodcuttingIdleMotionDuringInteractionOnlyCommandTraffic() {
        TestHost host = new TestHost();
        host.hasActiveSession = true;
        host.hasOtherNonInteractionSession = false;
        host.idleSkillContext = IdleSkillContext.WOODCUTTING;
        host.hasPendingCommandRows = true;
        host.idleMotorOwnerActive = true;

        IdleSuppressionService service = new IdleSuppressionService(host);
        service.suppressIdleMotionIfCommandTrafficActive();

        assertFalse(host.releasePendingIdleCameraDragCalled);
        assertFalse(host.clearPendingMouseMoveCalled);
        assertFalse(host.cancelMotorProgramCalled);
        assertFalse(host.releaseIdleMotorOwnershipCalled);
    }

    @Test
    void preservesFishingIdleMotionDuringInteractionOnlyCommandTraffic() {
        TestHost host = new TestHost();
        host.hasActiveSession = true;
        host.hasOtherNonInteractionSession = false;
        host.idleSkillContext = IdleSkillContext.FISHING;
        host.hasPendingCommandRows = true;
        host.idleMotorOwnerActive = true;

        IdleSuppressionService service = new IdleSuppressionService(host);
        service.suppressIdleMotionIfCommandTrafficActive();

        assertFalse(host.releasePendingIdleCameraDragCalled);
        assertFalse(host.clearPendingMouseMoveCalled);
        assertFalse(host.cancelMotorProgramCalled);
        assertFalse(host.releaseIdleMotorOwnershipCalled);
    }

    @Test
    void suppressesIdleMotionForGlobalContextWhenCommandTrafficIsActive() {
        TestHost host = new TestHost();
        host.hasActiveSession = true;
        host.hasOtherNonInteractionSession = false;
        host.idleSkillContext = IdleSkillContext.GLOBAL;
        host.hasPendingCommandRows = true;
        host.idleMotorOwnerActive = true;

        IdleSuppressionService service = new IdleSuppressionService(host);
        service.suppressIdleMotionIfCommandTrafficActive();

        assertTrue(host.releasePendingIdleCameraDragCalled);
        assertTrue(host.releaseIdleMotorOwnershipCalled);
    }

    private static final class TestHost implements IdleSuppressionService.Host {
        private boolean hasActiveSession = false;
        private boolean hasOtherNonInteractionSession = false;
        private IdleSkillContext idleSkillContext = IdleSkillContext.GLOBAL;
        private PendingMouseMove pendingMouseMove = null;
        private boolean hasActiveDropSweepSession = false;
        private boolean idleOwnedOffscreenPendingMove = false;
        private boolean interactionProgramActive = false;
        private boolean bankProgramActive = false;
        private boolean bankOpen = false;
        private boolean hasPendingCommandRows = false;
        private boolean idleMotorOwnerActive = false;
        private MotorProgram activeMotorProgram = null;
        private boolean releasePendingIdleCameraDragCalled = false;
        private boolean clearPendingMouseMoveCalled = false;
        private boolean cancelMotorProgramCalled = false;
        private boolean releaseIdleMotorOwnershipCalled = false;

        @Override
        public boolean hasActiveSession() {
            return hasActiveSession;
        }

        @Override
        public boolean hasActiveSessionOtherThan(String owner) {
            String normalized = owner == null ? "" : owner.trim().toLowerCase();
            if (ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION.equals(normalized)) {
                return hasOtherNonInteractionSession;
            }
            return false;
        }

        @Override
        public IdleSkillContext resolveIdleSkillContext() {
            return idleSkillContext;
        }

        @Override
        public PendingMouseMove pendingMouseMove() {
            return pendingMouseMove;
        }

        @Override
        public boolean hasActiveDropSweepSession() {
            return hasActiveDropSweepSession;
        }

        @Override
        public boolean isIdleOwnedOffscreenPendingMove(PendingMouseMove pending) {
            return idleOwnedOffscreenPendingMove;
        }

        @Override
        public boolean hasActiveMotorProgramForOwner(String owner) {
            String normalized = owner == null ? "" : owner.trim().toLowerCase();
            if (ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION.equals(normalized)) {
                return interactionProgramActive;
            }
            if (ExecutorMotorProfileCatalog.MOTOR_OWNER_BANK.equals(normalized)) {
                return bankProgramActive;
            }
            return false;
        }

        @Override
        public boolean isBankOpen() {
            return bankOpen;
        }

        @Override
        public boolean hasPendingCommandRows() {
            return hasPendingCommandRows;
        }

        @Override
        public boolean isIdleMotorOwnerActive() {
            return idleMotorOwnerActive;
        }

        @Override
        public void releasePendingIdleCameraDrag() {
            releasePendingIdleCameraDragCalled = true;
        }

        @Override
        public String normalizedMotorOwnerName(String owner) {
            return owner == null ? "" : owner.trim().toLowerCase();
        }

        @Override
        public void clearPendingMouseMove() {
            clearPendingMouseMoveCalled = true;
        }

        @Override
        public MotorProgram activeMotorProgram() {
            return activeMotorProgram;
        }

        @Override
        public void cancelMotorProgram(MotorProgram program, String reason) {
            cancelMotorProgramCalled = true;
        }

        @Override
        public void releaseIdleMotorOwnershipAfterSuppression() {
            releaseIdleMotorOwnershipCalled = true;
        }
    }
}
