package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactoryTest {
    @Test
    void exposesRuntimeBundleFactoryInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
