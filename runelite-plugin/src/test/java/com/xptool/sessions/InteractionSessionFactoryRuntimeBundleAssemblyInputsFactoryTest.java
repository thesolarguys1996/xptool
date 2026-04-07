package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleAssemblyInputsFactoryTest {
    @Test
    void createAssemblyFactoryInputsBuildsAssemblyInputsFromRuntimeBundleFactoryInputs() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleFactoryInputs.fromFactoryInputs(
                factoryInputs,
                "interaction"
            );

        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(
                runtimeBundleFactoryInputs
            );

        assertNull(assemblyFactoryInputs.executor);
        assertNull(assemblyFactoryInputs.sessionManager);
        assertNull(assemblyFactoryInputs.commandFacade);
        assertEquals("interaction", assemblyFactoryInputs.sessionInteractionKey);
    }

    @Test
    void createAssemblyFactoryInputsBuildsAssemblyInputsFromFactoryInputsAndSessionInteractionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertNull(assemblyFactoryInputs.executor);
        assertNull(assemblyFactoryInputs.sessionManager);
        assertNull(assemblyFactoryInputs.commandFacade);
        assertEquals("interaction", assemblyFactoryInputs.sessionInteractionKey);
    }
}
