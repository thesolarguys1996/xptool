package com.xptool.sessions;

final class InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory {
    private InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromAssemblyFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        return InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.createFromAssemblyFactoryInputs(
            assemblyFactoryInputs
        );
    }
}
