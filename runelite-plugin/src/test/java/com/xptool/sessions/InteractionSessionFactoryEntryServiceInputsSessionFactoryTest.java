package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;
import org.junit.jupiter.api.Test;

class InteractionSessionFactoryEntryServiceInputsSessionFactoryTest {
    @Test
    void exposesEntryServiceInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryEntryServiceInputsSessionFactory.class.getDeclaredMethod(
                "create",
                CommandExecutor.class,
                SessionManager.class,
                SessionCommandFacade.class
            )
        );
    }
}
