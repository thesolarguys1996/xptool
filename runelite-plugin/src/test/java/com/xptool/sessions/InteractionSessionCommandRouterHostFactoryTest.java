package com.xptool.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.motion.MotionProfile;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class InteractionSessionCommandRouterHostFactoryTest {
    @Test
    void createCommandRouterHostFromDelegatesRoutesAllCommandBranches() {
        JsonObject payload = new JsonObject();
        payload.addProperty("target", "oak");
        MotionProfile profile = MotionProfile.WOODCUT;
        Counter counter = new Counter();
        InteractionSessionCommandRouter.Host host =
            InteractionSessionCommandRouterHostFactory.createCommandRouterHostFromDelegates(
                (branchPayload, branchProfile) -> record(counter, "woodcut", branchPayload, branchProfile),
                (branchPayload, branchProfile) -> record(counter, "mine", branchPayload, branchProfile),
                (branchPayload, branchProfile) -> record(counter, "fish", branchPayload, branchProfile),
                (branchPayload, branchProfile) -> record(counter, "walk", branchPayload, branchProfile),
                branchPayload -> record(counter, "camera_nudge", branchPayload, null),
                (branchPayload, branchProfile) -> record(counter, "combat", branchPayload, branchProfile),
                branchPayload -> record(counter, "npc_context", branchPayload, null),
                branchPayload -> record(counter, "scene_object", branchPayload, null),
                (branchPayload, branchProfile) -> record(counter, "agility", branchPayload, branchProfile),
                branchPayload -> record(counter, "ground_item", branchPayload, null),
                branchPayload -> record(counter, "shop_buy", branchPayload, null),
                branchPayload -> record(counter, "world_hop", branchPayload, null),
                branchPayload -> record(counter, "eat_food", branchPayload, null),
                () -> decisionForReason("unsupported")
            );

        assertEquals("woodcut", host.executeWoodcutChopNearestTree(payload, profile).getReason());
        assertEquals("mine", host.executeMineNearestRock(payload, profile).getReason());
        assertEquals("fish", host.executeFishNearestSpot(payload, profile).getReason());
        assertEquals("walk", host.executeWalkToWorldPoint(payload, profile).getReason());
        assertEquals("camera_nudge", host.executeCameraNudgeSafe(payload).getReason());
        assertEquals("combat", host.executeCombatAttackNearestNpc(payload, profile).getReason());
        assertEquals("npc_context", host.executeNpcContextMenuTest(payload).getReason());
        assertEquals("scene_object", host.executeSceneObjectActionSafe(payload).getReason());
        assertEquals("agility", host.executeAgilityObstacleAction(payload, profile).getReason());
        assertEquals("ground_item", host.executeGroundItemActionSafe(payload).getReason());
        assertEquals("shop_buy", host.executeShopBuyItemSafe(payload).getReason());
        assertEquals("world_hop", host.executeWorldHopSafe(payload).getReason());
        assertEquals("eat_food", host.executeEatFoodSafe(payload).getReason());
        assertEquals("unsupported", host.rejectUnsupportedCommandType().getReason());
        assertEquals(13, counter.calls);
        assertSame(payload, counter.lastPayload);
        assertNull(counter.lastMotionProfile);
    }

    private static CommandExecutor.CommandDecision record(
        Counter counter,
        String reason,
        JsonObject payload,
        MotionProfile motionProfile
    ) {
        counter.calls++;
        counter.lastPayload = payload;
        counter.lastMotionProfile = motionProfile;
        return decisionForReason(reason);
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

    private static final class Counter {
        private int calls = 0;
        private JsonObject lastPayload = null;
        private MotionProfile lastMotionProfile = null;
    }
}
