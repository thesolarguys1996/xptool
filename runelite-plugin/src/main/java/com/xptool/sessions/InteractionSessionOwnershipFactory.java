package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import java.util.function.BooleanSupplier;

final class InteractionSessionOwnershipFactory {
    private InteractionSessionOwnershipFactory() {
        // Static factory utility.
    }

    static InteractionSessionOwnershipService createOwnershipService(
        CommandExecutor executor,
        SessionManager sessionManager,
        String sessionInteractionKey,
        InteractionPostClickSettleService interactionPostClickSettleService,
        InteractionSessionRegistrationService interactionSessionRegistrationService,
        InteractionSessionMotorOwnershipService interactionSessionMotorOwnershipService
    ) {
        return createOwnershipServiceFromHost(
            createOwnershipHost(
                executor,
                sessionManager,
                sessionInteractionKey,
                interactionPostClickSettleService,
                interactionSessionMotorOwnershipService::releaseInteractionMotorOwnership,
                interactionSessionRegistrationService::clearRegistration,
                interactionSessionRegistrationService::ensureRegistered,
                interactionSessionMotorOwnershipService::acquireOrRenewInteractionMotorOwnership
            )
        );
    }

    static InteractionSessionOwnershipService createOwnershipServiceFromHost(
        InteractionSessionOwnershipService.Host host
    ) {
        return new InteractionSessionOwnershipService(host);
    }

    static InteractionSessionOwnershipService.Host createOwnershipHost(
        CommandExecutor executor,
        SessionManager sessionManager,
        String sessionInteractionKey,
        InteractionPostClickSettleService interactionPostClickSettleService,
        Runnable releaseInteractionMotorOwnership,
        Runnable clearRegistration,
        Runnable ensureRegistered,
        BooleanSupplier acquireOrRenewMotorOwnership
    ) {
        return createOwnershipHostFromDelegates(
            executor::shouldOwnInteractionSession,
            interactionPostClickSettleService::hasPendingPostClickSettle,
            interactionPostClickSettleService::shouldAcquireMotorForPendingSettle,
            () -> executor.hasActiveMotorProgramForOwner(sessionInteractionKey),
            interactionPostClickSettleService::clearPendingPostClickSettle,
            clearRegistration,
            releaseInteractionMotorOwnership,
            ensureRegistered,
            () -> sessionManager.hasActiveSessionOtherThan(sessionInteractionKey),
            acquireOrRenewMotorOwnership,
            interactionPostClickSettleService::tryRunPostClickSettle
        );
    }

    static InteractionSessionOwnershipService.Host createOwnershipHostFromDelegates(
        BooleanSupplier shouldOwnForInteraction,
        BooleanSupplier hasPendingSettle,
        BooleanSupplier settleReadyForMotor,
        BooleanSupplier hasActiveInteractionMotorProgram,
        Runnable clearPendingSettle,
        Runnable clearRegistration,
        Runnable releaseInteractionMotorOwnership,
        Runnable ensureRegistered,
        BooleanSupplier hasActiveSessionOtherThanInteraction,
        BooleanSupplier acquireOrRenewInteractionMotorOwnership,
        Runnable tryRunPostClickSettle
    ) {
        return new InteractionSessionOwnershipService.Host() {
            @Override
            public boolean shouldOwnForInteraction() {
                return shouldOwnForInteraction.getAsBoolean();
            }

            @Override
            public boolean hasPendingSettle() {
                return hasPendingSettle.getAsBoolean();
            }

            @Override
            public boolean settleReadyForMotor() {
                return settleReadyForMotor.getAsBoolean();
            }

            @Override
            public boolean hasActiveInteractionMotorProgram() {
                return hasActiveInteractionMotorProgram.getAsBoolean();
            }

            @Override
            public void clearPendingSettle() {
                clearPendingSettle.run();
            }

            @Override
            public void clearRegistration() {
                clearRegistration.run();
            }

            @Override
            public void releaseInteractionMotorOwnership() {
                releaseInteractionMotorOwnership.run();
            }

            @Override
            public void ensureRegistered() {
                ensureRegistered.run();
            }

            @Override
            public boolean hasActiveSessionOtherThanInteraction() {
                return hasActiveSessionOtherThanInteraction.getAsBoolean();
            }

            @Override
            public boolean acquireOrRenewInteractionMotorOwnership() {
                return acquireOrRenewInteractionMotorOwnership.getAsBoolean();
            }

            @Override
            public void tryRunPostClickSettle() {
                tryRunPostClickSettle.run();
            }
        };
    }
}
