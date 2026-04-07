package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactoryTest {
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
            InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.createRuntimeBundleFactoryInputs(
                assemblyFactoryInputs
            );

        assertSame(assemblyFactoryInputs.executor, runtimeBundleFactoryInputs.factoryInputs.executor);
        assertSame(assemblyFactoryInputs.sessionManager, runtimeBundleFactoryInputs.factoryInputs.sessionManager);
        assertSame(assemblyFactoryInputs.commandFacade, runtimeBundleFactoryInputs.factoryInputs.commandFacade);
        assertEquals("interaction", runtimeBundleFactoryInputs.sessionInteractionKey);
    }
}
