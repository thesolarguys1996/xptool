package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryAssemblyInputsFactoryTest {
    @Test
    void createDefaultAssemblyFactoryInputsBuildsAssemblyFactoryInputsWithProvidedDefaultSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            InteractionSessionFactoryAssemblyInputsFactory.createDefaultAssemblyFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertNull(assemblyFactoryInputs.executor);
        assertNull(assemblyFactoryInputs.sessionManager);
        assertNull(assemblyFactoryInputs.commandFacade);
        assertEquals("interaction", assemblyFactoryInputs.sessionInteractionKey);
    }
}
