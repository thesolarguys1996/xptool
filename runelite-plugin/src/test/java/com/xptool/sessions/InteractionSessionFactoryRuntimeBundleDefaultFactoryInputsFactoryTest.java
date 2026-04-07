package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactoryTest {
    @Test
    void createDefaultRuntimeBundleFactoryInputsBuildsFactoryInputsWithPolicyDefaultSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
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
            InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertSame(factoryInputs, runtimeBundleFactoryInputs.factoryInputs);
        assertEquals("interaction", runtimeBundleFactoryInputs.sessionInteractionKey);
    }
}
