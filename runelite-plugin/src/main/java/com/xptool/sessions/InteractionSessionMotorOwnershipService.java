package com.xptool.sessions;

final class InteractionSessionMotorOwnershipService {
    interface Host {
        boolean acquireOrRenewInteractionMotorOwnership();

        void releaseInteractionMotorOwnership();
    }

    private final Host host;

    InteractionSessionMotorOwnershipService(Host host) {
        this.host = host;
    }

    boolean acquireOrRenewInteractionMotorOwnership() {
        return host.acquireOrRenewInteractionMotorOwnership();
    }

    void releaseInteractionMotorOwnership() {
        host.releaseInteractionMotorOwnership();
    }
}
