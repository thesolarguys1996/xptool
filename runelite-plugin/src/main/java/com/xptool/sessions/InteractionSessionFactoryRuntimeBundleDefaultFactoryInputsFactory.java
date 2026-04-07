package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory {
    private InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory() {
        // Static factory utility.
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return createDefaultRuntimeBundleFactoryInputs(
            factoryInputs,
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }
}
