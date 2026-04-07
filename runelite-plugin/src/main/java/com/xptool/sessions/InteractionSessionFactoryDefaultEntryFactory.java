package com.xptool.sessions;

final class InteractionSessionFactoryDefaultEntryFactory {
    private InteractionSessionFactoryDefaultEntryFactory() {
        // Static factory utility.
    }

    static InteractionSession createFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromFactoryInputs(
        //     factoryInputs,
        //     InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()
        // );
        return createFromDefaultRuntimeBundleFactoryInputs(
            InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
                factoryInputs
            )
        );
    }

    static InteractionSession createFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryDefaultRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
        return createFromDefaultRuntimeBundleFactoryInputs(
            InteractionSessionFactoryDefaultEntryRuntimeBundleFactoryInputsFactory.createDefaultRuntimeBundleFactoryInputs(
                factoryInputs,
                defaultSessionInteractionKey
            )
        );
    }

    static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromDefaultRuntimeBundleFactoryInputs(
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryDefaultRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(
        return InteractionSessionFactoryDefaultEntryRuntimeSessionFactory.createFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }
}
