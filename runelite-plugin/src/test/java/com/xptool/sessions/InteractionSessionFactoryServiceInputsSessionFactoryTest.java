package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;
import org.junit.jupiter.api.Test;

class InteractionSessionFactoryServiceInputsSessionFactoryTest {
    @Test
    void exposesServiceInputsSessionFactoryEntryPoints() throws NoSuchMethodException {
        assertNotNull(
            InteractionSessionFactoryServiceInputsSessionFactory.class.getDeclaredMethod(
                "createFromServices",
                CommandExecutor.class,
                SessionManager.class,
                SessionCommandFacade.class
            )
        );
    }
}
