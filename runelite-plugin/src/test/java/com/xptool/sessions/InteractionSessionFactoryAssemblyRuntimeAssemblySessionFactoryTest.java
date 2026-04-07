package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactoryTest {
    @Test
    void exposesAssemblyRuntimeAssemblySessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory.class.getDeclaredMethod(
                "createFromAssemblyFactoryInputs",
                InteractionSessionAssemblyFactoryInputs.class
            )
        );
    }
}
