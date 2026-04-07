package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeEntrySessionFactoryTest {
    @Test
    void exposesRuntimeEntrySessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryRuntimeEntrySessionFactory.class.getDeclaredMethod(
                "createFromRuntimeBundle",
                InteractionSessionRuntimeBundle.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeEntrySessionFactory.class.getDeclaredMethod(
                "createFromRuntimeOperations",
                InteractionSessionRuntimeOperations.class
            )
        );
    }
}
