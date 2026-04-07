package com.xptool.executor;

import com.xptool.activities.fishing.FishingActivityModule;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.mining.MiningCommandService;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.core.runtime.ActivityRegistry;
import com.xptool.sessions.BankSession;
import com.xptool.sessions.DropSession;
import com.xptool.sessions.InteractionSession;
import com.xptool.sessions.InteractionSessionFactory;
import com.xptool.sessions.SessionManager;
import com.xptool.motion.MotionProfile;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import com.xptool.systems.FishingTargetResolver;
import com.xptool.systems.AgilityTargetResolver;
import com.xptool.systems.MiningTargetResolver;
import com.xptool.systems.SceneCacheScanner;
import com.xptool.systems.WoodcuttingTargetResolver;
import java.awt.Point;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.Client;

final class ExecutorServiceWiring {
    private ExecutorServiceWiring() {
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A first, B second, C third);
    }

    @FunctionalInterface
    interface QuadFunction<A, B, C, D, R> {
        R apply(A first, B second, C third, D fourth);
    }

    static ExecutorServiceBundle createBundle(
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
        Supplier<WalkCommandService.Host> walkCommandHostSupplier,
        BiFunction<CombatTargetPolicy, CombatTargetResolver, BrutusCombatSystem.Host> brutusCombatHostFactory,
        TriFunction<BrutusCombatSystem, CombatTargetPolicy, CombatTargetResolver, CombatCommandService.Host> combatCommandHostFactory,
        Function<BrutusCombatSystem, InteractionCommandService.Host> interactionCommandHostFactory,
        Supplier<NpcContextMenuTestService.Host> npcContextMenuTestHostSupplier,
        Supplier<SceneObjectActionService.Host> sceneObjectActionHostSupplier,
        Supplier<GroundItemActionService.Host> groundItemActionHostSupplier,
        Supplier<ShopBuyCommandService.Host> shopBuyCommandHostSupplier,
        Supplier<WorldHopCommandService.Host> worldHopCommandHostSupplier,
        Supplier<SceneCacheScanner.Host> sceneCacheScannerHostSupplier,
        Supplier<DropRuntime.Host> dropRuntimeHostSupplier,
        QuadFunction<SessionManager, FishingCommandService, WoodcuttingCommandService, DropRuntime, IdleRuntime.Host> idleRuntimeHostFactory,
        Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier
    ) {
        BankCommandService bankCommandService = new BankCommandService(bankCommandHostSupplier.get());

        WoodcuttingTargetResolver woodcuttingTargetResolver =
            new WoodcuttingTargetResolver(woodcuttingTargetResolverHostSupplier.get());
        WoodcuttingCommandService woodcuttingCommandService =
            new WoodcuttingCommandService(woodcuttingCommandHostFactory.apply(woodcuttingTargetResolver));

        MiningTargetResolver miningTargetResolver = new MiningTargetResolver(miningTargetResolverHostSupplier.get());
        MiningCommandService miningCommandService =
            new MiningCommandService(miningCommandHostFactory.apply(miningTargetResolver));

        FishingActivityModule fishingActivityModule = new FishingActivityModule(
            fishingTargetResolverHostSupplier.get(),
            fishingCommandHostFactory
        );
        ActivityRegistry activityRegistry = new ActivityRegistry();
        activityRegistry.register(fishingActivityModule);

        CombatTargetPolicy combatTargetPolicy = new CombatTargetPolicy(combatTargetPolicyHostSupplier.get());
        CombatTargetResolver combatTargetResolver =
            new CombatTargetResolver(combatTargetResolverHostFactory.apply(combatTargetPolicy));

        NpcClickPointResolver npcClickPointResolver = new NpcClickPointResolver(
            isCombatCanvasPointUsable,
            combatHullUpperFallbackYRatio,
            combatHullTopFallbackYRatio,
            combatSmallHullMaxWidthPx,
            combatSmallHullMaxHeightPx,
            combatHullCandidateSearchRadiusPx
        );

        FishingCommandService fishingCommandService = fishingActivityModule.createService();
        WalkCommandService walkCommandService = new WalkCommandService(walkCommandHostSupplier.get());

        BrutusCombatSystem brutusCombatSystem =
            new BrutusCombatSystem(client, brutusCombatHostFactory.apply(combatTargetPolicy, combatTargetResolver));

        CombatCommandService combatCommandService =
            new CombatCommandService(combatCommandHostFactory.apply(
                brutusCombatSystem,
                combatTargetPolicy,
                combatTargetResolver
            ));

        InteractionCommandService interactionCommandService =
            new InteractionCommandService(interactionCommandHostFactory.apply(brutusCombatSystem));
        NpcContextMenuTestService npcContextMenuTestService =
            new NpcContextMenuTestService(npcContextMenuTestHostSupplier.get());
        SceneObjectActionService.Host sceneObjectActionHost = sceneObjectActionHostSupplier.get();
        SceneObjectActionService sceneObjectActionService =
            new SceneObjectActionService(sceneObjectActionHost);
        AgilityTargetResolver agilityTargetResolver = new AgilityTargetResolver(
            new AgilityTargetResolver.Host() {
                @Override
                public Iterable<net.runelite.api.TileObject> nearbySceneObjects() {
                    return sceneObjectActionHost.nearbySceneObjects();
                }

                @Override
                public String resolveSceneObjectName(net.runelite.api.TileObject targetObject) {
                    return sceneObjectActionHost.resolveSceneObjectName(targetObject);
                }

                @Override
                public String safeString(String value) {
                    return sceneObjectActionHost.safeString(value);
                }
            }
        );
        AgilityCommandService agilityCommandService =
            new AgilityCommandService(new AgilityCommandService.Host() {
                @Override
                public net.runelite.api.Player localPlayer() {
                    return sceneObjectActionHost.localPlayer();
                }

                @Override
                public Point resolveSceneObjectClickPoint(net.runelite.api.TileObject targetObject) {
                    return sceneObjectActionHost.resolveSceneObjectClickPoint(targetObject);
                }

                @Override
                public boolean isUsableCanvasPoint(Point point) {
                    return sceneObjectActionHost.isUsableCanvasPoint(point);
                }

                @Override
                public boolean moveInteractionCursorToCanvasPoint(Point canvasPoint) {
                    return sceneObjectActionHost.moveInteractionCursorToCanvasPoint(canvasPoint);
                }

                @Override
                public boolean isCursorNearTarget(Point canvasPoint) {
                    return sceneObjectActionHost.isCursorNearTarget(canvasPoint);
                }

                @Override
                public boolean isTopMenuOptionOnObject(net.runelite.api.TileObject targetObject, String... optionKeywords) {
                    return sceneObjectActionHost.isTopMenuOptionOnObject(targetObject, optionKeywords);
                }

                @Override
                public MotionProfile.ClickMotionSettings resolveClickMotion(
                    com.google.gson.JsonObject payload,
                    MotionProfile motionProfile
                ) {
                    return sceneObjectActionHost.resolveClickMotion(payload, motionProfile);
                }

                @Override
                public boolean waitForMotorActionReady(long maxWaitMs) {
                    return sceneObjectActionHost.waitForMotorActionReady(maxWaitMs);
                }

                @Override
                public long interactionMotorReadyWaitMaxMs() {
                    return sceneObjectActionHost.interactionMotorReadyWaitMaxMs();
                }

                @Override
                public MotorHandle scheduleMotorGesture(
                    CanvasPoint point,
                    MotorGestureType type,
                    MotorProfile profile
                ) {
                    return executor.scheduleMotorGesture(point, type, profile);
                }

                @Override
                public MotorProfile buildAgilityMoveAndClickProfile(
                    MotionProfile.ClickMotionSettings motion,
                    net.runelite.api.TileObject targetObject
                ) {
                    return executor.buildAgilityMoveAndClickProfile(motion, targetObject);
                }

                @Override
                public void noteInteractionActivityNow() {
                    executor.noteInteractionActivityNow();
                }

                @Override
                public void incrementClicksDispatched() {
                    executor.incrementClicksDispatched();
                }

                @Override
                public com.google.gson.JsonObject details(Object... kvPairs) {
                    return sceneObjectActionHost.details(kvPairs);
                }

                @Override
                public String safeString(String value) {
                    return sceneObjectActionHost.safeString(value);
                }

                @Override
                public CommandExecutor.CommandDecision accept(String reason, com.google.gson.JsonObject details) {
                    return sceneObjectActionHost.accept(reason, details);
                }

                @Override
                public CommandExecutor.CommandDecision reject(String reason) {
                    return sceneObjectActionHost.reject(reason);
                }
            }, agilityTargetResolver);
        GroundItemActionService groundItemActionService =
            new GroundItemActionService(groundItemActionHostSupplier.get());
        ShopBuyCommandService shopBuyCommandService =
            new ShopBuyCommandService(shopBuyCommandHostSupplier.get());
        WorldHopCommandService worldHopCommandService =
            new WorldHopCommandService(worldHopCommandHostSupplier.get());

        SceneCacheScanner sceneCacheScanner = new SceneCacheScanner(sceneCacheScannerHostSupplier.get());

        SessionCommandFacade sessionCommandFacade = new SessionCommandFacade(
            bankCommandService,
            woodcuttingCommandService,
            miningCommandService,
            fishingCommandService,
            agilityCommandService,
            walkCommandService,
            combatCommandService,
            npcContextMenuTestService::execute,
            payload -> sceneObjectActionService.execute(payload, MotionProfile.GENERIC_INTERACT),
            payload -> groundItemActionService.execute(payload, MotionProfile.GENERIC_INTERACT),
            payload -> shopBuyCommandService.executeBuyItem(payload, MotionProfile.GENERIC_INTERACT),
            payload -> worldHopCommandService.execute(payload, MotionProfile.GENERIC_INTERACT),
            interactionCommandService::executeCameraNudgeSafe,
            interactionCommandService::executeEatFoodSafe,
            unsupportedDecisionSupplier
        );

        BankSession bankSession = new BankSession(sessionCommandFacade);
        DropRuntime dropRuntime = new DropRuntime(dropRuntimeHostSupplier.get());
        DropCommandService dropCommandService = new DropCommandService(dropRuntime, unsupportedDecisionSupplier);
        DropSession dropSession = new DropSession(dropCommandService);
        SessionManager sessionManager = new SessionManager();
        InteractionSession interactionSession = InteractionSessionFactory.create(
            executor,
            sessionManager,
            sessionCommandFacade
        );
        IdleRuntime idleRuntime = new IdleRuntime(idleRuntimeHostFactory.apply(
            sessionManager,
            fishingCommandService,
            woodcuttingCommandService,
            dropRuntime
        ));

        return new ExecutorServiceBundle(
            combatTargetPolicy,
            npcClickPointResolver,
            brutusCombatSystem,
            sceneCacheScanner,
            bankSession,
            dropSession,
            sessionManager,
            interactionSession,
            idleRuntime,
            activityRegistry
        );
    }
}
