package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryDefaultEntryFactoryTest {
    @Test
    void defaultRuntimeBundleFactoryInputsFactoryBuildsInputsWithProvidedSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertSame(factoryInputs, runtimeBundleFactoryInputs.factoryInputs);
        assertEquals("interaction", runtimeBundleFactoryInputs.sessionInteractionKey);
    }

    @Test
    void exposesInteractionSessionFactoryDefaultEntryFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryDefaultEntryFactory.class.getDeclaredMethod(
                "createFromFactoryInputs",
                InteractionSessionFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryDefaultEntryFactory.class.getDeclaredMethod(
                "createFromFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryDefaultEntryFactory.class.getDeclaredMethod(
                "createFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
