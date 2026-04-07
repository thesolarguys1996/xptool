package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.mining.MiningCommandService;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.systems.FishingTargetResolver;
import com.xptool.systems.MiningTargetResolver;
import com.xptool.systems.WoodcuttingTargetResolver;
import java.awt.Point;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

final class ExecutorSkillingHostFactories {
    private ExecutorSkillingHostFactories() {
    }

    static Function<WoodcuttingTargetResolver, WoodcuttingCommandService.Host> createWoodcuttingCommandHostFactory(
        BooleanSupplier isDropSweepSessionActive,
        Runnable endDropSweepSession,
        LongSupplier lastDropSweepSessionEndedAtMs,
        Runnable extendWoodcutRetryWindow,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        IntSupplier currentPlayerAnimation,
        Supplier<WorldPoint> localPlayerWorldPoint,
        Runnable clearWoodcutOutcomeWaitWindow,
        Runnable clearWoodcutTargetAttempt,
        Runnable clearWoodcutDispatchAttempt,
        LongSupplier woodcutOutcomeWaitUntilMs,
        Supplier<WorldPoint> woodcutLastAttemptWorldPoint,
        LongSupplier woodcutApproachWaitUntilMs,
        Supplier<WorldPoint> woodcutLastDispatchWorldPoint,
        LongSupplier woodcutLastDispatchAtMs,
        LongSupplier woodcutSameTargetReclickCooldownMs,
        ExecutorSkillingDomainWiring.TriIntConsumer updateWoodcutBoundary,
        Runnable clearWoodcutBoundary,
        Consumer<TileObject> lockWoodcutTarget,
        Runnable clearWoodcutInteractionWindows,
        IntSupplier selectedWoodcutTargetCount,
        Function<TileObject, Point> resolveWoodcutHoverPoint,
        Predicate<Point> isUsableCanvasPoint,
        Runnable clearWoodcutTargetLock,
        Runnable clearWoodcutHoverPoint,
        BiConsumer<TileObject, Point> rememberInteractionAnchorForTileObject,
        ExecutorCombatDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        BiFunction<ClickMotionSettings, TileObject, MotorProfile> buildWoodcutMoveAndClickProfile,
        Runnable noteInteractionActivityNow,
        Consumer<TileObject> noteWoodcutTargetAttempt,
        ObjLongConsumer<TileObject> noteWoodcutDispatchAttempt,
        Runnable beginWoodcutOutcomeWaitWindow,
        Runnable incrementClicksDispatched,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<String, JsonObject, RuntimeDecision> acceptDecision,
        Function<String, RuntimeDecision> rejectDecision
    ) {
        return woodcuttingResolver -> ExecutorSkillingDomainWiring.createWoodcuttingCommandHost(
            isDropSweepSessionActive,
            endDropSweepSession,
            lastDropSweepSessionEndedAtMs,
            extendWoodcutRetryWindow,
            resolveClickMotion,
            currentPlayerAnimation,
            localPlayerWorldPoint,
            clearWoodcutOutcomeWaitWindow,
            clearWoodcutTargetAttempt,
            clearWoodcutDispatchAttempt,
            woodcutOutcomeWaitUntilMs,
            woodcutLastAttemptWorldPoint,
            woodcutApproachWaitUntilMs,
            woodcutLastDispatchWorldPoint,
            woodcutLastDispatchAtMs,
            woodcutSameTargetReclickCooldownMs,
            woodcuttingResolver::resolveNearestOakTreeTarget,
            woodcuttingResolver::resolveNearestWillowTreeTarget,
            woodcuttingResolver::resolveNearestNormalTreeTarget,
            woodcuttingResolver::resolveNearestTreeTargetInArea,
            updateWoodcutBoundary,
            clearWoodcutBoundary,
            woodcuttingResolver,
            lockWoodcutTarget,
            clearWoodcutInteractionWindows,
            selectedWoodcutTargetCount,
            resolveWoodcutHoverPoint,
            isUsableCanvasPoint,
            clearWoodcutTargetLock,
            clearWoodcutHoverPoint,
            rememberInteractionAnchorForTileObject,
            scheduleMotorGesture,
            buildWoodcutMoveAndClickProfile,
            noteInteractionActivityNow,
            noteWoodcutTargetAttempt,
            noteWoodcutDispatchAttempt,
            beginWoodcutOutcomeWaitWindow,
            incrementClicksDispatched,
            fatigueSnapshot,
            details,
            safeString,
            acceptDecision,
            rejectDecision
        );
    }

    static Function<MiningTargetResolver, MiningCommandService.Host> createMiningCommandHostFactory(
        BooleanSupplier isDropSweepSessionActive,
        Runnable endDropSweepSession,
        Runnable pruneMiningRockSuppression,
        Runnable extendMiningRetryWindow,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        IntSupplier currentPlayerAnimation,
        Runnable clearMiningOutcomeWaitWindow,
        LongSupplier miningOutcomeWaitUntilMs,
        Supplier<WorldPoint> lockedMiningWorldPoint,
        IntSupplier lockedMiningObjectId,
        BooleanSupplier hasLockedMiningTarget,
        Consumer<TileObject> lockMiningTarget,
        Runnable clearMiningInteractionWindows,
        IntSupplier selectedMiningTargetCount,
        Function<TileObject, Point> resolveMiningHoverPoint,
        Predicate<Point> isUsableCanvasPoint,
        Runnable clearMiningTargetLock,
        Runnable clearMiningHoverPoint,
        BiConsumer<TileObject, Point> rememberInteractionAnchorForTileObject,
        ExecutorCombatDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        BiFunction<ClickMotionSettings, TileObject, MotorProfile> buildMiningMoveAndClickProfile,
        Runnable noteInteractionActivityNow,
        ObjLongConsumer<WorldPoint> suppressMiningRockTarget,
        LongSupplier miningTargetReclickCooldownMs,
        Runnable beginMiningOutcomeWaitWindow,
        Runnable incrementClicksDispatched,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<String, JsonObject, RuntimeDecision> acceptDecision,
        Function<String, RuntimeDecision> rejectDecision
    ) {
        return miningResolver -> ExecutorSkillingDomainWiring.createMiningCommandHost(
            isDropSweepSessionActive,
            endDropSweepSession,
            pruneMiningRockSuppression,
            extendMiningRetryWindow,
            resolveClickMotion,
            currentPlayerAnimation,
            clearMiningOutcomeWaitWindow,
            miningOutcomeWaitUntilMs,
            miningResolver,
            lockedMiningWorldPoint,
            lockedMiningObjectId,
            hasLockedMiningTarget,
            lockMiningTarget,
            clearMiningInteractionWindows,
            selectedMiningTargetCount,
            resolveMiningHoverPoint,
            isUsableCanvasPoint,
            clearMiningTargetLock,
            clearMiningHoverPoint,
            rememberInteractionAnchorForTileObject,
            scheduleMotorGesture,
            buildMiningMoveAndClickProfile,
            noteInteractionActivityNow,
            suppressMiningRockTarget,
            miningTargetReclickCooldownMs,
            beginMiningOutcomeWaitWindow,
            incrementClicksDispatched,
            details,
            safeString,
            acceptDecision,
            rejectDecision
        );
    }

    static Function<FishingTargetResolver, FishingCommandService.Host> createFishingCommandHostFactory(
        BooleanSupplier isDropSweepSessionActive,
        Runnable endDropSweepSession,
        LongSupplier lastDropSweepSessionEndedAtMs,
        Runnable extendFishingRetryWindow,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        Supplier<Player> currentPlayer,
        BooleanSupplier isFishingLevelUpPromptVisible,
        BooleanSupplier dismissFishingLevelUpPrompt,
        Runnable clearFishingOutcomeWaitWindow,
        Runnable clearFishingTargetAttempt,
        BiFunction<JsonElement, JsonElement, Set<Integer>> parsePreferredNpcIds,
        LongSupplier fishingOutcomeWaitUntilMs,
        IntSupplier fishingLastAttemptNpcIndex,
        Supplier<WorldPoint> fishingLastAttemptWorldPoint,
        LongSupplier fishingApproachWaitUntilMs,
        Consumer<NPC> lockFishingTarget,
        Runnable clearFishingInteractionWindows,
        Runnable clearFishingInteractionWindowsPreserveDispatchSignal,
        Function<NPC, Point> resolveNpcClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Runnable clearFishingTargetLock,
        BiConsumer<NPC, Point> rememberInteractionAnchorForNpc,
        LongSupplier fishingLastDispatchAtMs,
        Supplier<WorldPoint> fishingLastDispatchWorldPoint,
        IntSupplier fishingLastDispatchNpcIndex,
        LongSupplier fishingSameTargetReclickCooldownMs,
        ExecutorCombatDomainWiring.MotorGestureScheduler scheduleMotorGesture,
        Function<ClickMotionSettings, MotorProfile> buildFishingMoveAndClickProfile,
        Runnable noteInteractionActivityNow,
        BiConsumer<Player, NPC> noteFishingTargetAttempt,
        ObjLongConsumer<NPC> noteFishingDispatchAttempt,
        Runnable beginFishingOutcomeWaitWindow,
        Runnable incrementClicksDispatched,
        Supplier<FatigueSnapshot> fatigueSnapshot,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<String, JsonObject, RuntimeDecision> acceptDecision,
        Function<String, RuntimeDecision> rejectDecision
    ) {
        return fishingResolver -> ExecutorSkillingDomainWiring.createFishingCommandHost(
            isDropSweepSessionActive,
            endDropSweepSession,
            lastDropSweepSessionEndedAtMs,
            extendFishingRetryWindow,
            resolveClickMotion,
            currentPlayer,
            isFishingLevelUpPromptVisible,
            dismissFishingLevelUpPrompt,
            clearFishingOutcomeWaitWindow,
            clearFishingTargetAttempt,
            parsePreferredNpcIds,
            fishingOutcomeWaitUntilMs,
            fishingLastAttemptNpcIndex,
            fishingLastAttemptWorldPoint,
            fishingApproachWaitUntilMs,
            fishingResolver,
            fishingResolver::resolveNearestFishingTarget,
            lockFishingTarget,
            clearFishingInteractionWindows,
            clearFishingInteractionWindowsPreserveDispatchSignal,
            resolveNpcClickPoint,
            isUsableCanvasPoint,
            clearFishingTargetLock,
            rememberInteractionAnchorForNpc,
            fishingLastDispatchAtMs,
            fishingLastDispatchWorldPoint,
            fishingLastDispatchNpcIndex,
            fishingSameTargetReclickCooldownMs,
            scheduleMotorGesture,
            buildFishingMoveAndClickProfile,
            noteInteractionActivityNow,
            noteFishingTargetAttempt,
            noteFishingDispatchAttempt,
            beginFishingOutcomeWaitWindow,
            incrementClicksDispatched,
            fatigueSnapshot,
            details,
            safeString,
            acceptDecision,
            rejectDecision
        );
    }
}
