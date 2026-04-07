package com.xptool.sessions;

final class InteractionSessionFactoryAssemblyFactoryInputsSessionFactory {
    private InteractionSessionFactoryAssemblyFactoryInputsSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromAssemblyFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        return InteractionSessionFactory.createFromRuntimeBundleFactoryInputs(
            InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(
                assemblyFactoryInputs
            )
        );
    }
}
