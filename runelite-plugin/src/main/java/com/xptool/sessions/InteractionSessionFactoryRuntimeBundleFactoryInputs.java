package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleFactoryInputs {
    final InteractionSessionFactoryInputs factoryInputs;
    final String sessionInteractionKey;

    private InteractionSessionFactoryRuntimeBundleFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String sessionInteractionKey
    ) {
        this.factoryInputs = factoryInputs;
        this.sessionInteractionKey = sessionInteractionKey;
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs fromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        return fromFactoryInputs(
            factoryInputs,
            InteractionSessionFactoryRuntimeBundleKeyPolicy.defaultSessionInteractionKey()
        );
    }

    static InteractionSessionFactoryRuntimeBundleFactoryInputs fromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String sessionInteractionKey
    ) {
        return new InteractionSessionFactoryRuntimeBundleFactoryInputs(
            factoryInputs,
            sessionInteractionKey
        );
    }

    InteractionSessionAssemblyFactoryInputs createAssemblyFactoryInputs() {
        return InteractionSessionFactoryRuntimeBundleAssemblyInputsFactory.createAssemblyFactoryInputs(
            factoryInputs,
            sessionInteractionKey
        );
    }
}
