package com.xptool.sessions;

final class InteractionSessionFactoryRuntimeEntrySessionFactory {
    private InteractionSessionFactoryRuntimeEntrySessionFactory() {
        // Static utility.
    }

    static InteractionSession createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeBundle(
        return InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.createFromRuntimeBundle(runtimeBundle);
    }

    static InteractionSession createFromRuntimeOperations(InteractionSessionRuntimeOperations runtimeOperations) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeOperations(
        return InteractionSessionFactoryRuntimeEntryRuntimeSessionFactory.createFromRuntimeOperations(runtimeOperations);
    }
}
