package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class InteractionSessionAssemblyFactoryInputsTest {
    @Test
    void forSessionRetainsProvidedReferencesAndSessionKey() {
        InteractionSessionAssemblyFactoryInputs inputs = InteractionSessionAssemblyFactoryInputs.forSession(
            null,
            null,
            null,
            "interaction"
        );

        assertNull(inputs.executor);
        assertNull(inputs.sessionManager);
        assertNull(inputs.commandFacade);
        assertEquals("interaction", inputs.sessionInteractionKey);
    }

    @Test
    void forDefaultSessionRetainsProvidedReferencesAndDefaultSessionKey() {
        InteractionSessionAssemblyFactoryInputs inputs = InteractionSessionAssemblyFactoryInputs.forDefaultSession(
            null,
            null,
            null,
            "default_interaction"
        );

        assertNull(inputs.executor);
        assertNull(inputs.sessionManager);
        assertNull(inputs.commandFacade);
        assertEquals("default_interaction", inputs.sessionInteractionKey);
    }
}
