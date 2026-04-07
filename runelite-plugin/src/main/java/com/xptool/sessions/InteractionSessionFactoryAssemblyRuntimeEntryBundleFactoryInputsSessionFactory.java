package com.xptool.sessions;

final class InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory {
    private InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs
    ) {
        return InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromRuntimeBundleFactoryInputs(
            runtimeBundleFactoryInputs
        );
    }
}
