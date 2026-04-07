package com.xptool.sessions;

final class InteractionSessionFactoryFactoryInputsDefaultSessionFactory {
    private InteractionSessionFactoryFactoryInputsDefaultSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs,
        String defaultSessionInteractionKey
    ) {
        return InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(
            factoryInputs,
            defaultSessionInteractionKey
        );
    }
}
