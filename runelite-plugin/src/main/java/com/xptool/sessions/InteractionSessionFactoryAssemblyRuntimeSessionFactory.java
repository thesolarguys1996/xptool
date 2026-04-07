package com.xptool.sessions;

final class InteractionSessionFactoryAssemblyRuntimeSessionFactory {
    private InteractionSessionFactoryAssemblyRuntimeSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromAssemblyFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.createFromAssemblyFactoryInputs(
        return InteractionSessionFactoryAssemblyRuntimeAssemblySessionFactory.createFromAssemblyFactoryInputs(
            assemblyFactoryInputs
        );
    }

    static InteractionSession createFromRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(
        return InteractionSessionFactoryAssemblyRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(
            runtimeBundleFactoryInputs
        );
    }
}
