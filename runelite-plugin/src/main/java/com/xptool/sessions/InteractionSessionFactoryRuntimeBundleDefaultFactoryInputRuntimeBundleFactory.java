package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory {
    private InteractionSessionFactoryRuntimeBundleDefaultFactoryInputRuntimeBundleFactory() {
        // Static factory utility.
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }
}
