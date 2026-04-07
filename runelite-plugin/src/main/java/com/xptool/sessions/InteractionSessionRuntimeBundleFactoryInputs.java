package com.xptool.sessions;

final class InteractionSessionRuntimeBundleFactoryInputs {
    final InteractionSessionCommandRouter interactionSessionCommandRouter;
    final InteractionSessionRegistrationService interactionSessionRegistrationService;
    final InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService;
    final InteractionPostClickSettleService interactionPostClickSettleService;
    final InteractionSessionClickEventService interactionSessionClickEventService;
    final InteractionSessionOwnershipService interactionSessionOwnershipService;
    final InteractionSessionShutdownService interactionSessionShutdownService;

    private InteractionSessionRuntimeBundleFactoryInputs(
        InteractionSessionCommandRouter interactionSessionCommandRouter,
        InteractionSessionRegistrationService interactionSessionRegistrationService,
        InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService,
        InteractionPostClickSettleService interactionPostClickSettleService,
        InteractionSessionClickEventService interactionSessionClickEventService,
        InteractionSessionOwnershipService interactionSessionOwnershipService,
        InteractionSessionShutdownService interactionSessionShutdownService
    ) {
        this.interactionSessionCommandRouter = interactionSessionCommandRouter;
        this.interactionSessionRegistrationService = interactionSessionRegistrationService;
        this.interactionSessionMotorOwnershipService = interactionSessionMotorOwnershipService;
        this.interactionPostClickSettleService = interactionPostClickSettleService;
        this.interactionSessionClickEventService = interactionSessionClickEventService;
        this.interactionSessionOwnershipService = interactionSessionOwnershipService;
        this.interactionSessionShutdownService = interactionSessionShutdownService;
    }

    static InteractionSessionRuntimeBundleFactoryInputs fromServices(
        InteractionSessionCommandRouter interactionSessionCommandRouter,
        InteractionSessionRegistrationService interactionSessionRegistrationService,
        InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService,
        InteractionPostClickSettleService interactionPostClickSettleService,
        InteractionSessionClickEventService interactionSessionClickEventService,
        InteractionSessionOwnershipService interactionSessionOwnershipService,
        InteractionSessionShutdownService interactionSessionShutdownService
    ) {
        return new InteractionSessionRuntimeBundleFactoryInputs(
            interactionSessionCommandRouter,
            interactionSessionRegistrationService,
            interactionSessionMotorOwnershipService,
            interactionPostClickSettleService,
            interactionSessionClickEventService,
            interactionSessionOwnershipService,
            interactionSessionShutdownService
        );
    }

    InteractionSessionRuntimeOperationsBundle createRuntimeOperationsBundle() {
        return new InteractionSessionRuntimeOperationsBundle(
            interactionSessionCommandRouter,
            interactionSessionClickEventService,
            interactionSessionOwnershipService,
            interactionSessionShutdownService
        );
    }

    InteractionSessionRuntimeControlBundle createRuntimeControlBundle() {
        return new InteractionSessionRuntimeControlBundle(
            interactionSessionRegistrationService,
            interactionSessionMotorOwnershipService,
            interactionPostClickSettleService
        );
    }
}
