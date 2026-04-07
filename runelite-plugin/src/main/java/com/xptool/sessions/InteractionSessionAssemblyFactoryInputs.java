package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;

final class InteractionSessionAssemblyFactoryInputs {
    final CommandExecutor executor;
    final SessionManager sessionManager;
    final SessionCommandFacade commandFacade;
    final String sessionInteractionKey;

    private InteractionSessionAssemblyFactoryInputs(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade,
        String sessionInteractionKey
    ) {
        this.executor = executor;
        this.sessionManager = sessionManager;
        this.commandFacade = commandFacade;
        this.sessionInteractionKey = sessionInteractionKey;
    }

    static InteractionSessionAssemblyFactoryInputs forDefaultSession(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade,
        String defaultSessionInteractionKey
    ) {
        return forSession(
            executor,
            sessionManager,
            commandFacade,
            defaultSessionInteractionKey
        );
    }

    static InteractionSessionAssemblyFactoryInputs forSession(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade,
        String sessionInteractionKey
    ) {
        return new InteractionSessionAssemblyFactoryInputs(
            executor,
            sessionManager,
            commandFacade,
            sessionInteractionKey
        );
    }
}
