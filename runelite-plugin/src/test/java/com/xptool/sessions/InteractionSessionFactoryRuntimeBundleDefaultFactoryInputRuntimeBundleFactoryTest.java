package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactoryTest {
    @Test
    void exposesRuntimeBundleDefaultFactoryInputRuntimeBundleFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
