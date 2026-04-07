package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import java.awt.Point;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
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
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

final class ExecutorCombatDomainWiring {
    @FunctionalInterface
    interface MotorGestureScheduler {
        MotorHandle schedule(CanvasPoint point, MotorGestureType type, MotorProfile profile);
    }

    @FunctionalInterface
    interface ResolveNearestWalkableTarget {
        WorldPoint resolve(WorldPoint localWorldPoint, WorldPoint targetWorldPoint, int maxRadiusTiles);
    }

    @FunctionalInterface
    interface BrutusDodgeFromEatHandler {
        Optional<CommandExecutor.CommandDecision> handle(JsonObject payload, long now);
    }

    @FunctionalInterface
    interface BrutusNpcNearbyChecker {
        boolean isNearby(Player local, int scanRangeTiles);
    }

    @FunctionalInterface
    interface JsonIntReader {
        int read(JsonElement element, int fallback);
    }

    @FunctionalInterface
    interface ResolveClickMotion {
        ClickMotionSettings apply(JsonObject payload, MotionProfile motionProfile);
    }

    @FunctionalInterface
    interface ParsePreferredNpcIds {
        Set<Integer> parse(JsonElement targetNpcIdElement, JsonElement targetNpcIdsElement);
    }

    @FunctionalInterface
    interface CombatAnchorStaleChecker {
        boolean test(Player local, int targetWorldX, int targetWorldY, int targetMaxDistance);
    }

    @FunctionalInterface
    interface CombatBoundaryUpdater {
        void update(int targetWorldX, int targetWorldY, int targetMaxDistance);
    }

    @FunctionalInterface
    interface UpdateBrutusDodgeProgressState {
        void update(Player local, long now);
    }

    @FunctionalInterface
    interface IntLongConsumer {
        void accept(int value, long durationMs);
    }

    @FunctionalInterface
    interface MaybeHandleBrutusDodge {
        Optional<CommandExecutor.CommandDecision> handle(
            Player local,
            ClickMotionSettings motion,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance,
            int maxChaseDistance,
            long now
        );
    }

    @FunctionalInterface
    interface NpcPreferredTargetMatcher {
        boolean matches(NPC npc, int preferredNpcId, Set<Integer> preferredNpcIds, String preferredNpcNameHint);
    }

    @FunctionalInterface
    interface NpcWithinCombatAreaChecker {
        boolean isWithin(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance);
    }

    @FunctionalInterface
    interface NpcWithinChaseDistanceChecker {
        boolean isWithin(Player local, NPC npc, int maxChaseDistance);
    }

    @FunctionalInterface
    interface ResolveNpcTargetingLocal {
        Optional<NPC> resolve(
            Player local,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance,
            int maxChaseDistance,
            boolean brutusOnly
        );
    }

    @FunctionalInterface
    interface ResolveNearestCombatTarget {
        Optional<NPC> resolve(
            Player local,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance,
            int maxChaseDistance,
            boolean brutusOnly
        );
    }

    private ExecutorCombatDomainWiring() {
    }

    static WalkCommandService.Host createWalkCommandHost(
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<JsonObject, MotionProfile, ClickMotionSettings> resolveClickMotion,
        Supplier<Player> localPlayer,
        Function<WorldPoint, Point> resolveWorldTileClickPoint,
        Function<WorldPoint, Point> resolveWorldTileMinimapClickPoint,
        ResolveNearestWalkableTarget resolveNearestWalkableTarget,
        Predicate<Point> isUsableCanvasPoint,
        MotorGestureScheduler scheduleMotorGesture,
        Function<ClickMotionSettings, MotorProfile> buildWalkMoveAndClickProfile,
        Runnable noteInteractionActivityNow,
        Runnable incrementClicksDispatched
    ) {
        return new WalkCommandService.Host() {
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
            public String safeString(String value) {
                return safeString.apply(value);
            }

            @Override
            public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
                return resolveClickMotion.apply(payload, motionProfile);
            }

            @Override
            public Player localPlayer() {
                return localPlayer.get();
            }

            @Override
            public Point resolveWorldTileClickPoint(WorldPoint worldPoint) {
                return resolveWorldTileClickPoint.apply(worldPoint);
            }

            @Override
            public Point resolveWorldTileMinimapClickPoint(WorldPoint worldPoint) {
                return resolveWorldTileMinimapClickPoint.apply(worldPoint);
            }

            @Override
            public WorldPoint resolveNearestWalkableWorldPoint(
                WorldPoint localWorldPoint,
                WorldPoint targetWorldPoint,
                int maxRadiusTiles
            ) {
                return resolveNearestWalkableTarget.resolve(localWorldPoint, targetWorldPoint, maxRadiusTiles);
            }

            @Override
            public boolean isUsableCanvasPoint(Point point) {
                return isUsableCanvasPoint.test(point);
            }

            @Override
            public MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile) {
                return scheduleMotorGesture.schedule(point, type, profile);
            }

            @Override
            public MotorProfile buildWalkMoveAndClickProfile(ClickMotionSettings motion) {
                return buildWalkMoveAndClickProfile.apply(motion);
            }

            @Override
            public void noteInteractionActivityNow() {
                noteInteractionActivityNow.run();
            }

            @Override
            public void incrementClicksDispatched() {
                incrementClicksDispatched.run();
            }
        };
    }

    static BrutusCombatSystem.Host createBrutusCombatSystemHost(
        CombatTargetPolicy combatTargetPolicy,
        CombatTargetResolver combatTargetResolver,
        CombatRuntime combatRuntime,
        Predicate<NPC> isAttackableNpc,
        Supplier<MotorProgram> activeMotorProgram,
        Runnable clearActiveMotorProgram,
        Function<String, String> normalizedMotorOwnerName,
        Supplier<String> interactionMotorOwner,
        BiConsumer<MotorProgram, String> cancelMotorProgram,
        Runnable clearPendingMouseMove,
        Predicate<Point> isCombatCanvasPointUsable,
        MotorGestureScheduler scheduleMotorGesture,
        Function<ClickMotionSettings, MotorProfile> buildCombatDodgeMoveAndClickProfile,
        Function<String, String> safeString,
        IntSupplier currentExecutorTick,
        Runnable clearCombatOutcomeWaitWindow,
        Runnable clearCombatTargetAttempt,
        Runnable noteInteractionActivityNow,
        Runnable incrementClicksDispatched,
        Function<Object[], JsonObject> details,
        BooleanSupplier hasCombatBoundary,
        LongSupplier combatRecenterMinCooldownMs,
        LongSupplier combatRecenterMaxCooldownMs,
        LongBinaryOperator randomBetween,
        LongConsumer setCombatRecenterCooldownUntilMs,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision
    ) {
        return new BrutusCombatSystem.Host() {
            @Override
            public boolean isAttackableNpc(NPC npc) {
                return isAttackableNpc.test(npc);
            }

            @Override
            public boolean npcMatchesPreferredTarget(
                NPC npc,
                int preferredNpcId,
                Set<Integer> preferredNpcIds,
                String preferredNpcNameHint
            ) {
                return combatTargetPolicy.npcMatchesPreferredTarget(npc, preferredNpcId, preferredNpcIds, preferredNpcNameHint);
            }

            @Override
            public boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return combatTargetPolicy.isNpcWithinCombatArea(npc, targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance) {
                return combatTargetPolicy.isNpcWithinCombatChaseDistance(local, npc, maxChaseDistance);
            }

            @Override
            public Optional<NPC> resolveNearestCombatTarget(
                Player local,
                int preferredNpcId,
                Set<Integer> preferredNpcIds,
                String preferredNpcNameHint,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance,
                int maxChaseDistance,
                boolean requireAttackable
            ) {
                return combatTargetResolver.resolveNearestCombatTarget(
                    local,
                    preferredNpcId,
                    preferredNpcIds,
                    preferredNpcNameHint,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance,
                    maxChaseDistance,
                    requireAttackable
                );
            }

            @Override
            public MotorProgram activeMotorProgram() {
                return activeMotorProgram.get();
            }

            @Override
            public void clearActiveMotorProgram() {
                clearActiveMotorProgram.run();
            }

            @Override
            public String normalizedMotorOwnerName(String owner) {
                return normalizedMotorOwnerName.apply(owner);
            }

            @Override
            public String interactionMotorOwner() {
                return interactionMotorOwner.get();
            }

            @Override
            public void cancelMotorProgram(MotorProgram program, String reason) {
                cancelMotorProgram.accept(program, reason);
            }

            @Override
            public void clearPendingMouseMove() {
                clearPendingMouseMove.run();
            }

            @Override
            public boolean isCombatCanvasPointUsable(Point point) {
                return isCombatCanvasPointUsable.test(point);
            }

            @Override
            public MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile) {
                return scheduleMotorGesture.schedule(point, type, profile);
            }

            @Override
            public MotorProfile buildCombatDodgeMoveAndClickProfile(ClickMotionSettings motion) {
                return buildCombatDodgeMoveAndClickProfile.apply(motion);
            }

            @Override
            public String safeString(String value) {
                return safeString.apply(value);
            }

            @Override
            public int currentExecutorTick() {
                return currentExecutorTick.getAsInt();
            }

            @Override
            public void incrementCombatTargetUnavailableStreak() {
                combatRuntime.incrementTargetUnavailableStreak();
            }

            @Override
            public int combatTargetUnavailableStreak() {
                return combatRuntime.targetUnavailableStreak();
            }

            @Override
            public void resetCombatTargetUnavailableStreak() {
                combatRuntime.resetTargetUnavailableStreak();
            }

            @Override
            public void clearCombatOutcomeWaitWindow() {
                clearCombatOutcomeWaitWindow.run();
            }

            @Override
            public void clearCombatTargetAttempt() {
                clearCombatTargetAttempt.run();
            }

            @Override
            public void noteInteractionActivityNow() {
                noteInteractionActivityNow.run();
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
            public long combatRecenterCooldownUntilMs() {
                return combatRuntime.recenterCooldownUntilMs();
            }

            @Override
            public boolean hasCombatBoundary() {
                return hasCombatBoundary.getAsBoolean();
            }

            @Override
            public int combatBoundaryCenterX() {
                return combatRuntime.boundaryCenterX();
            }

            @Override
            public int combatBoundaryCenterY() {
                return combatRuntime.boundaryCenterY();
            }

            @Override
            public int combatBoundaryRadiusTiles() {
                return combatRuntime.boundaryRadiusTiles();
            }

            @Override
            public long combatRecenterMinCooldownMs() {
                return combatRecenterMinCooldownMs.getAsLong();
            }

            @Override
            public long combatRecenterMaxCooldownMs() {
                return combatRecenterMaxCooldownMs.getAsLong();
            }

            @Override
            public long randomBetween(long minInclusive, long maxInclusive) {
                return randomBetween.applyAsLong(minInclusive, maxInclusive);
            }

            @Override
            public void setCombatRecenterCooldownUntilMs(long atMs) {
                setCombatRecenterCooldownUntilMs.accept(atMs);
            }

            @Override
            public CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject detailsJson) {
                return acceptDecision.apply(reason, detailsJson);
            }

            @Override
            public CommandExecutor.CommandDecision rejectDecision(String reason) {
                return rejectDecision.apply(reason);
            }
        };
    }

    static CombatCommandService.Host createCombatCommandHost(
        BooleanSupplier isDropSweepSessionActive,
        Runnable endDropSweepSession,
        Runnable extendCombatRetryWindow,
        ResolveClickMotion resolveClickMotion,
        ParsePreferredNpcIds parsePreferredNpcIds,
        IntFunction<String> resolvePreferredNpcNameHint,
        Supplier<String> combatEncounterProfileBrutus,
        Supplier<Player> currentPlayer,
        CombatAnchorStaleChecker isCombatAnchorLikelyStale,
        CombatBoundaryUpdater updateCombatBoundary,
        Runnable pruneCombatNpcSuppression,
        LongConsumer pruneBrutusDodgeTileSuppression,
        UpdateBrutusDodgeProgressState updateBrutusDodgeProgressState,
        IntSupplier combatLastAttemptNpcIndex,
        LongSupplier combatOutcomeWaitUntilMs,
        LongPredicate isCombatPostOutcomeSettleGraceActive,
        IntLongConsumer suppressCombatNpcTarget,
        LongSupplier combatTargetReclickCooldownMs,
        Runnable clearCombatTargetAttempt,
        MaybeHandleBrutusDodge maybeHandleBrutusDodge,
        LongSupplier brutusLastDodgeAtMs,
        LongSupplier combatBrutusPostDodgeHoldMs,
        Runnable resetCombatTargetUnavailableStreak,
        Predicate<NPC> isBrutusNpc,
        Predicate<NPC> isAttackableNpc,
        NpcPreferredTargetMatcher npcMatchesPreferredTarget,
        NpcWithinCombatAreaChecker isNpcWithinCombatArea,
        NpcWithinChaseDistanceChecker isNpcWithinCombatChaseDistance,
        Runnable clearCombatOutcomeWaitWindow,
        ResolveNpcTargetingLocal resolveNpcTargetingLocal,
        ResolveNearestCombatTarget resolveNearestCombatTarget,
        Function<NPC, Point> resolveNpcClickPoint,
        Predicate<Point> isCombatCanvasPointUsable,
        IntSupplier combatTargetClickFallbackAttempts,
        Runnable incrementCombatTargetUnavailableStreak,
        LongSupplier combatPostAttemptTargetSettleGraceMs,
        Runnable clearCombatInteractionWindows,
        IntSupplier combatSuppressedNpcCount,
        BiConsumer<NPC, Point> rememberInteractionAnchorForNpc,
        MotorGestureScheduler scheduleMotorGesture,
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
        return new CombatCommandService.Host() {
            @Override
            public boolean isDropSweepSessionActive() {
                return isDropSweepSessionActive.getAsBoolean();
            }

            @Override
            public void endDropSweepSession() {
                endDropSweepSession.run();
            }

            @Override
            public void extendCombatRetryWindow() {
                extendCombatRetryWindow.run();
            }

            @Override
            public ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
                return resolveClickMotion.apply(payload, motionProfile);
            }

            @Override
            public Set<Integer> parsePreferredNpcIds(JsonElement targetNpcIdElement, JsonElement targetNpcIdsElement) {
                return parsePreferredNpcIds.parse(targetNpcIdElement, targetNpcIdsElement);
            }

            @Override
            public String resolvePreferredNpcNameHint(int preferredNpcId) {
                return resolvePreferredNpcNameHint.apply(preferredNpcId);
            }

            @Override
            public String combatEncounterProfileBrutus() {
                return combatEncounterProfileBrutus.get();
            }

            @Override
            public Player currentPlayer() {
                return currentPlayer.get();
            }

            @Override
            public boolean isCombatAnchorLikelyStale(Player local, int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return isCombatAnchorLikelyStale.test(local, targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public void updateCombatBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance) {
                updateCombatBoundary.update(targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public void pruneCombatNpcSuppression() {
                pruneCombatNpcSuppression.run();
            }

            @Override
            public void pruneBrutusDodgeTileSuppression(long now) {
                pruneBrutusDodgeTileSuppression.accept(now);
            }

            @Override
            public void updateBrutusDodgeProgressState(Player local, long now) {
                updateBrutusDodgeProgressState.update(local, now);
            }

            @Override
            public int combatLastAttemptNpcIndex() {
                return combatLastAttemptNpcIndex.getAsInt();
            }

            @Override
            public long combatOutcomeWaitUntilMs() {
                return combatOutcomeWaitUntilMs.getAsLong();
            }

            @Override
            public boolean isCombatPostOutcomeSettleGraceActive(long now) {
                return isCombatPostOutcomeSettleGraceActive.test(now);
            }

            @Override
            public void suppressCombatNpcTarget(int npcIndex, long durationMs) {
                suppressCombatNpcTarget.accept(npcIndex, durationMs);
            }

            @Override
            public long combatTargetReclickCooldownMs() {
                return combatTargetReclickCooldownMs.getAsLong();
            }

            @Override
            public void clearCombatTargetAttempt() {
                clearCombatTargetAttempt.run();
            }

            @Override
            public Optional<CommandExecutor.CommandDecision> maybeHandleBrutusDodge(
                Player local,
                ClickMotionSettings motion,
                int preferredNpcId,
                Set<Integer> preferredNpcIds,
                String preferredNpcNameHint,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance,
                int maxChaseDistance,
                long now
            ) {
                return maybeHandleBrutusDodge.handle(
                    local,
                    motion,
                    preferredNpcId,
                    preferredNpcIds,
                    preferredNpcNameHint,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance,
                    maxChaseDistance,
                    now
                );
            }

            @Override
            public long brutusLastDodgeAtMs() {
                return brutusLastDodgeAtMs.getAsLong();
            }

            @Override
            public long combatBrutusPostDodgeHoldMs() {
                return combatBrutusPostDodgeHoldMs.getAsLong();
            }

            @Override
            public void resetCombatTargetUnavailableStreak() {
                resetCombatTargetUnavailableStreak.run();
            }

            @Override
            public boolean isBrutusNpc(NPC npc) {
                return isBrutusNpc.test(npc);
            }

            @Override
            public boolean isAttackableNpc(NPC npc) {
                return isAttackableNpc.test(npc);
            }

            @Override
            public boolean npcMatchesPreferredTarget(
                NPC npc,
                int preferredNpcId,
                Set<Integer> preferredNpcIds,
                String preferredNpcNameHint
            ) {
                return npcMatchesPreferredTarget.matches(npc, preferredNpcId, preferredNpcIds, preferredNpcNameHint);
            }

            @Override
            public boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return isNpcWithinCombatArea.isWithin(npc, targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance) {
                return isNpcWithinCombatChaseDistance.isWithin(local, npc, maxChaseDistance);
            }

            @Override
            public void clearCombatOutcomeWaitWindow() {
                clearCombatOutcomeWaitWindow.run();
            }

            @Override
            public Optional<NPC> resolveNpcTargetingLocal(
                Player local,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance,
                int maxChaseDistance,
                boolean brutusOnly
            ) {
                return resolveNpcTargetingLocal.resolve(
                    local,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance,
                    maxChaseDistance,
                    brutusOnly
                );
            }

            @Override
            public Optional<NPC> resolveNearestCombatTarget(
                Player local,
                int preferredNpcId,
                Set<Integer> preferredNpcIds,
                String preferredNpcNameHint,
                int targetWorldX,
                int targetWorldY,
                int targetMaxDistance,
                int maxChaseDistance,
                boolean brutusOnly
            ) {
                return resolveNearestCombatTarget.resolve(
                    local,
                    preferredNpcId,
                    preferredNpcIds,
                    preferredNpcNameHint,
                    targetWorldX,
                    targetWorldY,
                    targetMaxDistance,
                    maxChaseDistance,
                    brutusOnly
                );
            }

            @Override
            public Point resolveNpcClickPoint(NPC npc) {
                return resolveNpcClickPoint.apply(npc);
            }

            @Override
            public boolean isCombatCanvasPointUsable(Point point) {
                return isCombatCanvasPointUsable.test(point);
            }

            @Override
            public int combatTargetClickFallbackAttempts() {
                return combatTargetClickFallbackAttempts.getAsInt();
            }

            @Override
            public void incrementCombatTargetUnavailableStreak() {
                incrementCombatTargetUnavailableStreak.run();
            }

            @Override
            public long combatPostAttemptTargetSettleGraceMs() {
                return combatPostAttemptTargetSettleGraceMs.getAsLong();
            }

            @Override
            public void clearCombatInteractionWindows() {
                clearCombatInteractionWindows.run();
            }

            @Override
            public int combatSuppressedNpcCount() {
                return combatSuppressedNpcCount.getAsInt();
            }

            @Override
            public void rememberInteractionAnchorForNpc(NPC npc, Point fallbackCanvasPoint) {
                rememberInteractionAnchorForNpc.accept(npc, fallbackCanvasPoint);
            }

            @Override
            public MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile) {
                return scheduleMotorGesture.schedule(point, type, profile);
            }

            @Override
            public MotorProfile buildCombatMoveAndClickProfile(ClickMotionSettings motion) {
                return buildCombatMoveAndClickProfile.apply(motion);
            }

            @Override
            public void noteInteractionActivityNow() {
                noteInteractionActivityNow.run();
            }

            @Override
            public void noteCombatTargetAttempt(NPC npc) {
                noteCombatTargetAttempt.accept(npc);
            }

            @Override
            public void beginCombatOutcomeWaitWindow() {
                beginCombatOutcomeWaitWindow.run();
            }

            @Override
            public void incrementClicksDispatched() {
                incrementClicksDispatched.run();
            }

            @Override
            public long combatContestedTargetSuppressionMs() {
                return combatContestedTargetSuppressionMs.getAsLong();
            }

            @Override
            public long randomBetween(long minInclusive, long maxInclusive) {
                return randomBetween.applyAsLong(minInclusive, maxInclusive);
            }

            @Override
            public FatigueSnapshot fatigueSnapshot() {
                return fatigueSnapshot.get();
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

    static InteractionCommandService.Host createInteractionCommandHost(
        Function<String, CommandExecutor.CommandDecision> rejectDecision,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<Object[], JsonObject> details,
        Supplier<Player> localPlayer,
        BrutusDodgeFromEatHandler maybeHandleBrutusDodgeFromEat,
        IntSupplier playerEatAnimationId,
        LongSupplier combatLastEatDispatchAtMs,
        LongConsumer setCombatLastEatDispatchAtMs,
        LongSupplier combatEatDispatchMinIntervalMs,
        Function<Player, String> detectNearbyBrutusTelegraphName,
        IntSupplier currentExecutorTick,
        IntSupplier brutusLastTelegraphTick,
        IntSupplier combatBrutusEatPriorityWindowTicks,
        LongSupplier brutusLastDodgeAtMs,
        LongSupplier combatBrutusPostDodgeHoldMs,
        IntSupplier brutusLastNoSafeTileTick,
        BooleanSupplier isBrutusNoSafeTilePressureActive,
        LongPredicate isBrutusDodgeProgressActive,
        IntSupplier targetUnavailableStreak,
        IntSupplier combatRecenterUnavailableStreakMin,
        BrutusNpcNearbyChecker isBrutusNpcNearby,
        IntSupplier combatBrutusNearbyScanRangeTiles,
        IntSupplier brutusNoSafeTileStreak,
        IntSupplier combatBrutusNoSafeTileRecoveryWindowTicks,
        JsonIntReader asInt,
        java.util.function.IntFunction<Optional<Integer>> findInventorySlot,
        BooleanSupplier canPerformMotorActionNow,
        IntPredicate clickInventorySlot,
        BooleanSupplier nudgeCameraYawLeft,
        BooleanSupplier nudgeCameraYawRight,
        BooleanSupplier nudgeCameraPitchUp,
        BooleanSupplier nudgeCameraPitchDown,
        Supplier<JsonObject> cameraLastNudgeDetails
    ) {
        return new InteractionCommandService.Host() {
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
            public Player localPlayer() {
                return localPlayer.get();
            }

            @Override
            public Optional<CommandExecutor.CommandDecision> maybeHandleBrutusDodgeFromEat(JsonObject payload, long now) {
                return maybeHandleBrutusDodgeFromEat.handle(payload, now);
            }

            @Override
            public int playerEatAnimationId() {
                return playerEatAnimationId.getAsInt();
            }

            @Override
            public long combatLastEatDispatchAtMs() {
                return combatLastEatDispatchAtMs.getAsLong();
            }

            @Override
            public void setCombatLastEatDispatchAtMs(long atMs) {
                setCombatLastEatDispatchAtMs.accept(atMs);
            }

            @Override
            public long combatEatDispatchMinIntervalMs() {
                return combatEatDispatchMinIntervalMs.getAsLong();
            }

            @Override
            public String detectNearbyBrutusTelegraphName(Player local) {
                return detectNearbyBrutusTelegraphName.apply(local);
            }

            @Override
            public int currentExecutorTick() {
                return currentExecutorTick.getAsInt();
            }

            @Override
            public int brutusLastTelegraphTick() {
                return brutusLastTelegraphTick.getAsInt();
            }

            @Override
            public int combatBrutusEatPriorityWindowTicks() {
                return combatBrutusEatPriorityWindowTicks.getAsInt();
            }

            @Override
            public long brutusLastDodgeAtMs() {
                return brutusLastDodgeAtMs.getAsLong();
            }

            @Override
            public long combatBrutusPostDodgeHoldMs() {
                return combatBrutusPostDodgeHoldMs.getAsLong();
            }

            @Override
            public int brutusLastNoSafeTileTick() {
                return brutusLastNoSafeTileTick.getAsInt();
            }

            @Override
            public boolean isBrutusNoSafeTilePressureActive() {
                return isBrutusNoSafeTilePressureActive.getAsBoolean();
            }

            @Override
            public boolean isBrutusDodgeProgressActive(long now) {
                return isBrutusDodgeProgressActive.test(now);
            }

            @Override
            public int targetUnavailableStreak() {
                return targetUnavailableStreak.getAsInt();
            }

            @Override
            public int combatRecenterUnavailableStreakMin() {
                return combatRecenterUnavailableStreakMin.getAsInt();
            }

            @Override
            public boolean isBrutusNpcNearby(Player local, int scanRangeTiles) {
                return isBrutusNpcNearby.isNearby(local, scanRangeTiles);
            }

            @Override
            public int combatBrutusNearbyScanRangeTiles() {
                return combatBrutusNearbyScanRangeTiles.getAsInt();
            }

            @Override
            public int brutusNoSafeTileStreak() {
                return brutusNoSafeTileStreak.getAsInt();
            }

            @Override
            public int combatBrutusNoSafeTileRecoveryWindowTicks() {
                return combatBrutusNoSafeTileRecoveryWindowTicks.getAsInt();
            }

            @Override
            public int asInt(JsonElement element, int fallback) {
                return asInt.read(element, fallback);
            }

            @Override
            public Optional<Integer> findInventorySlot(int itemId) {
                return findInventorySlot.apply(itemId);
            }

            @Override
            public boolean canPerformMotorActionNow() {
                return canPerformMotorActionNow.getAsBoolean();
            }

            @Override
            public boolean clickInventorySlot(int slot) {
                return clickInventorySlot.test(slot);
            }

            @Override
            public boolean nudgeCameraYawLeft() {
                return nudgeCameraYawLeft.getAsBoolean();
            }

            @Override
            public boolean nudgeCameraYawRight() {
                return nudgeCameraYawRight.getAsBoolean();
            }

            @Override
            public boolean nudgeCameraPitchUp() {
                return nudgeCameraPitchUp.getAsBoolean();
            }

            @Override
            public boolean nudgeCameraPitchDown() {
                return nudgeCameraPitchDown.getAsBoolean();
            }

            @Override
            public JsonObject cameraLastNudgeDetails() {
                return cameraLastNudgeDetails.get();
            }
        };
    }

    static NpcContextMenuTestService.Host createNpcContextMenuTestHost(
        Supplier<Player> localPlayer,
        Supplier<Iterable<NPC>> npcs,
        Function<NPC, Point> resolveVariedNpcClickPoint,
        Predicate<Point> isUsableCanvasPoint,
        Predicate<Point> moveInteractionCursorToCanvasPoint,
        Predicate<Point> isCursorNearTarget,
        BiPredicate<NPC, String[]> isTopMenuOptionOnNpc,
        Predicate<Point> clickPrimaryAt,
        BiPredicate<Point, String[]> selectContextMenuOptionAt,
        Function<Object[], JsonObject> details,
        Function<String, String> safeString,
        BiFunction<String, JsonObject, CommandExecutor.CommandDecision> acceptDecision,
        Function<String, CommandExecutor.CommandDecision> rejectDecision
    ) {
        return new NpcContextMenuTestService.Host() {
            @Override
            public Player localPlayer() {
                return localPlayer.get();
            }

            @Override
            public Iterable<NPC> npcs() {
                return npcs.get();
            }

            @Override
            public Point resolveVariedNpcClickPoint(NPC npc) {
                return resolveVariedNpcClickPoint.apply(npc);
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
            public boolean isTopMenuOptionOnNpc(NPC npc, String... optionKeywords) {
                return isTopMenuOptionOnNpc.test(npc, optionKeywords);
            }

            @Override
            public boolean clickPrimaryAt(Point canvasPoint) {
                return clickPrimaryAt.test(canvasPoint);
            }

            @Override
            public boolean selectContextMenuOptionAt(Point canvasPoint, String... optionKeywords) {
                return selectContextMenuOptionAt.test(canvasPoint, optionKeywords);
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

    static CombatTargetPolicy.Host createCombatTargetPolicyHost(CommandExecutor executor, Client client) {
        return new CombatTargetPolicy.Host() {
            @Override
            public Player localPlayer() {
                return client.getLocalPlayer();
            }

            @Override
            public boolean isBrutusNpc(NPC npc) {
                return executor.isBrutusNpcFromCombatSystem(npc);
            }
        };
    }

    static CombatTargetResolver.Host createCombatTargetResolverHost(
        CommandExecutor executor,
        Client client,
        CombatTargetPolicy combatPolicy
    ) {
        return new CombatTargetResolver.Host() {
            @Override
            public net.runelite.api.WorldView topLevelWorldView() {
                return client.getTopLevelWorldView();
            }

            @Override
            public boolean isCombatNpcSuppressed(int npcIndex) {
                return executor.isCombatNpcSuppressed(npcIndex);
            }

            @Override
            public boolean npcMatchesPreferredTarget(
                NPC npc,
                int preferredNpcId,
                java.util.Set<Integer> preferredNpcIds,
                String preferredNpcNameHint
            ) {
                return combatPolicy.npcMatchesPreferredTarget(npc, preferredNpcId, preferredNpcIds, preferredNpcNameHint);
            }

            @Override
            public boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance) {
                return combatPolicy.isNpcWithinCombatArea(npc, targetWorldX, targetWorldY, targetMaxDistance);
            }

            @Override
            public boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance) {
                return combatPolicy.isNpcWithinCombatChaseDistance(local, npc, maxChaseDistance);
            }

            @Override
            public boolean isBrutusNpc(NPC npc) {
                return executor.isBrutusNpcFromCombatSystem(npc);
            }

            @Override
            public boolean isAttackableNpc(NPC npc) {
                return executor.isAttackableNpc(npc);
            }
        };
    }
}
