package com.xptool.sessions;

final class InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory {
    private InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromAssemblyFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        return InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromAssemblyFactoryInputs(
            assemblyFactoryInputs
        );
    }
}
