package com.xptool.sessions;

import java.util.function.Function;

final class InteractionSessionRegistrationFactory {
    private InteractionSessionRegistrationFactory() {
        // Static factory utility.
    }

    static InteractionSessionRegistrationService createRegistrationService(
        SessionManager sessionManager,
        String sessionInteractionKey
    ) {
        return createRegistrationServiceFromHost(
            createRegistrationHost(sessionManager),
            sessionInteractionKey
        );
    }

    static InteractionSessionRegistrationService createRegistrationServiceFromHost(
        InteractionSessionRegistrationService.Host host,
        String sessionInteractionKey
    ) {
        return new InteractionSessionRegistrationService(host, sessionInteractionKey);
    }

    static InteractionSessionRegistrationService.Host createRegistrationHost(SessionManager sessionManager) {
        return createRegistrationHostFromDelegates(sessionManager::registerSession);
    }

    static InteractionSessionRegistrationService.Host createRegistrationHostFromDelegates(
        Function<String, SessionManager.Registration> registerSession
    ) {
        return new InteractionSessionRegistrationService.Host() {
            @Override
            public SessionManager.Registration registerSession(String sessionName) {
                return registerSession.apply(sessionName);
            }
        };
    }
}
