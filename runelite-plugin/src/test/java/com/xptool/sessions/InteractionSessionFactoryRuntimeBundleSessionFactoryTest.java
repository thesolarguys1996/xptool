package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleSessionFactoryTest {
    @Test
    void exposesRuntimeBundleSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeBundle",
                InteractionSessionRuntimeBundle.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleSessionFactory.class.getDeclaredMethod(
                "createFromRuntimeOperations",
                InteractionSessionRuntimeOperations.class
            )
        );
    }
}
