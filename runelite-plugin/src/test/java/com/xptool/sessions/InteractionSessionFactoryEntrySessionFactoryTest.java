package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;
import org.junit.jupiter.api.Test;

class InteractionSessionFactoryEntrySessionFactoryTest {
    @Test
    void exposesEntrySessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryEntrySessionFactory.class.getDeclaredMethod(
                "create",
                CommandExecutor.class,
                SessionManager.class,
                SessionCommandFacade.class
            )
        );
        assertNotNull(
            InteractionSessionFactoryEntrySessionFactory.class.getDeclaredMethod(
                "createFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            )
        );
    }
}
