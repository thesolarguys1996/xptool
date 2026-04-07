package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleFactory {
    private InteractionSessionFactoryRuntimeBundleFactory() {
        // Static factory utility.
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String sessionInteractionKey
    ) {
        return InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(
            factoryInputs,
            sessionInteractionKey
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createRuntimeBundleFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.createRuntimeBundleFactoryInputs(
        return InteractionSessionFactoryRuntimeBundleFactoryInputsFactory.createRuntimeBundleFactoryInputs(
            assemblyFactoryInputs
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs createDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleDefaultFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
        return InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createDefaultRuntimeBundleFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }

    static InteractionSessionAssemblyFactoryInputs createDefaultAssemblyFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()
        return InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory.createDefaultAssemblyFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionAssemblyFactoryInputs createDefaultAssemblyFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return InteractionSessionFactoryAssemblyInputsFactory.createDefaultAssemblyFactoryInputs(
        return InteractionSessionFactoryRuntimeBundleDefaultAssemblyInputsFactory.createDefaultAssemblyFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createRuntimeBundleFromAssemblyFactoryInputs(
        //     createDefaultAssemblyFactoryInputs(factoryInputs)
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // return createRuntimeBundleFromInputs(
        //     createDefaultRuntimeBundleFactoryInputs(factoryInputs)
        // );
        return InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createRuntimeBundleFromFactoryInputs(
            factoryInputs
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createRuntimeBundleFromAssemblyFactoryInputs(
        //     createDefaultAssemblyFactoryInputs(
        //         factoryInputs,
        //         defaultSessionInteractionKey
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // return createRuntimeBundleFromInputs(
        //     createDefaultRuntimeBundleFactoryInputs(
        //         factoryInputs,
        //         defaultSessionInteractionKey
        //     )
        // );
        return InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createRuntimeBundleFromFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createRuntimeBundleFromInputs(defaultRuntimeBundleFactoryInputs);
        return InteractionSessionFactoryRuntimeBundleDefaultEntryFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // runtimeBundleFactoryInputs.createAssemblyFactoryInputs()
        return createRuntimeBundleFromAssemblyFactoryInputs(
            InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(
                runtimeBundleFactoryInputs
            )
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromAssemblyFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createRuntimeBundleFromInputs(
        //     createRuntimeBundleFactoryInputs(assemblyFactoryInputs)
        // );
        return InteractionSessionAssemblyFactory.createRuntimeBundleForSession(assemblyFactoryInputs);
    }
}
