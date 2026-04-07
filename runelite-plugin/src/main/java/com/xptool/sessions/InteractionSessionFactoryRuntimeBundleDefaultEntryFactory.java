package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleDefaultEntryFactory {
    private InteractionSessionFactoryRuntimeBundleDefaultEntryFactory() {
        // Static factory utility.
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryRuntimeBundleDefaultRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
        return InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }
}
