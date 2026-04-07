package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;

final class InteractionSessionFactoryInputs {
    final CommandExecutor executor;
    final SessionManager sessionManager;
    final SessionCommandFacade commandFacade;

    private InteractionSessionFactoryInputs(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade
    ) {
        this.executor = executor;
        this.sessionManager = sessionManager;
        this.commandFacade = commandFacade;
    }

    static InteractionSessionFactoryInputs fromServices(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade
    ) {
        return new InteractionSessionFactoryInputs(
            executor,
            sessionManager,
            commandFacade
        );
    }

    InteractionSessionAssemblyFactoryInputs createAssemblyFactoryInputs(String sessionInteractionKey) {
        return InteractionSessionAssemblyFactoryInputs.forDefaultSession(
            executor,
            sessionManager,
            commandFacade,
            sessionInteractionKey
        );
    }
}
