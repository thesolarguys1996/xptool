package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;

final class InteractionSessionFactoryEntryServiceInputsSessionFactory {
    private InteractionSessionFactoryEntryServiceInputsSessionFactory() {
        // Static utility.
    }

    static InteractionSession create(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade
    ) {
        return InteractionSessionFactoryServiceInputsSessionFactory.createFromServices(
            executor,
            sessionManager,
            commandFacade
        );
    }
}
