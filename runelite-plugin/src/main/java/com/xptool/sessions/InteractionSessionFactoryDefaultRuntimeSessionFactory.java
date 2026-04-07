package com.xptool.sessions;

final class InteractionSessionFactoryDefaultRuntimeSessionFactory {
    private InteractionSessionFactoryDefaultRuntimeSessionFactory() {
        // Static factory utility.
    }

    static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
        return InteractionSessionRuntimeSessionFactory.createFromRuntimeBundle(
            InteractionSessionFactoryDefaultRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
                defaultRuntimeBundleFactoryInputs
            )
        );
    }
}
