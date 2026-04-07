package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleDefaultEntryFactoryTest {
    @Test
    void createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithPolicyDefaultSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createDefaultRuntimeBundleFactoryInputs(
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
            InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createDefaultRuntimeBundleFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertSame(factoryInputs, runtimeBundleFactoryInputs.factoryInputs);
        assertEquals("interaction", runtimeBundleFactoryInputs.sessionInteractionKey);
    }

    @Test
    void exposesRuntimeBundleDefaultEntryFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromFactoryInputs",
                InteractionSessionFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
