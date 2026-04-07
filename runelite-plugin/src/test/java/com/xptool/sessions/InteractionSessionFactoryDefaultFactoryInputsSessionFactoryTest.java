package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryDefaultFactoryInputsSessionFactoryTest {
    @Test
    void exposesDefaultFactoryInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryDefaultFactoryInputsSessionFactory.class.getDeclaredMethod(
                "createFromFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
    }
}
