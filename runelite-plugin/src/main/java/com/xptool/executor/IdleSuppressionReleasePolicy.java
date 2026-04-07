package com.xptool.executor;

final class IdleSuppressionReleasePolicy {
    private IdleSuppressionReleasePolicy() {
    }

    static boolean shouldReleaseIdleOwnership(
        boolean idleMotorOwnerActive,
        boolean idlePendingMoveCleared,
        boolean idleProgramCancelled
    ) {
        return idleMotorOwnerActive || idlePendingMoveCleared || idleProgramCancelled;
    }
}
