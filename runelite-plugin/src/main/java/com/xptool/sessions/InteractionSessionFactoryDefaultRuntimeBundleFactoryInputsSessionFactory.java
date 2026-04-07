package com.xptool.sessions;

final class InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory {
    private InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        return InteractionSessionFactoryDefaultEntryFactory.createFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }
}
