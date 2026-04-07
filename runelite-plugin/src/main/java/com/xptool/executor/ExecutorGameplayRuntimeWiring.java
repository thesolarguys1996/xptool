package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.mining.MiningCommandService;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.motion.MotionProfile;
import com.xptool.sessions.SessionManager;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import com.xptool.systems.FishingTargetResolver;
import com.xptool.systems.MiningTargetResolver;
import com.xptool.systems.SceneCacheScanner;
import com.xptool.systems.WoodcuttingTargetResolver;
import java.awt.Point;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
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
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class ExecutorGameplayRuntimeWiring {
    private ExecutorGameplayRuntimeWiring() {
    }

    static ExecutorGameplayRuntimeBundle createBundle(
        Client client,
        CommandExecutor executor,
        ExecutorGameplayRuntimeInputs.ServiceHosts serviceHosts,
        ExecutorGameplayRuntimeInputs.RuntimeInputs runtimeInputs
    ) {
        return createBundle(
            client,
            executor,
            serviceHosts.bankCommandHostSupplier,
            serviceHosts.woodcuttingTargetResolverHostSupplier,
            serviceHosts.woodcuttingCommandHostFactory,
            serviceHosts.miningTargetResolverHostSupplier,
            serviceHosts.miningCommandHostFactory,
            serviceHosts.fishingTargetResolverHostSupplier,
            serviceHosts.combatTargetPolicyHostSupplier,
            serviceHosts.combatTargetResolverHostFactory,
            serviceHosts.isCombatCanvasPointUsable,
            serviceHosts.combatHullUpperFallbackYRatio,
            serviceHosts.combatHullTopFallbackYRatio,
            serviceHosts.combatSmallHullMaxWidthPx,
            serviceHosts.combatSmallHullMaxHeightPx,
            serviceHosts.combatHullCandidateSearchRadiusPx,
            serviceHosts.fishingCommandHostFactory,
            serviceHosts.walkCommandHostFactory,
            serviceHosts.brutusCombatHostFactory,
            serviceHosts.combatCommandHostFactory,
            serviceHosts.interactionCommandHostFactory,
            serviceHosts.npcContextMenuTestHostFactory,
            serviceHosts.sceneObjectActionHostFactory,
            serviceHosts.groundItemActionHostFactory,
            serviceHosts.shopBuyCommandHostFactory,
            serviceHosts.worldHopCommandHostFactory,
            serviceHosts.sceneCacheScannerHostSupplier,
            serviceHosts.unsupportedDecisionSupplier,
            runtimeInputs.currentExecutorTick,
            runtimeInputs.currentPlayerAnimation,
            runtimeInputs.isAnimationActive,
            runtimeInputs.isDropSweepSessionActive,
            runtimeInputs.dropSweepItemId,
            runtimeInputs.dropSweepNextSlot,
            runtimeInputs.dropSweepLastDispatchTick,
            runtimeInputs.dropSweepDispatchFailStreak,
            runtimeInputs.dropSweepAwaitingFirstCursorSync,
            runtimeInputs.setDropSweepNextSlot,
            runtimeInputs.setDropSweepLastDispatchTick,
            runtimeInputs.setDropSweepAwaitingFirstCursorSync,
            runtimeInputs.setDropSweepProgressCheckPending,
            runtimeInputs.beginDropSweepSession,
            runtimeInputs.endDropSweepSession,
            runtimeInputs.updateDropSweepProgressState,
            runtimeInputs.noteDropSweepDispatchFailure,
            runtimeInputs.noteDropSweepDispatchSuccess,
            runtimeInputs.findInventorySlotFrom,
            runtimeInputs.resolveInventorySlotPoint,
            runtimeInputs.resolveInventorySlotBasePoint,
            runtimeInputs.centerOfDropSweepRegionCanvas,
            runtimeInputs.isCursorNearDropTarget,
            runtimeInputs.scheduleDropMoveGesture,
            runtimeInputs.acquireOrRenewDropMotorOwner,
            runtimeInputs.isLoggedInAndBankClosed,
            runtimeInputs.dispatchInventoryDropAction,
            runtimeInputs.applyDropPerceptionDelay,
            runtimeInputs.incrementClicksDispatched,
            runtimeInputs.fatigueSnapshot,
            runtimeInputs.onDropCadenceProfileSelected,
            runtimeInputs.onIdleCadenceTuningSelected,
            runtimeInputs.details,
            runtimeInputs.emitDropDebug,
            runtimeInputs.acceptDecision,
            runtimeInputs.rejectDecision,
            runtimeInputs.hasActiveDropSweepSession,
            runtimeInputs.isIdleInterActionWindowOpen,
            runtimeInputs.resolveIdleSkillContext,
            runtimeInputs.isIdleActionWindowOpen,
            runtimeInputs.isIdleCameraWindowOpen,
            runtimeInputs.idleWindowGateSnapshot,
            runtimeInputs.isIdleAnimationActiveNow,
            runtimeInputs.isIdleInteractionDelaySatisfied,
            runtimeInputs.isIdleCameraInteractionDelaySatisfied,
            runtimeInputs.lastInteractionClickSerial,
            runtimeInputs.isCursorOutsideClientWindow,
            runtimeInputs.acquireOrRenewIdleMotorOwnership,
            runtimeInputs.canPerformIdleMotorActionNow,
            runtimeInputs.performIdleCameraMicroAdjust,
            runtimeInputs.resolveIdleHoverTargetCanvasPoint,
            runtimeInputs.performIdleCursorMove,
            runtimeInputs.resolveIdleDriftTargetCanvasPoint,
            runtimeInputs.resolveIdleOffscreenTargetScreenPoint,
            runtimeInputs.performIdleOffscreenCursorMove,
            runtimeInputs.resolveIdleParkingTargetCanvasPoint,
            runtimeInputs.resolveFishingIdleMode,
            runtimeInputs.resolveActivityIdlePolicy,
            runtimeInputs.activeIdleCadenceTuning,
            runtimeInputs.emitIdleEvent,
            runtimeInputs.isRandomEventRuntimeEnabled,
            runtimeInputs.isRandomEventRuntimeArmed,
            runtimeInputs.isLoggedIn,
            runtimeInputs.isBankOpen,
            runtimeInputs.hasActiveInteractionMotorProgram,
            runtimeInputs.acquireOrRenewInteractionMotorOwnership,
            runtimeInputs.releaseInteractionMotorOwnership,
            runtimeInputs.localPlayer,
            runtimeInputs.npcs,
            runtimeInputs.resolveVariedNpcClickPoint,
            runtimeInputs.isUsableCanvasPoint,
            runtimeInputs.moveInteractionCursorToCanvasPoint,
            runtimeInputs.isCursorNearRandomEventTarget,
            runtimeInputs.selectRandomEventDismissMenuOptionAt,
            runtimeInputs.randomBetween,
            runtimeInputs.randomEventPreAttemptCooldownMinMs,
            runtimeInputs.randomEventPreAttemptCooldownMaxMs,
            runtimeInputs.randomEventSuccessCooldownMinMs,
            runtimeInputs.randomEventSuccessCooldownMaxMs,
            runtimeInputs.randomEventFailureRetryCooldownMinMs,
            runtimeInputs.randomEventFailureRetryCooldownMaxMs,
            runtimeInputs.randomEventCursorReadyHoldMs,
            runtimeInputs.emitRandomEventEvent,
            runtimeInputs.isTopMenuBankOnObject,
            runtimeInputs.isTopMenuChopOnTree,
            runtimeInputs.isTopMenuMineOnRock,
            runtimeInputs.hasAttackEntryOnNpc,
            runtimeInputs.reserveMotorCooldown
        );
    }

    static ExecutorGameplayRuntimeBundle createBundle(
        Client client,
        CommandExecutor executor,
        Supplier<BankCommandService.Host> bankCommandHostSupplier,
        Supplier<WoodcuttingTargetResolver.Host> woodcuttingTargetResolverHostSupplier,
        Function<WoodcuttingTargetResolver, WoodcuttingCommandService.Host> woodcuttingCommandHostFactory,
        Supplier<MiningTargetResolver.Host> miningTargetResolverHostSupplier,
        Function<MiningTargetResolver, MiningCommandService.Host> miningCommandHostFactory,
        Supplier<FishingTargetResolver.Host> fishingTargetResolverHostSupplier,
        Supplier<CombatTargetPolicy.Host> combatTargetPolicyHostSupplier,
        Function<CombatTargetPolicy, CombatTargetResolver.Host> combatTargetResolverHostFactory,
        Predicate<Point> isCombatCanvasPointUsable,
        double combatHullUpperFallbackYRatio,
        double combatHullTopFallbackYRatio,
        int combatSmallHullMaxWidthPx,
        int combatSmallHullMaxHeightPx,
        int combatHullCandidateSearchRadiusPx,
        Function<FishingTargetResolver, FishingCommandService.Host> fishingCommandHostFactory,
        Function<Client, WalkCommandService.Host> walkCommandHostFactory,
        BiFunction<CombatTargetPolicy, CombatTargetResolver, BrutusCombatSystem.Host> brutusCombatHostFactory,
        ExecutorServiceWiring.TriFunction<BrutusCombatSystem, CombatTargetPolicy, CombatTargetResolver, CombatCommandService.Host> combatCommandHostFactory,
        Function<BrutusCombatSystem, InteractionCommandService.Host> interactionCommandHostFactory,
        Function<Client, NpcContextMenuTestService.Host> npcContextMenuTestHostFactory,
        Function<Client, SceneObjectActionService.Host> sceneObjectActionHostFactory,
        Function<Client, GroundItemActionService.Host> groundItemActionHostFactory,
        Function<Client, ShopBuyCommandService.Host> shopBuyCommandHostFactory,
        Function<Client, WorldHopCommandService.Host> worldHopCommandHostFactory,
        Supplier<SceneCacheScanner.Host> sceneCacheScannerHostSupplier,
        Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier,
        IntSupplier currentExecutorTick,
        IntSupplier currentPlayerAnimation,
        ExecutorRuntimeDomainWiring.IntPredicate isAnimationActive,
        BooleanSupplier isDropSweepSessionActive,
        IntSupplier dropSweepItemId,
        IntSupplier dropSweepNextSlot,
        IntSupplier dropSweepLastDispatchTick,
        IntSupplier dropSweepDispatchFailStreak,
        BooleanSupplier dropSweepAwaitingFirstCursorSync,
        IntConsumer setDropSweepNextSlot,
        IntConsumer setDropSweepLastDispatchTick,
        ExecutorRuntimeDomainWiring.IntBooleanSetter setDropSweepAwaitingFirstCursorSync,
        ExecutorRuntimeDomainWiring.IntBooleanSetter setDropSweepProgressCheckPending,
        ExecutorRuntimeDomainWiring.IntSetConsumer beginDropSweepSession,
        Runnable endDropSweepSession,
        ExecutorRuntimeDomainWiring.IntPredicate updateDropSweepProgressState,
        BooleanSupplier noteDropSweepDispatchFailure,
        Runnable noteDropSweepDispatchSuccess,
        ExecutorRuntimeDomainWiring.IntIntOptionalResolver findInventorySlotFrom,
        IntFunction<Optional<Point>> resolveInventorySlotPoint,
        IntFunction<Optional<Point>> resolveInventorySlotBasePoint,
        Supplier<Optional<Point>> centerOfDropSweepRegionCanvas,
        Predicate<Point> isCursorNearDropTarget,
        Function<Point, MotorHandle> scheduleDropMoveGesture,
        BooleanSupplier acquireOrRenewDropMotorOwner,
        BooleanSupplier isLoggedInAndBankClosed,
        ExecutorRuntimeDomainWiring.IntIntPointPredicate dispatchInventoryDropAction,
        Runnable applyDropPerceptionDelay,
        Runnable incrementClicksDispatched,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        Consumer<String> onDropCadenceProfileSelected,
        Consumer<IdleCadenceTuning> onIdleCadenceTuningSelected,
        Function<Object[], JsonObject> details,
        BiConsumer<String, JsonObject> emitDropDebug,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        BooleanSupplier hasActiveDropSweepSession,
        BooleanSupplier isIdleInterActionWindowOpen,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        BooleanSupplier isIdleActionWindowOpen,
        BooleanSupplier isIdleCameraWindowOpen,
        Supplier<JsonObject> idleWindowGateSnapshot,
        BooleanSupplier isIdleAnimationActiveNow,
        BooleanSupplier isIdleInteractionDelaySatisfied,
        BooleanSupplier isIdleCameraInteractionDelaySatisfied,
        Supplier<Long> lastInteractionClickSerial,
        BooleanSupplier isCursorOutsideClientWindow,
        BooleanSupplier acquireOrRenewIdleMotorOwnership,
        BooleanSupplier canPerformIdleMotorActionNow,
        BooleanSupplier performIdleCameraMicroAdjust,
        Supplier<Optional<Point>> resolveIdleHoverTargetCanvasPoint,
        Predicate<Point> performIdleCursorMove,
        Supplier<Optional<Point>> resolveIdleDriftTargetCanvasPoint,
        Supplier<Optional<Point>> resolveIdleOffscreenTargetScreenPoint,
        Predicate<Point> performIdleOffscreenCursorMove,
        Supplier<Optional<Point>> resolveIdleParkingTargetCanvasPoint,
        Function<IdleSkillContext, FishingIdleMode> resolveFishingIdleMode,
        Function<IdleSkillContext, ActivityIdlePolicy> resolveActivityIdlePolicy,
        Supplier<IdleCadenceTuning> activeIdleCadenceTuning,
        BiConsumer<String, JsonObject> emitIdleEvent,
        BooleanSupplier isRandomEventRuntimeEnabled,
        BooleanSupplier isRandomEventRuntimeArmed,
        BooleanSupplier isLoggedIn,
        BooleanSupplier isBankOpen,
        BooleanSupplier hasActiveInteractionMotorProgram,
        BooleanSupplier acquireOrRenewInteractionMotorOwnership,
        Runnable releaseInteractionMotorOwnership,
        Supplier<Player> localPlayer,
        Supplier<Iterable<NPC>> npcs,
        Function<NPC, Point> resolveVariedNpcClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearRandomEventTarget,
        Predicate<Point> selectRandomEventDismissMenuOptionAt,
        LongBinaryOperator randomBetween,
        LongSupplier randomEventPreAttemptCooldownMinMs,
        LongSupplier randomEventPreAttemptCooldownMaxMs,
        LongSupplier randomEventSuccessCooldownMinMs,
        LongSupplier randomEventSuccessCooldownMaxMs,
        LongSupplier randomEventFailureRetryCooldownMinMs,
        LongSupplier randomEventFailureRetryCooldownMaxMs,
        LongSupplier randomEventCursorReadyHoldMs,
        BiConsumer<String, JsonObject> emitRandomEventEvent,
        BooleanSupplier isTopMenuBankOnObject,
        Predicate<TileObject> isTopMenuChopOnTree,
        Predicate<TileObject> isTopMenuMineOnRock,
        BooleanSupplier hasAttackEntryOnNpc,
        java.util.function.LongConsumer reserveMotorCooldown
    ) {
        ExecutorServiceBundle serviceBundle = ExecutorServiceWiring.createBundle(
            client,
            executor,
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
            () -> walkCommandHostFactory.apply(client),
            brutusCombatHostFactory,
            combatCommandHostFactory,
            interactionCommandHostFactory,
            () -> npcContextMenuTestHostFactory.apply(client),
            () -> sceneObjectActionHostFactory.apply(client),
            () -> groundItemActionHostFactory.apply(client),
            () -> shopBuyCommandHostFactory.apply(client),
            () -> worldHopCommandHostFactory.apply(client),
            sceneCacheScannerHostSupplier,
            () -> createDropRuntimeHost(
                currentExecutorTick,
                currentPlayerAnimation,
                isAnimationActive,
                isDropSweepSessionActive,
                dropSweepItemId,
                dropSweepNextSlot,
                dropSweepLastDispatchTick,
                dropSweepDispatchFailStreak,
                dropSweepAwaitingFirstCursorSync,
                setDropSweepNextSlot,
                setDropSweepLastDispatchTick,
                setDropSweepAwaitingFirstCursorSync,
                setDropSweepProgressCheckPending,
                beginDropSweepSession,
                endDropSweepSession,
                updateDropSweepProgressState,
                noteDropSweepDispatchFailure,
                noteDropSweepDispatchSuccess,
                findInventorySlotFrom,
                resolveInventorySlotPoint,
                resolveInventorySlotBasePoint,
                centerOfDropSweepRegionCanvas,
                isCursorNearDropTarget,
                scheduleDropMoveGesture,
                acquireOrRenewDropMotorOwner,
                isLoggedInAndBankClosed,
                dispatchInventoryDropAction,
                applyDropPerceptionDelay,
                incrementClicksDispatched,
                fatigueSnapshot,
                onDropCadenceProfileSelected,
                onIdleCadenceTuningSelected,
                resolveIdleSkillContext,
                resolveFishingIdleMode,
                details,
                emitDropDebug,
                acceptDecision,
                rejectDecision
            ),
            (sessionManager, fishingCommandService, woodcuttingCommandService, dropRuntime) -> createIdleRuntimeHost(
                sessionManager,
                hasActiveDropSweepSession,
                isIdleInterActionWindowOpen,
                resolveIdleSkillContext,
                isIdleActionWindowOpen,
                isIdleCameraWindowOpen,
                idleWindowGateSnapshot,
                isIdleAnimationActiveNow,
                isIdleInteractionDelaySatisfied,
                isIdleCameraInteractionDelaySatisfied,
                lastInteractionClickSerial,
                isCursorOutsideClientWindow,
                acquireOrRenewIdleMotorOwnership,
                canPerformIdleMotorActionNow,
                performIdleCameraMicroAdjust,
                resolveIdleHoverTargetCanvasPoint,
                performIdleCursorMove,
                resolveIdleDriftTargetCanvasPoint,
                resolveIdleOffscreenTargetScreenPoint,
                performIdleOffscreenCursorMove,
                resolveIdleParkingTargetCanvasPoint,
                resolveFishingIdleMode,
                resolveActivityIdlePolicy,
                activeIdleCadenceTuning,
                fatigueSnapshot,
                () -> {
                    IdleSkillContext idleContext = resolveIdleSkillContext.get();
                    if (idleContext == IdleSkillContext.WOODCUTTING) {
                        return woodcuttingCommandService.isOffscreenIdleSuppressedNow();
                    }
                    return fishingCommandService.isOffscreenIdleSuppressedNow();
                },
                () -> {
                    IdleSkillContext idleContext = resolveIdleSkillContext.get();
                    if (idleContext == IdleSkillContext.WOODCUTTING) {
                        return woodcuttingCommandService.idleOffscreenSuppressionRemainingMs();
                    }
                    return fishingCommandService.idleOffscreenSuppressionRemainingMs();
                },
                dropRuntime::isFishingInventoryFullAfkActiveNow,
                dropRuntime::fishingInventoryFullAfkRemainingMs,
                emitIdleEvent
            ),
            unsupportedDecisionSupplier
        );

        RandomEventDismissRuntime randomEventDismissRuntime = new RandomEventDismissRuntime(
            createRandomEventDismissRuntimeHost(
                serviceBundle.sessionManager,
                isRandomEventRuntimeEnabled,
                isRandomEventRuntimeArmed,
                isLoggedIn,
                isBankOpen,
                hasActiveInteractionMotorProgram,
                acquireOrRenewInteractionMotorOwnership,
                releaseInteractionMotorOwnership,
                localPlayer,
                npcs,
                resolveVariedNpcClickPoint,
                isUsableCanvasPoint,
                moveInteractionCursorToCanvasPoint,
                isCursorNearRandomEventTarget,
                selectRandomEventDismissMenuOptionAt,
                randomBetween,
                randomEventPreAttemptCooldownMinMs,
                randomEventPreAttemptCooldownMaxMs,
                randomEventSuccessCooldownMinMs,
                randomEventSuccessCooldownMaxMs,
                randomEventFailureRetryCooldownMinMs,
                randomEventFailureRetryCooldownMaxMs,
                randomEventCursorReadyHoldMs,
                details,
                emitRandomEventEvent
            )
        );

        MotorProgramLifecycleEngine motorProgramLifecycleEngine =
            ExecutorEngineWiring.createMotorProgramLifecycleEngine(
                isTopMenuBankOnObject,
                isTopMenuChopOnTree,
                isTopMenuMineOnRock,
                hasAttackEntryOnNpc,
                reserveMotorCooldown
            );

        return new ExecutorGameplayRuntimeBundle(
            serviceBundle,
            randomEventDismissRuntime,
            motorProgramLifecycleEngine
        );
    }

    static WalkCommandService.Host createWalkCommandHost(
        Client client,
        Predicate<Point> isUsableCanvasPoint,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<com.google.gson.JsonObject, com.xptool.motion.MotionProfile, com.xptool.motion.MotionProfile.ClickMotionSettings> resolveClickMotion,
        ExecutorCombatDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        Function<com.xptool.motion.MotionProfile.ClickMotionSettings, MotorProfile> buildWalkMoveAndClickProfile,
        Runnable noteInteractionActivityNow,
        Runnable incrementClicksDispatched
    ) {
        BrutusNavigation walkNavigation = new BrutusNavigation(client, isUsableCanvasPoint);
        return ExecutorCombatDomainWiring.createWalkCommandHost(
            rejectDecision,
            acceptDecision,
            details,
            safeString,
            resolveClickMotion,
            client::getLocalPlayer,
            walkNavigation::resolveWorldTileClickPoint,
            walkNavigation::resolveWorldTileMinimapClickPoint,
            walkNavigation::resolveNearestWalkableWorldPoint,
            isUsableCanvasPoint,
            scheduleMotorGesture,
            buildWalkMoveAndClickProfile,
            noteInteractionActivityNow,
            incrementClicksDispatched
        );
    }

    static NpcContextMenuTestService.Host createNpcContextMenuTestHost(
        Client client,
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
        return ExecutorCombatDomainWiring.createNpcContextMenuTestHost(
            client::getLocalPlayer,
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

    static SceneObjectActionService.Host createSceneObjectActionHost(
        Client client,
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
        return new SceneObjectActionService.Host() {
            @Override
            public Player localPlayer() {
                return client.getLocalPlayer();
            }

            @Override
            public Iterable<TileObject> nearbySceneObjects() {
                return nearbySceneObjects.get();
            }

            @Override
            public String resolveSceneObjectName(TileObject targetObject) {
                return resolveSceneObjectName.apply(targetObject);
            }

            @Override
            public Point resolveSceneObjectClickPoint(TileObject targetObject) {
                return resolveSceneObjectClickPoint.apply(targetObject);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public boolean moveInteractionCursorToCanvasPoint(Point canvasPoint) {
                return moveInteractionCursorToCanvasPoint.test(canvasPoint);
            }

            @Override
            public boolean isCursorNearTarget(Point canvasPoint) {
                return isCursorNearTarget.test(canvasPoint);
            }

            @Override
            public boolean isTopMenuOptionOnObject(TileObject targetObject, String... optionKeywords) {
                return isTopMenuOptionOnObject.test(targetObject, optionKeywords);
            }

            @Override
            public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
                return resolveClickMotion.apply(payload, motionProfile);
            }

            @Override
            public boolean waitForMotorActionReady(long maxWaitMs) {
                return waitForMotorActionReady.test(maxWaitMs);
            }

            @Override
            public long interactionMotorReadyWaitMaxMs() {
                return interactionMotorReadyWaitMaxMs.getAsLong();
            }

            @Override
            public boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion) {
                return clickCanvasPoint.apply(canvasPoint, motion);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public String safeString(String value) {
                return safeString.apply(value);
            }

            @Override
            public CommandExecutor.CommandDecision accept(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public CommandExecutor.CommandDecision reject(String reason) {
                return rejectDecision.apply(reason);
            }
        };
    }

    static GroundItemActionService.Host createGroundItemActionHost(
        Client client,
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
        return new GroundItemActionService.Host() {
            @Override
            public Player localPlayer() {
                return client.getLocalPlayer();
            }

            @Override
            public Iterable<GroundItemRef> nearbyGroundItems() {
                return nearbyGroundItems.get();
            }

            @Override
            public String resolveGroundItemName(int itemId) {
                return resolveGroundItemName.apply(itemId);
            }

            @Override
            public Point resolveGroundItemClickPoint(GroundItemRef groundItem) {
                return resolveGroundItemClickPoint.apply(groundItem);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public boolean moveInteractionCursorToCanvasPoint(Point canvasPoint) {
                return moveInteractionCursorToCanvasPoint.test(canvasPoint);
            }

            @Override
            public boolean isCursorNearTarget(Point canvasPoint) {
                return isCursorNearTarget.test(canvasPoint);
            }

            @Override
            public boolean isTopMenuOptionOnGroundItem(GroundItemRef groundItem, String... optionKeywords) {
                return isTopMenuOptionOnGroundItem.test(groundItem, optionKeywords);
            }

            @Override
            public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
                return resolveClickMotion.apply(payload, motionProfile);
            }

            @Override
            public boolean waitForMotorActionReady(long maxWaitMs) {
                return waitForMotorActionReady.test(maxWaitMs);
            }

            @Override
            public long interactionMotorReadyWaitMaxMs() {
                return interactionMotorReadyWaitMaxMs.getAsLong();
            }

            @Override
            public boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion) {
                return clickCanvasPoint.apply(canvasPoint, motion);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public String safeString(String value) {
                return safeString.apply(value);
            }

            @Override
            public CommandExecutor.CommandDecision accept(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public CommandExecutor.CommandDecision reject(String reason) {
                return rejectDecision.apply(reason);
            }
        };
    }

    static CombatCommandService.Host createCombatCommandHost(
        Client client,
        BrutusCombatSystem brutusSystem,
        CombatTargetPolicy combatPolicy,
        CombatTargetResolver combatResolver,
        BooleanSupplier isDropSweepSessionActive,
        Runnable endDropSweepSession,
        Runnable extendCombatRetryWindow,
        ExecutorCombatDomainWiring.ResolveClickMotion resolveClickMotion,
        ExecutorCombatDomainWiring.ParsePreferredNpcIds parsePreferredNpcIds,
        IntFunction<String> resolvePreferredNpcNameHint,
        ExecutorCombatDomainWiring.CombatAnchorStaleChecker isCombatAnchorLikelyStale,
        ExecutorCombatDomainWiring.CombatBoundaryUpdater updateCombatBoundary,
        Runnable pruneCombatNpcSuppression,
        LongConsumer pruneBrutusDodgeTileSuppression,
        ExecutorCombatDomainWiring.UpdateBrutusDodgeProgressState updateBrutusDodgeProgressState,
        IntSupplier combatLastAttemptNpcIndex,
        LongSupplier combatOutcomeWaitUntilMs,
        LongPredicate isCombatPostOutcomeSettleGraceActive,
        ExecutorCombatDomainWiring.IntLongConsumer suppressCombatNpcTarget,
        LongSupplier combatTargetReclickCooldownMs,
        Runnable clearCombatTargetAttempt,
        ExecutorCombatDomainWiring.MaybeHandleBrutusDodge maybeHandleBrutusDodge,
        LongSupplier brutusLastDodgeAtMs,
        LongSupplier combatBrutusPostDodgeHoldMs,
        Runnable resetCombatTargetUnavailableStreak,
        Predicate<NPC> isAttackableNpc,
        Runnable clearCombatOutcomeWaitWindow,
        Function<NPC, Point> resolveNpcClickPoint,
        Predicate<Point> isCombatCanvasPointUsable,
        IntSupplier combatTargetClickFallbackAttempts,
        Runnable incrementCombatTargetUnavailableStreak,
        LongSupplier combatPostAttemptTargetSettleGraceMs,
        Runnable clearCombatInteractionWindows,
        IntSupplier combatSuppressedNpcCount,
        BiConsumer<NPC, Point> rememberInteractionAnchorForNpc,
        ExecutorCombatDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        Function<ClickMotionSettings, MotorProfile> buildCombatMoveAndClickProfile,
        Runnable noteInteractionActivityNow,
        Consumer<NPC> noteCombatTargetAttempt,
        Runnable beginCombatOutcomeWaitWindow,
        Runnable incrementClicksDispatched,
        LongSupplier combatContestedTargetSuppressionMs,
        LongBinaryOperator randomBetween,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision
    ) {
        return ExecutorCombatDomainWiring.createCombatCommandHost(
            isDropSweepSessionActive,
            endDropSweepSession,
            extendCombatRetryWindow,
            resolveClickMotion,
            parsePreferredNpcIds,
            resolvePreferredNpcNameHint,
            brutusSystem::encounterProfile,
            client::getLocalPlayer,
            isCombatAnchorLikelyStale,
            updateCombatBoundary,
            pruneCombatNpcSuppression,
            pruneBrutusDodgeTileSuppression,
            updateBrutusDodgeProgressState,
            combatLastAttemptNpcIndex,
            combatOutcomeWaitUntilMs,
            isCombatPostOutcomeSettleGraceActive,
            suppressCombatNpcTarget,
            combatTargetReclickCooldownMs,
            clearCombatTargetAttempt,
            maybeHandleBrutusDodge,
            brutusLastDodgeAtMs,
            combatBrutusPostDodgeHoldMs,
            resetCombatTargetUnavailableStreak,
            brutusSystem::isBrutusNpc,
            isAttackableNpc,
            combatPolicy::npcMatchesPreferredTarget,
            combatPolicy::isNpcWithinCombatArea,
            combatPolicy::isNpcWithinCombatChaseDistance,
            clearCombatOutcomeWaitWindow,
            combatResolver::resolveNpcTargetingLocal,
            combatResolver::resolveNearestCombatTarget,
            resolveNpcClickPoint,
            isCombatCanvasPointUsable,
            combatTargetClickFallbackAttempts,
            incrementCombatTargetUnavailableStreak,
            combatPostAttemptTargetSettleGraceMs,
            clearCombatInteractionWindows,
            combatSuppressedNpcCount,
            rememberInteractionAnchorForNpc,
            scheduleMotorGesture,
            buildCombatMoveAndClickProfile,
            noteInteractionActivityNow,
            noteCombatTargetAttempt,
            beginCombatOutcomeWaitWindow,
            incrementClicksDispatched,
            combatContestedTargetSuppressionMs,
            randomBetween,
            fatigueSnapshot,
            details,
            safeString,
            acceptDecision,
            rejectDecision
        );
    }

    static InteractionCommandService.Host createInteractionCommandHost(
        Client client,
        BrutusCombatSystem brutusSystem,
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
        return ExecutorCombatDomainWiring.createInteractionCommandHost(
            rejectDecision,
            acceptDecision,
            details,
            client::getLocalPlayer,
            (payload, now) -> {
                Player local = client.getLocalPlayer();
                if (local == null) {
                    return Optional.empty();
                }
                ClickMotionSettings motion = resolveClickMotion.apply(payload, MotionProfile.COMBAT);
                return brutusSystem.maybeHandleDodgeFromEat(local, motion, now);
            },
            () -> 829,
            combatLastEatDispatchAtMs,
            setCombatLastEatDispatchAtMs,
            combatEatDispatchMinIntervalMs,
            brutusSystem::detectNearbyTelegraphName,
            currentExecutorTick,
            brutusSystem::lastTelegraphTick,
            brutusSystem::eatPriorityWindowTicks,
            brutusSystem::lastDodgeAtMs,
            brutusSystem::postDodgeHoldMs,
            brutusSystem::lastNoSafeTileTick,
            brutusSystem::isNoSafeTilePressureActive,
            brutusSystem::isDodgeProgressActive,
            combatTargetUnavailableStreak,
            () -> 2,
            brutusSystem::isBrutusNpcNearby,
            brutusSystem::nearbyScanRangeTiles,
            brutusSystem::noSafeTileStreak,
            brutusSystem::noSafeTileRecoveryWindowTicks,
            asInt,
            findInventorySlot,
            canPerformMotorActionNow,
            clickInventorySlot,
            nudgeCameraYawLeft,
            nudgeCameraYawRight,
            nudgeCameraPitchUp,
            nudgeCameraPitchDown,
            cameraLastNudgeDetails
        );
    }

    static ShopBuyCommandService.Host createShopBuyCommandHost(
        Client client,
        Function<Object[], JsonObject> details,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        Function<Widget, Optional<Point>> centerOfWidget,
        Predicate<Point> isUsableCanvasPoint,
        BiPredicate<Boolean, Boolean> focusClientWindowAndCanvas,
        LongPredicate waitForMotorActionReady,
        LongSupplier bankMotorReadyWaitMaxMs,
        BiFunction<Point, ClickMotionSettings, Boolean> tryPrimaryClick
    ) {
        return new ShopBuyCommandService.Host() {
            @Override
            public CommandExecutor.CommandDecision accept(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public CommandExecutor.CommandDecision reject(String reason) {
                return rejectDecision.apply(reason);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public boolean isShopOpen() {
                Widget shop = client.getWidget(InterfaceID.Shopmain.UNIVERSE);
                if (shop != null && !shop.isHidden()) {
                    return true;
                }
                Widget items = client.getWidget(InterfaceID.Shopmain.ITEMS);
                return items != null && !items.isHidden();
            }

            @Override
            public Widget widgetByPackedId(int packedWidgetId) {
                return client.getWidget(packedWidgetId);
            }

            @Override
            public Optional<Point> centerOfWidget(Widget widget) {
                return centerOfWidget.apply(widget);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas) {
                return focusClientWindowAndCanvas.test(focusWindow, focusCanvas);
            }

            @Override
            public boolean waitForMotorActionReady(long timeoutMs) {
                return waitForMotorActionReady.test(timeoutMs);
            }

            @Override
            public long menuActionReadyWaitMaxMs() {
                return bankMotorReadyWaitMaxMs.getAsLong();
            }

            @Override
            public boolean tryPrimaryClick(Point targetPoint, ClickMotionSettings motion) {
                return tryPrimaryClick.apply(targetPoint, motion);
            }
        };
    }

    static WorldHopCommandService.Host createWorldHopCommandHost(
        Client client,
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
        return new WorldHopCommandService.Host() {
            @Override
            public CommandExecutor.CommandDecision accept(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public CommandExecutor.CommandDecision reject(String reason) {
                return rejectDecision.apply(reason);
            }

            @Override
            public JsonObject details(Object... kvPairs) {
                return details.apply(kvPairs);
            }

            @Override
            public boolean isLoggedIn() {
                return client.getGameState() == net.runelite.api.GameState.LOGGED_IN;
            }

            @Override
            public int currentWorld() {
                return client.getWorld();
            }

            @Override
            public Widget widgetByPackedId(int packedWidgetId) {
                return client.getWidget(packedWidgetId);
            }

            @Override
            public Optional<Widget> findVisibleWidgetByKeywords(String... keywords) {
                return findVisibleWidgetByKeywords.apply(keywords);
            }

            @Override
            public Optional<Point> centerOfWidget(Widget widget) {
                return centerOfWidget.apply(widget);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public boolean focusClientWindowAndCanvas(boolean focusWindow, boolean focusCanvas) {
                return focusClientWindowAndCanvas.test(focusWindow, focusCanvas);
            }

            @Override
            public boolean waitForMotorActionReady(long timeoutMs) {
                return waitForMotorActionReady.test(timeoutMs);
            }

            @Override
            public long menuActionReadyWaitMaxMs() {
                return randomEventMotorReadyWaitMaxMs.getAsLong();
            }

            @Override
            public boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion) {
                return clickCanvasPoint.apply(canvasPoint, motion);
            }
        };
    }

    private static DropRuntime.Host createDropRuntimeHost(
        IntSupplier currentExecutorTick,
        IntSupplier currentPlayerAnimation,
        ExecutorRuntimeDomainWiring.IntPredicate isAnimationActive,
        BooleanSupplier isDropSweepSessionActive,
        IntSupplier dropSweepItemId,
        IntSupplier dropSweepNextSlot,
        IntSupplier dropSweepLastDispatchTick,
        IntSupplier dropSweepDispatchFailStreak,
        BooleanSupplier dropSweepAwaitingFirstCursorSync,
        IntConsumer setDropSweepNextSlot,
        IntConsumer setDropSweepLastDispatchTick,
        ExecutorRuntimeDomainWiring.IntBooleanSetter setDropSweepAwaitingFirstCursorSync,
        ExecutorRuntimeDomainWiring.IntBooleanSetter setDropSweepProgressCheckPending,
        ExecutorRuntimeDomainWiring.IntSetConsumer beginDropSweepSession,
        Runnable endDropSweepSession,
        ExecutorRuntimeDomainWiring.IntPredicate updateDropSweepProgressState,
        BooleanSupplier noteDropSweepDispatchFailure,
        Runnable noteDropSweepDispatchSuccess,
        ExecutorRuntimeDomainWiring.IntIntOptionalResolver findInventorySlotFrom,
        IntFunction<Optional<Point>> resolveInventorySlotPoint,
        IntFunction<Optional<Point>> resolveInventorySlotBasePoint,
        Supplier<Optional<Point>> centerOfDropSweepRegionCanvas,
        Predicate<Point> isCursorNearDropTarget,
        Function<Point, MotorHandle> scheduleDropMoveGesture,
        BooleanSupplier acquireOrRenewDropMotorOwner,
        BooleanSupplier isLoggedInAndBankClosed,
        ExecutorRuntimeDomainWiring.IntIntPointPredicate dispatchInventoryDropAction,
        Runnable applyDropPerceptionDelay,
        Runnable incrementClicksDispatched,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        Consumer<String> onDropCadenceProfileSelected,
        Consumer<IdleCadenceTuning> onIdleCadenceTuningSelected,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        Function<IdleSkillContext, FishingIdleMode> resolveFishingIdleMode,
        Function<Object[], JsonObject> details,
        BiConsumer<String, JsonObject> emitDropDebug,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision
    ) {
        return ExecutorRuntimeDomainWiring.createDropRuntimeHost(
            currentExecutorTick,
            currentPlayerAnimation,
            isAnimationActive,
            isDropSweepSessionActive,
            dropSweepItemId,
            dropSweepNextSlot,
            dropSweepLastDispatchTick,
            dropSweepDispatchFailStreak,
            dropSweepAwaitingFirstCursorSync,
            setDropSweepNextSlot,
            setDropSweepLastDispatchTick,
            setDropSweepAwaitingFirstCursorSync,
            setDropSweepProgressCheckPending,
            beginDropSweepSession,
            endDropSweepSession,
            updateDropSweepProgressState,
            noteDropSweepDispatchFailure,
            noteDropSweepDispatchSuccess,
            findInventorySlotFrom,
            resolveInventorySlotPoint,
            resolveInventorySlotBasePoint,
            centerOfDropSweepRegionCanvas,
            isCursorNearDropTarget,
            scheduleDropMoveGesture,
            acquireOrRenewDropMotorOwner,
            isLoggedInAndBankClosed,
            dispatchInventoryDropAction,
            applyDropPerceptionDelay,
            incrementClicksDispatched,
            fatigueSnapshot,
            onDropCadenceProfileSelected,
            onIdleCadenceTuningSelected,
            resolveIdleSkillContext,
            resolveFishingIdleMode,
            details,
            emitDropDebug,
            acceptDecision,
            rejectDecision
        );
    }

    private static IdleRuntime.Host createIdleRuntimeHost(
        SessionManager sessionManager,
        BooleanSupplier hasActiveDropSweepSession,
        BooleanSupplier isIdleInterActionWindowOpen,
        Supplier<IdleSkillContext> resolveIdleSkillContext,
        BooleanSupplier isIdleActionWindowOpen,
        BooleanSupplier isIdleCameraWindowOpen,
        Supplier<JsonObject> idleWindowGateSnapshot,
        BooleanSupplier isIdleAnimationActiveNow,
        BooleanSupplier isIdleInteractionDelaySatisfied,
        BooleanSupplier isIdleCameraInteractionDelaySatisfied,
        Supplier<Long> lastInteractionClickSerial,
        BooleanSupplier isCursorOutsideClientWindow,
        BooleanSupplier acquireOrRenewIdleMotorOwnership,
        BooleanSupplier canPerformIdleMotorActionNow,
        BooleanSupplier performIdleCameraMicroAdjust,
        Supplier<Optional<Point>> resolveIdleHoverTargetCanvasPoint,
        Predicate<Point> performIdleCursorMove,
        Supplier<Optional<Point>> resolveIdleDriftTargetCanvasPoint,
        Supplier<Optional<Point>> resolveIdleOffscreenTargetScreenPoint,
        Predicate<Point> performIdleOffscreenCursorMove,
        Supplier<Optional<Point>> resolveIdleParkingTargetCanvasPoint,
        Function<IdleSkillContext, FishingIdleMode> resolveFishingIdleMode,
        Function<IdleSkillContext, ActivityIdlePolicy> resolveActivityIdlePolicy,
        Supplier<IdleCadenceTuning> activeIdleCadenceTuning,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        BooleanSupplier isFishingOffscreenIdleSuppressed,
        LongSupplier fishingOffscreenIdleSuppressionRemainingMs,
        BooleanSupplier isFishingInventoryFullAfkActive,
        LongSupplier fishingInventoryFullAfkRemainingMs,
        BiConsumer<String, JsonObject> emitIdleEvent
    ) {
        return ExecutorRuntimeDomainWiring.createIdleRuntimeHost(
            sessionManager::hasActiveSession,
            sessionManager::hasActiveSessionOtherThan,
            sessionManager::getActiveSession,
            hasActiveDropSweepSession,
            () -> sessionManager.releaseMotor(ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE),
            isIdleInterActionWindowOpen,
            resolveIdleSkillContext,
            isIdleActionWindowOpen,
            isIdleCameraWindowOpen,
            idleWindowGateSnapshot,
            isIdleAnimationActiveNow,
            isIdleInteractionDelaySatisfied,
            isIdleCameraInteractionDelaySatisfied,
            lastInteractionClickSerial,
            isCursorOutsideClientWindow,
            acquireOrRenewIdleMotorOwnership,
            canPerformIdleMotorActionNow,
            performIdleCameraMicroAdjust,
            resolveIdleHoverTargetCanvasPoint,
            performIdleCursorMove,
            resolveIdleDriftTargetCanvasPoint,
            resolveIdleOffscreenTargetScreenPoint,
            performIdleOffscreenCursorMove,
            resolveIdleParkingTargetCanvasPoint,
            resolveFishingIdleMode,
            resolveActivityIdlePolicy,
            activeIdleCadenceTuning,
            fatigueSnapshot,
            isFishingOffscreenIdleSuppressed,
            fishingOffscreenIdleSuppressionRemainingMs,
            isFishingInventoryFullAfkActive,
            fishingInventoryFullAfkRemainingMs,
            emitIdleEvent
        );
    }

    private static RandomEventDismissRuntime.Host createRandomEventDismissRuntimeHost(
        SessionManager sessionManager,
        BooleanSupplier isRuntimeEnabled,
        BooleanSupplier isRuntimeArmed,
        BooleanSupplier isLoggedIn,
        BooleanSupplier isBankOpen,
        BooleanSupplier hasActiveInteractionMotorProgram,
        BooleanSupplier acquireOrRenewInteractionMotorOwnership,
        Runnable releaseInteractionMotorOwnership,
        Supplier<Player> localPlayer,
        Supplier<Iterable<NPC>> npcs,
        Function<NPC, Point> resolveVariedNpcClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearTarget,
        Predicate<Point> selectDismissMenuOptionAt,
        LongBinaryOperator randomBetween,
        LongSupplier randomEventPreAttemptCooldownMinMs,
        LongSupplier randomEventPreAttemptCooldownMaxMs,
        LongSupplier randomEventSuccessCooldownMinMs,
        LongSupplier randomEventSuccessCooldownMaxMs,
        LongSupplier randomEventFailureRetryCooldownMinMs,
        LongSupplier randomEventFailureRetryCooldownMaxMs,
        LongSupplier randomEventCursorReadyHoldMs,
        Function<Object[], JsonObject> details,
        BiConsumer<String, JsonObject> emitRandomEventEvent
    ) {
        return ExecutorRuntimeDomainWiring.createRandomEventDismissRuntimeHost(
            isRuntimeEnabled,
            isRuntimeArmed,
            isLoggedIn,
            isBankOpen,
            sessionManager::hasActiveSessionOtherThan,
            hasActiveInteractionMotorProgram,
            acquireOrRenewInteractionMotorOwnership,
            releaseInteractionMotorOwnership,
            localPlayer,
            npcs,
            resolveVariedNpcClickPoint,
            isUsableCanvasPoint,
            moveInteractionCursorToCanvasPoint,
            isCursorNearTarget,
            selectDismissMenuOptionAt,
            randomBetween,
            randomEventPreAttemptCooldownMinMs,
            randomEventPreAttemptCooldownMaxMs,
            randomEventSuccessCooldownMinMs,
            randomEventSuccessCooldownMaxMs,
            randomEventFailureRetryCooldownMinMs,
            randomEventFailureRetryCooldownMaxMs,
            randomEventCursorReadyHoldMs,
            details,
            emitRandomEventEvent
        );
    }
}
