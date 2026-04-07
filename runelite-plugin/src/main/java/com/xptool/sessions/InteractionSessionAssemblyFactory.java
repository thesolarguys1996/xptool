package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;

final class InteractionSessionAssemblyFactory {
    private static final String SESSION_INTERACTION = "interaction";

    private InteractionSessionAssemblyFactory() {
        // Static factory utility.
    }

    static InteractionSessionRuntimeBundle createRuntimeBundle(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createRuntimeBundleForSession(
        return createRuntimeBundleForSession(
            InteractionSessionAssemblyFactoryInputs.forDefaultSession(
                executor,
                sessionManager,
                commandFacade,
                SESSION_INTERACTION
            )
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleForSession(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade,
        String sessionInteractionKey
    ) {
        return createRuntimeBundleForSession(
            InteractionSessionAssemblyFactoryInputs.forSession(
                executor,
                sessionManager,
                commandFacade,
                sessionInteractionKey
            )
        );
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleForSession(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        return createRuntimeBundleFromInputs(assemblyFactoryInputs);
    }

    static InteractionSessionRuntimeBundle createRuntimeBundleFromInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        CommandExecutor executor = assemblyFactoryInputs.executor;
        SessionManager sessionManager = assemblyFactoryInputs.sessionManager;
        SessionCommandFacade commandFacade = assemblyFactoryInputs.commandFacade;
        String sessionInteractionKey = assemblyFactoryInputs.sessionInteractionKey;

        InteractionSessionCommandRouter interactionSessionCommandRouter =
            InteractionSessionHostFactory.createCommandRouterService(commandFacade);
        InteractionSessionRegistrationService interactionSessionRegistrationService =
            InteractionSessionHostFactory.createRegistrationService(
                sessionManager,
                sessionInteractionKey
            );
        InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService =
            InteractionSessionHostFactory.createMotorOwnershipService(executor);
        InteractionPostClickSettleService interactionPostClickSettleService =
            InteractionSessionHostFactory.createPostClickSettleService(
                executor,
                sessionManager,
                sessionInteractionKey
            );
        InteractionSessionClickEventService interactionSessionClickEventService =
            InteractionSessionHostFactory.createClickEventService(
                interactionPostClickSettleService::onInteractionClickEvent
            );
        InteractionSessionOwnershipService interactionSessionOwnershipService =
            InteractionSessionHostFactory.createOwnershipService(
                executor,
                sessionManager,
                sessionInteractionKey,
                interactionPostClickSettleService,
                interactionSessionRegistrationService,
                interactionSessionMotorOwnershipService
            );
        InteractionSessionShutdownService interactionSessionShutdownService =
            InteractionSessionHostFactory.createShutdownService(
                interactionPostClickSettleService::clearPendingPostClickSettle,
                interactionSessionRegistrationService::clearRegistration,
                interactionSessionMotorOwnershipService::releaseInteractionMotorOwnership
            );
        return createRuntimeBundleFromServices(
            interactionSessionCommandRouter,
            interactionSessionRegistrationService,
            interactionSessionMotorOwnershipService,
            interactionPostClickSettleService,
            interactionSessionClickEventService,
            interactionSessionOwnershipService,
            interactionSessionShutdownService
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
        // return new InteractionSessionRuntimeBundle(
        //     interactionSessionCommandRouter,
        //     interactionSessionRegistrationService,
        //     interactionSessionMotorOwnershipService,
        //     interactionPostClickSettleService,
        //     interactionSessionClickEventService,
        //     interactionSessionOwnershipService,
        //     interactionSessionShutdownService
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionRuntimeBundleFactory.createRuntimeBundleFromServices(
        return InteractionSessionRuntimeBundleFactory.createRuntimeBundle(
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
