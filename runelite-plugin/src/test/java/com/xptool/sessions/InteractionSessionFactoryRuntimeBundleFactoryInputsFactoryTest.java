package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleFactoryInputsFactoryTest {
    @Test
    void createRuntimeBundleFactoryInputsBuildsTypedInputsFromFactoryInputsWithDefaultKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(
                factoryInputs
            );

        assertSame(factoryInputs, runtimeBundleFactoryInputs.factoryInputs);
        assertEquals(
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey(),
            runtimeBundleFactoryInputs.sessionInteractionKey
        );
    }

    @Test
    void createRuntimeBundleFactoryInputsBuildsTypedInputsFromFactoryInputsWithProvidedKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertSame(factoryInputs, runtimeBundleFactoryInputs.factoryInputs);
        assertEquals("interaction", runtimeBundleFactoryInputs.sessionInteractionKey);
    }

    @Test
    void createRuntimeBundleFactoryInputsBuildsTypedInputsFromAssemblyInputs() {
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            InteractionSessionAssemblyFactoryInputs.forSession(
                null,
                null,
                null,
                "interaction"
            );

        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(
                assemblyFactoryInputs
            );

        assertSame(assemblyFactoryInputs.executor, runtimeBundleFactoryInputs.factoryInputs.executor);
        assertSame(assemblyFactoryInputs.sessionManager, runtimeBundleFactoryInputs.factoryInputs.sessionManager);
        assertSame(assemblyFactoryInputs.commandFacade, runtimeBundleFactoryInputs.factoryInputs.commandFacade);
        assertEquals("interaction", runtimeBundleFactoryInputs.sessionInteractionKey);
    }
}
