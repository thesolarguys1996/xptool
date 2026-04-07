package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.mining.MiningCommandService;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import com.xptool.systems.FishingTargetResolver;
import com.xptool.systems.MiningTargetResolver;
import com.xptool.systems.SceneCacheScanner;
import com.xptool.systems.WoodcuttingTargetResolver;
import java.awt.Point;
import java.util.Optional;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;

final class ExecutorGameplayRuntimeInputs {
    private ExecutorGameplayRuntimeInputs() {
    }

    static final class ServiceHosts {
        final Supplier<BankCommandService.Host> bankCommandHostSupplier;
        final Supplier<WoodcuttingTargetResolver.Host> woodcuttingTargetResolverHostSupplier;
        final Function<WoodcuttingTargetResolver, WoodcuttingCommandService.Host> woodcuttingCommandHostFactory;
        final Supplier<MiningTargetResolver.Host> miningTargetResolverHostSupplier;
        final Function<MiningTargetResolver, MiningCommandService.Host> miningCommandHostFactory;
        final Supplier<FishingTargetResolver.Host> fishingTargetResolverHostSupplier;
        final Supplier<CombatTargetPolicy.Host> combatTargetPolicyHostSupplier;
        final Function<CombatTargetPolicy, CombatTargetResolver.Host> combatTargetResolverHostFactory;
        final Predicate<Point> isCombatCanvasPointUsable;
        final double combatHullUpperFallbackYRatio;
        final double combatHullTopFallbackYRatio;
        final int combatSmallHullMaxWidthPx;
        final int combatSmallHullMaxHeightPx;
        final int combatHullCandidateSearchRadiusPx;
        final Function<FishingTargetResolver, FishingCommandService.Host> fishingCommandHostFactory;
        final Function<Client, WalkCommandService.Host> walkCommandHostFactory;
        final BiFunction<CombatTargetPolicy, CombatTargetResolver, BrutusCombatSystem.Host> brutusCombatHostFactory;
        final ExecutorServiceWiring.TriFunction<BrutusCombatSystem, CombatTargetPolicy, CombatTargetResolver, CombatCommandService.Host> combatCommandHostFactory;
        final Function<BrutusCombatSystem, InteractionCommandService.Host> interactionCommandHostFactory;
        final Function<Client, NpcContextMenuTestService.Host> npcContextMenuTestHostFactory;
        final Function<Client, SceneObjectActionService.Host> sceneObjectActionHostFactory;
        final Function<Client, GroundItemActionService.Host> groundItemActionHostFactory;
        final Function<Client, ShopBuyCommandService.Host> shopBuyCommandHostFactory;
        final Function<Client, WorldHopCommandService.Host> worldHopCommandHostFactory;
        final Supplier<SceneCacheScanner.Host> sceneCacheScannerHostSupplier;
        final Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier;

        ServiceHosts(
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
            Supplier<CommandExecutor.CommandDecision> unsupportedDecisionSupplier
        ) {
            this.bankCommandHostSupplier = Objects.requireNonNull(bankCommandHostSupplier, "bankCommandHostSupplier");
            this.woodcuttingTargetResolverHostSupplier =
                Objects.requireNonNull(woodcuttingTargetResolverHostSupplier, "woodcuttingTargetResolverHostSupplier");
            this.woodcuttingCommandHostFactory =
                Objects.requireNonNull(woodcuttingCommandHostFactory, "woodcuttingCommandHostFactory");
            this.miningTargetResolverHostSupplier =
                Objects.requireNonNull(miningTargetResolverHostSupplier, "miningTargetResolverHostSupplier");
            this.miningCommandHostFactory =
                Objects.requireNonNull(miningCommandHostFactory, "miningCommandHostFactory");
            this.fishingTargetResolverHostSupplier =
                Objects.requireNonNull(fishingTargetResolverHostSupplier, "fishingTargetResolverHostSupplier");
            this.combatTargetPolicyHostSupplier =
                Objects.requireNonNull(combatTargetPolicyHostSupplier, "combatTargetPolicyHostSupplier");
            this.combatTargetResolverHostFactory =
                Objects.requireNonNull(combatTargetResolverHostFactory, "combatTargetResolverHostFactory");
            this.isCombatCanvasPointUsable = Objects.requireNonNull(isCombatCanvasPointUsable, "isCombatCanvasPointUsable");
            this.combatHullUpperFallbackYRatio = combatHullUpperFallbackYRatio;
            this.combatHullTopFallbackYRatio = combatHullTopFallbackYRatio;
            this.combatSmallHullMaxWidthPx = combatSmallHullMaxWidthPx;
            this.combatSmallHullMaxHeightPx = combatSmallHullMaxHeightPx;
            this.combatHullCandidateSearchRadiusPx = combatHullCandidateSearchRadiusPx;
            this.fishingCommandHostFactory =
                Objects.requireNonNull(fishingCommandHostFactory, "fishingCommandHostFactory");
            this.walkCommandHostFactory = Objects.requireNonNull(walkCommandHostFactory, "walkCommandHostFactory");
            this.brutusCombatHostFactory = Objects.requireNonNull(brutusCombatHostFactory, "brutusCombatHostFactory");
            this.combatCommandHostFactory = Objects.requireNonNull(combatCommandHostFactory, "combatCommandHostFactory");
            this.interactionCommandHostFactory =
                Objects.requireNonNull(interactionCommandHostFactory, "interactionCommandHostFactory");
            this.npcContextMenuTestHostFactory =
                Objects.requireNonNull(npcContextMenuTestHostFactory, "npcContextMenuTestHostFactory");
            this.sceneObjectActionHostFactory =
                Objects.requireNonNull(sceneObjectActionHostFactory, "sceneObjectActionHostFactory");
            this.groundItemActionHostFactory =
                Objects.requireNonNull(groundItemActionHostFactory, "groundItemActionHostFactory");
            this.shopBuyCommandHostFactory =
                Objects.requireNonNull(shopBuyCommandHostFactory, "shopBuyCommandHostFactory");
            this.worldHopCommandHostFactory =
                Objects.requireNonNull(worldHopCommandHostFactory, "worldHopCommandHostFactory");
            this.sceneCacheScannerHostSupplier =
                Objects.requireNonNull(sceneCacheScannerHostSupplier, "sceneCacheScannerHostSupplier");
            this.unsupportedDecisionSupplier =
                Objects.requireNonNull(unsupportedDecisionSupplier, "unsupportedDecisionSupplier");
        }
    }

    static final class RuntimeInputs {
        IntSupplier currentExecutorTick;
        IntSupplier currentPlayerAnimation;
        ExecutorRuntimeDomainWiring.IntPredicate isAnimationActive;
        BooleanSupplier isDropSweepSessionActive;
        IntSupplier dropSweepItemId;
        IntSupplier dropSweepNextSlot;
        IntSupplier dropSweepLastDispatchTick;
        IntSupplier dropSweepDispatchFailStreak;
        BooleanSupplier dropSweepAwaitingFirstCursorSync;
        IntConsumer setDropSweepNextSlot;
        IntConsumer setDropSweepLastDispatchTick;
        ExecutorRuntimeDomainWiring.IntBooleanSetter setDropSweepAwaitingFirstCursorSync;
        ExecutorRuntimeDomainWiring.IntBooleanSetter setDropSweepProgressCheckPending;
        ExecutorRuntimeDomainWiring.IntSetConsumer beginDropSweepSession;
        Runnable endDropSweepSession;
        ExecutorRuntimeDomainWiring.IntPredicate updateDropSweepProgressState;
        BooleanSupplier noteDropSweepDispatchFailure;
        Runnable noteDropSweepDispatchSuccess;
        ExecutorRuntimeDomainWiring.IntIntOptionalResolver findInventorySlotFrom;
        IntFunction<Optional<Point>> resolveInventorySlotPoint;
        IntFunction<Optional<Point>> resolveInventorySlotBasePoint;
        Supplier<Optional<Point>> centerOfDropSweepRegionCanvas;
        Predicate<Point> isCursorNearDropTarget;
        Function<Point, MotorHandle> scheduleDropMoveGesture;
        BooleanSupplier acquireOrRenewDropMotorOwner;
        BooleanSupplier isLoggedInAndBankClosed;
        ExecutorRuntimeDomainWiring.IntIntPointPredicate dispatchInventoryDropAction;
        Runnable applyDropPerceptionDelay;
        Runnable incrementClicksDispatched;
        Supplier<FatigueSnapshot> fatigueSnapshot;
        Consumer<String> onDropCadenceProfileSelected;
        Consumer<IdleCadenceTuning> onIdleCadenceTuningSelected;
        Function<Object[], JsonObject> details;
        BiConsumer<String, JsonObject> emitDropDebug;
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision;
        Function<String, CommandExecutor.CommandDecision> rejectDecision;
        BooleanSupplier hasActiveDropSweepSession;
        BooleanSupplier isIdleInterActionWindowOpen;
        Supplier<IdleSkillContext> resolveIdleSkillContext;
        BooleanSupplier isIdleActionWindowOpen;
        BooleanSupplier isIdleCameraWindowOpen;
        Supplier<JsonObject> idleWindowGateSnapshot;
        BooleanSupplier isIdleAnimationActiveNow;
        BooleanSupplier isIdleInteractionDelaySatisfied;
        BooleanSupplier isIdleCameraInteractionDelaySatisfied;
        Supplier<Long> lastInteractionClickSerial;
        BooleanSupplier isCursorOutsideClientWindow;
        BooleanSupplier acquireOrRenewIdleMotorOwnership;
        BooleanSupplier canPerformIdleMotorActionNow;
        BooleanSupplier performIdleCameraMicroAdjust;
        Supplier<Optional<Point>> resolveIdleHoverTargetCanvasPoint;
        Predicate<Point> performIdleCursorMove;
        Supplier<Optional<Point>> resolveIdleDriftTargetCanvasPoint;
        Supplier<Optional<Point>> resolveIdleOffscreenTargetScreenPoint;
        Predicate<Point> performIdleOffscreenCursorMove;
        Supplier<Optional<Point>> resolveIdleParkingTargetCanvasPoint;
        Function<IdleSkillContext, FishingIdleMode> resolveFishingIdleMode;
        Function<IdleSkillContext, ActivityIdlePolicy> resolveActivityIdlePolicy;
        Supplier<IdleCadenceTuning> activeIdleCadenceTuning;
        BiConsumer<String, JsonObject> emitIdleEvent;
        BooleanSupplier isRandomEventRuntimeEnabled;
        BooleanSupplier isRandomEventRuntimeArmed;
        BooleanSupplier isLoggedIn;
        BooleanSupplier isBankOpen;
        BooleanSupplier hasActiveInteractionMotorProgram;
        BooleanSupplier acquireOrRenewInteractionMotorOwnership;
        Runnable releaseInteractionMotorOwnership;
        Supplier<Player> localPlayer;
        Supplier<Iterable<NPC>> npcs;
        Function<NPC, Point> resolveVariedNpcClickPoint;
        Predicate<Point> isUsableCanvasPoint;
        Predicate<Point> moveInteractionCursorToCanvasPoint;
        Predicate<Point> isCursorNearRandomEventTarget;
        Predicate<Point> selectRandomEventDismissMenuOptionAt;
        LongBinaryOperator randomBetween;
        LongSupplier randomEventPreAttemptCooldownMinMs;
        LongSupplier randomEventPreAttemptCooldownMaxMs;
        LongSupplier randomEventSuccessCooldownMinMs;
        LongSupplier randomEventSuccessCooldownMaxMs;
        LongSupplier randomEventFailureRetryCooldownMinMs;
        LongSupplier randomEventFailureRetryCooldownMaxMs;
        LongSupplier randomEventCursorReadyHoldMs;
        BiConsumer<String, JsonObject> emitRandomEventEvent;
        BooleanSupplier isTopMenuBankOnObject;
        Predicate<TileObject> isTopMenuChopOnTree;
        Predicate<TileObject> isTopMenuMineOnRock;
        BooleanSupplier hasAttackEntryOnNpc;
        LongConsumer reserveMotorCooldown;
    }
}
