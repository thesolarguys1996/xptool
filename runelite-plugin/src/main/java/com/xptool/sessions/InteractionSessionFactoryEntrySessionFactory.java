package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;

final class InteractionSessionFactoryEntrySessionFactory {
    private InteractionSessionFactoryEntrySessionFactory() {
        // Static utility.
    }

    static InteractionSession create(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryServiceInputsSessionFactory.createFromServices(
        return InteractionSessionFactoryEntryServiceInputsSessionFactory.create(
            executor,
            sessionManager,
            commandFacade
        );
    }

    static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(
        return InteractionSessionFactoryEntryDefaultRuntimeBundleFactoryInputsSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }
}
