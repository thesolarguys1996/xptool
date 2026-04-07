package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryAssemblyFactoryInputsSessionFactoryTest {
    @Test
    void exposesAssemblyFactoryInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.class.getDeclaredMethod(
                "createFromAssemblyFactoryInputs",
                InteractionSessionAssemblyFactoryInputs.class
            )
        );
    }
}
