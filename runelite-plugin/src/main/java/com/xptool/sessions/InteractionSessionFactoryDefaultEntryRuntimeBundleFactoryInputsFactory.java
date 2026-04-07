package com.xptool.sessions;

final class InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory {
    private InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory() {
        // Static factory utility.
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }
}
