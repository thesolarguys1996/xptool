package com.xptool.sessions;

final class InteractionSessionRuntimeSessionFactory {
    private InteractionSessionRuntimeSessionFactory() {
        // Static factory utility.
    }

    static InteractionSession createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle) {
        return createFromRuntimeOperations(
            InteractionSessionRuntimeOperationsFactory.createFromRuntimeBundle(runtimeBundle)
        );
    }

    static InteractionSession createFromRuntimeOperations(InteractionSessionRuntimeOperations runtimeOperations) {
        return new InteractionSession(runtimeOperations);
    }
}
