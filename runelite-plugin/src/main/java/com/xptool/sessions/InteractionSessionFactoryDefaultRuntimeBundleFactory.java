package com.xptool.sessions;

final class InteractionSessionFactoryDefaultRuntimeBundleFactory {
    private InteractionSessionFactoryDefaultRuntimeBundleFactory() {
        // Static factory utility.
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }
}
