package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import java.awt.Point;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;

final class CombatCommandService {
    private static final int COMBAT_PRE_ATTACK_PAUSE_BASE_CHANCE_PERCENT = 3;
    private static final int COMBAT_PRE_ATTACK_PAUSE_FATIGUE_CHANCE_BONUS_MAX_PERCENT = 10;
    private static final long COMBAT_PRE_ATTACK_PAUSE_MIN_MS = 40L;
    private static final long COMBAT_PRE_ATTACK_PAUSE_MAX_MS = 140L;
    private static final int COMBAT_PRE_ATTACK_PAUSE_FATIGUE_EXTRA_MAX_MS = 80;
    private static final long COMBAT_PRE_ATTACK_PAUSE_ROLL_MIN_GAP_MS = 120L;
    private static final long COMBAT_PRE_ATTACK_PAUSE_ROLL_MAX_GAP_MS = 320L;
    private static final long COMBAT_TARGET_READY_HOLD_MIN_MS = 35L;
    private static final long COMBAT_TARGET_READY_HOLD_MAX_MS = 95L;
    private static final double COMBAT_RECLICK_JITTER_MIN_SCALE = 0.84;
    private static final double COMBAT_RECLICK_JITTER_MAX_SCALE = 1.18;
    private static final long COMBAT_RECLICK_COOLDOWN_MIN_MS = 860L;

    interface Host {
        boolean isDropSweepSessionActive();

        void endDropSweepSession();

        void extendCombatRetryWindow();

        ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile);

        Set<Integer> parsePreferredNpcIds(JsonElement targetNpcIdElement, JsonElement targetNpcIdsElement);

        String resolvePreferredNpcNameHint(int preferredNpcId);

        String combatEncounterProfileBrutus();

        Player currentPlayer();

        boolean isCombatAnchorLikelyStale(Player local, int targetWorldX, int targetWorldY, int targetMaxDistance);

        void updateCombatBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance);

        void pruneCombatNpcSuppression();

        void pruneBrutusDodgeTileSuppression(long now);

        void updateBrutusDodgeProgressState(Player local, long now);

        int combatLastAttemptNpcIndex();

        long combatOutcomeWaitUntilMs();

        boolean isCombatPostOutcomeSettleGraceActive(long now);

        void suppressCombatNpcTarget(int npcIndex, long durationMs);

        long combatTargetReclickCooldownMs();

        void clearCombatTargetAttempt();

        Optional<CommandExecutor.CommandDecision> maybeHandleBrutusDodge(
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

        long brutusLastDodgeAtMs();

        long combatBrutusPostDodgeHoldMs();

        void resetCombatTargetUnavailableStreak();

        boolean isBrutusNpc(NPC npc);

        boolean isAttackableNpc(NPC npc);

        boolean npcMatchesPreferredTarget(
            NPC npc,
            int preferredNpcId,
            Set<Integer> preferredNpcIds,
            String preferredNpcNameHint
        );

        boolean isNpcWithinCombatArea(NPC npc, int targetWorldX, int targetWorldY, int targetMaxDistance);

        boolean isNpcWithinCombatChaseDistance(Player local, NPC npc, int maxChaseDistance);

        void clearCombatOutcomeWaitWindow();

        Optional<NPC> resolveNpcTargetingLocal(
            Player local,
            int targetWorldX,
            int targetWorldY,
            int targetMaxDistance,
            int maxChaseDistance,
            boolean brutusOnly
        );

        Optional<NPC> resolveNearestCombatTarget(
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

        Point resolveNpcClickPoint(NPC npc);

        boolean isCombatCanvasPointUsable(Point point);

        int combatTargetClickFallbackAttempts();

        void incrementCombatTargetUnavailableStreak();

        long combatPostAttemptTargetSettleGraceMs();

        void clearCombatInteractionWindows();

        int combatSuppressedNpcCount();

        void rememberInteractionAnchorForNpc(NPC npc, Point fallbackCanvasPoint);

        MotorHandle scheduleMotorGesture(CanvasPoint point, MotorGestureType type, MotorProfile profile);

        MotorProfile buildCombatMoveAndClickProfile(ClickMotionSettings motion);

        void noteInteractionActivityNow();

        void noteCombatTargetAttempt(NPC npc);

        void beginCombatOutcomeWaitWindow();

        void incrementClicksDispatched();

        long combatContestedTargetSuppressionMs();

        long randomBetween(long minInclusive, long maxInclusive);

        FatigueSnapshot fatigueSnapshot();

        JsonObject details(Object... kvPairs);

        String safeString(String value);

        CommandExecutor.CommandDecision accept(String reason, JsonObject details);

        CommandExecutor.CommandDecision reject(String reason);
    }

    private final Host host;
    private long preAttackPauseUntilMs = 0L;
    private long nextPreAttackPauseRollAtMs = 0L;
    private int readyTargetNpcIndex = -1;
    private long readyTargetSinceMs = Long.MIN_VALUE;
    private long readyTargetHoldMs = 0L;

    CombatCommandService(Host host) {
        this.host = host;
    }

    CommandExecutor.CommandDecision executeCombatAttackNearestNpc(JsonObject payload, MotionProfile motionProfile) {
        if (host.isDropSweepSessionActive()) {
            host.endDropSweepSession();
        }
        host.extendCombatRetryWindow();
        JsonObject safePayload = payload == null ? new JsonObject() : payload;
        ClickMotionSettings motion = host.resolveClickMotion(safePayload, motionProfile);
        Set<Integer> preferredNpcIds = host.parsePreferredNpcIds(
            safePayload.get("targetNpcId"),
            safePayload.get("targetNpcIds")
        );
        int preferredNpcId = asInt(safePayload.get("targetNpcId"), -1);
        if (preferredNpcId <= 0 && !preferredNpcIds.isEmpty()) {
            preferredNpcId = preferredNpcIds.iterator().next();
        }
        String preferredNpcNameHint = host.resolvePreferredNpcNameHint(preferredNpcId);
        String encounterProfile = safeString(asString(safePayload.get("encounterProfile"))).trim().toLowerCase(Locale.ROOT);
        boolean brutusEncounter = host.combatEncounterProfileBrutus().equals(encounterProfile);
        int targetWorldX = asInt(safePayload.get("targetWorldX"), -1);
        int targetWorldY = asInt(safePayload.get("targetWorldY"), -1);
        int targetMaxDistance = Math.max(1, asInt(safePayload.get("targetMaxDistance"), 18));
        int maxChaseDistance = Math.max(1, asInt(safePayload.get("maxChaseDistance"), 10));
        Player local = host.currentPlayer();
        if (local == null) {
            return host.reject("combat_player_unavailable");
        }
        if (host.isCombatAnchorLikelyStale(local, targetWorldX, targetWorldY, targetMaxDistance)) {
            targetWorldX = -1;
            targetWorldY = -1;
        }
        host.updateCombatBoundary(targetWorldX, targetWorldY, targetMaxDistance);
        long now = System.currentTimeMillis();
        host.pruneCombatNpcSuppression();
        host.pruneBrutusDodgeTileSuppression(now);
        host.updateBrutusDodgeProgressState(local, now);

        int animation = local.getAnimation();
        boolean animationActive = isAnimationActive(animation);
        if (host.combatLastAttemptNpcIndex() >= 0 && now > host.combatOutcomeWaitUntilMs()) {
            Actor currentInteracting = local.getInteracting();
            boolean stillInteractingWithAttempt = currentInteracting instanceof NPC
                && ((NPC) currentInteracting).getIndex() == host.combatLastAttemptNpcIndex();
            boolean postAttemptGraceActive = host.isCombatPostOutcomeSettleGraceActive(now);
            if (!stillInteractingWithAttempt && !animationActive && !postAttemptGraceActive) {
                long staleAttemptSuppressMs = variedCombatTargetReclickCooldownMs(
                    host.combatTargetReclickCooldownMs(),
                    now,
                    host.combatLastAttemptNpcIndex()
                );
                host.suppressCombatNpcTarget(host.combatLastAttemptNpcIndex(), staleAttemptSuppressMs);
            }
            if (!stillInteractingWithAttempt && !postAttemptGraceActive) {
                host.clearCombatTargetAttempt();
            }
        }

        if (brutusEncounter) {
            Optional<CommandExecutor.CommandDecision> brutusDecision = host.maybeHandleBrutusDodge(
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
            if (brutusDecision.isPresent()) {
                return brutusDecision.get();
            }
            long sinceLastDodgeMs = now - host.brutusLastDodgeAtMs();
            if (sinceLastDodgeMs >= 0L && sinceLastDodgeMs < host.combatBrutusPostDodgeHoldMs()) {
                host.resetCombatTargetUnavailableStreak();
                return host.accept(
                    "combat_brutus_post_dodge_hold",
                    host.details("waitMsRemaining", host.combatBrutusPostDodgeHoldMs() - sinceLastDodgeMs)
                );
            }
        }

        Actor interacting = local.getInteracting();
        if (interacting instanceof NPC) {
            NPC engagedNpc = (NPC) interacting;
            Actor engagedTarget = engagedNpc.getInteracting();
            boolean npcTargetsLocal = engagedTarget instanceof Player && engagedTarget == local;
            if ((npcTargetsLocal || animationActive)
                && (!brutusEncounter || host.isBrutusNpc(engagedNpc))
                && host.isAttackableNpc(engagedNpc)
                && host.npcMatchesPreferredTarget(engagedNpc, preferredNpcId, preferredNpcIds, preferredNpcNameHint)
                && host.isNpcWithinCombatArea(engagedNpc, targetWorldX, targetWorldY, targetMaxDistance)
                && host.isNpcWithinCombatChaseDistance(local, engagedNpc, maxChaseDistance)) {
                boolean contestedSuppressed = false;
                long contestedSuppressMs = 0L;
                if (!npcTargetsLocal) {
                    long reclickCooldownMs = variedCombatTargetReclickCooldownMs(
                        host.combatTargetReclickCooldownMs(),
                        now,
                        engagedNpc.getIndex()
                    );
                    contestedSuppressMs = Math.max(
                        0L,
                        Math.min(host.combatContestedTargetSuppressionMs(), reclickCooldownMs)
                    );
                    if (contestedSuppressMs > 0L) {
                        host.suppressCombatNpcTarget(engagedNpc.getIndex(), contestedSuppressMs);
                        contestedSuppressed = true;
                    }
                }
                host.clearCombatOutcomeWaitWindow();
                host.clearCombatTargetAttempt();
                host.resetCombatTargetUnavailableStreak();
                return host.accept(
                    "combat_target_already_engaged",
                    host.details(
                        "npcId", engagedNpc.getId(),
                        "npcIndex", engagedNpc.getIndex(),
                        "npcTargetsLocal", npcTargetsLocal,
                        "contestedSuppressed", contestedSuppressed,
                        "contestedSuppressMs", contestedSuppressMs,
                        "targetNpcId", preferredNpcId,
                        "targetWorldX", targetWorldX,
                        "targetWorldY", targetWorldY,
                        "targetMaxDistance", targetMaxDistance,
                        "maxChaseDistance", maxChaseDistance
                    )
                );
            }
        }

        Optional<NPC> inboundAttacker = host.resolveNpcTargetingLocal(
            local,
            targetWorldX,
            targetWorldY,
            targetMaxDistance,
            maxChaseDistance,
            brutusEncounter
        );
        if (inboundAttacker.isPresent()) {
            NPC engagedNpc = inboundAttacker.get();
            host.clearCombatOutcomeWaitWindow();
            host.clearCombatTargetAttempt();
            host.resetCombatTargetUnavailableStreak();
            return host.accept(
                "combat_target_already_engaged",
                host.details(
                    "npcId", engagedNpc.getId(),
                    "npcIndex", engagedNpc.getIndex(),
                    "npcTargetsLocal", true,
                    "detectedBy", "npc_targeting_local_scan",
                    "targetNpcId", preferredNpcId,
                    "targetWorldX", targetWorldX,
                    "targetWorldY", targetWorldY,
                    "targetMaxDistance", targetMaxDistance,
                    "maxChaseDistance", maxChaseDistance
                )
            );
        }

        if (animationActive) {
            host.clearCombatOutcomeWaitWindow();
            host.clearCombatTargetAttempt();
            host.resetCombatTargetUnavailableStreak();
            return host.accept("combat_busy_animation_active", host.details("animation", animation));
        }

        if (now <= host.combatOutcomeWaitUntilMs()) {
            host.resetCombatTargetUnavailableStreak();
            return host.accept(
                "combat_waiting_outcome_window",
                host.details("waitMsRemaining", Math.max(0L, host.combatOutcomeWaitUntilMs() - now))
            );
        }

        NPC targetNpc = null;
        Point targetCanvas = null;
        int unclickableCandidates = 0;
        int attempts = 0;
        int attemptBudget = resolveFallbackAttemptBudget();
        while (attempts < attemptBudget) {
            Optional<NPC> candidate = host.resolveNearestCombatTarget(
                local,
                preferredNpcId,
                preferredNpcIds,
                preferredNpcNameHint,
                targetWorldX,
                targetWorldY,
                targetMaxDistance,
                maxChaseDistance,
                brutusEncounter
            );
            if (candidate.isEmpty()) {
                break;
            }
            NPC npc = candidate.get();
            Point canvas = host.resolveNpcClickPoint(npc);
            if (canvas != null && host.isCombatCanvasPointUsable(canvas)) {
                targetNpc = npc;
                targetCanvas = canvas;
                break;
            }
            long unclickableSuppressMs = variedCombatTargetReclickCooldownMs(
                host.combatTargetReclickCooldownMs(),
                now,
                npc.getIndex()
            );
            host.suppressCombatNpcTarget(npc.getIndex(), unclickableSuppressMs);
            unclickableCandidates++;
            attempts++;
        }
        if (targetNpc == null || targetCanvas == null) {
            clearReadyTargetState();
            host.incrementCombatTargetUnavailableStreak();
            if (host.isCombatPostOutcomeSettleGraceActive(now) && unclickableCandidates == 0 && attempts == 0) {
                long waitMsRemaining = Math.max(
                    0L,
                    (host.combatOutcomeWaitUntilMs() + host.combatPostAttemptTargetSettleGraceMs()) - now
                );
                return host.accept(
                    "combat_waiting_target_settle_window",
                    host.details(
                        "waitMsRemaining", waitMsRemaining,
                        "targetNpcId", preferredNpcId,
                        "targetWorldX", targetWorldX,
                        "targetWorldY", targetWorldY,
                        "targetMaxDistance", targetMaxDistance,
                        "maxChaseDistance", maxChaseDistance,
                        "suppressedNpcCount", host.combatSuppressedNpcCount(),
                        "unclickableCandidates", unclickableCandidates,
                        "fallbackAttempts", attempts,
                        "fallbackAttemptBudget", attemptBudget
                    )
                );
            }
            host.clearCombatInteractionWindows();
            return host.accept(
                "combat_target_unavailable",
                host.details(
                    "targetNpcId", preferredNpcId,
                    "targetWorldX", targetWorldX,
                    "targetWorldY", targetWorldY,
                    "targetMaxDistance", targetMaxDistance,
                    "maxChaseDistance", maxChaseDistance,
                    "suppressedNpcCount", host.combatSuppressedNpcCount(),
                    "unclickableCandidates", unclickableCandidates,
                    "fallbackAttempts", attempts,
                    "fallbackAttemptBudget", attemptBudget
                )
            );
        }
        host.rememberInteractionAnchorForNpc(targetNpc, targetCanvas);
        host.resetCombatTargetUnavailableStreak();
        Optional<CommandExecutor.CommandDecision> preAttackPauseDecision =
            maybeDeferForFatigueCadence(now, brutusEncounter, targetNpc);
        if (preAttackPauseDecision.isPresent()) {
            return preAttackPauseDecision.get();
        }
        Optional<CommandExecutor.CommandDecision> readyHoldDecision =
            maybeDeferForTargetReadyHold(targetNpc, now, targetWorldX, targetWorldY, targetMaxDistance, maxChaseDistance);
        if (readyHoldDecision.isPresent()) {
            return readyHoldDecision.get();
        }

        Point refreshedCanvas = host.resolveNpcClickPoint(targetNpc);
        if (refreshedCanvas != null && host.isCombatCanvasPointUsable(refreshedCanvas)) {
            targetCanvas = refreshedCanvas;
            host.rememberInteractionAnchorForNpc(targetNpc, targetCanvas);
        }

        int targetId = targetNpc.getId();
        MotorHandle handle = host.scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(targetCanvas),
            MotorGestureType.MOVE_AND_CLICK,
            host.buildCombatMoveAndClickProfile(motion)
        );
        if (handle.status == MotorGestureStatus.COMPLETE) {
            clearReadyTargetState();
            host.noteInteractionActivityNow();
            host.noteCombatTargetAttempt(targetNpc);
            host.beginCombatOutcomeWaitWindow();
            host.incrementClicksDispatched();
            return host.accept(
                "combat_left_click_dispatched",
                host.details(
                    "target", "attackable_npc",
                    "npcId", targetId,
                    "npcIndex", targetNpc.getIndex(),
                    "targetNpcId", preferredNpcId,
                    "targetWorldX", targetWorldX,
                    "targetWorldY", targetWorldY,
                    "targetMaxDistance", targetMaxDistance,
                    "maxChaseDistance", maxChaseDistance,
                    "motorGestureId", handle.id
                )
            );
        }
        if (handle.status == MotorGestureStatus.FAILED || handle.status == MotorGestureStatus.CANCELLED) {
            clearReadyTargetState();
            long failedDispatchSuppressMs = variedCombatTargetReclickCooldownMs(
                host.combatTargetReclickCooldownMs(),
                now,
                targetNpc.getIndex()
            );
            host.suppressCombatNpcTarget(targetNpc.getIndex(), failedDispatchSuppressMs);
            host.clearCombatInteractionWindows();
            host.clearCombatTargetAttempt();
            return host.reject("combat_motor_gesture_" + safeString(handle.reason));
        }
        clearReadyTargetState();
        host.noteCombatTargetAttempt(targetNpc);
        host.beginCombatOutcomeWaitWindow();
        return host.accept(
            "combat_motor_gesture_in_flight",
            host.details(
                "target", "attackable_npc",
                "npcId", targetId,
                "npcIndex", targetNpc.getIndex(),
                "targetNpcId", preferredNpcId,
                "targetWorldX", targetWorldX,
                "targetWorldY", targetWorldY,
                "targetMaxDistance", targetMaxDistance,
                "maxChaseDistance", maxChaseDistance,
                "motorGestureId", handle.id,
                "motorStatus", handle.status.name(),
                "motorReason", handle.reason
            )
        );
    }

    private Optional<CommandExecutor.CommandDecision> maybeDeferForTargetReadyHold(
        NPC targetNpc,
        long now,
        int targetWorldX,
        int targetWorldY,
        int targetMaxDistance,
        int maxChaseDistance
    ) {
        if (targetNpc == null) {
            clearReadyTargetState();
            return Optional.empty();
        }
        int targetIndex = targetNpc.getIndex();
        if (targetIndex < 0) {
            clearReadyTargetState();
            return Optional.empty();
        }
        if (readyTargetNpcIndex != targetIndex || readyTargetSinceMs <= 0L) {
            readyTargetNpcIndex = targetIndex;
            readyTargetSinceMs = now;
            readyTargetHoldMs = host.randomBetween(COMBAT_TARGET_READY_HOLD_MIN_MS, COMBAT_TARGET_READY_HOLD_MAX_MS);
            return Optional.of(
                host.accept(
                    "combat_target_ready_hold",
                    host.details(
                        "npcId", targetNpc.getId(),
                        "npcIndex", targetIndex,
                        "targetWorldX", targetWorldX,
                        "targetWorldY", targetWorldY,
                        "targetMaxDistance", targetMaxDistance,
                        "maxChaseDistance", maxChaseDistance,
                        "readyAgeMs", 0L,
                        "requiredHoldMs", readyTargetHoldMs
                    )
                )
            );
        }
        long requiredHoldMs = Math.max(COMBAT_TARGET_READY_HOLD_MIN_MS, readyTargetHoldMs);
        long readyAgeMs = Math.max(0L, now - readyTargetSinceMs);
        if (readyAgeMs < requiredHoldMs) {
            return Optional.of(
                host.accept(
                    "combat_target_ready_hold",
                    host.details(
                        "npcId", targetNpc.getId(),
                        "npcIndex", targetIndex,
                        "targetWorldX", targetWorldX,
                        "targetWorldY", targetWorldY,
                        "targetMaxDistance", targetMaxDistance,
                        "maxChaseDistance", maxChaseDistance,
                        "readyAgeMs", readyAgeMs,
                        "requiredHoldMs", requiredHoldMs
                    )
                )
            );
        }
        return Optional.empty();
    }

    private void clearReadyTargetState() {
        readyTargetNpcIndex = -1;
        readyTargetSinceMs = Long.MIN_VALUE;
        readyTargetHoldMs = 0L;
    }

    private static boolean isAnimationActive(int animation) {
        return animation != -1 && animation != 0;
    }

    private int resolveFallbackAttemptBudget() {
        int configured = Math.max(1, host.combatTargetClickFallbackAttempts());
        if (configured <= 1) {
            return 1;
        }
        int minBudget = Math.max(1, configured - 2);
        int maxBudget = configured;
        long sampled = host.randomBetween(minBudget, maxBudget);
        return (int) Math.max(minBudget, Math.min(maxBudget, sampled));
    }

    private String safeString(String value) {
        if (host != null) {
            return host.safeString(value);
        }
        return value == null ? "" : value;
    }

    private Optional<CommandExecutor.CommandDecision> maybeDeferForFatigueCadence(
        long now,
        boolean brutusEncounter,
        NPC targetNpc
    ) {
        if (brutusEncounter) {
            return Optional.empty();
        }
        if (now <= preAttackPauseUntilMs) {
            FatigueSnapshot fatigue = fatigueSnapshot();
            return Optional.of(
                host.accept(
                    "combat_pre_attack_fatigue_pause",
                    host.details(
                        "waitMsRemaining", Math.max(0L, preAttackPauseUntilMs - now),
                        "npcId", targetNpc == null ? -1 : targetNpc.getId(),
                        "npcIndex", targetNpc == null ? -1 : targetNpc.getIndex(),
                        "fatigueLoad", fatigue.loadPercent(),
                        "fatigueBand", fatigue.band(),
                        "newlyArmed", false
                    )
                )
            );
        }
        if (now < nextPreAttackPauseRollAtMs) {
            return Optional.empty();
        }
        FatigueSnapshot fatigue = fatigueSnapshot();
        int chancePercent = clampPercent(
            COMBAT_PRE_ATTACK_PAUSE_BASE_CHANCE_PERCENT
                + scaledByFatiguePercent(fatigue, COMBAT_PRE_ATTACK_PAUSE_FATIGUE_CHANCE_BONUS_MAX_PERCENT)
        );
        long roll = host.randomBetween(1L, 100L);
        long rollGapMs = host.randomBetween(COMBAT_PRE_ATTACK_PAUSE_ROLL_MIN_GAP_MS, COMBAT_PRE_ATTACK_PAUSE_ROLL_MAX_GAP_MS);
        if (roll > chancePercent) {
            nextPreAttackPauseRollAtMs = now + Math.max(40L, rollGapMs);
            return Optional.empty();
        }
        long basePauseMs = host.randomBetween(COMBAT_PRE_ATTACK_PAUSE_MIN_MS, COMBAT_PRE_ATTACK_PAUSE_MAX_MS);
        long fatigueExtraMs = scaledByFatiguePercent(fatigue, COMBAT_PRE_ATTACK_PAUSE_FATIGUE_EXTRA_MAX_MS);
        long pauseMs = Math.max(24L, basePauseMs + fatigueExtraMs);
        preAttackPauseUntilMs = now + pauseMs;
        nextPreAttackPauseRollAtMs =
            preAttackPauseUntilMs + Math.max(60L, host.randomBetween(100L, 280L));
        return Optional.of(
            host.accept(
                "combat_pre_attack_fatigue_pause",
                host.details(
                    "waitMsRemaining", pauseMs,
                    "npcId", targetNpc == null ? -1 : targetNpc.getId(),
                    "npcIndex", targetNpc == null ? -1 : targetNpc.getIndex(),
                    "chancePercent", chancePercent,
                    "roll", roll,
                    "fatigueLoad", fatigue.loadPercent(),
                    "fatigueBand", fatigue.band(),
                    "newlyArmed", true
                )
            )
        );
    }

    private FatigueSnapshot fatigueSnapshot() {
        FatigueSnapshot snapshot = host == null ? null : host.fatigueSnapshot();
        return snapshot == null ? FatigueSnapshot.neutral() : snapshot;
    }

    private static int scaledByFatiguePercent(FatigueSnapshot fatigue, int maxValue) {
        int bounded = Math.max(0, maxValue);
        if (bounded <= 0 || fatigue == null) {
            return 0;
        }
        int loadPercent = clampPercent(fatigue.loadPercent());
        return (int) Math.round((loadPercent / 100.0) * bounded);
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String asString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long variedCombatTargetReclickCooldownMs(long baseCooldownMs, long dispatchAtMs, int npcIndex) {
        long base = Math.max(0L, baseCooldownMs);
        if (base <= 0L) {
            return COMBAT_RECLICK_COOLDOWN_MIN_MS;
        }
        long seed = dispatchAtMs ^ ((long) npcIndex * 73856093L) ^ 0xC6A4A7935BD1E995L;
        double unit = normalizedHashUnit(seed);
        double scaleRange = COMBAT_RECLICK_JITTER_MAX_SCALE - COMBAT_RECLICK_JITTER_MIN_SCALE;
        double scale = COMBAT_RECLICK_JITTER_MIN_SCALE + (scaleRange * unit);
        long scaled = Math.round((double) base * scale);
        return Math.max(COMBAT_RECLICK_COOLDOWN_MIN_MS, scaled);
    }

    private static double normalizedHashUnit(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        long positive = z & Long.MAX_VALUE;
        return (double) positive / (double) Long.MAX_VALUE;
    }
}
