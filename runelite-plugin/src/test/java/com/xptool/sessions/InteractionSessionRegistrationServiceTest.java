package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteractionSessionRegistrationServiceTest {
    @Test
    void ensureRegisteredIsIdempotentAndActivatesSession() {
        SessionManager sessionManager = new SessionManager();
        InteractionSessionRegistrationService service = new InteractionSessionRegistrationService(
            sessionManager,
            "interaction"
        );

        service.ensureRegistered();
        service.ensureRegistered();

        assertTrue(service.hasRegistration());
        assertTrue(sessionManager.hasActiveSession());
        assertFalse(sessionManager.hasActiveSessionOtherThan("interaction"));
    }

    @Test
    void clearRegistrationIsNoopWhenNotRegistered() {
        SessionManager sessionManager = new SessionManager();
        InteractionSessionRegistrationService service = new InteractionSessionRegistrationService(
            sessionManager,
            "interaction"
        );

        service.clearRegistration();

        assertFalse(service.hasRegistration());
        assertFalse(sessionManager.hasActiveSession());
    }

    @Test
    void clearRegistrationReleasesActiveSessionAndAllowsReregister() {
        SessionManager sessionManager = new SessionManager();
        InteractionSessionRegistrationService service = new InteractionSessionRegistrationService(
            sessionManager,
            "interaction"
        );

        service.ensureRegistered();
        assertTrue(sessionManager.hasActiveSession());
        service.clearRegistration();
        assertFalse(service.hasRegistration());
        assertFalse(sessionManager.hasActiveSession());

        service.ensureRegistered();
        assertTrue(service.hasRegistration());
        assertTrue(sessionManager.hasActiveSession());
    }
}
