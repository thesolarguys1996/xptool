package com.xptool.sessions;

final class InteractionSessionOwnershipService {
    interface Host {
        boolean shouldOwnForInteraction();

        boolean hasPendingSettle();

        boolean settleReadyForMotor();

        boolean hasActiveInteractionMotorProgram();

        void clearPendingSettle();

        void clearRegistration();

        void releaseInteractionMotorOwnership();

        void ensureRegistered();

        boolean hasActiveSessionOtherThanInteraction();

        boolean acquireOrRenewInteractionMotorOwnership();

        void tryRunPostClickSettle();
    }

    private final Host host;

    InteractionSessionOwnershipService(Host host) {
        this.host = host;
    }

    void onGameTick() {
        boolean shouldOwnForInteraction = host.shouldOwnForInteraction();
        boolean hasPendingSettle = host.hasPendingSettle();
        boolean settleReadyForMotor = host.settleReadyForMotor();
        boolean hasActiveInteractionMotorProgram = host.hasActiveInteractionMotorProgram();
        if (!shouldOwnForInteraction && !hasPendingSettle && !hasActiveInteractionMotorProgram) {
            host.clearPendingSettle();
            host.clearRegistration();
            host.releaseInteractionMotorOwnership();
            return;
        }

        host.ensureRegistered();
        if (host.hasActiveSessionOtherThanInteraction()) {
            if (!hasActiveInteractionMotorProgram) {
                host.clearPendingSettle();
                host.releaseInteractionMotorOwnership();
            }
            if (!host.shouldOwnForInteraction() && !hasActiveInteractionMotorProgram) {
                host.clearRegistration();
            }
            return;
        }
        if (!settleReadyForMotor && !hasActiveInteractionMotorProgram) {
            // Do not monopolize motor lock during passive interaction ownership windows.
            host.releaseInteractionMotorOwnership();
            return;
        }
        boolean ownsMotor = host.acquireOrRenewInteractionMotorOwnership();
        if (ownsMotor) {
            host.tryRunPostClickSettle();
        }
        if (!host.shouldOwnForInteraction()
            && !host.hasPendingSettle()
            && !hasActiveInteractionMotorProgram) {
            host.clearRegistration();
            host.releaseInteractionMotorOwnership();
        }
    }
}
