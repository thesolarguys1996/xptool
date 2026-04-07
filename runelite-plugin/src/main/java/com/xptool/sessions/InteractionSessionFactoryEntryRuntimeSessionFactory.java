package com.xptool.sessions;

final class InteractionSessionFactoryEntryRuntimeSessionFactory {
    private InteractionSessionFactoryEntryRuntimeSessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle) {
        return InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeBundle(runtimeBundle);
    }

    static InteractionSession createFromRuntimeOperations(InteractionSessionRuntimeOperations runtimeOperations) {
        return InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeOperations(runtimeOperations);
    }
}
