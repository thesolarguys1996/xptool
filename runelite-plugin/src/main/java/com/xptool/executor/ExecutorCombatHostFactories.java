package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import java.awt.Point;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.NPC;

final class ExecutorCombatHostFactories {
    private ExecutorCombatHostFactories() {
    }

    static BiFunction<CombatTargetPolicy, CombatTargetResolver, BrutusCombatSystem.Host> createBrutusCombatHostFactory(
        CombatRuntime combatRuntime,
        Predicate<NPC> isAttackableNpc,
        Supplier<MotorProgram> activeMotorProgram,
        Runnable clearActiveMotorProgram,
        Function<String, String> normalizedMotorOwnerName,
        Supplier<String> interactionMotorOwner,
        BiConsumer<MotorProgram, String> cancelMotorProgram,
        Runnable clearPendingMouseMove,
        Predicate<Point> isCombatCanvasPointUsable,
        ExecutorCombatDomainWiring.MotorGestureScheduler scheduleMotorGesture,
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
        return (combatPolicy, combatResolver) -> ExecutorCombatDomainWiring.createBrutusCombatSystemHost(
            combatPolicy,
            combatResolver,
            combatRuntime,
            isAttackableNpc,
            activeMotorProgram,
            clearActiveMotorProgram,
            normalizedMotorOwnerName,
            interactionMotorOwner,
            cancelMotorProgram,
            clearPendingMouseMove,
            isCombatCanvasPointUsable,
            scheduleMotorGesture,
            buildCombatDodgeMoveAndClickProfile,
            safeString,
            currentExecutorTick,
            clearCombatOutcomeWaitWindow,
            clearCombatTargetAttempt,
            noteInteractionActivityNow,
            incrementClicksDispatched,
            details,
            hasCombatBoundary,
            combatRecenterMinCooldownMs,
            combatRecenterMaxCooldownMs,
            randomBetween,
            setCombatRecenterCooldownUntilMs,
            acceptDecision,
            rejectDecision
        );
    }

    static ExecutorServiceWiring.TriFunction<BrutusCombatSystem, CombatTargetPolicy, CombatTargetResolver, CombatCommandService.Host> createCombatCommandHostFactory(
        Client client,
        BooleanSupplier isDropSweepSessionActive,
        Runnable endDropSweepSession,
        Runnable extendCombatRetryWindow,
        ExecutorCombatDomainWiring.ResolveClickMotion resolveClickMotion,
        ExecutorCombatDomainWiring.ParsePreferredNpcIds parsePreferredNpcIds,
        IntFunction<String> resolvePreferredNpcNameHint,
        ExecutorCombatDomainWiring.CombatAnchorStaleChecker isCombatAnchorLikelyStale,
        ExecutorCombatDomainWiring.CombatBoundaryUpdater updateCombatBoundary,
        Runnable pruneCombatNpcSuppression,
        IntSupplier combatLastAttemptNpcIndex,
        LongSupplier combatOutcomeWaitUntilMs,
        LongPredicate isCombatPostOutcomeSettleGraceActive,
        ExecutorCombatDomainWiring.IntLongConsumer suppressCombatNpcTarget,
        LongSupplier combatTargetReclickCooldownMs,
        Runnable clearCombatTargetAttempt,
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
        return (brutusSystem, combatPolicy, combatResolver) -> ExecutorGameplayRuntimeWiring.createCombatCommandHost(
            client,
            brutusSystem,
            combatPolicy,
            combatResolver,
            isDropSweepSessionActive,
            endDropSweepSession,
            extendCombatRetryWindow,
            resolveClickMotion,
            parsePreferredNpcIds,
            resolvePreferredNpcNameHint,
            isCombatAnchorLikelyStale,
            updateCombatBoundary,
            pruneCombatNpcSuppression,
            brutusSystem::pruneDodgeTileSuppression,
            brutusSystem::updateDodgeProgressState,
            combatLastAttemptNpcIndex,
            combatOutcomeWaitUntilMs,
            isCombatPostOutcomeSettleGraceActive,
            suppressCombatNpcTarget,
            combatTargetReclickCooldownMs,
            clearCombatTargetAttempt,
            brutusSystem::maybeHandleDodge,
            brutusSystem::lastDodgeAtMs,
            brutusSystem::postDodgeHoldMs,
            resetCombatTargetUnavailableStreak,
            isAttackableNpc,
            clearCombatOutcomeWaitWindow,
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
}
