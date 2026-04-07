package com.xptool.sessions;

final class InteractionSessionShutdownService {
    interface Host {
        void clearPendingPostClickSettle();

        void clearRegistration();

        void releaseInteractionMotorOwnership();
    }

    private final Host host;

    InteractionSessionShutdownService(Host host) {
        this.host = host;
    }

    void shutdown() {
        host.clearPendingPostClickSettle();
        host.clearRegistration();
        host.releaseInteractionMotorOwnership();
    }
}
