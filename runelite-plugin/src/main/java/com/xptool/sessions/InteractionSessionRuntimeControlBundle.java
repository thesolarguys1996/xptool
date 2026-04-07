package com.xptool.sessions;

final class InteractionSessionRuntimeControlBundle {
    final InteractionSessionRegistrationService interactionSessionRegistrationService;
    final InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService;
    final InteractionPostClickSettleService interactionPostClickSettleService;

    InteractionSessionRuntimeControlBundle(
        InteractionSessionRegistrationService interactionSessionRegistrationService,
        InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService,
        InteractionPostClickSettleService interactionPostClickSettleService
    ) {
        this.interactionSessionRegistrationService = interactionSessionRegistrationService;
        this.interactionSessionMotorOwnershipService = interactionSessionMotorOwnershipService;
        this.interactionPostClickSettleService = interactionPostClickSettleService;
    }
}
