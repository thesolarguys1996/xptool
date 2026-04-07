package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryRegistrationHostTest {
    @Test
    void createRegistrationHostFromDelegatesRoutesSessionRegistration() {
        SessionManager sessionManager = new SessionManager();
        String[] capturedSessionName = {null};
        int[] registerCalls = {0};
        InteractionSessionRegistrationService.Host host = InteractionSessionHostFactory.createRegistrationHostFromDelegates(
            sessionName -> {
                capturedSessionName[0] = sessionName;
                registerCalls[0]++;
                return sessionManager.registerSession(sessionName);
            }
        );

        SessionManager.Registration registration = host.registerSession("interaction");
        assertEquals(1, registerCalls[0]);
        assertEquals("interaction", capturedSessionName[0]);
        assertTrue(sessionManager.hasActiveSession());

        registration.close();
        assertFalse(sessionManager.hasActiveSession());
    }
}
