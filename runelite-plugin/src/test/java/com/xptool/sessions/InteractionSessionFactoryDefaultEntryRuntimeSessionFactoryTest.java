package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryDefaultEntryRuntimeSessionFactoryTest {
    @Test
    void exposesDefaultEntryRuntimeSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryDefaultEntryRuntimeSessionFactory.class.getDeclaredMethod(
                "createFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
