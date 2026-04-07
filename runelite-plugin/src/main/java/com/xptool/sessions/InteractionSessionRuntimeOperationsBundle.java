package com.xptool.sessions;

final class InteractionSessionRuntimeOperationsBundle {
    final InteractionSessionCommandRouter interactionSessionCommandRouter;
    final InteractionSessionClickEventService interactionSessionClickEventService;
    final InteractionSessionOwnershipService interactionSessionOwnershipService;
    final InteractionSessionShutdownService interactionSessionShutdownService;

    InteractionSessionRuntimeOperationsBundle(
        InteractionSessionCommandRouter interactionSessionCommandRouter,
        InteractionSessionClickEventService interactionSessionClickEventService,
        InteractionSessionOwnershipService interactionSessionOwnershipService,
        InteractionSessionShutdownService interactionSessionShutdownService
    ) {
        this.interactionSessionCommandRouter = interactionSessionCommandRouter;
        this.interactionSessionClickEventService = interactionSessionClickEventService;
        this.interactionSessionOwnershipService = interactionSessionOwnershipService;
        this.interactionSessionShutdownService = interactionSessionShutdownService;
    }
}
