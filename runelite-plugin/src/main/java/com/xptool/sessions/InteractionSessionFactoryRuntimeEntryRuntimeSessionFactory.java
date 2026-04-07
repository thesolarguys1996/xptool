package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory {
    private InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle) {
        return InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeBundle(runtimeBundle);
    }

    static InteractionSession createFromRuntimeOperations(InteractionSessionRuntimeOperations runtimeOperations) {
        return InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeOperations(runtimeOperations);
    }
}
