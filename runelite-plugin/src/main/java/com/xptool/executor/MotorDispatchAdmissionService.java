package com.xptool.executor;

import java.awt.Point;

final class MotorDispatchAdmissionService {
    interface Host {
        boolean isUsableCanvasPoint(Point canvasPoint);

        String normalizedMotorOwnerName(String owner);

        boolean isMotorOwner(String owner);

        boolean acquireOrRenewMotorOwner(String owner, long leaseMs);

        long motorProgramLeaseMsForOwner(String owner);

        MotorProgram activeMotorProgram();

        void clearActiveMotorProgram();

        long nextMotorProgramId();

        void setActiveMotorProgram(MotorProgram program);
    }

    private final Host host;
    private final MotorActionGate motorActionGate;

    MotorDispatchAdmissionService(Host host, int maxMouseMutationsPerTick) {
        this.host = host;
        this.motorActionGate = new MotorActionGate(maxMouseMutationsPerTick);
    }

    boolean canPerformMotorActionNow(String activeMotorOwnerContext) {
        String owner = host.normalizedMotorOwnerName(activeMotorOwnerContext);
        if (owner.isEmpty()) {
            return false;
        }
        if (!host.isMotorOwner(owner)) {
            return false;
        }
        return motorActionGate.isActionReadyNow();
    }

    boolean isMotorActionReadyNow() {
        return motorActionGate.isActionReadyNow();
    }

    boolean acquireOrRenewMotorOwner(String owner, long leaseMs) {
        String normalizedOwner = host.normalizedMotorOwnerName(owner);
        if (normalizedOwner.isEmpty()) {
            return false;
        }
        return host.acquireOrRenewMotorOwner(normalizedOwner, leaseMs);
    }

    void reserveMotorCooldown(long delayMs) {
        motorActionGate.reserveCooldown(delayMs);
    }

    void noteMotorAction() {
        motorActionGate.noteAction();
    }

    long actionSerial() {
        return motorActionGate.actionSerial();
    }

    boolean tryConsumeMouseMutationBudget() {
        return motorActionGate.tryConsumeMouseMutationBudget();
    }

    void resetMouseMutationBudget() {
        motorActionGate.resetMouseMutationBudget();
    }

    MotorHandle scheduleMotorGesture(CanvasPoint target, MotorGestureType type, MotorProfile profile) {
        CanvasPoint safeTarget = target == null ? null : new CanvasPoint(target.x, target.y);
        MotorProfile safeProfile = profile == null ? null : profile.copy();
        if (safeTarget == null || safeProfile == null || type == null) {
            return MotorHandle.failed(0L, type, "invalid_motor_gesture_request");
        }
        if (!host.isUsableCanvasPoint(safeTarget.toAwtPoint())) {
            return MotorHandle.failed(0L, type, "motor_target_unusable");
        }
        String owner = host.normalizedMotorOwnerName(safeProfile.owner);
        if (owner.isEmpty()) {
            return MotorHandle.failed(0L, type, "motor_owner_missing");
        }
        if (!host.acquireOrRenewMotorOwner(owner, host.motorProgramLeaseMsForOwner(owner))) {
            return MotorHandle.failed(0L, type, "motor_owner_unavailable");
        }

        MotorProgram current = host.activeMotorProgram();
        if (current != null) {
            if (current.matches(safeTarget, type, safeProfile)) {
                MotorHandle existing = current.toHandle();
                if (existing.isTerminal()) {
                    host.clearActiveMotorProgram();
                }
                return existing;
            }
            if (!current.toHandle().isTerminal()) {
                return MotorHandle.inFlight(current.id, "motor_program_busy");
            }
            host.clearActiveMotorProgram();
        }

        MotorProgram program = new MotorProgram(host.nextMotorProgramId(), safeTarget, type, safeProfile);
        host.setActiveMotorProgram(program);
        return program.toHandle();
    }
}
