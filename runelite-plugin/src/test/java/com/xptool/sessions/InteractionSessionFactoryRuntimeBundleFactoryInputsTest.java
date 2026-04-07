package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleFactoryInputsTest {
    @Test
    void fromFactoryInputsRetainsProvidedReferencesAndSessionInteractionKey() {
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

        assertSame(factoryInputs, runtimeBundleFactoryInputs.factoryInputs);
        assertEquals("interaction", runtimeBundleFactoryInputs.sessionInteractionKey);
    }

    @Test
    void fromFactoryInputsUsesPolicyDefaultSessionInteractionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleFactoryInputs.fromFactoryInputs(factoryInputs);

        assertEquals(
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey(),
            runtimeBundleFactoryInputs.sessionInteractionKey
        );
    }

    @Test
    void createAssemblyFactoryInputsBuildsAssemblyInputsFromStoredFactoryInputsAndSessionKey() {
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
            runtimeBundleFactoryInputs.createAssemblyFactoryInputs();

        assertNull(assemblyFactoryInputs.executor);
        assertNull(assemblyFactoryInputs.sessionManager);
        assertNull(assemblyFactoryInputs.commandFacade);
        assertEquals("interaction", assemblyFactoryInputs.sessionInteractionKey);
    }
}
