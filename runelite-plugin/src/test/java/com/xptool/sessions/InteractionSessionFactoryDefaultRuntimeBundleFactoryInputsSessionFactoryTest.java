package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactoryTest {
    @Test
    void exposesDefaultRuntimeBundleFactoryInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.class.getDeclaredMethod(
                "createFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
