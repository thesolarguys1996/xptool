package com.xptool.sessions;

final class InteractionSessionRegistrationService {
    interface Host {
        SessionManager.Registration registerSession(String sessionName);
    }

    private final Host host;
    private final String sessionName;
    private SessionManager.Registration registration = null;

    InteractionSessionRegistrationService(SessionManager sessionManager, String sessionName) {
        this(sessionManager::registerSession, sessionName);
    }

    InteractionSessionRegistrationService(Host host, String sessionName) {
        this.host = host;
        this.sessionName = sessionName;
    }

    void ensureRegistered() {
        if (registration != null) {
            return;
        }
        registration = host.registerSession(sessionName);
    }

    void clearRegistration() {
        if (registration == null) {
            return;
        }
        registration.close();
        registration = null;
    }

    boolean hasRegistration() {
        return registration != null;
    }
}
