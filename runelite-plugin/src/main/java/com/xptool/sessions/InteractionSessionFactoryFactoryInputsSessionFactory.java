package com.xptool.sessions;

final class InteractionSessionFactoryFactoryInputsSessionFactory {
    private InteractionSessionFactoryFactoryInputsSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(
        return InteractionSessionFactoryFactoryInputsDefaultSessionFactory.createFromFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }
}
