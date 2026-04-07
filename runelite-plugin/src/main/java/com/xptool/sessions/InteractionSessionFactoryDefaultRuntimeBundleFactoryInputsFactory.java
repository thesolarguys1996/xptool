package com.xptool.sessions;

final class InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory {
    private InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory() {
        // Static factory utility.
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }
}
