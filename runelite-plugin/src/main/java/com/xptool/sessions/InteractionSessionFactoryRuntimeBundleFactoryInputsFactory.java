package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleFactoryInputsFactory {
    private InteractionSessionFactoryRuntimeBundleFactoryInputsFactory() {
        // Static factory utility.
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return createRuntimeBundleFactoryInputs(
            factoryInputs,
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String sessionInteractionKey
    ) {
        return InteractionSessionFactoryRuntimeBundleFactoryInputs.fromFactoryInputs(
            factoryInputs,
            sessionInteractionKey
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        return createRuntimeBundleFactoryInputs(
            InteractionSessionFactoryInputs.fromServices(
                assemblyFactoryInputs.executor,
                assemblyFactoryInputs.sessionManager,
                assemblyFactoryInputs.commandFacade
            ),
            assemblyFactoryInputs.sessionInteractionKey
        );
    }
}
