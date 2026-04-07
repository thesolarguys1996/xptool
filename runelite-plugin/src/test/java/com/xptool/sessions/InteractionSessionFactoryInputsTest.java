package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryInputsTest {
    @Test
    void fromServicesRetainsProvidedReferences() {
        InteractionSessionFactoryInputs inputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        assertNull(inputs.executor);
        assertNull(inputs.sessionManager);
        assertNull(inputs.commandFacade);
    }

    @Test
    void createAssemblyFactoryInputsBuildsDefaultSessionInputs() {
        InteractionSessionFactoryInputs inputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            inputs.createAssemblyFactoryInputs("interaction");

        assertNull(assemblyFactoryInputs.executor);
        assertNull(assemblyFactoryInputs.sessionManager);
        assertNull(assemblyFactoryInputs.commandFacade);
        assertEquals("interaction", assemblyFactoryInputs.sessionInteractionKey);
    }
}
