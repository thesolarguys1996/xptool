package com.xptool.sessions;

final class InteractionSessionRuntimeOperationsFactory {
    private InteractionSessionRuntimeOperationsFactory() {
        // Static factory utility.
    }

    static InteractionSessionRuntimeOperations createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle) {
        return createFromRuntimeOperationsBundle(runtimeBundle.interactionSessionRuntimeOperationsBundle());
    }

    static InteractionSessionRuntimeOperations createFromRuntimeOperationsBundle(
        InteractionSessionRuntimeOperationsBundle runtimeOperationsBundle
    ) {
        return createFromServices(
            runtimeOperationsBundle.interactionSessionCommandRouter,
            runtimeOperationsBundle.interactionSessionClickEventService,
            runtimeOperationsBundle.interactionSessionOwnershipService,
            runtimeOperationsBundle.interactionSessionShutdownService
        );
    }

    static InteractionSessionRuntimeOperations createFromServices(
        InteractionSessionCommandRouter interactionSessionCommandRouter,
        InteractionSessionClickEventService interactionSessionClickEventService,
        InteractionSessionOwnershipService interactionSessionOwnershipService,
        InteractionSessionShutdownService interactionSessionShutdownService
    ) {
        return new InteractionSessionRuntimeOperations(
            interactionSessionCommandRouter,
            interactionSessionClickEventService,
            interactionSessionOwnershipService,
            interactionSessionShutdownService
        );
    }
}
