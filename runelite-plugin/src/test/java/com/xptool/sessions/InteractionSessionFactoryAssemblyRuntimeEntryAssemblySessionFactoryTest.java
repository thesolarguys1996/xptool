package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactoryTest {
    @Test
    void exposesAssemblyRuntimeEntryAssemblySessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory.class.getDeclaredMethod(
                "createFromAssemblyFactoryInputs",
                InteractionSessionAssemblyFactoryInputs.class
            )
        );
    }
}
