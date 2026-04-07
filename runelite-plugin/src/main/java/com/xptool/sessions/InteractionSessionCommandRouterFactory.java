package com.xptool.sessions;

import com.google.gson.JsonObject;
import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;
import com.xptool.motion.MotionProfile;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

final class InteractionSessionCommandRouterFactory {
    private InteractionSessionCommandRouterFactory() {
        // Static factory utility.
    }

    static InteractionSessionCommandRouter createCommandRouterService(SessionCommandFacade commandFacade) {
        BiFunction<JsonObject, MotionProfile, CommandExecutor.CommandDecision> woodcutDelegate =
            (payload, motionProfile) -> commandFacade.executeWoodcutChopNearestTree(payload, motionProfile);
        return createCommandRouterServiceFromHost(
            createCommandRouterHostFromDelegates(
                woodcutDelegate,
                commandFacade::executeMineNearestRock,
                commandFacade::executeFishNearestSpot,
                commandFacade::executeWalkToWorldPoint,
                commandFacade::executeCameraNudgeSafe,
                commandFacade::executeCombatAttackNearestNpc,
                commandFacade::executeNpcContextMenuTest,
                commandFacade::executeSceneObjectActionSafe,
                commandFacade::executeAgilityObstacleAction,
                commandFacade::executeGroundItemActionSafe,
                commandFacade::executeShopBuyItemSafe,
                commandFacade::executeWorldHopSafe,
                commandFacade::executeEatFoodSafe,
                commandFacade::rejectUnsupportedCommandType
            )
        );
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
        return InteractionSessionCommandRouterHostFactory.createCommandRouterHostFromDelegates(
            executeWoodcutChopNearestTree,
            executeMineNearestRock,
            executeFishNearestSpot,
            executeWalkToWorldPoint,
            executeCameraNudgeSafe,
            executeCombatAttackNearestNpc,
            executeNpcContextMenuTest,
            executeSceneObjectActionSafe,
            executeAgilityObstacleAction,
            executeGroundItemActionSafe,
            executeShopBuyItemSafe,
            executeWorldHopSafe,
            executeEatFoodSafe,
            rejectUnsupportedCommandType
        );
    }

    static InteractionSessionCommandRouter createCommandRouterServiceFromHost(InteractionSessionCommandRouter.Host host) {
        return new InteractionSessionCommandRouter(host);
    }
}
