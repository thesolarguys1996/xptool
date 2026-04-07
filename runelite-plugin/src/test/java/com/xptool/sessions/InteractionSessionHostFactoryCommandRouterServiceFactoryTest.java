package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;
import com.xptool.motion.MotionProfile;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class InteractionSessionHostFactoryCommandRouterServiceFactoryTest {
    @Test
    void createCommandRouterServiceRoutesUnsupportedBranchFromFacade() {
        SessionCommandFacade facade = testFacade();
        InteractionSessionCommandRouter router = InteractionSessionHostFactory.createCommandRouterService(facade);
        JsonObject payload = new JsonObject();
        payload.addProperty("target", "oak");

        CommandExecutor.CommandDecision decision = router.execute("UNKNOWN", payload, MotionProfile.WOODCUT);

        assertEquals("unsupported", decision.getReason());
    }

    @Test
    void createCommandRouterServiceFromHostRoutesExecuteAndUnsupportedBranches() {
        TestHost host = new TestHost();
        InteractionSessionCommandRouter router = InteractionSessionHostFactory.createCommandRouterServiceFromHost(host);
        JsonObject payload = new JsonObject();
        payload.addProperty("target", "oak");

        assertTrue(router.supports("WOODCUT_CHOP_NEAREST_TREE_SAFE"));

        CommandExecutor.CommandDecision woodcutDecision = router.execute(
            "WOODCUT_CHOP_NEAREST_TREE_SAFE",
            payload,
            MotionProfile.WOODCUT
        );
        assertEquals("woodcut", woodcutDecision.getReason());
        assertEquals("woodcut", host.lastMethod);
        assertSame(payload, host.lastPayload);
        assertSame(MotionProfile.WOODCUT, host.lastMotionProfile);

        CommandExecutor.CommandDecision unsupportedDecision = router.execute("UNKNOWN", payload, MotionProfile.COMBAT);
        assertEquals("unsupported", unsupportedDecision.getReason());
        assertEquals("unsupported", host.lastMethod);
        assertNull(host.lastPayload);
        assertNull(host.lastMotionProfile);
        assertNotNull(unsupportedDecision);
    }

    private static final class TestHost implements InteractionSessionCommandRouter.Host {
        private String lastMethod = "";
        private JsonObject lastPayload = null;
        private MotionProfile lastMotionProfile = null;

        @Override
        public CommandExecutor.CommandDecision executeWoodcutChopNearestTree(JsonObject payload, MotionProfile motionProfile) {
            return mark("woodcut", payload, motionProfile);
        }

        @Override
        public CommandExecutor.CommandDecision executeMineNearestRock(JsonObject payload, MotionProfile motionProfile) {
            return mark("mine", payload, motionProfile);
        }

        @Override
        public CommandExecutor.CommandDecision executeFishNearestSpot(JsonObject payload, MotionProfile motionProfile) {
            return mark("fish", payload, motionProfile);
        }

        @Override
        public CommandExecutor.CommandDecision executeWalkToWorldPoint(JsonObject payload, MotionProfile motionProfile) {
            return mark("walk", payload, motionProfile);
        }

        @Override
        public CommandExecutor.CommandDecision executeCameraNudgeSafe(JsonObject payload) {
            return mark("camera_nudge", payload, null);
        }

        @Override
        public CommandExecutor.CommandDecision executeCombatAttackNearestNpc(JsonObject payload, MotionProfile motionProfile) {
            return mark("combat", payload, motionProfile);
        }

        @Override
        public CommandExecutor.CommandDecision executeNpcContextMenuTest(JsonObject payload) {
            return mark("npc_context", payload, null);
        }

        @Override
        public CommandExecutor.CommandDecision executeSceneObjectActionSafe(JsonObject payload) {
            return mark("scene_object", payload, null);
        }

        @Override
        public CommandExecutor.CommandDecision executeAgilityObstacleAction(JsonObject payload, MotionProfile motionProfile) {
            return mark("agility", payload, motionProfile);
        }

        @Override
        public CommandExecutor.CommandDecision executeGroundItemActionSafe(JsonObject payload) {
            return mark("ground_item", payload, null);
        }

        @Override
        public CommandExecutor.CommandDecision executeShopBuyItemSafe(JsonObject payload) {
            return mark("shop_buy", payload, null);
        }

        @Override
        public CommandExecutor.CommandDecision executeWorldHopSafe(JsonObject payload) {
            return mark("world_hop", payload, null);
        }

        @Override
        public CommandExecutor.CommandDecision executeEatFoodSafe(JsonObject payload) {
            return mark("eat_food", payload, null);
        }

        @Override
        public CommandExecutor.CommandDecision rejectUnsupportedCommandType() {
            return mark("unsupported", null, null);
        }

        private CommandExecutor.CommandDecision mark(String method, JsonObject payload, MotionProfile motionProfile) {
            lastMethod = method;
            lastPayload = payload;
            lastMotionProfile = motionProfile;
            return decisionForReason(method);
        }

    }

    private static SessionCommandFacade testFacade() {
        return new SessionCommandFacade(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            payload -> decisionForReason("npc_context"),
            payload -> decisionForReason("scene_object"),
            payload -> decisionForReason("ground_item"),
            payload -> decisionForReason("shop_buy"),
            payload -> decisionForReason("world_hop"),
            payload -> decisionForReason("camera_nudge"),
            payload -> decisionForReason("eat_food"),
            () -> decisionForReason("unsupported")
        );
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
