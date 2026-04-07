package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory {
    private InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory() {
        // Static factory utility.
    }

    static InteractionSessionAssemblyFactoryInputs createDefaultAssemblyFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return createDefaultAssemblyFactoryInputs(
            factoryInputs,
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()
        );
    }

    static InteractionSessionAssemblyFactoryInputs createDefaultAssemblyFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }
}
