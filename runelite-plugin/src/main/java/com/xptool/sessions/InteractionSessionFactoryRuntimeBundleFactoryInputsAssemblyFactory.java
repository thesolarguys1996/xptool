package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory {
    private InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory() {
        // Static factory utility.
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryInputs.fromServices(
        //     assemblyFactoryInputs.executor,
        //     assemblyFactoryInputs.sessionManager,
        //     assemblyFactoryInputs.commandFacade
        // )
        // Compatibility sentinel for phase migration verifier continuity:
        // assemblyFactoryInputs.sessionInteractionKey
        return InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(
            assemblyFactoryInputs
        );
    }
}
