package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryFactoryInputsSessionFactoryTest {
    @Test
    void exposesFactoryInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryFactoryInputsSessionFactory.class.getDeclaredMethod(
                "createFromFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
    }
}
