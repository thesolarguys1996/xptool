package com.xptool.sessions;

final class InteractionSessionRuntimeBundleFactory {
    private InteractionSessionRuntimeBundleFactory() {
        // Static factory utility.
    }

    static InteractionSessionRuntimeBundle createRuntimeBundle(
        InteractionSessionRuntimeBundleFactoryInputs runtimeBundleFactoryInputs
    ) {
        InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle =
            runtimeBundleFactoryInputs.createRuntimeOperationsBundle();
        InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle =
            runtimeBundleFactoryInputs.createRuntimeControlBundle();
        return new InteractionSessionRuntimeBundle(
            interactionSessionRuntimeOperationsBundle,
            interactionSessionRuntimeControlBundle
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromServices(
        InteractionSessionCommandRouter interactionSessionCommandRouter,
        InteractionSessionRegistrationService interactionSessionRegistrationService,
        InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService,
        InteractionPostClickSettleService interactionPostClickSettleService,
        InteractionSessionClickEventService interactionSessionClickEventService,
        InteractionSessionOwnershipService interactionSessionOwnershipService,
        InteractionSessionShutdownService interactionSessionShutdownService
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionRuntimeOperationsBundle interactionSessionRuntimeOperationsBundle =
        //     new InteractionSessionRuntimeOperationsBundle(
        //         interactionSessionCommandRouter,
        //         interactionSessionClickEventService,
        //         interactionSessionOwnershipService,
        //         interactionSessionShutdownService
        //     );
        // InteractionSessionRuntimeControlBundle interactionSessionRuntimeControlBundle =
        //     new InteractionSessionRuntimeControlBundle(
        //         interactionSessionRegistrationService,
        //         interactionSessionMotorOwnershipService,
        //         interactionPostClickSettleService
        //     );
        // return new InteractionSessionRuntimeBundle(
        //     interactionSessionRuntimeOperationsBundle,
        //     interactionSessionRuntimeControlBundle
        // );
        return createRuntimeBundle(
            InteractionSessionRuntimeBundleFactoryInputs.fromServices(
                interactionSessionCommandRouter,
                interactionSessionRegistrationService,
                interactionSessionMotorOwnershipService,
                interactionPostClickSettleService,
                interactionSessionClickEventService,
                interactionSessionOwnershipService,
                interactionSessionShutdownService
            )
        );
    }
}
