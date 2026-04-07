package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryFactoryInputsDefaultSessionFactoryTest {
    @Test
    void exposesFactoryInputsDefaultSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryFactoryInputsDefaultSessionFactory.class.getDeclaredMethod(
                "createFromFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
    }
}
