package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory {
    private InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs
    ) {
        return InteractionSessionFactory.createFromRuntimeBundle(
            InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(runtimeBundleFactoryInputs)
        );
    }
}
