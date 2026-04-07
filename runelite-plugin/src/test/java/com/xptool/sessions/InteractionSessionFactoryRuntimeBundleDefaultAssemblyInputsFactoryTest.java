package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactoryTest {
    @Test
    void createDefaultAssemblyFactoryInputsBuildsAssemblyInputsWithPolicyDefaultSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory.createDefaultAssemblyFactoryInputs(
                factoryInputs
            );

        assertNull(assemblyFactoryInputs.executor);
        assertNull(assemblyFactoryInputs.sessionManager);
        assertNull(assemblyFactoryInputs.commandFacade);
        assertEquals(
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey(),
            assemblyFactoryInputs.sessionInteractionKey
        );
    }

    @Test
    void createDefaultAssemblyFactoryInputsBuildsAssemblyInputsWithProvidedSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory.createDefaultAssemblyFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertNull(assemblyFactoryInputs.executor);
        assertNull(assemblyFactoryInputs.sessionManager);
        assertNull(assemblyFactoryInputs.commandFacade);
        assertEquals("interaction", assemblyFactoryInputs.sessionInteractionKey);
    }
}
