package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryRegistrationServiceFactoryTest {
    @Test
    void createRegistrationServiceEnsuresAndClearsRegistrationLifecycle() {
        SessionManager sessionManager = new SessionManager();
        InteractionSessionRegistrationService service = InteractionSessionHostFactory.createRegistrationService(
            sessionManager,
            "interaction"
        );

        service.ensureRegistered();
        assertTrue(sessionManager.hasActiveSession());

        service.clearRegistration();
        assertFalse(sessionManager.hasActiveSession());

        service.ensureRegistered();
        assertTrue(sessionManager.hasActiveSession());
    }

    @Test
    void createRegistrationServiceFromHostEnsuresAndClearsRegistrationLifecycle() {
        SessionManager sessionManager = new SessionManager();
        int[] registerCalls = {0};
        InteractionSessionRegistrationService service = InteractionSessionHostFactory.createRegistrationServiceFromHost(
            sessionName -> {
                registerCalls[0]++;
                return sessionManager.registerSession(sessionName);
            },
            "interaction"
        );

        service.ensureRegistered();
        service.ensureRegistered();
        assertEquals(1, registerCalls[0]);
        assertTrue(sessionManager.hasActiveSession());

        service.clearRegistration();
        assertFalse(sessionManager.hasActiveSession());

        service.ensureRegistered();
        assertEquals(2, registerCalls[0]);
        assertTrue(sessionManager.hasActiveSession());
    }
}
