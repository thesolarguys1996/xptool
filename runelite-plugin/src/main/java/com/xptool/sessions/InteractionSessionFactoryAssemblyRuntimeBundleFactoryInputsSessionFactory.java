package com.xptool.sessions;

final class InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory {
    private InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(
            runtimeBundleFactoryInputs
        );
    }
}
