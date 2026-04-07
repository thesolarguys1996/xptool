package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.motion.MotionProfile;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class InteractionSessionCommandRouterTest {
    @Test
    void supportsRecognizedInteractionCommandTypes() {
        TestHost host = new TestHost();
        InteractionSessionCommandRouter router = new InteractionSessionCommandRouter(host);

        assertTrue(router.supports("WOODCUT_CHOP_NEAREST_TREE_SAFE"));
        assertTrue(router.supports("MINE_NEAREST_ROCK_SAFE"));
        assertTrue(router.supports("FISH_NEAREST_SPOT_SAFE"));
        assertTrue(router.supports("WALK_TO_WORLDPOINT_SAFE"));
        assertTrue(router.supports("CAMERA_NUDGE_SAFE"));
        assertTrue(router.supports("COMBAT_ATTACK_NEAREST_NPC_SAFE"));
        assertTrue(router.supports("NPC_CONTEXT_MENU_TEST"));
        assertTrue(router.supports("SCENE_OBJECT_ACTION_SAFE"));
        assertTrue(router.supports("AGILITY_OBSTACLE_ACTION_SAFE"));
        assertTrue(router.supports("GROUND_ITEM_ACTION_SAFE"));
        assertTrue(router.supports("SHOP_BUY_ITEM_SAFE"));
        assertTrue(router.supports("WORLD_HOP_SAFE"));
        assertTrue(router.supports("EAT_FOOD_SAFE"));
        assertFalse(router.supports("UNKNOWN_COMMAND"));
        assertFalse(router.supports(null));
    }

    @Test
    void executeRoutesSkillingCommandsWithPayloadAndMotionProfile() {
        TestHost host = new TestHost();
        InteractionSessionCommandRouter router = new InteractionSessionCommandRouter(host);
        JsonObject payload = new JsonObject();
        payload.addProperty("target", "oak");
        MotionProfile profile = MotionProfile.WOODCUT;

        CommandExecutor.CommandDecision decision = router.execute(
            "WOODCUT_CHOP_NEAREST_TREE_SAFE",
            payload,
            profile
        );

        assertEquals("woodcut", decision.getReason());
        assertEquals("woodcut", host.lastMethod);
        assertSame(payload, host.lastPayload);
        assertSame(profile, host.lastMotionProfile);
    }

    @Test
    void executeRoutesCameraAndUnsupportedCommandsWithoutMotionProfileDependency() {
        TestHost host = new TestHost();
        InteractionSessionCommandRouter router = new InteractionSessionCommandRouter(host);
        JsonObject payload = new JsonObject();
        payload.addProperty("dx", 1);

        CommandExecutor.CommandDecision cameraDecision = router.execute("CAMERA_NUDGE_SAFE", payload, null);
        assertEquals("camera_nudge", cameraDecision.getReason());
        assertEquals("camera_nudge", host.lastMethod);
        assertSame(payload, host.lastPayload);
        assertNull(host.lastMotionProfile);

        CommandExecutor.CommandDecision unsupportedDecision = router.execute("NOT_SUPPORTED", payload, MotionProfile.COMBAT);
        assertEquals("unsupported", unsupportedDecision.getReason());
        assertEquals("unsupported", host.lastMethod);
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
}
