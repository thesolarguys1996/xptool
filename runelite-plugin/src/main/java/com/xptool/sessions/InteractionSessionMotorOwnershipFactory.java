package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import java.util.function.BooleanSupplier;

final class InteractionSessionMotorOwnershipFactory {
    private InteractionSessionMotorOwnershipFactory() {
        // Static factory utility.
    }

    static InteractionSessionMotorOwnershipService createMotorOwnershipService(CommandExecutor executor) {
        return createMotorOwnershipServiceFromHost(
            createMotorOwnershipHost(executor)
        );
    }

    static InteractionSessionMotorOwnershipService createMotorOwnershipServiceFromHost(
        InteractionSessionMotorOwnershipService.Host host
    ) {
        return new InteractionSessionMotorOwnershipService(host);
    }

    static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHost(CommandExecutor executor) {
        return createMotorOwnershipHostFromDelegates(
            executor::acquireOrRenewInteractionMotorOwnership,
            executor::releaseInteractionMotorOwnership
        );
    }

    static InteractionSessionMotorOwnershipService.Host createMotorOwnershipHostFromDelegates(
        BooleanSupplier acquireOrRenewInteractionMotorOwnership,
        Runnable releaseInteractionMotorOwnership
    ) {
        return new InteractionSessionMotorOwnershipService.Host() {
            @Override
            public boolean acquireOrRenewInteractionMotorOwnership() {
                return acquireOrRenewInteractionMotorOwnership.getAsBoolean();
            }

            @Override
            public void releaseInteractionMotorOwnership() {
                releaseInteractionMotorOwnership.run();
            }
        };
    }
}
