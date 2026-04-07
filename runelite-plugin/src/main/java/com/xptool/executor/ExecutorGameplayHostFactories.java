package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import java.awt.Point;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;

final class ExecutorGameplayHostFactories {
    private ExecutorGameplayHostFactories() {
    }

    static ExecutorGameplayRuntimeInputs.ServiceHosts createGameplayServiceHosts(
        Supplier<BankCommandService.Host> bankCommandHostSupplier,
        Supplier<com.xptool.systems.WoodcuttingTargetResolver.Host> woodcuttingTargetResolverHostSupplier,
        Function<com.xptool.systems.WoodcuttingTargetResolver, com.xptool.activities.woodcutting.WoodcuttingCommandService.Host> woodcuttingCommandHostFactory,
        Supplier<com.xptool.systems.MiningTargetResolver.Host> miningTargetResolverHostSupplier,
        Function<com.xptool.systems.MiningTargetResolver, com.xptool.activities.mining.MiningCommandService.Host> miningCommandHostFactory,
        Supplier<com.xptool.systems.FishingTargetResolver.Host> fishingTargetResolverHostSupplier,
        Supplier<com.xptool.systems.CombatTargetPolicy.Host> combatTargetPolicyHostSupplier,
        Function<com.xptool.systems.CombatTargetPolicy, com.xptool.systems.CombatTargetResolver.Host> combatTargetResolverHostFactory,
        Predicate<Point> isCombatCanvasPointUsable,
        double combatHullUpperFallbackYRatio,
        double combatHullTopFallbackYRatio,
        int combatSmallHullMaxWidthPx,
        int combatSmallHullMaxHeightPx,
        int combatHullCandidateSearchRadiusPx,
        Function<com.xptool.systems.FishingTargetResolver, com.xptool.activities.fishing.FishingCommandService.Host> fishingCommandHostFactory,
        Function<Client, WalkCommandService.Host> walkCommandHostFactory,
        BiFunction<CombatTargetPolicy, CombatTargetResolver, BrutusCombatSystem.Host> brutusCombatHostFactory,
        ExecutorServiceWiring.TriFunction<BrutusCombatSystem, CombatTargetPolicy, CombatTargetResolver, CombatCommandService.Host> combatCommandHostFactory,
        Function<BrutusCombatSystem, InteractionCommandService.Host> interactionCommandHostFactory,
        Function<Client, NpcContextMenuTestService.Host> npcContextMenuTestHostFactory,
        Function<Client, SceneObjectActionService.Host> sceneObjectActionHostFactory,
        Function<Client, GroundItemActionService.Host> groundItemActionHostFactory,
        Function<Client, ShopBuyCommandService.Host> shopBuyCommandHostFactory,
        Function<Client, WorldHopCommandService.Host> worldHopCommandHostFactory,
        Supplier<com.xptool.systems.SceneCacheScanner.Host> sceneCacheScannerHostSupplier,
        Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier
    ) {
        return new ExecutorGameplayRuntimeInputs.ServiceHosts(
            bankCommandHostSupplier,
            woodcuttingTargetResolverHostSupplier,
            woodcuttingCommandHostFactory,
            miningTargetResolverHostSupplier,
            miningCommandHostFactory,
            fishingTargetResolverHostSupplier,
            combatTargetPolicyHostSupplier,
            combatTargetResolverHostFactory,
            isCombatCanvasPointUsable,
            combatHullUpperFallbackYRatio,
            combatHullTopFallbackYRatio,
            combatSmallHullMaxWidthPx,
            combatSmallHullMaxHeightPx,
            combatHullCandidateSearchRadiusPx,
            fishingCommandHostFactory,
            walkCommandHostFactory,
            brutusCombatHostFactory,
            combatCommandHostFactory,
            interactionCommandHostFactory,
            npcContextMenuTestHostFactory,
            sceneObjectActionHostFactory,
            groundItemActionHostFactory,
            shopBuyCommandHostFactory,
            worldHopCommandHostFactory,
            sceneCacheScannerHostSupplier,
            unsupportedDecisionSupplier
        );
    }

    static Function<Client, WalkCommandService.Host> createWalkCommandHostFactory(
        Predicate<Point> isUsableCanvasPoint,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        ExecutorCombatDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        Function<ClickMotionSettings, MotorProfile> buildWalkMoveAndClickProfile,
        Runnable noteInteractionActivityNow,
        Runnable incrementClicksDispatched
    ) {
        return runtimeClient -> ExecutorGameplayRuntimeWiring.createWalkCommandHost(
            runtimeClient,
            isUsableCanvasPoint,
            acceptDecision,
            rejectDecision,
            details,
            safeString,
            resolveClickMotion,
            scheduleMotorGesture,
            buildWalkMoveAndClickProfile,
            noteInteractionActivityNow,
            incrementClicksDispatched
        );
    }

    static Function<BrutusCombatSystem, InteractionCommandService.Host> createInteractionCommandHostFactory(
        Client client,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        IntSupplier combatTargetUnavailableStreak,
        LongSupplier combatEatDispatchMinIntervalMs,
        LongConsumer setCombatLastEatDispatchAtMs,
        LongSupplier combatLastEatDispatchAtMs,
        IntSupplier currentExecutorTick,
        IntFunction<Optional<Integer>> findInventorySlot,
        BooleanSupplier canPerformMotorActionNow,
        IntPredicate clickInventorySlot,
        BooleanSupplier nudgeCameraYawLeft,
        BooleanSupplier nudgeCameraYawRight,
        BooleanSupplier nudgeCameraPitchUp,
        BooleanSupplier nudgeCameraPitchDown,
        Supplier<JsonObject> cameraLastNudgeDetails,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<Object[], JsonObject> details,
        ExecutorCombatDomainWiring.JsonIntReader asInt
    ) {
        return brutusSystem -> ExecutorGameplayRuntimeWiring.createInteractionCommandHost(
            client,
            brutusSystem,
            resolveClickMotion,
            combatTargetUnavailableStreak,
            combatEatDispatchMinIntervalMs,
            setCombatLastEatDispatchAtMs,
            combatLastEatDispatchAtMs,
            currentExecutorTick,
            findInventorySlot,
            canPerformMotorActionNow,
            clickInventorySlot,
            nudgeCameraYawLeft,
            nudgeCameraYawRight,
            nudgeCameraPitchUp,
            nudgeCameraPitchDown,
            cameraLastNudgeDetails,
            rejectDecision,
            acceptDecision,
            details,
            asInt
        );
    }

    static Function<Client, NpcContextMenuTestService.Host> createNpcContextMenuTestHostFactory(
        Supplier<Iterable<NPC>> currentNpcs,
        Function<NPC, Point> resolveVariedNpcClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearRandomEventTarget,
        BiPredicate<NPC, String[]> isTopMenuOptionOnNpc,
        Predicate<Point> clickNpcContextPrimaryAt,
        BiPredicate<Point, String[]> selectNpcContextMenuOptionAt,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision
    ) {
        return runtimeClient -> ExecutorGameplayRuntimeWiring.createNpcContextMenuTestHost(
            runtimeClient,
            currentNpcs,
            resolveVariedNpcClickPoint,
            isUsableCanvasPoint,
            moveInteractionCursorToCanvasPoint,
            isCursorNearRandomEventTarget,
            isTopMenuOptionOnNpc,
            clickNpcContextPrimaryAt,
            selectNpcContextMenuOptionAt,
            details,
            safeString,
            acceptDecision,
            rejectDecision
        );
    }

    static Function<Client, SceneObjectActionService.Host> createSceneObjectActionHostFactory(
        Supplier<Iterable<TileObject>> nearbySceneObjects,
        Function<TileObject, String> resolveSceneObjectName,
        Function<TileObject, Point> resolveSceneObjectClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearTarget,
        BiPredicate<TileObject, String[]> isTopMenuOptionOnObject,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        LongPredicate waitForMotorActionReady,
        LongSupplier interactionMotorReadyWaitMaxMs,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision
    ) {
        return runtimeClient -> ExecutorGameplayRuntimeWiring.createSceneObjectActionHost(
            runtimeClient,
            nearbySceneObjects,
            resolveSceneObjectName,
            resolveSceneObjectClickPoint,
            isUsableCanvasPoint,
            moveInteractionCursorToCanvasPoint,
            isCursorNearTarget,
            isTopMenuOptionOnObject,
            resolveClickMotion,
            waitForMotorActionReady,
            interactionMotorReadyWaitMaxMs,
            clickCanvasPoint,
            details,
            safeString,
            acceptDecision,
            rejectDecision
        );
    }

    static Function<Client, GroundItemActionService.Host> createGroundItemActionHostFactory(
        Supplier<Iterable<GroundItemRef>> nearbyGroundItems,
        IntFunction<String> resolveGroundItemName,
        Function<GroundItemRef, Point> resolveGroundItemClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearTarget,
        BiPredicate<GroundItemRef, String[]> isTopMenuOptionOnGroundItem,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        LongPredicate waitForMotorActionReady,
        LongSupplier interactionMotorReadyWaitMaxMs,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision
    ) {
        return runtimeClient -> ExecutorGameplayRuntimeWiring.createGroundItemActionHost(
            runtimeClient,
            nearbyGroundItems,
            resolveGroundItemName,
            resolveGroundItemClickPoint,
            isUsableCanvasPoint,
            moveInteractionCursorToCanvasPoint,
            isCursorNearTarget,
            isTopMenuOptionOnGroundItem,
            resolveClickMotion,
            waitForMotorActionReady,
            interactionMotorReadyWaitMaxMs,
            clickCanvasPoint,
            details,
            safeString,
            acceptDecision,
            rejectDecision
        );
    }

    static Function<Client, ShopBuyCommandService.Host> createShopBuyCommandHostFactory(
        Function<Object[], JsonObject> details,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<Widget, Optional<Point>> centerOfWidget,
        Predicate<Point> isUsableCanvasPoint,
        BiPredicate<Boolean, Boolean> focusClientWindowAndCanvas,
        LongPredicate waitForMotorActionReady,
        LongSupplier bankMotorReadyWaitMaxMs,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint
    ) {
        return runtimeClient -> ExecutorGameplayRuntimeWiring.createShopBuyCommandHost(
            runtimeClient,
            details,
            acceptDecision,
            rejectDecision,
            centerOfWidget,
            isUsableCanvasPoint,
            focusClientWindowAndCanvas,
            waitForMotorActionReady,
            bankMotorReadyWaitMaxMs,
            clickCanvasPoint
        );
    }

    static Function<Client, WorldHopCommandService.Host> createWorldHopCommandHostFactory(
        Function<Object[], JsonObject> details,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<String[], Optional<Widget>> findVisibleWidgetByKeywords,
        Function<Widget, Optional<Point>> centerOfWidget,
        Predicate<Point> isUsableCanvasPoint,
        BiPredicate<Boolean, Boolean> focusClientWindowAndCanvas,
        LongPredicate waitForMotorActionReady,
        LongSupplier randomEventMotorReadyWaitMaxMs,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint
    ) {
        return runtimeClient -> ExecutorGameplayRuntimeWiring.createWorldHopCommandHost(
            runtimeClient,
            details,
            acceptDecision,
            rejectDecision,
            findVisibleWidgetByKeywords,
            centerOfWidget,
            isUsableCanvasPoint,
            focusClientWindowAndCanvas,
            waitForMotorActionReady,
            randomEventMotorReadyWaitMaxMs,
            clickCanvasPoint
        );
    }
}
