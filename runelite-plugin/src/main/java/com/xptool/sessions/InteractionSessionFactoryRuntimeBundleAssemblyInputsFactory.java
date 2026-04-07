package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory {
    private InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory() {
        // Static factory utility.
    }

    static InteractionSessionAssemblyFactoryInputs createAssemblyFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs
    ) {
        return createAssemblyFactoryInputs(
            runtimeBundleFactoryInputs.factoryInputs,
            runtimeBundleFactoryInputs.sessionInteractionKey
        );
    }

    static InteractionSessionAssemblyFactoryInputs createAssemblyFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String sessionInteractionKey
    ) {
        return factoryInputs.createAssemblyFactoryInputs(sessionInteractionKey);
    }
}
