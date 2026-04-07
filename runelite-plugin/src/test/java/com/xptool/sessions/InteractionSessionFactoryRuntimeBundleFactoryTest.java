package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class InteractionSessionFactoryRuntimeBundleFactoryTest {
    @Test
    void createDefaultAssemblyFactoryInputsBuildsAssemblyFactoryInputsWithProvidedDefaultSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(
                factoryInputs,
                "interaction"
            );

        assertNull(assemblyFactoryInputs.executor);
        assertNull(assemblyFactoryInputs.sessionManager);
        assertNull(assemblyFactoryInputs.commandFacade);
        assertEquals("interaction", assemblyFactoryInputs.sessionInteractionKey);
    }

    @Test
    void createDefaultAssemblyFactoryInputsUsesPolicyDefaultSessionKey() {
        InteractionSessionFactoryInputs factoryInputs = InteractionSessionFactoryInputs.fromServices(
            null,
            null,
            null
        );

        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs =
            InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(factoryInputs);

        assertEquals(
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey(),
            assemblyFactoryInputs.sessionInteractionKey
        );
    }

    @Test
    void exposesRuntimeBundleRoutingEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFactoryInputs",
                InteractionSessionFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFactoryInputs",
                InteractionSessionAssemblyFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createDefaultAssemblyFactoryInputs",
                InteractionSessionFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromFactoryInputs",
                InteractionSessionFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromFactoryInputs",
                InteractionSessionFactoryInputs.class,
                String.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryRuntimeBundleFactory.class.getDeclaredMethod(
                "createRuntimeBundleFromAssemblyFactoryInputs",
                InteractionSessionAssemblyFactoryInputs.class
            )
        );
    }
}
