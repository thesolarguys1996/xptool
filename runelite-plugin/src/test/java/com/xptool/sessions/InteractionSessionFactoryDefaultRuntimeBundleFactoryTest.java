package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryDefaultRuntimeBundleFactoryTest {
    @Test
    void exposesDefaultRuntimeBundleFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryDefaultRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
