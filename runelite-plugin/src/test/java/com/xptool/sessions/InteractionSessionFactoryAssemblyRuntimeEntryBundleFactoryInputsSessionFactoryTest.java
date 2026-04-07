package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactoryTest {
    @Test
    void exposesAssemblyRuntimeEntryBundleFactoryInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
