package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.mining.MiningCommandService;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import com.xptool.systems.FishingTargetResolver;
import com.xptool.systems.MiningTargetResolver;
import com.xptool.systems.SceneCacheScanner;
import com.xptool.systems.WoodcuttingTargetResolver;
import java.awt.Point;
import java.awt.Robot;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

final class ExecutorGameplayServiceHostsAssembler {
    private ExecutorGameplayServiceHostsAssembler() {
    }

    static Supplier<BankCommandService.Host> createBankCommandHostSupplier(
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BooleanSupplier isBankOpen,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        Function<JsonObject, Optional<TileObject>> resolveOpenBankTarget,
        Function<TileObject, Point> resolveBankObjectClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        BiConsumer<TileObject, Point> rememberInteractionAnchorForTileObject,
        ExecutorBankDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        BiFunction<ClickMotionSettings, TileObject, MotorProfile> buildBankMoveAndClickProfile,
        Runnable incrementClicksDispatched,
        IntFunction<Optional<Integer>> findVisibleBankItemSlot,
        IntFunction<Optional<Integer>> findBankItemSlot,
        IntFunction<Widget> resolveBankItemSlotWidget,
        Supplier<Client> clientSupplier,
        ExecutorBankDomainWiring.SlotCenterResolver slotCenter,
        ExecutorBankDomainWiring.PrepareBankWidgetHover prepareBankWidgetHover,
        ExecutorBankDomainWiring.WidgetOpChooser chooseWidgetOpByKeywordPriority,
        BooleanSupplier tryConsumeWorkBudget,
        BooleanSupplier humanizedBankWidgetActionsEnabled,
        LongSupplier bankMotorReadyWaitMaxMs,
        LongPredicate waitForMotorActionReady,
        ExecutorBankDomainWiring.HumanizedWidgetAction tryHumanizedBankWidgetAction,
        Predicate<String> typeWithdrawQuantity,
        Function<Widget, String> summarizeWidgetActions,
        IntFunction<Optional<Integer>> findInventorySlot,
        IntFunction<Optional<Point>> resolveInventorySlotPoint,
        IntFunction<Widget> resolveInventorySlotWidget,
        Function<JsonElement, Set<Integer>> parseExcludeItemIds,
        Function<Set<Integer>, Optional<Integer>> findFirstInventoryItemNotIn,
        BiConsumer<JsonObject, JsonObject> copyMotionFields,
        Supplier<Robot> getOrCreateRobot,
        LongConsumer sleepQuietly,
        LongBinaryOperator randomBetween,
        LongSupplier bankSearchKeyMinDelayMs,
        LongSupplier bankSearchKeyMaxDelayMs,
        ExecutorBankDomainWiring.RobotCharTyper typeBankSearchChar,
        BooleanSupplier isBankPinPromptVisible,
        Function<Widget, Optional<Point>> centerOfWidget,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint,
        Supplier<ClickMotionSettings> genericInteractClickSettings
    ) {
        return ExecutorBankHostFactories.createBankCommandHostSupplier(
            acceptDecision,
            rejectDecision,
            details,
            safeString,
            isBankOpen,
            resolveClickMotion,
            resolveOpenBankTarget,
            resolveBankObjectClickPoint,
            isUsableCanvasPoint,
            rememberInteractionAnchorForTileObject,
            scheduleMotorGesture,
            buildBankMoveAndClickProfile,
            incrementClicksDispatched,
            findVisibleBankItemSlot,
            findBankItemSlot,
            resolveBankItemSlotWidget,
            clientSupplier,
            slotCenter,
            prepareBankWidgetHover,
            chooseWidgetOpByKeywordPriority,
            tryConsumeWorkBudget,
            humanizedBankWidgetActionsEnabled,
            bankMotorReadyWaitMaxMs,
            waitForMotorActionReady,
            tryHumanizedBankWidgetAction,
            typeWithdrawQuantity,
            summarizeWidgetActions,
            findInventorySlot,
            resolveInventorySlotPoint,
            resolveInventorySlotWidget,
            parseExcludeItemIds,
            findFirstInventoryItemNotIn,
            copyMotionFields,
            getOrCreateRobot,
            sleepQuietly,
            randomBetween,
            bankSearchKeyMinDelayMs,
            bankSearchKeyMaxDelayMs,
            typeBankSearchChar,
            isBankPinPromptVisible,
            centerOfWidget,
            clickCanvasPoint,
            genericInteractClickSettings
        );
    }

    static Supplier<WoodcuttingTargetResolver.Host> createWoodcuttingTargetResolverHostSupplier(
        Supplier<WorldPoint> localPlayerWorldPoint,
        Supplier<WorldPoint> lockedWoodcutWorldPoint,
        Supplier<WorldPoint> preferredSelectedWoodcutWorldPoint,
        IntSupplier selectedWoodcutTargetCount,
        Supplier<Iterable<TileObject>> cachedTreeObjects,
        Supplier<Iterable<TileObject>> cachedNormalTreeObjects,
        Supplier<Iterable<TileObject>> cachedOakTreeObjects,
        Supplier<Iterable<TileObject>> cachedWillowTreeObjects,
        Predicate<WorldPoint> hasSelectedTreeTargetNear,
        java.util.function.BiPredicate<WorldPoint, WorldPoint> worldPointsMatch,
        BiFunction<Iterable<TileObject>, WoodcuttingTargetResolver.WorldDistanceProvider, Optional<TileObject>>
            selectBestCursorAwareTarget
    ) {
        return ExecutorResolverHostFactories.createWoodcuttingTargetResolverHostSupplier(
            localPlayerWorldPoint,
            lockedWoodcutWorldPoint,
            preferredSelectedWoodcutWorldPoint,
            selectedWoodcutTargetCount,
            cachedTreeObjects,
            cachedNormalTreeObjects,
            cachedOakTreeObjects,
            cachedWillowTreeObjects,
            hasSelectedTreeTargetNear,
            worldPointsMatch,
            selectBestCursorAwareTarget
        );
    }

    static Supplier<MiningTargetResolver.Host> createMiningTargetResolverHostSupplier(
        IntSupplier selectedMiningTargetCount,
        Supplier<WorldPoint> localPlayerWorldPoint,
        Supplier<Iterable<TileObject>> cachedRockObjects,
        Predicate<TileObject> isRockObjectCandidate,
        Predicate<WorldPoint> isMiningRockSuppressed,
        Predicate<WorldPoint> hasSelectedRockTargetNear,
        BiFunction<Iterable<TileObject>, MiningTargetResolver.WorldDistanceProvider, Optional<TileObject>>
            selectBestCursorAwareTarget
    ) {
        return ExecutorResolverHostFactories.createMiningTargetResolverHostSupplier(
            selectedMiningTargetCount,
            localPlayerWorldPoint,
            cachedRockObjects,
            isRockObjectCandidate,
            isMiningRockSuppressed,
            hasSelectedRockTargetNear,
            selectBestCursorAwareTarget
        );
    }

    static Supplier<FishingTargetResolver.Host> createFishingTargetResolverHostSupplier(
        CommandExecutor executor,
        Client runtimeClient
    ) {
        return ExecutorResolverHostFactories.createFishingTargetResolverHostSupplier(executor, runtimeClient);
    }

    static Supplier<CombatTargetPolicy.Host> createCombatTargetPolicyHostSupplier(
        CommandExecutor executor,
        Client runtimeClient
    ) {
        return ExecutorResolverHostFactories.createCombatTargetPolicyHostSupplier(executor, runtimeClient);
    }

    static Function<CombatTargetPolicy, CombatTargetResolver.Host> createCombatTargetResolverHostFactory(
        CommandExecutor executor,
        Client runtimeClient
    ) {
        return ExecutorResolverHostFactories.createCombatTargetResolverHostFactory(executor, runtimeClient);
    }

    static Supplier<SceneCacheScanner.Host> createSceneCacheScannerHostSupplier(
        CommandExecutor executor,
        Client runtimeClient
    ) {
        return ExecutorResolverHostFactories.createSceneCacheScannerHostSupplier(executor, runtimeClient);
    }

    static ExecutorGameplayRuntimeInputs.ServiceHosts create(
        Client runtimeClient,
        Supplier<BankCommandService.Host> bankCommandHostSupplier,
        Supplier<WoodcuttingTargetResolver.Host> woodcuttingTargetResolverHostSupplier,
        Function<WoodcuttingTargetResolver, WoodcuttingCommandService.Host> woodcuttingCommandHostFactory,
        Supplier<MiningTargetResolver.Host> miningTargetResolverHostSupplier,
        Function<MiningTargetResolver, MiningCommandService.Host> miningCommandHostFactory,
        Supplier<FishingTargetResolver.Host> fishingTargetResolverHostSupplier,
        Supplier<CombatTargetPolicy.Host> combatTargetPolicyHostSupplier,
        Function<CombatTargetPolicy, CombatTargetResolver.Host> combatTargetResolverHostFactory,
        Function<FishingTargetResolver, FishingCommandService.Host> fishingCommandHostFactory,
        BiFunction<CombatTargetPolicy, CombatTargetResolver, BrutusCombatSystem.Host> brutusCombatHostFactory,
        ExecutorServiceWiring.TriFunction<BrutusCombatSystem, CombatTargetPolicy, CombatTargetResolver, CombatCommandService.Host> combatCommandHostFactory,
        Supplier<SceneCacheScanner.Host> sceneCacheScannerHostSupplier,
        Predicate<Point> isUsableCanvasPoint,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        ExecutorCombatDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        Function<ClickMotionSettings, MotorProfile> buildWalkMoveAndClickProfile,
        Runnable noteInteractionActivityNow,
        Runnable incrementClicksDispatched,
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
        ExecutorCombatDomainWiring.JsonIntReader asInt,
        Supplier<Iterable<NPC>> currentNpcs,
        Function<NPC, Point> resolveVariedNpcClickPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearRandomEventTarget,
        BiPredicate<NPC, String[]> isTopMenuOptionOnNpc,
        Predicate<Point> clickNpcContextPrimaryAt,
        BiPredicate<Point, String[]> selectNpcContextMenuOptionAt,
        Supplier<Iterable<TileObject>> currentNearbySceneObjects,
        Function<TileObject, String> resolveSceneObjectName,
        Function<TileObject, Point> resolveSceneObjectClickPoint,
        BiPredicate<TileObject, String[]> isTopMenuOptionOnObject,
        LongPredicate waitForMotorActionReady,
        LongSupplier randomEventMotorReadyWaitMaxMs,
        BiFunction<Point, ClickMotionSettings, Boolean> clickCanvasPoint,
        Supplier<Iterable<GroundItemRef>> currentNearbyGroundItems,
        IntFunction<String> resolveGroundItemName,
        Function<GroundItemRef, Point> resolveGroundItemClickPoint,
        BiPredicate<GroundItemRef, String[]> isTopMenuOptionOnGroundItem,
        Function<Widget, Optional<Point>> centerOfWidget,
        BiPredicate<Boolean, Boolean> focusClientWindowAndCanvas,
        LongSupplier bankMotorReadyWaitMaxMs,
        Function<String[], Optional<Widget>> findVisibleWidgetByKeywords,
        BiPredicate<Boolean, Boolean> worldHopFocusClientWindowAndCanvas,
        Predicate<Point> isCombatCanvasPointUsable,
        double combatHullUpperFallbackYRatio,
        double combatHullTopFallbackYRatio,
        int combatSmallHullMaxWidthPx,
        int combatSmallHullMaxHeightPx,
        int combatHullCandidateSearchRadiusPx,
        Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier
    ) {
        Function<Client, WalkCommandService.Host> walkCommandHostFactory =
            ExecutorGameplayHostFactories.createWalkCommandHostFactory(
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
        Function<BrutusCombatSystem, InteractionCommandService.Host> interactionCommandHostFactory =
            ExecutorGameplayHostFactories.createInteractionCommandHostFactory(
                runtimeClient,
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
        Function<Client, NpcContextMenuTestService.Host> npcContextMenuTestHostFactory =
            ExecutorGameplayHostFactories.createNpcContextMenuTestHostFactory(
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
        Function<Client, SceneObjectActionService.Host> sceneObjectActionHostFactory =
            ExecutorGameplayHostFactories.createSceneObjectActionHostFactory(
                currentNearbySceneObjects,
                resolveSceneObjectName,
                resolveSceneObjectClickPoint,
                isUsableCanvasPoint,
                moveInteractionCursorToCanvasPoint,
                isCursorNearRandomEventTarget,
                isTopMenuOptionOnObject,
                resolveClickMotion,
                waitForMotorActionReady,
                randomEventMotorReadyWaitMaxMs,
                clickCanvasPoint,
                details,
                safeString,
                acceptDecision,
                rejectDecision
            );
        Function<Client, GroundItemActionService.Host> groundItemActionHostFactory =
            ExecutorGameplayHostFactories.createGroundItemActionHostFactory(
                currentNearbyGroundItems,
                resolveGroundItemName,
                resolveGroundItemClickPoint,
                isUsableCanvasPoint,
                moveInteractionCursorToCanvasPoint,
                isCursorNearRandomEventTarget,
                isTopMenuOptionOnGroundItem,
                resolveClickMotion,
                waitForMotorActionReady,
                randomEventMotorReadyWaitMaxMs,
                clickCanvasPoint,
                details,
                safeString,
                acceptDecision,
                rejectDecision
            );
        Function<Client, ShopBuyCommandService.Host> shopBuyCommandHostFactory =
            ExecutorGameplayHostFactories.createShopBuyCommandHostFactory(
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
        Function<Client, WorldHopCommandService.Host> worldHopCommandHostFactory =
            ExecutorGameplayHostFactories.createWorldHopCommandHostFactory(
                details,
                acceptDecision,
                rejectDecision,
                findVisibleWidgetByKeywords,
                centerOfWidget,
                isUsableCanvasPoint,
                worldHopFocusClientWindowAndCanvas,
                waitForMotorActionReady,
                randomEventMotorReadyWaitMaxMs,
                clickCanvasPoint
            );
        return ExecutorGameplayHostFactories.createGameplayServiceHosts(
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
}
