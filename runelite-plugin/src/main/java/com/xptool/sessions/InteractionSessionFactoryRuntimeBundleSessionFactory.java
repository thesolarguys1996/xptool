package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeBundleSessionFactory {
    private InteractionSessionFactoryRuntimeBundleSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle) {
        return InteractionSessionRuntimeSessionFactory.createFromRuntimeBundle(runtimeBundle);
    }

    static InteractionSession createFromRuntimeOperations(InteractionSessionRuntimeOperations runtimeOperations) {
        return InteractionSessionRuntimeSessionFactory.createFromRuntimeOperations(runtimeOperations);
    }
}
