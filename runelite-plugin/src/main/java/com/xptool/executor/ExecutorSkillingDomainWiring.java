package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.mining.MiningCommandService;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.core.motor.MotorDispatchResult;
import com.xptool.core.motor.MotorDispatchStatus;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.systems.FishingTargetResolver;
import com.xptool.systems.MiningTargetResolver;
import com.xptool.systems.WoodcuttingTargetResolver;
import java.awt.Point;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

final class ExecutorSkillingDomainWiring {
    @FunctionalInterface
    interface TriIntFunction<T> {
        T apply(int a, int b, int c);
    }

    @FunctionalInterface
    interface TriIntConsumer {
        void accept(int a, int b, int c);
    }

    private ExecutorSkillingDomainWiring() {
    }

    static WoodcuttingTargetResolver.Host createWoodcuttingTargetResolverHost(
        Supplier<WorldPoint> localPlayerWorldPoint,
        Supplier<WorldPoint> lockedWoodcutWorldPoint,
        Supplier<WorldPoint> preferredSelectedWoodcutWorldPoint,
        IntSupplier selectedWoodcutTargetCount,
        Supplier<Iterable<TileObject>> cachedTreeObjects,
        Supplier<Iterable<TileObject>> cachedNormalTreeObjects,
        Supplier<Iterable<TileObject>> cachedOakTreeObjects,
        Supplier<Iterable<TileObject>> cachedWillowTreeObjects,
        Predicate<WorldPoint> hasSelectedTreeTargetNear,
        BiPredicate<WorldPoint, WorldPoint> worldPointsMatch,
        BiFunction<Iterable<TileObject>, WoodcuttingTargetResolver.WorldDistanceProvider, Optional<TileObject>>
            selectBestCursorAwareTarget
    ) {
        return new WoodcuttingTargetResolver.Host() {
            @Override
            public WorldPoint localPlayerWorldPoint() {
                return localPlayerWorldPoint.get();
            }

            @Override
            public WorldPoint lockedWoodcutWorldPoint() {
                return lockedWoodcutWorldPoint.get();
            }

            @Override
            public WorldPoint preferredSelectedWoodcutWorldPoint() {
                return preferredSelectedWoodcutWorldPoint.get();
            }

            @Override
            public int selectedWoodcutTargetCount() {
                return selectedWoodcutTargetCount.getAsInt();
            }

            @Override
            public Iterable<TileObject> cachedTreeObjects() {
                return cachedTreeObjects.get();
            }

            @Override
            public Iterable<TileObject> cachedNormalTreeObjects() {
                return cachedNormalTreeObjects.get();
            }

            @Override
            public Iterable<TileObject> cachedOakTreeObjects() {
                return cachedOakTreeObjects.get();
            }

            @Override
            public Iterable<TileObject> cachedWillowTreeObjects() {
                return cachedWillowTreeObjects.get();
            }

            @Override
            public boolean hasSelectedTreeTargetNear(WorldPoint worldPoint) {
                return hasSelectedTreeTargetNear.test(worldPoint);
            }

            @Override
            public boolean worldPointsMatch(WorldPoint a, WorldPoint b) {
                return worldPointsMatch.test(a, b);
            }

            @Override
            public Optional<TileObject> selectBestCursorAwareTarget(
                Iterable<TileObject> candidates,
                WoodcuttingTargetResolver.WorldDistanceProvider worldDistanceProvider
            ) {
                return selectBestCursorAwareTarget.apply(candidates, worldDistanceProvider);
            }
        };
    }

    static MiningTargetResolver.Host createMiningTargetResolverHost(
        IntSupplier selectedMiningTargetCount,
        Supplier<WorldPoint> localPlayerWorldPoint,
        Supplier<Iterable<TileObject>> cachedRockObjects,
        Predicate<TileObject> isRockObjectCandidate,
        Predicate<WorldPoint> isMiningRockSuppressed,
        Predicate<WorldPoint> hasSelectedRockTargetNear,
        BiFunction<Iterable<TileObject>, MiningTargetResolver.WorldDistanceProvider, Optional<TileObject>>
            selectBestCursorAwareTarget
    ) {
        return new MiningTargetResolver.Host() {
            @Override
            public int selectedMiningTargetCount() {
                return selectedMiningTargetCount.getAsInt();
            }

            @Override
            public WorldPoint localPlayerWorldPoint() {
                return localPlayerWorldPoint.get();
            }

            @Override
            public Iterable<TileObject> cachedRockObjects() {
                return cachedRockObjects.get();
            }

            @Override
            public boolean isRockObjectCandidate(TileObject candidate) {
                return isRockObjectCandidate.test(candidate);
            }

            @Override
            public boolean isMiningRockSuppressed(WorldPoint worldPoint) {
                return isMiningRockSuppressed.test(worldPoint);
            }

            @Override
            public boolean hasSelectedRockTargetNear(WorldPoint worldPoint) {
                return hasSelectedRockTargetNear.test(worldPoint);
            }

            @Override
            public Optional<TileObject> selectBestCursorAwareTarget(
                Iterable<TileObject> candidates,
                MiningTargetResolver.WorldDistanceProvider worldDistanceProvider
            ) {
                return selectBestCursorAwareTarget.apply(candidates, worldDistanceProvider);
            }
        };
    }

    static WoodcuttingCommandService.Host createWoodcuttingCommandHost(
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
        TriIntFunction<Optional<TileObject>> resolveNearestOakTreeTarget,
        TriIntFunction<Optional<TileObject>> resolveNearestWillowTreeTarget,
        TriIntFunction<Optional<TileObject>> resolveNearestNormalTreeTarget,
        TriIntFunction<Optional<TileObject>> resolveNearestTreeTargetInArea,
        TriIntConsumer updateWoodcutBoundary,
        Runnable clearWoodcutBoundary,
        WoodcuttingTargetResolver woodcuttingTargetResolver,
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
        return new WoodcuttingCommandService.Host() {
            @Override
            public boolean isDropSweepSessionActive() {
                return isDropSweepSessionActive.getAsBoolean();
            }

            @Override
            public void endDropSweepSession() {
                endDropSweepSession.run();
            }

            @Override
            public long lastDropSweepSessionEndedAtMs() {
                return lastDropSweepSessionEndedAtMs.getAsLong();
            }

            @Override
            public void extendWoodcutRetryWindow() {
                extendWoodcutRetryWindow.run();
            }

            @Override
            public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
                return resolveClickMotion.apply(payload, motionProfile);
            }

            @Override
            public int currentPlayerAnimation() {
                return currentPlayerAnimation.getAsInt();
            }

            @Override
            public WorldPoint localPlayerWorldPoint() {
                return localPlayerWorldPoint.get();
            }

            @Override
            public void clearWoodcutOutcomeWaitWindow() {
                clearWoodcutOutcomeWaitWindow.run();
            }

            @Override
            public void clearWoodcutTargetAttempt() {
                clearWoodcutTargetAttempt.run();
            }

            @Override
            public void clearWoodcutDispatchAttempt() {
                clearWoodcutDispatchAttempt.run();
            }

            @Override
            public long woodcutOutcomeWaitUntilMs() {
                return woodcutOutcomeWaitUntilMs.getAsLong();
            }

            @Override
            public WorldPoint woodcutLastAttemptWorldPoint() {
                return woodcutLastAttemptWorldPoint.get();
            }

            @Override
            public long woodcutApproachWaitUntilMs() {
                return woodcutApproachWaitUntilMs.getAsLong();
            }

            @Override
            public WorldPoint woodcutLastDispatchWorldPoint() {
                return woodcutLastDispatchWorldPoint.get();
            }

            @Override
            public long woodcutLastDispatchAtMs() {
                return woodcutLastDispatchAtMs.getAsLong();
            }

            @Override
            public long woodcutSameTargetReclickCooldownMs() {
                return woodcutSameTargetReclickCooldownMs.getAsLong();
            }

            @Override
            public Optional<TileObject> resolveLockedOakTreeTarget() {
                return woodcuttingTargetResolver.resolveLockedOakTreeTarget();
            }

            @Override
            public Optional<TileObject> resolveNearestOakTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return resolveNearestOakTreeTarget.apply(targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public Optional<TileObject> resolveLockedWillowTreeTarget() {
                return woodcuttingTargetResolver.resolveLockedWillowTreeTarget();
            }

            @Override
            public Optional<TileObject> resolveNearestWillowTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return resolveNearestWillowTreeTarget.apply(targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public Optional<TileObject> resolveLockedSelectedTreeTarget() {
                return woodcuttingTargetResolver.resolveLockedSelectedTreeTarget();
            }

            @Override
            public Optional<TileObject> resolvePreferredSelectedTreeTarget() {
                return woodcuttingTargetResolver.resolvePreferredSelectedTreeTarget();
            }

            @Override
            public Optional<TileObject> resolveNearestSelectedTreeTarget() {
                return woodcuttingTargetResolver.resolveNearestSelectedTreeTarget();
            }

            @Override
            public Optional<TileObject> resolveNearestTreeTargetInArea(int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return resolveNearestTreeTargetInArea.apply(targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public Optional<TileObject> resolveLockedNormalTreeTarget() {
                return woodcuttingTargetResolver.resolveLockedNormalTreeTarget();
            }

            @Override
            public Optional<TileObject> resolveNearestNormalTreeTarget(int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return resolveNearestNormalTreeTarget.apply(targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public void lockWoodcutTarget(TileObject targetObject) {
                lockWoodcutTarget.accept(targetObject);
            }

            @Override
            public void clearWoodcutInteractionWindows() {
                clearWoodcutInteractionWindows.run();
            }

            @Override
            public int selectedWoodcutTargetCount() {
                return selectedWoodcutTargetCount.getAsInt();
            }

            @Override
            public Point resolveWoodcutHoverPoint(TileObject targetObject) {
                return resolveWoodcutHoverPoint.apply(targetObject);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public void clearWoodcutTargetLock() {
                clearWoodcutTargetLock.run();
            }

            @Override
            public void clearWoodcutHoverPoint() {
                clearWoodcutHoverPoint.run();
            }

            @Override
            public void updateWoodcutBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance) {
                updateWoodcutBoundary.accept(targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public void clearWoodcutBoundary() {
                clearWoodcutBoundary.run();
            }

            @Override
            public void rememberInteractionAnchorForTileObject(TileObject targetObject, Point point) {
                rememberInteractionAnchorForTileObject.accept(targetObject, point);
            }

            @Override
            public MotorDispatchResult dispatchWoodcutMoveAndClick(
                Point canvasPoint,
                ClickMotionSettings motion,
                TileObject targetObject
            ) {
                MotorHandle handle = scheduleMotorGesture.schedule(
                    CanvasPoint.fromAwtPoint(canvasPoint),
                    MotorGestureType.MOVE_AND_CLICK,
                    buildWoodcutMoveAndClickProfile.apply(motion, targetObject)
                );
                if (handle == null) {
                    return new MotorDispatchResult(0L, MotorDispatchStatus.FAILED, "missing_motor_handle");
                }
                return new MotorDispatchResult(
                    handle.id,
                    toMotorDispatchStatus(handle.status),
                    handle.reason
                );
            }

            @Override
            public void noteInteractionActivityNow() {
                noteInteractionActivityNow.run();
            }

            @Override
            public void noteWoodcutTargetAttempt(TileObject targetObject) {
                noteWoodcutTargetAttempt.accept(targetObject);
            }

            @Override
            public void noteWoodcutDispatchAttempt(TileObject targetObject, long now) {
                noteWoodcutDispatchAttempt.accept(targetObject, now);
            }

            @Override
            public void beginWoodcutOutcomeWaitWindow() {
                beginWoodcutOutcomeWaitWindow.run();
            }

            @Override
            public void incrementClicksDispatched() {
                incrementClicksDispatched.run();
            }

            @Override
            public com.xptool.core.runtime.FatigueSnapshot fatigueSnapshot() {
                FatigueSnapshot snapshot = fatigueSnapshot.get();
                if (snapshot == null) {
                    return com.xptool.core.runtime.FatigueSnapshot.neutral();
                }
                return com.xptool.core.runtime.FatigueSnapshot.fromLoad(snapshot.load01());
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
            public RuntimeDecision accept(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public RuntimeDecision reject(String reason) {
                return rejectDecision.apply(reason);
            }
        };
    }

    static MiningCommandService.Host createMiningCommandHost(
        BooleanSupplier isDropSweepSessionActive,
        Runnable endDropSweepSession,
        Runnable pruneMiningRockSuppression,
        Runnable extendMiningRetryWindow,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        IntSupplier currentPlayerAnimation,
        Runnable clearMiningOutcomeWaitWindow,
        LongSupplier miningOutcomeWaitUntilMs,
        MiningTargetResolver miningTargetResolver,
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
        return new MiningCommandService.Host() {
            @Override
            public boolean isDropSweepSessionActive() {
                return isDropSweepSessionActive.getAsBoolean();
            }

            @Override
            public void endDropSweepSession() {
                endDropSweepSession.run();
            }

            @Override
            public void pruneMiningRockSuppression() {
                pruneMiningRockSuppression.run();
            }

            @Override
            public void extendMiningRetryWindow() {
                extendMiningRetryWindow.run();
            }

            @Override
            public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
                return resolveClickMotion.apply(payload, motionProfile);
            }

            @Override
            public int currentPlayerAnimation() {
                return currentPlayerAnimation.getAsInt();
            }

            @Override
            public void clearMiningOutcomeWaitWindow() {
                clearMiningOutcomeWaitWindow.run();
            }

            @Override
            public long miningOutcomeWaitUntilMs() {
                return miningOutcomeWaitUntilMs.getAsLong();
            }

            @Override
            public Optional<TileObject> resolveNearestSelectedRockTargetExcludingLocked() {
                return miningTargetResolver.resolveNearestSelectedRockTargetExcluding(lockedMiningWorldPoint.get());
            }

            @Override
            public Optional<TileObject> resolveNearestSelectedRockTarget() {
                return miningTargetResolver.resolveNearestSelectedRockTarget();
            }

            @Override
            public boolean hasLockedMiningTarget() {
                return hasLockedMiningTarget.getAsBoolean();
            }

            @Override
            public Optional<TileObject> resolveLockedRockTarget() {
                return miningTargetResolver.resolveLockedRockTarget(lockedMiningWorldPoint.get(), lockedMiningObjectId.getAsInt());
            }

            @Override
            public Optional<TileObject> resolveNearestRockTarget() {
                return miningTargetResolver.resolveNearestRockTarget();
            }

            @Override
            public Optional<TileObject> resolveNearestRockTargetExcluding(WorldPoint excludedWorldPoint) {
                return miningTargetResolver.resolveNearestRockTargetExcluding(excludedWorldPoint);
            }

            @Override
            public void lockMiningTarget(TileObject targetObject) {
                lockMiningTarget.accept(targetObject);
            }

            @Override
            public void clearMiningInteractionWindows() {
                clearMiningInteractionWindows.run();
            }

            @Override
            public int selectedMiningTargetCount() {
                return selectedMiningTargetCount.getAsInt();
            }

            @Override
            public Point resolveMiningHoverPoint(TileObject targetObject) {
                return resolveMiningHoverPoint.apply(targetObject);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public void clearMiningTargetLock() {
                clearMiningTargetLock.run();
            }

            @Override
            public void clearMiningHoverPoint() {
                clearMiningHoverPoint.run();
            }

            @Override
            public void rememberInteractionAnchorForTileObject(TileObject targetObject, Point point) {
                rememberInteractionAnchorForTileObject.accept(targetObject, point);
            }

            @Override
            public MotorDispatchResult dispatchMiningMoveAndClick(
                Point canvasPoint,
                ClickMotionSettings motion,
                TileObject targetObject
            ) {
                MotorHandle handle = scheduleMotorGesture.schedule(
                    CanvasPoint.fromAwtPoint(canvasPoint),
                    MotorGestureType.MOVE_AND_CLICK,
                    buildMiningMoveAndClickProfile.apply(motion, targetObject)
                );
                if (handle == null) {
                    return new MotorDispatchResult(0L, MotorDispatchStatus.FAILED, "missing_motor_handle");
                }
                return new MotorDispatchResult(
                    handle.id,
                    toMotorDispatchStatus(handle.status),
                    handle.reason
                );
            }

            @Override
            public void noteInteractionActivityNow() {
                noteInteractionActivityNow.run();
            }

            @Override
            public void suppressMiningRockTarget(WorldPoint worldPoint, long durationMs) {
                suppressMiningRockTarget.accept(worldPoint, durationMs);
            }

            @Override
            public long miningTargetReclickCooldownMs() {
                return miningTargetReclickCooldownMs.getAsLong();
            }

            @Override
            public void beginMiningOutcomeWaitWindow() {
                beginMiningOutcomeWaitWindow.run();
            }

            @Override
            public void incrementClicksDispatched() {
                incrementClicksDispatched.run();
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
            public RuntimeDecision accept(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public RuntimeDecision reject(String reason) {
                return rejectDecision.apply(reason);
            }
        };
    }

    static FishingCommandService.Host createFishingCommandHost(
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
        FishingTargetResolver fishingTargetResolver,
        BiFunction<Player, Set<Integer>, Optional<NPC>> resolveNearestFishingTarget,
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
        return new FishingCommandService.Host() {
            @Override
            public boolean isDropSweepSessionActive() {
                return isDropSweepSessionActive.getAsBoolean();
            }

            @Override
            public void endDropSweepSession() {
                endDropSweepSession.run();
            }

            @Override
            public long lastDropSweepSessionEndedAtMs() {
                return lastDropSweepSessionEndedAtMs.getAsLong();
            }

            @Override
            public void extendFishingRetryWindow() {
                extendFishingRetryWindow.run();
            }

            @Override
            public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
                return resolveClickMotion.apply(payload, motionProfile);
            }

            @Override
            public Player currentPlayer() {
                return currentPlayer.get();
            }

            @Override
            public boolean isFishingLevelUpPromptVisible() {
                return isFishingLevelUpPromptVisible.getAsBoolean();
            }

            @Override
            public boolean dismissFishingLevelUpPrompt() {
                return dismissFishingLevelUpPrompt.getAsBoolean();
            }

            @Override
            public void clearFishingOutcomeWaitWindow() {
                clearFishingOutcomeWaitWindow.run();
            }

            @Override
            public void clearFishingTargetAttempt() {
                clearFishingTargetAttempt.run();
            }

            @Override
            public Set<Integer> parsePreferredNpcIds(JsonElement targetNpcIdElement, JsonElement targetNpcIdsElement) {
                return parsePreferredNpcIds.apply(targetNpcIdElement, targetNpcIdsElement);
            }

            @Override
            public long fishingOutcomeWaitUntilMs() {
                return fishingOutcomeWaitUntilMs.getAsLong();
            }

            @Override
            public int fishingLastAttemptNpcIndex() {
                return fishingLastAttemptNpcIndex.getAsInt();
            }

            @Override
            public WorldPoint fishingLastAttemptWorldPoint() {
                return fishingLastAttemptWorldPoint.get();
            }

            @Override
            public long fishingApproachWaitUntilMs() {
                return fishingApproachWaitUntilMs.getAsLong();
            }

            @Override
            public Optional<NPC> resolveLockedFishingTarget(Set<Integer> preferredNpcIds) {
                return fishingTargetResolver.resolveLockedFishingTarget(preferredNpcIds);
            }

            @Override
            public Optional<NPC> resolveNearestFishingTarget(Player local, Set<Integer> preferredNpcIds) {
                return resolveNearestFishingTarget.apply(local, preferredNpcIds);
            }

            @Override
            public List<NPC> resolveNearestFishingTargets(Player local, Set<Integer> preferredNpcIds, int maxTargets) {
                return fishingTargetResolver.resolveNearestFishingTargets(local, preferredNpcIds, maxTargets);
            }

            @Override
            public void lockFishingTarget(NPC npc) {
                lockFishingTarget.accept(npc);
            }

            @Override
            public void clearFishingInteractionWindows() {
                clearFishingInteractionWindows.run();
            }

            @Override
            public void clearFishingInteractionWindowsPreserveDispatchSignal() {
                clearFishingInteractionWindowsPreserveDispatchSignal.run();
            }

            @Override
            public Point resolveNpcClickPoint(NPC npc) {
                return resolveNpcClickPoint.apply(npc);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public void clearFishingTargetLock() {
                clearFishingTargetLock.run();
            }

            @Override
            public void rememberInteractionAnchorForNpc(NPC npc, Point point) {
                rememberInteractionAnchorForNpc.accept(npc, point);
            }

            @Override
            public long fishingLastDispatchAtMs() {
                return fishingLastDispatchAtMs.getAsLong();
            }

            @Override
            public WorldPoint fishingLastDispatchWorldPoint() {
                return fishingLastDispatchWorldPoint.get();
            }

            @Override
            public int fishingLastDispatchNpcIndex() {
                return fishingLastDispatchNpcIndex.getAsInt();
            }

            @Override
            public long fishingSameTargetReclickCooldownMs() {
                return fishingSameTargetReclickCooldownMs.getAsLong();
            }

            @Override
            public MotorDispatchResult dispatchFishingMoveAndClick(Point canvasPoint, ClickMotionSettings motion) {
                MotorHandle handle = scheduleMotorGesture.schedule(
                    CanvasPoint.fromAwtPoint(canvasPoint),
                    MotorGestureType.MOVE_AND_CLICK,
                    buildFishingMoveAndClickProfile.apply(motion)
                );
                if (handle == null) {
                    return new MotorDispatchResult(0L, MotorDispatchStatus.FAILED, "missing_motor_handle");
                }
                return new MotorDispatchResult(
                    handle.id,
                    toMotorDispatchStatus(handle.status),
                    handle.reason
                );
            }

            @Override
            public void noteInteractionActivityNow() {
                noteInteractionActivityNow.run();
            }

            @Override
            public void noteFishingTargetAttempt(Player local, NPC targetNpc) {
                noteFishingTargetAttempt.accept(local, targetNpc);
            }

            @Override
            public void noteFishingDispatchAttempt(NPC targetNpc, long now) {
                noteFishingDispatchAttempt.accept(targetNpc, now);
            }

            @Override
            public void beginFishingOutcomeWaitWindow() {
                beginFishingOutcomeWaitWindow.run();
            }

            @Override
            public void incrementClicksDispatched() {
                incrementClicksDispatched.run();
            }

            @Override
            public com.xptool.core.runtime.FatigueSnapshot fatigueSnapshot() {
                FatigueSnapshot snapshot = fatigueSnapshot.get();
                if (snapshot == null) {
                    return com.xptool.core.runtime.FatigueSnapshot.neutral();
                }
                return com.xptool.core.runtime.FatigueSnapshot.fromLoad(snapshot.load01());
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
            public RuntimeDecision accept(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public RuntimeDecision reject(String reason) {
                return rejectDecision.apply(reason);
            }
        };
    }

    static FishingTargetResolver.Host createFishingTargetResolverHost(CommandExecutor executor, Client client) {
        return new FishingTargetResolver.Host() {
            @Override
            public int lockedFishingNpcIndex() {
                return executor.lockedFishingNpcIndexValue();
            }

            @Override
            public WorldPoint lockedFishingWorldPoint() {
                return executor.lockedFishingWorldPointValue();
            }

            @Override
            public net.runelite.api.WorldView topLevelWorldView() {
                return client.getTopLevelWorldView();
            }

            @Override
            public boolean isFishingSpotNpcCandidate(NPC npc) {
                return executor.isFishingSpotNpcCandidate(npc);
            }

            @Override
            public boolean worldPointsExactMatch(WorldPoint a, WorldPoint b) {
                return executor.worldPointsExactMatchForHost(a, b);
            }

            @Override
            public void clearFishingTargetLock() {
                executor.clearFishingTargetLock();
            }
        };
    }

    private static MotorDispatchStatus toMotorDispatchStatus(MotorGestureStatus status) {
        if (status == null) {
            return MotorDispatchStatus.FAILED;
        }
        switch (status) {
            case COMPLETE:
                return MotorDispatchStatus.COMPLETE;
            case SCHEDULED:
                return MotorDispatchStatus.SCHEDULED;
            case IN_FLIGHT:
                return MotorDispatchStatus.IN_FLIGHT;
            case FAILED:
                return MotorDispatchStatus.FAILED;
            case CANCELLED:
                return MotorDispatchStatus.CANCELLED;
            default:
                return MotorDispatchStatus.FAILED;
        }
    }
}
