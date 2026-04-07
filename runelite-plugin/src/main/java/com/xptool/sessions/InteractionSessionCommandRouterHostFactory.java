package com.xptool.sessions;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.motion.MotionProfile;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

final class InteractionSessionCommandRouterHostFactory {
    private InteractionSessionCommandRouterHostFactory() {
        // Static factory utility.
    }

    static InteractionSessionCommandRouter.Host createCommandRouterHostFromDelegates(
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeWoodcutChopNearestTree,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeMineNearestRock,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeFishNearestSpot,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeWalkToWorldPoint,
        Function<JsonObject, CommandExecutor.CommandDecision> executeCameraNudgeSafe,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeCombatAttackNearestNpc,
        Function<JsonObject, CommandExecutor.CommandDecision> executeNpcContextMenuTest,
        Function<JsonObject, CommandExecutor.CommandDecision> executeSceneObjectActionSafe,
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> executeAgilityObstacleAction,
        Function<JsonObject, CommandExecutor.CommandDecision> executeGroundItemActionSafe,
        Function<JsonObject, CommandExecutor.CommandDecision> executeShopBuyItemSafe,
        Function<JsonObject, CommandExecutor.CommandDecision> executeWorldHopSafe,
        Function<JsonObject, CommandExecutor.CommandDecision> executeEatFoodSafe,
        Supplier<CommandExecutor.CommandDecision> rejectUnsupportedCommandType
    ) {
        return new InteractionSessionCommandRouter.Host() {
            @Override
            public CommandExecutor.CommandDecision executeWoodcutChopNearestTree(
                JsonObject payload,
                MotionProfile motionProfile
            ) {
                return executeWoodcutChopNearestTree.apply(payload, motionProfile);
            }

            @Override
            public CommandExecutor.CommandDecision executeMineNearestRock(JsonObject payload, MotionProfile motionProfile) {
                return executeMineNearestRock.apply(payload, motionProfile);
            }

            @Override
            public CommandExecutor.CommandDecision executeFishNearestSpot(JsonObject payload, MotionProfile motionProfile) {
                return executeFishNearestSpot.apply(payload, motionProfile);
            }

            @Override
            public CommandExecutor.CommandDecision executeWalkToWorldPoint(
                JsonObject payload,
                MotionProfile motionProfile
            ) {
                return executeWalkToWorldPoint.apply(payload, motionProfile);
            }

            @Override
            public CommandExecutor.CommandDecision executeCameraNudgeSafe(JsonObject payload) {
                return executeCameraNudgeSafe.apply(payload);
            }

            @Override
            public CommandExecutor.CommandDecision executeCombatAttackNearestNpc(
                JsonObject payload,
                MotionProfile motionProfile
            ) {
                return executeCombatAttackNearestNpc.apply(payload, motionProfile);
            }

            @Override
            public CommandExecutor.CommandDecision executeNpcContextMenuTest(JsonObject payload) {
                return executeNpcContextMenuTest.apply(payload);
            }

            @Override
            public CommandExecutor.CommandDecision executeSceneObjectActionSafe(JsonObject payload) {
                return executeSceneObjectActionSafe.apply(payload);
            }

            @Override
            public CommandExecutor.CommandDecision executeAgilityObstacleAction(
                JsonObject payload,
                MotionProfile motionProfile
            ) {
                return executeAgilityObstacleAction.apply(payload, motionProfile);
            }

            @Override
            public CommandExecutor.CommandDecision executeGroundItemActionSafe(JsonObject payload) {
                return executeGroundItemActionSafe.apply(payload);
            }

            @Override
            public CommandExecutor.CommandDecision executeShopBuyItemSafe(JsonObject payload) {
                return executeShopBuyItemSafe.apply(payload);
            }

            @Override
            public CommandExecutor.CommandDecision executeWorldHopSafe(JsonObject payload) {
                return executeWorldHopSafe.apply(payload);
            }

            @Override
            public CommandExecutor.CommandDecision executeEatFoodSafe(JsonObject payload) {
                return executeEatFoodSafe.apply(payload);
            }

            @Override
            public CommandExecutor.CommandDecision rejectUnsupportedCommandType() {
                return rejectUnsupportedCommandType.get();
            }
        };
    }
}
