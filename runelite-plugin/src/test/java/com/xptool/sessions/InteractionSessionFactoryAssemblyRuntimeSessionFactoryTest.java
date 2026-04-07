package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryAssemblyRuntimeSessionFactoryTest {
    @Test
    void exposesAssemblyRuntimeSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryAssemblyRuntimeSessionFactory.class.getDeclaredMethod(
                "createFromAssemblyFactoryInputs",
                InteractionSessionAssemblyFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryAssemblyRuntimeSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
