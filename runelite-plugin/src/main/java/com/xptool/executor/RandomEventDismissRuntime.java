package com.xptool.executor;

import com.google.gson.JsonObject;
import java.awt.Point;
import java.util.Locale;
import java.util.Optional;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

final class RandomEventDismissRuntime {
    private static final String SESSION_INTERACTION = "interaction";
    private static final int MAX_TARGET_DISTANCE_TILES = 12;
    private static final int MAX_READY_DISTANCE_TILES = 3;
    private static final int TARGET_STABILITY_MIN_TICKS = 2;
    private static final int LOCAL_STATIONARY_MIN_TICKS = 2;
    private static final int TARGET_REPEAT_ZONE_GRID_PX = 28;
    private static final int TARGET_REPEAT_HISTORY_SIZE = 12;
    private static final long TARGET_REPEAT_GUARD_MIN_MS = 1200L;
    private static final long TARGET_REPEAT_GUARD_MAX_MS = 2800L;
    private static final long VERIFY_PENDING_MIN_MS = 320L;
    private static final long VERIFY_PENDING_MAX_MS = 920L;
    private static final long CONFLICT_DEFER_EMIT_MIN_INTERVAL_MS = 700L;
    private static final long FAILURE_RETRY_STREAK_STEP_MS = 260L;
    private static final long FAILURE_RETRY_STREAK_CAP_MS = 2600L;
    private static final int FAILURE_RETRY_TICK_STEP_CAP = 4;

    private enum DismissPhase {
        DETECT,
        PRE_HOVER,
        INTERACT,
        VERIFY,
        DEFER_RETRY
    }

    interface Host {
        boolean isRuntimeEnabled();

        boolean isRuntimeArmed();

        boolean isLoggedIn();

        boolean isBankOpen();

        boolean hasActiveSessionOtherThan(String sessionName);

        boolean hasActiveInteractionMotorProgram();

        boolean acquireOrRenewInteractionMotorOwnership();

        void releaseInteractionMotorOwnership();

        Player localPlayer();

        Iterable<NPC> npcs();

        Point resolveVariedNpcClickPoint(NPC npc);

        boolean isUsableCanvasPoint(Point point);

        boolean moveInteractionCursorToCanvasPoint(Point canvasPoint);

        boolean isCursorNearTarget(Point canvasPoint);

        boolean selectDismissMenuOptionAt(Point canvasPoint);

        long randomBetween(long minInclusive, long maxInclusive);
        long randomEventPreAttemptCooldownMinMs();
        long randomEventPreAttemptCooldownMaxMs();
        long randomEventSuccessCooldownMinMs();
        long randomEventSuccessCooldownMaxMs();
        long randomEventFailureRetryCooldownMinMs();
        long randomEventFailureRetryCooldownMaxMs();
        long randomEventCursorReadyHoldMs();

        JsonObject details(Object... kvPairs);

        void emitRandomEventEvent(String reason, JsonObject details);
    }

    private final Host host;
    private long cooldownUntilMs = 0L;
    private int retryAtTick = Integer.MIN_VALUE;
    private int failureStreak = 0;
    private int lastAttemptNpcIndex = -1;
    private int candidateStableTicks = 0;
    private int observedCandidateNpcIndex = -1;
    private int localStationaryTicks = 0;
    private WorldPoint lastLocalWorldPoint = null;
    private int cursorReadyNpcIndex = -1;
    private long cursorReadySinceMs = Long.MIN_VALUE;
    private long cursorReadyHoldTargetMs = Long.MIN_VALUE;
    private int lastDispatchNpcIndex = -1;
    private long sameTargetRetryGuardUntilMs = 0L;
    private int stableTargetNpcIndex = -1;
    private Point stableTargetCanvasPoint = null;
    private final long[] dismissedTargetZoneHistory = new long[TARGET_REPEAT_HISTORY_SIZE];
    private final long[] dismissedTargetZoneUntilMs = new long[TARGET_REPEAT_HISTORY_SIZE];
    private int dismissedTargetZoneWriteIndex = 0;
    private int verifyNpcIndex = -1;
    private long verifyPendingUntilMs = 0L;
    private DismissPhase phase = DismissPhase.DETECT;
    private int phaseEnteredTick = Integer.MIN_VALUE;
    private long phaseEnteredAtMs = 0L;
    private long lastConflictDeferEmitAtMs = Long.MIN_VALUE;

    RandomEventDismissRuntime(Host host) {
        this.host = host;
        for (int i = 0; i < dismissedTargetZoneHistory.length; i++) {
            dismissedTargetZoneHistory[i] = Long.MIN_VALUE;
            dismissedTargetZoneUntilMs[i] = 0L;
        }
    }

    void onGameTick(int tick) {
        long now = System.currentTimeMillis();
        if (!host.isRuntimeEnabled()) {
            resetState();
            return;
        }
        if (!host.isRuntimeArmed()) {
            resetState();
            return;
        }
        if (!host.isLoggedIn() || host.isBankOpen()) {
            resetState();
            return;
        }
        if (host.hasActiveSessionOtherThan(SESSION_INTERACTION)) {
            maybeEmitConflictDeferred("dismiss_deferred_conflict", tick, now, "session_conflict");
            return;
        }
        if (host.hasActiveInteractionMotorProgram()) {
            maybeEmitConflictDeferred("dismiss_deferred_conflict", tick, now, "interaction_motor_program_in_flight");
            return;
        }

        if (tick < retryAtTick || now < cooldownUntilMs) {
            return;
        }

        Player local = host.localPlayer();
        if (local == null) {
            return;
        }
        setPhase(DismissPhase.DETECT, tick, now);
        updateLocalMovement(local.getWorldLocation());
        Optional<TargetCandidate> candidateOpt = resolveBestDismissCandidate(local);
        if (candidateOpt.isEmpty()) {
            failureStreak = 0;
            lastAttemptNpcIndex = -1;
            candidateStableTicks = 0;
            observedCandidateNpcIndex = -1;
            clearCursorReadyHold();
            clearVerifyPending();
            return;
        }
        TargetCandidate candidate = candidateOpt.get();
        if (shouldDeferVerifyPending(candidate, tick, now)) {
            return;
        }

        if (!host.acquireOrRenewInteractionMotorOwnership()) {
            return;
        }
        try {
            noteCandidateObserved(candidate.npc.getIndex());
            if (shouldDeferAttemptBeforeMove(candidate, tick, now, null)) {
                return;
            }
            setPhase(DismissPhase.PRE_HOVER, tick, now);
            Point targetPoint = resolveStableTargetPoint(candidate.npc);
            if (!host.isUsableCanvasPoint(targetPoint)) {
                noteFailure("unusable_target_point", candidate, tick, now, targetPoint);
                return;
            }
            if (shouldDeferRepeatTarget(candidate, targetPoint, tick, now)) {
                return;
            }
            boolean cursorNearTarget = host.isCursorNearTarget(targetPoint);
            if (!cursorNearTarget) {
                if (!host.moveInteractionCursorToCanvasPoint(targetPoint)) {
                    noteFailure("cursor_move_failed", candidate, tick, now, targetPoint);
                    return;
                }
                cursorNearTarget = host.isCursorNearTarget(targetPoint);
            }
            if (!cursorNearTarget) {
                clearCursorReadyHold();
                noteDeferred("cursor_not_settled", candidate, tick, now, targetPoint);
                return;
            }
            if (shouldDeferDismissAfterCursorReady(candidate, tick, now, targetPoint)) {
                return;
            }
            setPhase(DismissPhase.INTERACT, tick, now);
            noteDispatchAttempt(candidate, now);
            emitAttempt(candidate, tick, now, targetPoint);
            if (host.selectDismissMenuOptionAt(targetPoint)) {
                noteSuccess(candidate, tick, now, targetPoint);
                return;
            }
            noteFailure("dismiss_context_menu_select_failed", candidate, tick, now, targetPoint);
        } finally {
            host.releaseInteractionMotorOwnership();
        }
    }

    private Optional<TargetCandidate> resolveBestDismissCandidate(Player local) {
        WorldPoint localPoint = local.getWorldLocation();
        if (localPoint == null) {
            return Optional.empty();
        }
        Actor localInteracting = local.getInteracting();
        TargetCandidate best = null;
        int bestScore = Integer.MIN_VALUE;

        for (NPC npc : host.npcs()) {
            if (npc == null) {
                continue;
            }
            NPCComposition comp = npc.getTransformedComposition();
            if (comp == null) {
                comp = npc.getComposition();
            }
            if (!hasDismissAction(comp)) {
                continue;
            }
            WorldPoint npcPoint = npc.getWorldLocation();
            if (npcPoint == null) {
                continue;
            }
            int distance = localPoint.distanceTo(npcPoint);
            if (distance < 0 || distance > MAX_TARGET_DISTANCE_TILES) {
                continue;
            }
            Actor npcInteracting = npc.getInteracting();
            boolean targetsLocal = npcInteracting == local;
            boolean localTargetsNpc = localInteracting == npc;
            if (!targetsLocal && !localTargetsNpc) {
                continue;
            }
            int score = scoreCandidate(distance, targetsLocal, localTargetsNpc, npc.getIndex());
            if (score > bestScore) {
                bestScore = score;
                best = new TargetCandidate(npc, comp, distance, targetsLocal, localTargetsNpc);
            }
        }
        return Optional.ofNullable(best);
    }

    private int scoreCandidate(int distance, boolean targetsLocal, boolean localTargetsNpc, int npcIndex) {
        int distanceScore = Math.max(0, 220 - (distance * 16));
        int localTargetingScore = targetsLocal ? 1200 : 0;
        int reciprocalTargetingScore = localTargetsNpc ? 700 : 0;
        int recencyScore = npcIndex >= 0 && npcIndex == lastAttemptNpcIndex ? 60 : 0;
        return distanceScore + localTargetingScore + reciprocalTargetingScore + recencyScore;
    }

    private void noteSuccess(TargetCandidate candidate, int tick, long now, Point point) {
        long minCooldownMs = preAttemptBoundedMin(successCooldownMinMs());
        long maxCooldownMs = Math.max(minCooldownMs, successCooldownMaxMs());
        setPhase(DismissPhase.VERIFY, tick, now);
        failureStreak = 0;
        lastAttemptNpcIndex = candidate.npc.getIndex();
        retryAtTick = tick + 1;
        cooldownUntilMs = now + host.randomBetween(minCooldownMs, maxCooldownMs);
        armVerifyPending(candidate, now);
        rememberDismissedTargetZone(candidate, point, now);
        emitRuntimeEvent(
            "random_event_dismiss_dispatched",
            host.details(
                "dismissMode", "right_click_menu",
                "npcIndex", candidate.npc.getIndex(),
                "npcId", candidate.npc.getId(),
                "npcName", normalizedNpcName(candidate.comp),
                "distanceTiles", candidate.distanceTiles,
                "targetsLocal", candidate.targetsLocal,
                "localTargetsNpc", candidate.localTargetsNpc,
                "candidateStableTicks", candidateStableTicks,
                "localStationaryTicks", localStationaryTicks,
                "clickX", point == null ? -1 : point.x,
                "clickY", point == null ? -1 : point.y,
                "cooldownMs", Math.max(0L, cooldownUntilMs - now),
                "verifyPendingMs", Math.max(0L, verifyPendingUntilMs - now)
            ),
            tick,
            now
        );
    }

    private void emitAttempt(TargetCandidate candidate, int tick, long now, Point point) {
        emitRuntimeEvent(
            "random_event_dismiss_attempt",
            host.details(
                "dismissMode", "right_click_menu",
                "npcIndex", candidate.npc.getIndex(),
                "npcId", candidate.npc.getId(),
                "npcName", normalizedNpcName(candidate.comp),
                "distanceTiles", candidate.distanceTiles,
                "targetsLocal", candidate.targetsLocal,
                "localTargetsNpc", candidate.localTargetsNpc,
                "candidateStableTicks", candidateStableTicks,
                "localStationaryTicks", localStationaryTicks,
                "attemptTick", tick,
                "clickX", point == null ? -1 : point.x,
                "clickY", point == null ? -1 : point.y
            ),
            tick,
            now
        );
    }

    private boolean shouldDeferAttemptBeforeMove(TargetCandidate candidate, int tick, long now, Point point) {
        if (candidate == null) {
            return true;
        }
        if (shouldDeferSameTargetRetryGuard(candidate, tick, now, point)) {
            return true;
        }
        if (candidate.distanceTiles > MAX_READY_DISTANCE_TILES) {
            noteDeferred("target_distance_not_ready", candidate, tick, now, point);
            return true;
        }
        if (candidateStableTicks < TARGET_STABILITY_MIN_TICKS) {
            noteDeferred("target_candidate_settling", candidate, tick, now, point);
            return true;
        }
        if (localStationaryTicks < LOCAL_STATIONARY_MIN_TICKS) {
            noteDeferred("local_position_settling", candidate, tick, now, point);
            return true;
        }
        return false;
    }

    private boolean shouldDeferSameTargetRetryGuard(TargetCandidate candidate, int tick, long now, Point point) {
        if (candidate == null || candidate.npc == null) {
            return false;
        }
        int npcIndex = candidate.npc.getIndex();
        if (npcIndex < 0 || npcIndex != lastDispatchNpcIndex || now >= sameTargetRetryGuardUntilMs) {
            return false;
        }
        setPhase(DismissPhase.DEFER_RETRY, tick, now);
        retryAtTick = Math.max(retryAtTick, tick + 1);
        cooldownUntilMs = Math.max(cooldownUntilMs, sameTargetRetryGuardUntilMs);
        emitRuntimeEvent(
            "random_event_same_target_retry_guard",
            host.details(
                "dismissMode", "right_click_menu",
                "guardReason", "same_target_retry_guard_active",
                "npcIndex", npcIndex,
                "npcId", candidate.npc.getId(),
                "npcName", normalizedNpcName(candidate.comp),
                "distanceTiles", candidate.distanceTiles,
                "targetsLocal", candidate.targetsLocal,
                "localTargetsNpc", candidate.localTargetsNpc,
                "candidateStableTicks", candidateStableTicks,
                "localStationaryTicks", localStationaryTicks,
                "clickX", point == null ? -1 : point.x,
                "clickY", point == null ? -1 : point.y,
                "retryAtTick", retryAtTick,
                "waitMs", Math.max(1L, sameTargetRetryGuardUntilMs - now)
            ),
            tick,
            now
        );
        return true;
    }

    private void noteDispatchAttempt(TargetCandidate candidate, long now) {
        if (candidate == null || candidate.npc == null) {
            return;
        }
        int npcIndex = candidate.npc.getIndex();
        if (npcIndex < 0) {
            return;
        }
        lastDispatchNpcIndex = npcIndex;
        long minGuardMs = Math.max(180L, preAttemptCooldownMinMs());
        long maxGuardMs = Math.max(minGuardMs, preAttemptCooldownMaxMs());
        long streakExtraMs = Math.min(580L, (long) Math.max(0, failureStreak) * 110L);
        long guardMs = host.randomBetween(minGuardMs, maxGuardMs) + streakExtraMs;
        sameTargetRetryGuardUntilMs = Math.max(sameTargetRetryGuardUntilMs, now + guardMs);
    }

    private void noteDeferred(String deferReason, TargetCandidate candidate, int tick, long now, Point point) {
        long minCooldownMs = preAttemptBoundedMin(preAttemptCooldownMinMs());
        long maxCooldownMs = Math.max(minCooldownMs, preAttemptCooldownMaxMs());
        setPhase(DismissPhase.DEFER_RETRY, tick, now);
        retryAtTick = tick + 1;
        cooldownUntilMs = Math.max(cooldownUntilMs, now + host.randomBetween(minCooldownMs, maxCooldownMs));
        emitRuntimeEvent(
            "random_event_dismiss_deferred",
            host.details(
                "dismissMode", "right_click_menu",
                "deferReason", normalizeMenuToken(deferReason),
                "npcIndex", candidate == null ? -1 : candidate.npc.getIndex(),
                "npcId", candidate == null ? -1 : candidate.npc.getId(),
                "npcName", candidate == null ? "" : normalizedNpcName(candidate.comp),
                "distanceTiles", candidate == null ? -1 : candidate.distanceTiles,
                "targetsLocal", candidate != null && candidate.targetsLocal,
                "localTargetsNpc", candidate != null && candidate.localTargetsNpc,
                "candidateStableTicks", candidateStableTicks,
                "localStationaryTicks", localStationaryTicks,
                "clickX", point == null ? -1 : point.x,
                "clickY", point == null ? -1 : point.y,
                "retryAtTick", retryAtTick,
                "cooldownMs", Math.max(0L, cooldownUntilMs - now)
            ),
            tick,
            now
        );
    }

    private void noteFailure(String failureReason, TargetCandidate candidate, int tick, long now, Point point) {
        long minCooldownMs = preAttemptBoundedMin(failureRetryCooldownMinMs());
        long maxCooldownMs = Math.max(minCooldownMs, failureRetryCooldownMaxMs());
        setPhase(DismissPhase.DEFER_RETRY, tick, now);
        clearVerifyPending();
        failureStreak++;
        int retryTickBackoff = Math.min(FAILURE_RETRY_TICK_STEP_CAP, 1 + (failureStreak / 2));
        retryAtTick = tick + retryTickBackoff;
        long baseCooldownMs = host.randomBetween(minCooldownMs, maxCooldownMs);
        long streakExtraMs = Math.min(
            FAILURE_RETRY_STREAK_CAP_MS,
            (long) Math.max(0, failureStreak - 1) * FAILURE_RETRY_STREAK_STEP_MS
        );
        cooldownUntilMs = now + baseCooldownMs + streakExtraMs;
        JsonObject retryDetails = host.details(
            "dismissMode", "right_click_menu",
            "failureReason", normalizeMenuToken(failureReason),
            "npcIndex", candidate == null ? -1 : candidate.npc.getIndex(),
            "npcId", candidate == null ? -1 : candidate.npc.getId(),
            "npcName", candidate == null ? "" : normalizedNpcName(candidate.comp),
            "distanceTiles", candidate == null ? -1 : candidate.distanceTiles,
            "targetsLocal", candidate != null && candidate.targetsLocal,
            "localTargetsNpc", candidate != null && candidate.localTargetsNpc,
            "candidateStableTicks", candidateStableTicks,
            "localStationaryTicks", localStationaryTicks,
            "clickX", point == null ? -1 : point.x,
            "clickY", point == null ? -1 : point.y,
            "failureStreak", failureStreak,
            "retryAtTick", retryAtTick,
            "cooldownMs", Math.max(0L, cooldownUntilMs - now)
        );
        emitRuntimeEvent(
            "random_event_dismiss_retry_scheduled",
            retryDetails,
            tick,
            now
        );
        if ("dismiss_context_menu_select_failed".equals(normalizeMenuToken(failureReason))) {
            emitRuntimeEvent(
                "random_event_verify_failed_retry_scheduled",
                host.details(
                    "dismissMode", "right_click_menu",
                    "failureReason", "verify_failed_retry_scheduled",
                    "npcIndex", candidate == null ? -1 : candidate.npc.getIndex(),
                    "npcId", candidate == null ? -1 : candidate.npc.getId(),
                    "retryAtTick", retryAtTick,
                    "cooldownMs", Math.max(0L, cooldownUntilMs - now)
                ),
                tick,
                now
            );
        }
    }

    private boolean shouldDeferVerifyPending(TargetCandidate candidate, int tick, long now) {
        if (candidate == null || candidate.npc == null) {
            clearVerifyPending();
            return false;
        }
        if (verifyNpcIndex < 0 || verifyPendingUntilMs <= 0L) {
            return false;
        }
        if (now >= verifyPendingUntilMs) {
            clearVerifyPending();
            return false;
        }
        int npcIndex = candidate.npc.getIndex();
        if (npcIndex < 0 || npcIndex != verifyNpcIndex) {
            return false;
        }
        setPhase(DismissPhase.VERIFY, tick, now);
        noteDeferred("verify_pending", candidate, tick, now, null);
        return true;
    }

    private boolean shouldDeferRepeatTarget(TargetCandidate candidate, Point point, int tick, long now) {
        if (candidate == null || candidate.npc == null || point == null) {
            return false;
        }
        long key = dismissedTargetZoneKey(candidate, point);
        if (key == Long.MIN_VALUE) {
            return false;
        }
        pruneDismissedTargetHistory(now);
        long remainingMs = dismissedTargetZoneRemainingMs(key, now);
        if (remainingMs <= 0L) {
            return false;
        }
        cooldownUntilMs = Math.max(cooldownUntilMs, now + remainingMs);
        noteDeferred("target_repeat_blocked", candidate, tick, now, point);
        return true;
    }

    private void maybeEmitConflictDeferred(String deferReason, int tick, long now, String conflictType) {
        if (now > 0L && lastConflictDeferEmitAtMs > 0L
            && (now - lastConflictDeferEmitAtMs) < CONFLICT_DEFER_EMIT_MIN_INTERVAL_MS) {
            return;
        }
        lastConflictDeferEmitAtMs = now;
        setPhase(DismissPhase.DEFER_RETRY, tick, now);
        emitRuntimeEvent(
            "random_event_dismiss_deferred_conflict",
            host.details(
                "dismissMode", "right_click_menu",
                "deferReason", normalizeMenuToken(deferReason),
                "conflictType", normalizeMenuToken(conflictType),
                "retryAtTick", retryAtTick,
                "cooldownMs", Math.max(0L, cooldownUntilMs - now)
            ),
            tick,
            now
        );
    }

    private void armVerifyPending(TargetCandidate candidate, long now) {
        if (candidate == null || candidate.npc == null) {
            clearVerifyPending();
            return;
        }
        int npcIndex = candidate.npc.getIndex();
        if (npcIndex < 0) {
            clearVerifyPending();
            return;
        }
        verifyNpcIndex = npcIndex;
        long verifyMs = host.randomBetween(VERIFY_PENDING_MIN_MS, VERIFY_PENDING_MAX_MS);
        verifyPendingUntilMs = now + Math.max(60L, verifyMs);
    }

    private void clearVerifyPending() {
        verifyNpcIndex = -1;
        verifyPendingUntilMs = 0L;
    }

    private void rememberDismissedTargetZone(TargetCandidate candidate, Point point, long now) {
        long key = dismissedTargetZoneKey(candidate, point);
        if (key == Long.MIN_VALUE || dismissedTargetZoneHistory.length <= 0) {
            return;
        }
        long guardMs = host.randomBetween(TARGET_REPEAT_GUARD_MIN_MS, TARGET_REPEAT_GUARD_MAX_MS);
        dismissedTargetZoneHistory[dismissedTargetZoneWriteIndex] = key;
        dismissedTargetZoneUntilMs[dismissedTargetZoneWriteIndex] = now + Math.max(200L, guardMs);
        dismissedTargetZoneWriteIndex =
            (dismissedTargetZoneWriteIndex + 1) % dismissedTargetZoneHistory.length;
    }

    private void pruneDismissedTargetHistory(long now) {
        for (int i = 0; i < dismissedTargetZoneHistory.length; i++) {
            if (dismissedTargetZoneHistory[i] == Long.MIN_VALUE) {
                continue;
            }
            if (now >= dismissedTargetZoneUntilMs[i]) {
                dismissedTargetZoneHistory[i] = Long.MIN_VALUE;
                dismissedTargetZoneUntilMs[i] = 0L;
            }
        }
    }

    private long dismissedTargetZoneRemainingMs(long key, long now) {
        for (int i = 0; i < dismissedTargetZoneHistory.length; i++) {
            if (dismissedTargetZoneHistory[i] != key) {
                continue;
            }
            long remaining = dismissedTargetZoneUntilMs[i] - now;
            if (remaining > 0L) {
                return remaining;
            }
        }
        return 0L;
    }

    private static long dismissedTargetZoneKey(TargetCandidate candidate, Point point) {
        if (candidate == null || candidate.npc == null || point == null) {
            return Long.MIN_VALUE;
        }
        int npcId = Math.max(0, candidate.npc.getId());
        int npcIndex = Math.max(0, candidate.npc.getIndex());
        int zoneX = Math.max(0, point.x / TARGET_REPEAT_ZONE_GRID_PX);
        int zoneY = Math.max(0, point.y / TARGET_REPEAT_ZONE_GRID_PX);
        long idBits = npcId & 0xFFFFFL;
        long indexBits = npcIndex & 0xFFFFFL;
        long zoneXBits = zoneX & 0x1FFFL;
        long zoneYBits = zoneY & 0x1FFFL;
        return (idBits << 46) | (indexBits << 26) | (zoneXBits << 13) | zoneYBits;
    }

    private void setPhase(DismissPhase nextPhase, int tick, long now) {
        if (nextPhase == null) {
            return;
        }
        if (phase == nextPhase && phaseEnteredTick != Integer.MIN_VALUE) {
            return;
        }
        phase = nextPhase;
        phaseEnteredTick = tick;
        phaseEnteredAtMs = now;
    }

    private void emitRuntimeEvent(String reason, JsonObject details, int tick, long now) {
        JsonObject payload = details == null ? new JsonObject() : details;
        payload.addProperty("phase", normalizePhase(phase));
        payload.addProperty("phaseEnteredTick", phaseEnteredTick);
        payload.addProperty("phaseAgeTicks", elapsedTicksSince(tick, phaseEnteredTick));
        payload.addProperty("phaseAgeMs", elapsedMillisSince(now, phaseEnteredAtMs));
        payload.addProperty("verifyPendingMs", Math.max(0L, verifyPendingUntilMs - now));
        host.emitRandomEventEvent(reason, payload);
    }

    private void resetState() {
        cooldownUntilMs = 0L;
        retryAtTick = Integer.MIN_VALUE;
        failureStreak = 0;
        lastAttemptNpcIndex = -1;
        lastDispatchNpcIndex = -1;
        sameTargetRetryGuardUntilMs = 0L;
        candidateStableTicks = 0;
        observedCandidateNpcIndex = -1;
        localStationaryTicks = 0;
        lastLocalWorldPoint = null;
        clearVerifyPending();
        phase = DismissPhase.DETECT;
        phaseEnteredTick = Integer.MIN_VALUE;
        phaseEnteredAtMs = 0L;
        lastConflictDeferEmitAtMs = Long.MIN_VALUE;
        for (int i = 0; i < dismissedTargetZoneHistory.length; i++) {
            dismissedTargetZoneHistory[i] = Long.MIN_VALUE;
            dismissedTargetZoneUntilMs[i] = 0L;
        }
        dismissedTargetZoneWriteIndex = 0;
        clearCursorReadyHold();
    }

    private boolean shouldDeferDismissAfterCursorReady(
        TargetCandidate candidate,
        int tick,
        long now,
        Point point
    ) {
        if (candidate == null || candidate.npc == null) {
            clearCursorReadyHold();
            return true;
        }
        int npcIndex = candidate.npc.getIndex();
        if (npcIndex < 0) {
            clearCursorReadyHold();
            return true;
        }
        if (npcIndex != cursorReadyNpcIndex || cursorReadySinceMs <= 0L) {
            cursorReadyNpcIndex = npcIndex;
            cursorReadySinceMs = now;
            cursorReadyHoldTargetMs = sampleCursorReadyHoldTargetMs(candidate);
            noteDeferred("cursor_ready_hold", candidate, tick, now, point);
            return true;
        }
        long readyAgeMs = Math.max(0L, now - cursorReadySinceMs);
        long cursorReadyHoldMs = cursorReadyHoldTargetMs > 0L
            ? cursorReadyHoldTargetMs
            : sampleCursorReadyHoldTargetMs(candidate);
        cursorReadyHoldMs = Math.max(40L, cursorReadyHoldMs);
        if (readyAgeMs < cursorReadyHoldMs) {
            noteDeferred("cursor_ready_hold", candidate, tick, now, point);
            return true;
        }
        return false;
    }

    private long sampleCursorReadyHoldTargetMs(TargetCandidate candidate) {
        long baseHoldMs = Math.max(40L, cursorReadyHoldMs());
        long minHoldMs = Math.max(40L, Math.round(baseHoldMs * 0.62));
        long maxHoldMs = Math.max(minHoldMs, Math.round(baseHoldMs * 1.45));
        int distanceTiles = candidate == null ? 0 : Math.max(0, candidate.distanceTiles);
        long distanceBiasMs = Math.min(140L, (long) distanceTiles * 18L);
        maxHoldMs = Math.max(minHoldMs, maxHoldMs + distanceBiasMs);
        return host.randomBetween(minHoldMs, maxHoldMs);
    }

    private void clearCursorReadyHold() {
        cursorReadyNpcIndex = -1;
        cursorReadySinceMs = Long.MIN_VALUE;
        cursorReadyHoldTargetMs = Long.MIN_VALUE;
        stableTargetNpcIndex = -1;
        stableTargetCanvasPoint = null;
    }

    private Point resolveStableTargetPoint(NPC npc) {
        if (npc == null) {
            clearCursorReadyHold();
            return null;
        }
        int npcIndex = npc.getIndex();
        if (npcIndex < 0) {
            clearCursorReadyHold();
            return null;
        }
        if (npcIndex == stableTargetNpcIndex && host.isUsableCanvasPoint(stableTargetCanvasPoint)) {
            return new Point(stableTargetCanvasPoint);
        }
        Point resolved = host.resolveVariedNpcClickPoint(npc);
        if (resolved == null || !host.isUsableCanvasPoint(resolved)) {
            return null;
        }
        stableTargetNpcIndex = npcIndex;
        stableTargetCanvasPoint = new Point(resolved);
        return new Point(stableTargetCanvasPoint);
    }

    private void updateLocalMovement(WorldPoint localPoint) {
        if (localPoint == null || lastLocalWorldPoint == null) {
            localStationaryTicks = 0;
            lastLocalWorldPoint = localPoint;
            return;
        }
        if (worldPointsSameTile(localPoint, lastLocalWorldPoint)) {
            localStationaryTicks = Math.min(60, localStationaryTicks + 1);
        } else {
            localStationaryTicks = 0;
        }
        lastLocalWorldPoint = localPoint;
    }

    private void noteCandidateObserved(int npcIndex) {
        if (npcIndex >= 0 && npcIndex == observedCandidateNpcIndex) {
            candidateStableTicks = Math.min(60, candidateStableTicks + 1);
            return;
        }
        observedCandidateNpcIndex = npcIndex;
        candidateStableTicks = 0;
    }

    private static boolean worldPointsSameTile(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getPlane() == b.getPlane()
            && a.getX() == b.getX()
            && a.getY() == b.getY();
    }

    private static boolean hasDismissAction(NPCComposition comp) {
        if (comp == null || !comp.isInteractible()) {
            return false;
        }
        String[] actions = comp.getActions();
        if (actions == null || actions.length == 0) {
            return false;
        }
        for (String action : actions) {
            String normalized = normalizeMenuToken(action);
            if (normalized.equals("dismiss") || normalized.startsWith("dismiss ")) {
                return true;
            }
        }
        return false;
    }

    private static String normalizedNpcName(NPCComposition comp) {
        if (comp == null) {
            return "";
        }
        return normalizeMenuToken(comp.getName());
    }

    private static String normalizeMenuToken(String raw) {
        String safe = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return safe.replaceAll("<[^>]*>", "").trim();
    }

    private static String normalizePhase(DismissPhase phase) {
        if (phase == null) {
            return "detect";
        }
        return phase.name().toLowerCase(Locale.ROOT);
    }

    private static int elapsedTicksSince(int nowTick, int startTick) {
        if (startTick == Integer.MIN_VALUE) {
            return -1;
        }
        long elapsed = (long) nowTick - (long) startTick;
        if (elapsed <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (elapsed >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) elapsed;
    }

    private static long elapsedMillisSince(long nowMs, long startMs) {
        if (startMs <= 0L) {
            return -1L;
        }
        long elapsed = nowMs - startMs;
        if (elapsed <= 0L) {
            return 0L;
        }
        return elapsed;
    }

    private long preAttemptCooldownMinMs() {
        return positiveOrMinimum(host.randomEventPreAttemptCooldownMinMs());
    }

    private long preAttemptCooldownMaxMs() {
        return positiveOrMinimum(host.randomEventPreAttemptCooldownMaxMs());
    }

    private long successCooldownMinMs() {
        return positiveOrMinimum(host.randomEventSuccessCooldownMinMs());
    }

    private long successCooldownMaxMs() {
        return positiveOrMinimum(host.randomEventSuccessCooldownMaxMs());
    }

    private long failureRetryCooldownMinMs() {
        return positiveOrMinimum(host.randomEventFailureRetryCooldownMinMs());
    }

    private long failureRetryCooldownMaxMs() {
        return positiveOrMinimum(host.randomEventFailureRetryCooldownMaxMs());
    }

    private long cursorReadyHoldMs() {
        return positiveOrMinimum(host.randomEventCursorReadyHoldMs());
    }

    private static long positiveOrMinimum(long value) {
        if (value <= 0L) {
            return 1L;
        }
        return value;
    }

    private static long preAttemptBoundedMin(long value) {
        return Math.max(1L, value);
    }

    private static final class TargetCandidate {
        private final NPC npc;
        private final NPCComposition comp;
        private final int distanceTiles;
        private final boolean targetsLocal;
        private final boolean localTargetsNpc;

        private TargetCandidate(
            NPC npc,
            NPCComposition comp,
            int distanceTiles,
            boolean targetsLocal,
            boolean localTargetsNpc
        ) {
            this.npc = npc;
            this.comp = comp;
            this.distanceTiles = distanceTiles;
            this.targetsLocal = targetsLocal;
            this.localTargetsNpc = localTargetsNpc;
        }
    }
}
