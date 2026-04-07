package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.motion.MotionProfile;
import java.awt.Point;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class InteractionSessionRuntimeBundleFactoryTest {
    @Test
    void createRuntimeBundleFromServicesRetainsServiceReferences() {
        InteractionSessionCommandRouter router = InteractionSessionCommandRouterFactory.createCommandRouterServiceFromHost(
            new InteractionSessionCommandRouter.Host() {
                @Override
                public CommandExecutor.CommandDecision executeWoodcutChopNearestTree(
                    JsonObject payload,
                    MotionProfile motionProfile
                ) {
                    return decisionForReason("woodcut");
                }

                @Override
                public CommandExecutor.CommandDecision executeMineNearestRock(JsonObject payload, MotionProfile motionProfile) {
                    return decisionForReason("mine");
                }

                @Override
                public CommandExecutor.CommandDecision executeFishNearestSpot(JsonObject payload, MotionProfile motionProfile) {
                    return decisionForReason("fish");
                }

                @Override
                public CommandExecutor.CommandDecision executeWalkToWorldPoint(JsonObject payload, MotionProfile motionProfile) {
                    return decisionForReason("walk");
                }

                @Override
                public CommandExecutor.CommandDecision executeCameraNudgeSafe(JsonObject payload) {
                    return decisionForReason("camera_nudge");
                }

                @Override
                public CommandExecutor.CommandDecision executeCombatAttackNearestNpc(
                    JsonObject payload,
                    MotionProfile motionProfile
                ) {
                    return decisionForReason("combat");
                }

                @Override
                public CommandExecutor.CommandDecision executeNpcContextMenuTest(JsonObject payload) {
                    return decisionForReason("npc_context");
                }

                @Override
                public CommandExecutor.CommandDecision executeSceneObjectActionSafe(JsonObject payload) {
                    return decisionForReason("scene_object");
                }

                @Override
                public CommandExecutor.CommandDecision executeAgilityObstacleAction(
                    JsonObject payload,
                    MotionProfile motionProfile
                ) {
                    return decisionForReason("agility");
                }

                @Override
                public CommandExecutor.CommandDecision executeGroundItemActionSafe(JsonObject payload) {
                    return decisionForReason("ground_item");
                }

                @Override
                public CommandExecutor.CommandDecision executeShopBuyItemSafe(JsonObject payload) {
                    return decisionForReason("shop_buy");
                }

                @Override
                public CommandExecutor.CommandDecision executeWorldHopSafe(JsonObject payload) {
                    return decisionForReason("world_hop");
                }

                @Override
                public CommandExecutor.CommandDecision executeEatFoodSafe(JsonObject payload) {
                    return decisionForReason("eat_food");
                }

                @Override
                public CommandExecutor.CommandDecision rejectUnsupportedCommandType() {
                    return decisionForReason("unsupported");
                }
            }
        );
        InteractionSessionRegistrationService registrationService =
            InteractionSessionRegistrationFactory.createRegistrationServiceFromHost(
                sessionName -> new SessionManager().registerSession(sessionName),
                "interaction"
            );
        InteractionSessionMotorOwnershipService motorOwnershipService =
            InteractionSessionMotorOwnershipFactory.createMotorOwnershipServiceFromHost(
                new InteractionSessionMotorOwnershipService.Host() {
                    @Override
                    public boolean acquireOrRenewInteractionMotorOwnership() {
                        return true;
                    }

                    @Override
                    public void releaseInteractionMotorOwnership() {
                        // No-op for mapping assertion.
                    }
                }
            );
        InteractionPostClickSettleService postClickSettleService =
            InteractionPostClickSettleFactory.createPostClickSettleServiceFromHost(
                new InteractionPostClickSettleService.Host() {
                    @Override
                    public boolean hasActiveSessionOtherThanInteraction() {
                        return false;
                    }

                    @Override
                    public boolean hasPendingCommandRows() {
                        return false;
                    }

                    @Override
                    public long currentMotorActionSerial() {
                        return 0L;
                    }

                    @Override
                    public boolean performInteractionPostClickSettleMove(Point settleAnchor) {
                        return false;
                    }

                    @Override
                    public long nowMs() {
                        return 0L;
                    }

                    @Override
                    public int randomPercentRoll() {
                        return 0;
                    }

                    @Override
                    public long randomLongInclusive(long minInclusive, long maxInclusive) {
                        return minInclusive;
                    }
                }
            );
        InteractionSessionClickEventService clickEventService =
            InteractionSessionClickEventFactory.createClickEventServiceFromHost(clickEvent -> {
                // No-op for mapping assertion.
            });
        InteractionSessionOwnershipService ownershipService =
            InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(
                new InteractionSessionOwnershipService.Host() {
                    @Override
                    public boolean shouldOwnForInteraction() {
                        return false;
                    }

                    @Override
                    public boolean hasPendingSettle() {
                        return false;
                    }

                    @Override
                    public boolean settleReadyForMotor() {
                        return false;
                    }

                    @Override
                    public boolean hasActiveInteractionMotorProgram() {
                        return false;
                    }

                    @Override
                    public void clearPendingSettle() {
                        // No-op for mapping assertion.
                    }

                    @Override
                    public void clearRegistration() {
                        // No-op for mapping assertion.
                    }

                    @Override
                    public void releaseInteractionMotorOwnership() {
                        // No-op for mapping assertion.
                    }

                    @Override
                    public void ensureRegistered() {
                        // No-op for mapping assertion.
                    }

                    @Override
                    public boolean hasActiveSessionOtherThanInteraction() {
                        return false;
                    }

                    @Override
                    public boolean acquireOrRenewInteractionMotorOwnership() {
                        return false;
                    }

                    @Override
                    public void tryRunPostClickSettle() {
                        // No-op for mapping assertion.
                    }
                }
            );
        InteractionSessionShutdownService shutdownService =
            InteractionSessionShutdownFactory.createShutdownServiceFromHost(
                new InteractionSessionShutdownService.Host() {
                    @Override
                    public void clearPendingPostClickSettle() {
                        // No-op for mapping assertion.
                    }

                    @Override
                    public void clearRegistration() {
                        // No-op for mapping assertion.
                    }

                    @Override
                    public void releaseInteractionMotorOwnership() {
                        // No-op for mapping assertion.
                    }
                }
            );

        InteractionSessionRuntimeBundle bundle = InteractionSessionRuntimeBundleFactory.createRuntimeBundleFromServices(
            router,
            registrationService,
            motorOwnershipService,
            postClickSettleService,
            clickEventService,
            ownershipService,
            shutdownService
        );

        InteractionSessionRuntimeOperationsBundle runtimeOperationsBundle =
            bundle.interactionSessionRuntimeOperationsBundle();
        assertSame(router, runtimeOperationsBundle.interactionSessionCommandRouter);
        assertSame(clickEventService, runtimeOperationsBundle.interactionSessionClickEventService);
        assertSame(ownershipService, runtimeOperationsBundle.interactionSessionOwnershipService);
        assertSame(shutdownService, runtimeOperationsBundle.interactionSessionShutdownService);

        InteractionSessionRuntimeControlBundle runtimeControlBundle = bundle.interactionSessionRuntimeControlBundle();
        assertSame(registrationService, runtimeControlBundle.interactionSessionRegistrationService);
        assertSame(motorOwnershipService, runtimeControlBundle.interactionSessionMotorOwnershipService);
        assertSame(postClickSettleService, runtimeControlBundle.interactionPostClickSettleService);

        InteractionSessionRuntimeBundleFactoryInputs runtimeBundleFactoryInputs =
            InteractionSessionRuntimeBundleFactoryInputs.fromServices(
                router,
                registrationService,
                motorOwnershipService,
                postClickSettleService,
                clickEventService,
                ownershipService,
                shutdownService
            );
        InteractionSessionRuntimeBundle typedInputBundle = InteractionSessionRuntimeBundleFactory.createRuntimeBundle(
            runtimeBundleFactoryInputs
        );
        InteractionSessionRuntimeOperationsBundle typedInputRuntimeOperationsBundle =
            typedInputBundle.interactionSessionRuntimeOperationsBundle();
        assertSame(router, typedInputRuntimeOperationsBundle.interactionSessionCommandRouter);
        assertSame(clickEventService, typedInputRuntimeOperationsBundle.interactionSessionClickEventService);
        assertSame(ownershipService, typedInputRuntimeOperationsBundle.interactionSessionOwnershipService);
        assertSame(shutdownService, typedInputRuntimeOperationsBundle.interactionSessionShutdownService);

        InteractionSessionRuntimeControlBundle typedInputRuntimeControlBundle =
            typedInputBundle.interactionSessionRuntimeControlBundle();
        assertSame(registrationService, typedInputRuntimeControlBundle.interactionSessionRegistrationService);
        assertSame(motorOwnershipService, typedInputRuntimeControlBundle.interactionSessionMotorOwnershipService);
        assertSame(postClickSettleService, typedInputRuntimeControlBundle.interactionPostClickSettleService);
    }

    private static CommandExecutor.CommandDecision decisionForReason(String reason) {
        try {
            Constructor<CommandExecutor.CommandDecision> ctor = CommandExecutor.CommandDecision.class
                .getDeclaredConstructor(boolean.class, String.class, JsonObject.class);
            ctor.setAccessible(true);
            return ctor.newInstance(true, reason, null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to construct CommandDecision for test", ex);
        }
    }
}
