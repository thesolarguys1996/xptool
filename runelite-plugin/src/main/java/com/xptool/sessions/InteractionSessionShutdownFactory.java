package com.xptool.sessions;

final class InteractionSessionShutdownFactory {
    private InteractionSessionShutdownFactory() {
        // Static factory utility.
    }

    static InteractionSessionShutdownService createShutdownService(
        Runnable clearPendingPostClickSettle,
        Runnable clearRegistration,
        Runnable releaseInteractionMotorOwnership
    ) {
        return createShutdownServiceFromHost(
            createShutdownHost(
                clearPendingPostClickSettle,
                clearRegistration,
                releaseInteractionMotorOwnership
            )
        );
    }

    static InteractionSessionShutdownService createShutdownServiceFromHost(InteractionSessionShutdownService.Host host) {
        return new InteractionSessionShutdownService(host);
    }

    static InteractionSessionShutdownService.Host createShutdownHost(
        Runnable clearPendingPostClickSettle,
        Runnable clearRegistration,
        Runnable releaseInteractionMotorOwnership
    ) {
        return createShutdownHostFromDelegates(
            clearPendingPostClickSettle,
            clearRegistration,
            releaseInteractionMotorOwnership
        );
    }

    static InteractionSessionShutdownService.Host createShutdownHostFromDelegates(
        Runnable clearPendingPostClickSettle,
        Runnable clearRegistration,
        Runnable releaseInteractionMotorOwnership
    ) {
        return new InteractionSessionShutdownService.Host() {
            @Override
            public void clearPendingPostClickSettle() {
                clearPendingPostClickSettle.run();
            }

            @Override
            public void clearRegistration() {
                clearRegistration.run();
            }

            @Override
            public void releaseInteractionMotorOwnership() {
                releaseInteractionMotorOwnership.run();
            }
        };
    }
}
