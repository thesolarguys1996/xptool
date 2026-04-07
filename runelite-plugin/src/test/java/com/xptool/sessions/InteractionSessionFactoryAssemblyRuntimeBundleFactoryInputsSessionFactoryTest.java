package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactoryTest {
    @Test
    void exposesAssemblyRuntimeBundleFactoryInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
