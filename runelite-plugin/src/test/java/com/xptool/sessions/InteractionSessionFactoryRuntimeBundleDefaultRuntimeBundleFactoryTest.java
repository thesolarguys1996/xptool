package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactoryTest {
    @Test
    void createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithPolicyDefaultSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
                factoryInputs
            );

        assertSame(factoryInputs, runtimeBundleFactoryInputs.factoryInputs);
        assertEquals(
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey(),
            runtimeBundleFactoryInputs.sessionInteractionKey
        );
    }

    @Test
    void createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithProvidedSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertSame(factoryInputs, runtimeBundleFactoryInputs.factoryInputs);
        assertEquals("interaction", runtimeBundleFactoryInputs.sessionInteractionKey);
    }

    @Test
    void exposesDefaultRuntimeBundleFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromFactoryInputs",
                InteractionSessionFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
