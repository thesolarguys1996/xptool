package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.MotorGestureMode;
import org.junit.jupiter.api.Test;

class MotorProgramTerminalServiceTest {
    @Test
    void completeCancelFailDelegateLifecycleAndApplyIdleOwnerRelease() {
        TestHost host = new TestHost();
        MotorProgramTerminalService service = new MotorProgramTerminalService(
            new MotorProgramLifecycleEngine(host),
            host
        );

        MotorProgram idleProgram = programWithOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE);
        service.completeMotorProgram(idleProgram, "done");
        assertEquals(MotorGestureStatus.COMPLETE, idleProgram.status);
        assertEquals("done", idleProgram.resultReason);
        assertEquals(1, host.idleReleaseCount);
        assertTrue(host.lastReservedCooldownMs > 0L);

        MotorProgram interactionProgram = programWithOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);
        service.cancelMotorProgram(interactionProgram, "cancelled");
        assertEquals(MotorGestureStatus.CANCELLED, interactionProgram.status);
        assertEquals("cancelled", interactionProgram.resultReason);
        assertEquals(1, host.idleReleaseCount);

        service.failMotorProgram(interactionProgram, "failed");
        assertEquals(MotorGestureStatus.FAILED, interactionProgram.status);
        assertEquals("failed", interactionProgram.resultReason);
        assertEquals(1, host.idleReleaseCount);

        service.releaseIdleMotorOwnershipAfterSuppression();
        service.releaseIdleMotorOwnershipForRuntimeTeardown();
        assertEquals(3, host.idleReleaseCount);
    }

    @Test
    void validateMotorProgramMenuDelegatesToLifecycleEngine() {
        TestHost host = new TestHost();
        MotorProgramTerminalService service = new MotorProgramTerminalService(
            new MotorProgramLifecycleEngine(host),
            host
        );

        host.bankMenuVisible = true;
        MotorProgram bankMenuProgram = new MotorProgram(
            1L,
            new CanvasPoint(10, 10),
            MotorGestureType.MOVE_AND_CLICK,
            new MotorProfile(
                ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION,
                ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD,
                MotorGestureMode.GENERAL,
                false,
                true,
                MotionProfile.GENERIC_INTERACT.directClickSettings,
                MotorMenuValidationMode.BANK_TOP_OPTION,
                null,
                0,
                1,
                2,
                1L
            )
        );

        assertTrue(service.validateMotorProgramMenu(bankMenuProgram));
        host.bankMenuVisible = false;
        assertFalse(service.validateMotorProgramMenu(bankMenuProgram));
    }

    private static MotorProgram programWithOwner(String owner) {
        return new MotorProgram(
            1L,
            new CanvasPoint(10, 10),
            MotorGestureType.MOVE_AND_CLICK,
            new MotorProfile(
                owner,
                ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD,
                MotorGestureMode.GENERAL,
                false,
                true,
                MotionProfile.GENERIC_INTERACT.directClickSettings,
                MotorMenuValidationMode.NONE,
                null,
                0,
                1,
                2,
                20L
            )
        );
    }

    private static final class TestHost implements MotorProgramLifecycleEngine.Host, MotorProgramTerminalService.Host {
        long lastReservedCooldownMs = 0L;
        int idleReleaseCount = 0;
        boolean bankMenuVisible = false;

        @Override
        public boolean isTopMenuBankOnObject() {
            return bankMenuVisible;
        }

        @Override
        public boolean isTopMenuChopOnTree(net.runelite.api.TileObject targetObject) {
            return false;
        }

        @Override
        public boolean isTopMenuMineOnRock(net.runelite.api.TileObject targetObject) {
            return false;
        }

        @Override
        public boolean hasAttackEntryOnNpc() {
            return false;
        }

        @Override
        public void reserveMotorCooldown(long ms) {
            lastReservedCooldownMs = ms;
        }

        @Override
        public void releaseIdleMotorOwnership() {
            idleReleaseCount++;
        }

        @Override
        public String normalizedMotorOwnerName(String owner) {
            return ExecutorValueParsers.safeString(owner).trim().toLowerCase();
        }
    }
}
