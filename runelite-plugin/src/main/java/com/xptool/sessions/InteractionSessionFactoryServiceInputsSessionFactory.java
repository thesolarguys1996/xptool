package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;

final class InteractionSessionFactoryServiceInputsSessionFactory {
    private InteractionSessionFactoryServiceInputsSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromServices(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade
    ) {
        return InteractionSessionFactory.createFromFactoryInputs(
            InteractionSessionFactoryInputs.fromServices(
                executor,
                sessionManager,
                commandFacade
            )
        );
    }
}
