package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.executor.InteractionClickEvent;
import com.xptool.motion.MotionProfile;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class InteractionSessionFactoryTest {
    @Test
    void createFromRuntimeBundleBuildsSessionThatDelegatesToRuntimeServices() {
        int[] clickEvents = {0};
        int[] ownershipTicks = {0};
        int[] shutdownCalls = {0};

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
                        return false;
                    }

                    @Override
                    public void releaseInteractionMotorOwnership() {
                        // No-op for delegation assertion.
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
                    public boolean performInteractionPostClickSettleMove(java.awt.Point settleAnchor) {
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
            InteractionSessionClickEventFactory.createClickEventServiceFromHost(clickEvent -> clickEvents[0]++);
        InteractionSessionOwnershipService ownershipService =
            InteractionSessionOwnershipFactory.createOwnershipServiceFromHost(
                new InteractionSessionOwnershipService.Host() {
                    @Override
                    public boolean shouldOwnForInteraction() {
                        ownershipTicks[0]++;
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
                        // No-op for delegation assertion.
                    }

                    @Override
                    public void clearRegistration() {
                        // No-op for delegation assertion.
                    }

                    @Override
                    public void releaseInteractionMotorOwnership() {
                        // No-op for delegation assertion.
                    }

                    @Override
                    public void ensureRegistered() {
                        // No-op for delegation assertion.
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
                        // No-op for delegation assertion.
                    }
                }
            );
        InteractionSessionShutdownService shutdownService =
            InteractionSessionShutdownFactory.createShutdownServiceFromHost(
                new InteractionSessionShutdownService.Host() {
                    @Override
                    public void clearPendingPostClickSettle() {
                        shutdownCalls[0]++;
                    }

                    @Override
                    public void clearRegistration() {
                        shutdownCalls[0]++;
                    }

                    @Override
                    public void releaseInteractionMotorOwnership() {
                        shutdownCalls[0]++;
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
        InteractionSession session = InteractionSessionFactory.createFromRuntimeBundle(bundle);

        assertTrue(session.supports("WOODCUT_CHOP_NEAREST_TREE_SAFE"));
        CommandExecutor.CommandDecision decision = session.execute(
            "WOODCUT_CHOP_NEAREST_TREE_SAFE",
            new JsonObject(),
            MotionProfile.WOODCUT
        );
        assertEquals("woodcut", decision.getReason());

        session.onInteractionClickEvent(
            new InteractionClickEvent(
                1L,
                1,
                0L,
                "interaction",
                "fishing_world",
                null,
                null,
                null,
                0L
            )
        );
        assertEquals(1, clickEvents[0]);

        session.onGameTick(1);
        assertEquals(1, ownershipTicks[0]);

        session.shutdown();
        assertEquals(3, shutdownCalls[0]);
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

    @Test
    void exposesFactoryDefaultEntryRoutingEntryPoints() throws NoSuchMethodException {
        assertTrue(
            InteractionSessionFactory.class.getDeclaredMethod(
                "createFromDefaultRuntimeBundleFactoryInputs",
                InteractionSessionFactoryRuntimeBundleFactoryInputs.class
            ) != null
        );
    }
}
