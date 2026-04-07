package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory {
    private InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory() {
        // Static factory utility.
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
            createDefaultRuntimeBundleFactoryInputs(factoryInputs)
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
            createDefaultRuntimeBundleFactoryInputs(
                factoryInputs,
                defaultSessionInteractionKey
            )
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }
}
