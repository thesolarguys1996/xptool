package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.MotorGestureMode;
import java.awt.Point;
import org.junit.jupiter.api.Test;

class MotorDispatchAdmissionServiceTest {
    @Test
    void canPerformMotorActionNowRequiresOwnerLeaseAndCooldownReadiness() {
        TestHost host = new TestHost();
        MotorDispatchAdmissionService service = new MotorDispatchAdmissionService(host, 2);

        assertFalse(service.canPerformMotorActionNow(""));
        assertFalse(service.canPerformMotorActionNow(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION));

        host.motorOwnerActive = true;
        assertTrue(service.canPerformMotorActionNow(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION));

        service.reserveMotorCooldown(80L);
        assertFalse(service.canPerformMotorActionNow(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION));
    }

    @Test
    void scheduleMotorGestureRejectsInvalidOwnerAndUnavailableOwnerLease() {
        TestHost host = new TestHost();
        MotorDispatchAdmissionService service = new MotorDispatchAdmissionService(host, 2);
        CanvasPoint target = new CanvasPoint(100, 120);

        MotorHandle missingOwner = service.scheduleMotorGesture(
            target,
            MotorGestureType.MOVE_AND_CLICK,
            profile("")
        );
        assertEquals(MotorGestureStatus.FAILED, missingOwner.status);
        assertEquals("motor_owner_missing", missingOwner.reason);

        host.acquireAllowed = false;
        MotorHandle unavailableOwner = service.scheduleMotorGesture(
            target,
            MotorGestureType.MOVE_AND_CLICK,
            profile(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION)
        );
        assertEquals(MotorGestureStatus.FAILED, unavailableOwner.status);
        assertEquals("motor_owner_unavailable", unavailableOwner.reason);
    }

    @Test
    void scheduleMotorGestureReusesMatchingProgramAndRejectsBusyDifferentProgram() {
        TestHost host = new TestHost();
        MotorDispatchAdmissionService service = new MotorDispatchAdmissionService(host, 2);
        MotorProfile profile = profile(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);

        MotorHandle first = service.scheduleMotorGesture(
            new CanvasPoint(40, 50),
            MotorGestureType.MOVE_AND_CLICK,
            profile
        );
        assertEquals(MotorGestureStatus.SCHEDULED, first.status);
        assertNotNull(host.activeProgram);

        MotorHandle reused = service.scheduleMotorGesture(
            new CanvasPoint(40, 50),
            MotorGestureType.MOVE_AND_CLICK,
            profile
        );
        assertEquals(first.id, reused.id);
        assertEquals(MotorGestureStatus.SCHEDULED, reused.status);

        host.activeProgram.status = MotorGestureStatus.IN_FLIGHT;
        MotorHandle busy = service.scheduleMotorGesture(
            new CanvasPoint(55, 65),
            MotorGestureType.MOVE_AND_CLICK,
            profile
        );
        assertEquals(MotorGestureStatus.IN_FLIGHT, busy.status);
        assertEquals("motor_program_busy", busy.reason);
    }

    @Test
    void noteActionAndMutationBudgetAreTrackedByAdmissionService() {
        TestHost host = new TestHost();
        MotorDispatchAdmissionService service = new MotorDispatchAdmissionService(host, 1);

        assertEquals(0L, service.actionSerial());
        service.noteMotorAction();
        assertEquals(1L, service.actionSerial());

        assertTrue(service.tryConsumeMouseMutationBudget());
        assertFalse(service.tryConsumeMouseMutationBudget());
        service.resetMouseMutationBudget();
        assertTrue(service.tryConsumeMouseMutationBudget());
    }

    private static MotorProfile profile(String owner) {
        return new MotorProfile(
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
            12L
        );
    }

    private static final class TestHost implements MotorDispatchAdmissionService.Host {
        boolean usableCanvasPoint = true;
        boolean motorOwnerActive = false;
        boolean acquireAllowed = true;
        MotorProgram activeProgram = null;
        long nextProgramId = 1L;

        @Override
        public boolean isUsableCanvasPoint(Point canvasPoint) {
            return usableCanvasPoint && canvasPoint != null;
        }

        @Override
        public String normalizedMotorOwnerName(String owner) {
            return ExecutorValueParsers.safeString(owner).trim().toLowerCase();
        }

        @Override
        public boolean isMotorOwner(String owner) {
            return motorOwnerActive && ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION.equals(owner);
        }

        @Override
        public boolean acquireOrRenewMotorOwner(String owner, long leaseMs) {
            return acquireAllowed
                && ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION.equals(owner)
                && leaseMs > 0L;
        }

        @Override
        public long motorProgramLeaseMsForOwner(String owner) {
            return ExecutorMotorProfileCatalog.MOTOR_LEASE_INTERACTION_MS;
        }

        @Override
        public MotorProgram activeMotorProgram() {
            return activeProgram;
        }

        @Override
        public void clearActiveMotorProgram() {
            activeProgram = null;
        }

        @Override
        public long nextMotorProgramId() {
            return nextProgramId++;
        }

        @Override
        public void setActiveMotorProgram(MotorProgram program) {
            activeProgram = program;
        }
    }
}
