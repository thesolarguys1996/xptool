package com.xptool.sessions;

final class InteractionSessionFactoryAssemblyInputsFactory {
    private InteractionSessionFactoryAssemblyInputsFactory() {
        // Static factory utility.
    }

    static InteractionSessionAssemblyFactoryInputs createDefaultAssemblyFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return createAssemblyFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }

    static InteractionSessionAssemblyFactoryInputs createAssemblyFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String sessionInteractionKey
    ) {
        return factoryInputs.createAssemblyFactoryInputs(sessionInteractionKey);
    }
}
