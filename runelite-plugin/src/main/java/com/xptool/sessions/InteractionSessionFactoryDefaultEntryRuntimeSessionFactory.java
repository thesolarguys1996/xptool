package com.xptool.sessions;

final class InteractionSessionFactoryDefaultEntryRuntimeSessionFactory {
    private InteractionSessionFactoryDefaultEntryRuntimeSessionFactory() {
        // Static factory utility.
    }

    static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        return InteractionSessionFactoryDefaultRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }
}
