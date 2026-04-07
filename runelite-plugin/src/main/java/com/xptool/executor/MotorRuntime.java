package com.xptool.executor;

import java.awt.Point;

final class MotorRuntime {
    private final MotorRuntimePort port;

    MotorRuntime(MotorRuntimePort port) {
        this.port = port;
    }

    void advancePendingMouseMove(int tick) {
        if (port == null) {
            return;
        }
        PendingMouseMove pendingMove = port.pendingMouseMove();
        if (pendingMove == null) {
            return;
        }
        if (!port.isPendingMouseMoveOwnerValid(pendingMove)) {
            port.notePendingMoveCleared(pendingMove, "owner_invalid", tick);
            port.clearPendingMouseMove();
            return;
        }
        if (!port.isMotorActionReadyNow()) {
            port.notePendingMoveBlocked(pendingMove, "action_not_ready", tick);
            return;
        }
        port.notePendingMoveAge(pendingMove);
        boolean timedOut = port.pendingMoveHasExceededCommitTimeout(pendingMove);
        boolean targetInvalidated = port.pendingMoveTargetInvalidated(pendingMove);
        if (timedOut || targetInvalidated) {
            String clearReason;
            if (timedOut && targetInvalidated) {
                clearReason = "commit_timeout_and_target_invalidated";
            } else if (timedOut) {
                clearReason = "commit_timeout";
            } else {
                clearReason = "target_invalidated";
            }
            port.notePendingMoveCleared(pendingMove, clearReason, tick);
            port.clearPendingMouseMove();
            return;
        }
        if (tick < pendingMove.nextAllowedTick) {
            port.notePendingMoveRemainingDistance(pendingMove);
            port.notePendingMoveBlocked(pendingMove, "next_allowed_tick", tick);
            return;
        }
        if (!port.tryConsumeMouseMutationBudget()) {
            port.notePendingMoveRemainingDistance(pendingMove);
            port.notePendingMoveBlocked(pendingMove, "mouse_mutation_budget_unavailable", tick);
            return;
        }
        pendingMove.advanceOneStep();
        Point after = port.currentPointerLocationOr(null);
        port.notePendingMoveAdvanced(pendingMove, tick, after);
        port.noteMouseMutation(after);
        port.notePendingMoveRemainingDistance(pendingMove);
        if (pendingMove.complete(after, port.pendingMoveArrivalTolerancePx())) {
            port.notePendingMoveCleared(pendingMove, "complete", tick);
            port.clearPendingMouseMove();
            return;
        }
        // client.getTickCount() remains constant for many client pumps between game ticks.
        // Requiring tick+1 here can throttle pending moves to one step per game tick.
        pendingMove.nextAllowedTick = tick;
    }

    void tickMotorProgram(int tick) {
        if (port == null) {
            return;
        }
        MotorProgram program = port.activeMotorProgram();
        if (program == null) {
            return;
        }
        MotorHandle handle = program.toHandle();
        if (handle.isTerminal()) {
            return;
        }
        String owner = port.normalizedMotorOwnerName(program.profile.owner);
        if (owner.isEmpty() || !port.isSessionMotorOwner(owner)) {
            port.cancelMotorProgram(program, "motor_owner_lost");
            return;
        }
        if (!port.renewSessionMotor(owner, port.motorProgramLeaseMsForOwner(owner))) {
            port.cancelMotorProgram(program, "motor_owner_lost");
            return;
        }

        String previousOwner = port.pushMotorOwnerContext(owner);
        String previousClickType = port.pushClickTypeContext(program.profile.clickType);
        try {
            if (program.phase == MotorProgramPhase.WAITING_START) {
                if (!port.isMotorActionReadyNow()) {
                    return;
                }
                if (program.type == MotorGestureType.CLICK_ONLY) {
                    program.phase = program.profile.hoverSettleTicks > 0
                        ? MotorProgramPhase.HOVER_SETTLE
                        : (program.profile.menuValidationMode == MotorMenuValidationMode.NONE
                            ? MotorProgramPhase.CLICKING
                            : MotorProgramPhase.MENU_VALIDATE);
                    program.hoverSettleTicksRemaining = Math.max(0, program.profile.hoverSettleTicks);
                } else {
                    program.phase = MotorProgramPhase.MOVING;
                }
                program.status = MotorGestureStatus.IN_FLIGHT;
            }

            if (program.phase == MotorProgramPhase.MOVING) {
                port.advanceMotorProgramMove(program);
                return;
            }
            if (program.phase == MotorProgramPhase.HOVER_SETTLE) {
                if (program.hoverSettleTicksRemaining > 0) {
                    program.hoverSettleTicksRemaining--;
                    return;
                }
                program.phase = program.profile.menuValidationMode == MotorMenuValidationMode.NONE
                    ? MotorProgramPhase.CLICKING
                    : MotorProgramPhase.MENU_VALIDATE;
                return;
            }
            if (program.phase == MotorProgramPhase.MENU_VALIDATE) {
                if (port.validateMotorProgramMenu(program)) {
                    program.phase = MotorProgramPhase.CLICKING;
                    return;
                }
                program.menuValidationTicks++;
                if (program.menuValidationTicks >= Math.max(1, program.profile.maxMenuValidationTicks)) {
                    if (program.profile.menuValidationMode == MotorMenuValidationMode.COMBAT_TOP_ATTACK_ON_NPC) {
                        // Combat cursor-over checks can be briefly stale on moving NPCs.
                        // Avoid starvation loops by progressing to click after bounded validation wait.
                        program.phase = MotorProgramPhase.CLICKING;
                        return;
                    }
                    port.failMotorProgram(program, "motor_menu_validation_failed");
                }
                return;
            }
            if (program.phase == MotorProgramPhase.CLICKING) {
                port.runMotorProgramClick(program);
            }
        } finally {
            port.popClickTypeContext(previousClickType);
            port.popMotorOwnerContext(previousOwner);
        }
    }
}
