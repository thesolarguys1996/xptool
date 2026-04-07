package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeEntryRuntimeSessionFactoryTest {
    @Test
    void exposesRuntimeEntryRuntimeSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeBundle",
                InteractionSessionRuntimeBundle.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeOperations",
                InteractionSessionRuntimeOperations.class
            )
        );
    }
}
