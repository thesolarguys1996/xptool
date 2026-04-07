package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryEntryRuntimeSessionFactoryTest {
    @Test
    void exposesEntryRuntimeSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryEntryRuntimeSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeBundle",
                InteractionSessionRuntimeBundle.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryEntryRuntimeSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeOperations",
                InteractionSessionRuntimeOperations.class
            )
        );
    }
}
