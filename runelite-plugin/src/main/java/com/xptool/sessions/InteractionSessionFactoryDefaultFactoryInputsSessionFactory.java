package com.xptool.sessions;

final class InteractionSessionFactoryDefaultFactoryInputsSessionFactory {
    private InteractionSessionFactoryDefaultFactoryInputsSessionFactory() {
        // Static factory utility.
    }

    static InteractionSession createFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }
}
