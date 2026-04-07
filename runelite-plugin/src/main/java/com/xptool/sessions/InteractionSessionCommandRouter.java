package com.xptool.sessions;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.motion.MotionProfile;

final class InteractionSessionCommandRouter {
    interface Host {
        CommandExecutor.CommandDecision executeWoodcutChopNearestTree(JsonObject payload, MotionProfile motionProfile);

        CommandExecutor.CommandDecision executeMineNearestRock(JsonObject payload, MotionProfile motionProfile);

        CommandExecutor.CommandDecision executeFishNearestSpot(JsonObject payload, MotionProfile motionProfile);

        CommandExecutor.CommandDecision executeWalkToWorldPoint(JsonObject payload, MotionProfile motionProfile);

        CommandExecutor.CommandDecision executeCameraNudgeSafe(JsonObject payload);

        CommandExecutor.CommandDecision executeCombatAttackNearestNpc(JsonObject payload, MotionProfile motionProfile);

        CommandExecutor.CommandDecision executeNpcContextMenuTest(JsonObject payload);

        CommandExecutor.CommandDecision executeSceneObjectActionSafe(JsonObject payload);

        CommandExecutor.CommandDecision executeAgilityObstacleAction(JsonObject payload, MotionProfile motionProfile);

        CommandExecutor.CommandDecision executeGroundItemActionSafe(JsonObject payload);

        CommandExecutor.CommandDecision executeShopBuyItemSafe(JsonObject payload);

        CommandExecutor.CommandDecision executeWorldHopSafe(JsonObject payload);

        CommandExecutor.CommandDecision executeEatFoodSafe(JsonObject payload);

        CommandExecutor.CommandDecision rejectUnsupportedCommandType();
    }

    private final Host host;

    InteractionSessionCommandRouter(Host host) {
        this.host = host;
    }

    boolean supports(String commandType) {
        return "WOODCUT_CHOP_NEAREST_TREE_SAFE".equals(commandType)
            || "MINE_NEAREST_ROCK_SAFE".equals(commandType)
            || "FISH_NEAREST_SPOT_SAFE".equals(commandType)
            || "WALK_TO_WORLDPOINT_SAFE".equals(commandType)
            || "CAMERA_NUDGE_SAFE".equals(commandType)
            || "COMBAT_ATTACK_NEAREST_NPC_SAFE".equals(commandType)
            || "NPC_CONTEXT_MENU_TEST".equals(commandType)
            || "SCENE_OBJECT_ACTION_SAFE".equals(commandType)
            || "AGILITY_OBSTACLE_ACTION_SAFE".equals(commandType)
            || "GROUND_ITEM_ACTION_SAFE".equals(commandType)
            || "SHOP_BUY_ITEM_SAFE".equals(commandType)
            || "WORLD_HOP_SAFE".equals(commandType)
            || "EAT_FOOD_SAFE".equals(commandType);
    }

    CommandExecutor.CommandDecision execute(String commandType, JsonObject payload, MotionProfile motionProfile) {
        switch (commandType) {
            case "WOODCUT_CHOP_NEAREST_TREE_SAFE":
                return host.executeWoodcutChopNearestTree(payload, motionProfile);
            case "MINE_NEAREST_ROCK_SAFE":
                return host.executeMineNearestRock(payload, motionProfile);
            case "FISH_NEAREST_SPOT_SAFE":
                return host.executeFishNearestSpot(payload, motionProfile);
            case "WALK_TO_WORLDPOINT_SAFE":
                return host.executeWalkToWorldPoint(payload, motionProfile);
            case "CAMERA_NUDGE_SAFE":
                return host.executeCameraNudgeSafe(payload);
            case "COMBAT_ATTACK_NEAREST_NPC_SAFE":
                return host.executeCombatAttackNearestNpc(payload, motionProfile);
            case "NPC_CONTEXT_MENU_TEST":
                return host.executeNpcContextMenuTest(payload);
            case "SCENE_OBJECT_ACTION_SAFE":
                return host.executeSceneObjectActionSafe(payload);
            case "AGILITY_OBSTACLE_ACTION_SAFE":
                return host.executeAgilityObstacleAction(payload, motionProfile);
            case "GROUND_ITEM_ACTION_SAFE":
                return host.executeGroundItemActionSafe(payload);
            case "SHOP_BUY_ITEM_SAFE":
                return host.executeShopBuyItemSafe(payload);
            case "WORLD_HOP_SAFE":
                return host.executeWorldHopSafe(payload);
            case "EAT_FOOD_SAFE":
                return host.executeEatFoodSafe(payload);
            default:
                return host.rejectUnsupportedCommandType();
        }
    }
}
