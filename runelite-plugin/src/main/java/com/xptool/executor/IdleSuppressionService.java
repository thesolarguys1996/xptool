package com.xptool.executor;

import com.xptool.sessions.idle.IdleSkillContext;

final class IdleSuppressionService {
    interface Host {
        boolean hasActiveSession();
        boolean hasActiveSessionOtherThan(String owner);
        IdleSkillContext resolveIdleSkillContext();
        PendingMouseMove pendingMouseMove();
        boolean hasActiveDropSweepSession();
        boolean isIdleOwnedOffscreenPendingMove(PendingMouseMove pending);
        boolean hasActiveMotorProgramForOwner(String owner);
        boolean isBankOpen();
        boolean hasPendingCommandRows();
        boolean isIdleMotorOwnerActive();
        void releasePendingIdleCameraDrag();
        String normalizedMotorOwnerName(String owner);
        void clearPendingMouseMove();
        MotorProgram activeMotorProgram();
        void cancelMotorProgram(MotorProgram program, String reason);
        void releaseIdleMotorOwnershipAfterSuppression();
    }

    private final Host host;

    IdleSuppressionService(Host host) {
        this.host = host;
    }

    void suppressIdleMotionIfCommandTrafficActive() {
        boolean interactionOnlySessionActive =
            host.hasActiveSession()
                && !host.hasActiveSessionOtherThan(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);
        IdleSkillContext idleContext = host.resolveIdleSkillContext();
        boolean skillingIdleContextActive =
            idleContext == IdleSkillContext.FISHING || idleContext == IdleSkillContext.WOODCUTTING;
        PendingMouseMove pending = host.pendingMouseMove();
        boolean preserveOffscreenIdlePendingMove =
            !host.hasActiveDropSweepSession()
                && host.isIdleOwnedOffscreenPendingMove(pending);
        boolean preserveSkillingIdleMotion =
            interactionOnlySessionActive
                && skillingIdleContextActive
                && !host.hasActiveDropSweepSession();
        boolean nonIdleSessionActive =
            host.hasActiveSessionOtherThan(ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE);
        boolean interactionProgramActive =
            host.hasActiveMotorProgramForOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);
        boolean bankProgramActive =
            host.hasActiveMotorProgramForOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_BANK);
        if (preserveOffscreenIdlePendingMove) {
            return;
        }
        if (preserveSkillingIdleMotion
            && !interactionProgramActive
            && !bankProgramActive) {
            return;
        }
        if (!host.isBankOpen()
            && !host.hasPendingCommandRows()
            && !interactionProgramActive
            && !bankProgramActive
            && !nonIdleSessionActive) {
            return;
        }
        boolean idleMotorOwnerActive = host.isIdleMotorOwnerActive();
        host.releasePendingIdleCameraDrag();
        boolean idlePendingMoveCleared = false;
        if (pending != null
            && ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE.equals(host.normalizedMotorOwnerName(pending.owner))) {
            host.clearPendingMouseMove();
            idlePendingMoveCleared = true;
        }
        boolean idleProgramCancelled = false;
        MotorProgram program = host.activeMotorProgram();
        if (program != null
            && !program.toHandle().isTerminal()
            && program.profile != null
            && ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE.equals(host.normalizedMotorOwnerName(program.profile.owner))) {
            host.cancelMotorProgram(program, "non_idle_command_idle_suppressed");
            idleProgramCancelled = true;
        }
        boolean shouldReleaseIdleOwnership = IdleSuppressionReleasePolicy.shouldReleaseIdleOwnership(
            idleMotorOwnerActive,
            idlePendingMoveCleared,
            idleProgramCancelled
        );
        if (!shouldReleaseIdleOwnership) {
            return;
        }
        host.releaseIdleMotorOwnershipAfterSuppression();
    }
}
