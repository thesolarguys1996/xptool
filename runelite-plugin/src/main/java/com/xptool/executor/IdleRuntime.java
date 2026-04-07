package com.xptool.executor;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleBehaviorProfile;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class IdleRuntime {
    private static final String SESSION_INTERACTION = "interaction";
    private static final String SESSION_DROP_SWEEP = "drop_sweep";
    private static final int PARK_DWELL_MIN_TICKS = 8;
    private static final int PARK_DWELL_MAX_TICKS = 22;
    private static final int PARK_RETRY_MIN_INTERVAL_TICKS = 2;
    private static final int PARK_RETRY_MAX_INTERVAL_TICKS = 5;
    private static final int PARK_RETRY_FAIL_STREAK_LIMIT = 4;
    private static final int IDLE_REPEAT_RETRY_ATTEMPTS = 6;
    private static final int IDLE_ACTION_STREAK_SOFT_LIMIT = 2;
    private static final int IDLE_ACTION_STREAK_HARD_LIMIT = 5;
    private static final int IDLE_ACTION_STREAK_SOFT_PENALTY_STEP_PERCENT = 12;
    private static final int IDLE_ACTION_STREAK_HARD_PENALTY_STEP_PERCENT = 9;
    private static final int IDLE_ACTION_STREAK_PENALTY_CAP_PERCENT = 72;
    private static final int IDLE_ACTION_STREAK_REBOUND_STEP_PERCENT = 4;
    private static final int IDLE_ACTION_STREAK_REBOUND_CAP_PERCENT = 16;
    private static final int IDLE_ACTION_HISTORY_SIZE = 8;
    private static final int IDLE_BLEND_WINDOW_MIN_TICKS = 34;
    private static final int IDLE_BLEND_WINDOW_MAX_TICKS = 92;
    private static final int IDLE_CADENCE_ENVELOPE_WINDOW_MIN_TICKS = 26;
    private static final int IDLE_CADENCE_ENVELOPE_WINDOW_MAX_TICKS = 74;
    private static final int IDLE_GATE_TELEMETRY_MIN_INTERVAL_TICKS = 16;
    private static final int IDLE_NOOP_TELEMETRY_MIN_INTERVAL_TICKS = 20;
    private static final int WOODCUT_CADENCE_HESITATION_BASE_CHANCE_PERCENT = 16;
    private static final int WOODCUT_CADENCE_MICRO_BURST_BASE_CHANCE_PERCENT = 10;
    private static final int WOODCUT_CADENCE_REPEAT_BREAK_CHANCE_PERCENT = 42;
    private static final boolean IDLE_CAMERA_MICRO_ADJUST_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.idleCameraMicroAdjustEnabled", "false"));
    private static final boolean IDLE_OFFSCREEN_DEBUG_TELEMETRY_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.idleOffscreenDebugTelemetryEnabled", "true"));
    private static final ActivityIdlePolicyRegistry FALLBACK_IDLE_POLICY_REGISTRY =
        ActivityIdlePolicyRegistry.defaults();
    private static final IdleBehaviorProfile FALLBACK_BEHAVIOR_PROFILE =
        FALLBACK_IDLE_POLICY_REGISTRY.resolveForContext(IdleSkillContext.GLOBAL).behaviorProfile();
    private static final IdleBlendMode[] GLOBAL_BLEND_MODES = {
        IdleBlendMode.BASELINE,
        IdleBlendMode.HOVER_FOCUS,
        IdleBlendMode.DRIFT_FOCUS,
        IdleBlendMode.CAMERA_FOCUS,
        IdleBlendMode.PARK_FOCUS
    };
    private static final IdleBlendMode[] FISHING_BLEND_MODES = {
        IdleBlendMode.BASELINE,
        IdleBlendMode.HOVER_FOCUS,
        IdleBlendMode.DRIFT_FOCUS,
        IdleBlendMode.CAMERA_FOCUS
    };

    interface Host {
        boolean hasActiveSession();
        boolean hasActiveSessionOtherThan(String sessionName);
        Optional<String> activeSessionName();
        boolean hasActiveDropSweepSession();
        void releaseIdleMotorOwnership();
        boolean isIdleInterActionWindowOpen();
        IdleSkillContext resolveIdleSkillContext();
        boolean isIdleActionWindowOpen();
        boolean isIdleCameraWindowOpen();
        JsonObject idleWindowGateSnapshot();
        boolean isIdleAnimationActiveNow();
        boolean isIdleInteractionDelaySatisfied();
        boolean isIdleCameraInteractionDelaySatisfied();
        long lastInteractionClickSerial();
        boolean isCursorOutsideClientWindow();
        boolean acquireOrRenewIdleMotorOwnership();
        boolean canPerformIdleMotorActionNow();
        boolean performIdleCameraMicroAdjust();
        Optional<Point> resolveIdleHoverTargetCanvasPoint();
        boolean performIdleCursorMove(Point canvasTarget);
        Optional<Point> resolveIdleDriftTargetCanvasPoint();
        Optional<Point> resolveIdleOffscreenTargetScreenPoint();
        boolean performIdleOffscreenCursorMove(Point screenTarget);
        Optional<Point> resolveIdleParkingTargetCanvasPoint();
        FishingIdleMode resolveFishingIdleMode(IdleSkillContext context);
        ActivityIdlePolicy resolveActivityIdlePolicy(IdleSkillContext context);
        IdleCadenceTuning activeIdleCadenceTuning();
        FatigueSnapshot fatigueSnapshot();
        boolean isFishingOffscreenIdleSuppressed();
        long fishingOffscreenIdleSuppressionRemainingMs();
        boolean isFishingInventoryFullAfkActive();
        long fishingInventoryFullAfkRemainingMs();
        void emitIdleEvent(String reason, JsonObject details);
    }

    private final Host host;
    private final IdleActionRepetitionGuard repetitionGuard;
    private final IdleActionPatternDetector patternDetector;
    private int nextIdleTick = Integer.MIN_VALUE;
    private int parkedUntilTick = Integer.MIN_VALUE;
    private int idleMotionBurstActions = 0;
    private boolean parkAfterSessionCompletion = false;
    private int parkRetryFailStreak = 0;
    private boolean dropSweepConflictSeen = false;
    private int lastIdleGateTelemetryTick = Integer.MIN_VALUE;
    private String lastIdleGateTelemetryReason = "";
    private int lastOffscreenGateSnapshotTick = Integer.MIN_VALUE;
    private String lastOffscreenGateSnapshotCode = "";
    private int lastIdleNoopTelemetryTick = Integer.MIN_VALUE;
    private String lastIdleActionReason = "";
    private int lastIdleActionReasonStreak = 0;
    private long lastOffscreenBiasHandledClickSerial = -1L;
    private boolean idleMotorLeaseHeld = false;
    private int lastScheduledIdleDeltaTicks = Integer.MIN_VALUE;
    private int lastScheduledIdleDeltaRepeatCount = 0;
    private IdleBlendMode activeBlendMode = IdleBlendMode.BASELINE;
    private int blendModeUntilTick = Integer.MIN_VALUE;
    private String lastBlendTransitionReason = "init";
    private IdleCadenceEnvelope activeCadenceEnvelope = IdleCadenceEnvelope.STEADY;
    private int cadenceEnvelopeUntilTick = Integer.MIN_VALUE;
    private String lastCadenceEnvelopeReason = "init";
    private final String[] recentActionHistory = new String[IDLE_ACTION_HISTORY_SIZE];
    private int recentActionWriteIndex = 0;
    private int recentActionHistoryCount = 0;

    IdleRuntime(Host host) {
        this.host = host;
        this.repetitionGuard = new IdleActionRepetitionGuard();
        this.patternDetector = new IdleActionPatternDetector();
        this.lastOffscreenBiasHandledClickSerial = Math.max(-1L, host.lastInteractionClickSerial());
        resetIdleStrategyState();
    }

    void onGameTick(int tick) {
        IdleCadenceTuning idleCadenceTuning = resolveIdleCadenceTuning();
        boolean hasActiveSession = host.hasActiveSession();
        boolean interactionOnlySessionActive =
            hasActiveSession && !host.hasActiveSessionOtherThan(SESSION_INTERACTION);
        boolean dropSweepSessionActive = host.hasActiveDropSweepSession();
        IdleSkillContext idleContext = resolveSkillContext();

        if (dropSweepConflictSeen && !dropSweepSessionActive) {
            dropSweepConflictSeen = false;
            nextIdleTick = Math.max(nextIdleTick, tick + 1);
            parkAfterSessionCompletion = false;
            parkRetryFailStreak = 0;
            idleMotionBurstActions = 0;
            resetIdlePatternState();
        }

        if (hasActiveSession && !interactionOnlySessionActive) {
            String activeSession = host.activeSessionName().orElse("");
            boolean dropConflict = dropSweepSessionActive || SESSION_DROP_SWEEP.equals(activeSession);
            FishingIdleMode preGateFishingIdleMode = resolveFishingIdleMode(idleContext);
            boolean fishingInventoryFullAfkOverride = dropConflict
                && idleContext == IdleSkillContext.FISHING
                && preGateFishingIdleMode == FishingIdleMode.OFFSCREEN_BIASED
                && host.isFishingInventoryFullAfkActive();
            if (!fishingInventoryFullAfkOverride) {
                if (dropConflict) {
                    dropSweepConflictSeen = true;
                    parkAfterSessionCompletion = false;
                    parkRetryFailStreak = 0;
                    idleMotionBurstActions = 0;
                    resetIdlePatternState();
                } else {
                    parkAfterSessionCompletion = true;
                }
                maybeEmitIdleGateEvent(
                    tick,
                    "idle_gate_session_conflict",
                    details(
                        "hasActiveSession", true,
                        "interactionOnly", false,
                        "activeSession", activeSession,
                        "dropConflict", dropConflict
                    )
                );
                releaseIdleMotorOwnershipIfHeld();
                return;
            }
            maybeEmitIdleGateEvent(
                tick,
                "idle_gate_session_conflict_fishing_afk_override",
                details(
                    "hasActiveSession", true,
                    "interactionOnly", false,
                    "activeSession", activeSession,
                    "dropConflict", dropConflict,
                    "idleContext", normalizeContextName(idleContext),
                    "fishingIdleMode", normalizeFishingIdleMode(preGateFishingIdleMode),
                    "fishingInventoryFullAfkRemainingMs", Math.max(0L, host.fishingInventoryFullAfkRemainingMs())
                )
            );
        }
        boolean interactionWindowClosed = interactionOnlySessionActive && !host.isIdleInterActionWindowOpen();
        if (interactionWindowClosed) {
            maybeEmitIdleGateEvent(
                tick,
                "idle_gate_interaction_window_closed",
                details("hasActiveSession", true, "interactionOnly", true)
            );
        }
        FishingIdleMode fishingIdleMode = resolveFishingIdleMode(idleContext);
        boolean cameraOnlyEligible = interactionWindowClosed;
        boolean cameraOnlyMode = cameraOnlyEligible && IDLE_CAMERA_MICRO_ADJUST_ENABLED;
        boolean idleActionWindowOpen = host.isIdleActionWindowOpen();
        boolean idleCameraWindowOpen = cameraOnlyMode && host.isIdleCameraWindowOpen();
        if (!idleActionWindowOpen && !idleCameraWindowOpen) {
            JsonObject gateDetails = host.idleWindowGateSnapshot();
            gateDetails.addProperty("idleActionWindowOpen", idleActionWindowOpen);
            gateDetails.addProperty("idleCameraWindowOpen", idleCameraWindowOpen);
            gateDetails.addProperty("cameraOnlyEligible", cameraOnlyEligible);
            gateDetails.addProperty("cameraOnlyMode", cameraOnlyMode);
            gateDetails.addProperty("idleContext", normalizeContextName(idleContext));
            maybeEmitIdleGateEvent(tick, "idle_gate_action_window_closed", gateDetails);
            releaseIdleMotorOwnershipIfHeld();
            return;
        }
        boolean interactionDelaySatisfied =
            cameraOnlyMode
                ? host.isIdleCameraInteractionDelaySatisfied()
                : host.isIdleInteractionDelaySatisfied();
        if (!interactionDelaySatisfied) {
            maybeEmitIdleGateEvent(
                tick,
                cameraOnlyMode ? "idle_gate_camera_interaction_delay_active" : "idle_gate_interaction_delay_active",
                details(
                    "cameraOnlyMode", cameraOnlyMode,
                    "idleContext", normalizeContextName(idleContext)
                )
            );
            releaseIdleMotorOwnershipIfHeld();
            return;
        }
        if (parkAfterSessionCompletion && tick < nextIdleTick) {
            nextIdleTick = tick;
        }
        if (tick < parkedUntilTick) {
            maybeEmitIdleGateEvent(
                tick,
                "idle_gate_park_dwell_active",
                details("parkedUntilTick", parkedUntilTick, "ticksRemaining", parkedUntilTick - tick)
            );
            releaseIdleMotorOwnershipIfHeld();
            return;
        }
        if (tick < nextIdleTick) {
            maybeEmitIdleGateEvent(
                tick,
                "idle_gate_next_tick_not_reached",
                details("nextIdleTick", nextIdleTick, "ticksRemaining", nextIdleTick - tick)
            );
            releaseIdleMotorOwnershipIfHeld();
            return;
        }
        if (!host.acquireOrRenewIdleMotorOwnership()) {
            idleMotorLeaseHeld = false;
            maybeEmitIdleGateEvent(tick, "idle_gate_motor_owner_unavailable", null);
            return;
        }
        idleMotorLeaseHeld = true;
        if (!host.canPerformIdleMotorActionNow()) {
            maybeEmitIdleGateEvent(tick, "idle_gate_motor_cooldown_active", null);
            releaseIdleMotorOwnershipIfHeld();
            return;
        }

        FatigueSnapshot fatigue = fatigueSnapshot();
        IdleBlendMode blendMode = resolveIdleBlendMode(
            tick,
            idleContext,
            fishingIdleMode,
            cameraOnlyMode,
            fatigue
        );
        IdleActionOutcome outcome = performIdleAction(
            tick,
            idleContext,
            cameraOnlyMode,
            fatigue,
            fishingIdleMode,
            blendMode,
            idleCadenceTuning
        );
        boolean acted = outcome.acted;
        IdleCadenceEnvelope cadenceEnvelope = resolveCadenceEnvelope(
            tick,
            idleContext,
            fishingIdleMode,
            blendMode,
            fatigue,
            acted
        );
        scheduleNextTick(
            tick,
            acted,
            outcome.parked,
            idleContext,
            fatigue,
            fishingIdleMode,
            blendMode,
            cadenceEnvelope,
            idleCadenceTuning
        );
        if (acted && outcome.reason != null) {
            noteIdleActionExecuted(outcome.reason);
            JsonObject details = outcome.details == null ? new JsonObject() : outcome.details;
            details.addProperty("tick", tick);
            details.addProperty("nextIdleTick", nextIdleTick);
            details.addProperty("idleContext", normalizeContextName(idleContext));
            details.addProperty("fishingIdleMode", normalizeFishingIdleMode(resolveFishingIdleMode(idleContext)));
            details.addProperty("blendMode", blendMode.telemetryName());
            details.addProperty("blendModeUntilTick", blendModeUntilTick);
            details.addProperty("blendTransitionReason", lastBlendTransitionReason);
            details.addProperty("cadenceEnvelope", cadenceEnvelope.telemetryName());
            details.addProperty("cadenceEnvelopeUntilTick", cadenceEnvelopeUntilTick);
            details.addProperty("cadenceEnvelopeReason", lastCadenceEnvelopeReason);
            details.addProperty("fatigueLoad", fatigue.loadPercent());
            details.addProperty("fatigueBand", fatigue.band());
            details.addProperty("actionReasonStreak", lastIdleActionReasonStreak);
            details.addProperty("recentActionDiversity", recentActionDiversityScore());
            host.emitIdleEvent(outcome.reason, details);
        } else if (!acted) {
            coolDownIdleActionStreakOnNoop();
            maybeEmitIdleNoopEvent(tick, idleContext, fishingIdleMode, fatigue, blendMode, cadenceEnvelope);
        }
        if (acted) {
            parkRetryFailStreak = 0;
        }
        if (!acted && parkAfterSessionCompletion) {
            parkRetryFailStreak++;
            if (parkRetryFailStreak >= PARK_RETRY_FAIL_STREAK_LIMIT) {
                parkAfterSessionCompletion = false;
                parkRetryFailStreak = 0;
            }
            nextIdleTick = tick + ThreadLocalRandom.current().nextInt(
                PARK_RETRY_MIN_INTERVAL_TICKS,
                PARK_RETRY_MAX_INTERVAL_TICKS + 1
            );
        }
        boolean keepIdleMotorLease = acted && shouldHoldMotorLeaseForOutcome(outcome.reason);
        if (!keepIdleMotorLease) {
            releaseIdleMotorOwnershipIfHeld();
        }
    }

    private void releaseIdleMotorOwnershipIfHeld() {
        if (!idleMotorLeaseHeld) {
            return;
        }
        host.releaseIdleMotorOwnership();
        idleMotorLeaseHeld = false;
    }

    private IdleActionOutcome performIdleAction(
        int tick,
        IdleSkillContext idleContext,
        boolean cameraOnlyMode,
        FatigueSnapshot fatigue,
        FishingIdleMode fishingIdleMode,
        IdleBlendMode blendMode,
        IdleCadenceTuning idleCadenceTuning
    ) {
        if (cameraOnlyMode) {
            if (host.performIdleCameraMicroAdjust()) {
                idleMotionBurstActions++;
                return IdleActionOutcome.acted("idle_camera_micro_adjust", null);
            }
            return IdleActionOutcome.noop();
        }
        int noopBiasPercent = fatigue.idleNoopBiasPercent(6);
        if (noopBiasPercent > 0 && ThreadLocalRandom.current().nextInt(100) < noopBiasPercent) {
            return IdleActionOutcome.noop();
        }
        if (fishingIdleMode == FishingIdleMode.OFF) {
            return IdleActionOutcome.noop();
        }
        boolean offscreenOnlyMode = fishingIdleMode == FishingIdleMode.OFFSCREEN_BIASED;
        String offscreenMoveReason = offscreenMoveReason(idleContext);
        String offscreenHoldSuppressedReason = offscreenHoldSuppressedReason(idleContext);
        FishingIdleMode configuredFishingIdleMode = host.resolveFishingIdleMode(idleContext);
        if (configuredFishingIdleMode == null) {
            configuredFishingIdleMode = FishingIdleMode.STANDARD;
        }
        boolean watchModeOffscreenBiased = offscreenOnlyMode;
        if (offscreenOnlyMode) {
            long clickSerial = Math.max(0L, host.lastInteractionClickSerial());
            boolean hasUnconsumedClick = clickSerial > lastOffscreenBiasHandledClickSerial;
            boolean fishingInventoryFullAfkActive = host.isFishingInventoryFullAfkActive();
            if (!hasUnconsumedClick && !fishingInventoryFullAfkActive) {
                emitOffscreenDebugEvent(
                    tick,
                    "idle_offscreen_watch_mode_waiting_for_new_click",
                    details(
                        "tick", tick,
                        "idleContext", normalizeContextName(idleContext),
                        "lastInteractionClickSerial", clickSerial,
                        "lastHandledClickSerial", lastOffscreenBiasHandledClickSerial
                    )
                );
                emitOffscreenGateSnapshot(
                    tick,
                    "waiting_for_new_click",
                    idleContext,
                    configuredFishingIdleMode,
                    fishingIdleMode,
                    watchModeOffscreenBiased,
                    details(
                        "lastInteractionClickSerial", clickSerial,
                        "lastHandledClickSerial", lastOffscreenBiasHandledClickSerial
                    )
                );
                return IdleActionOutcome.noop();
            }
            if (host.isFishingOffscreenIdleSuppressed()) {
                long suppressionRemainingMs = Math.max(0L, host.fishingOffscreenIdleSuppressionRemainingMs());
                emitOffscreenDebugEvent(
                    tick,
                    offscreenHoldSuppressedReason,
                    details(
                        "tick", tick,
                        "idleContext", normalizeContextName(idleContext),
                        "configuredFishingIdleMode", normalizeFishingIdleMode(configuredFishingIdleMode),
                        "resolvedFishingIdleMode", normalizeFishingIdleMode(fishingIdleMode),
                        "watchModeOffscreenBiased", watchModeOffscreenBiased,
                        "suppressionRemainingMs", suppressionRemainingMs,
                        "mode", "watch_only"
                    )
                );
                emitOffscreenGateSnapshot(
                    tick,
                    "hold_suppressed",
                    idleContext,
                    configuredFishingIdleMode,
                    fishingIdleMode,
                    watchModeOffscreenBiased,
                    details("suppressionRemainingMs", suppressionRemainingMs)
                );
                return IdleActionOutcome.noop();
            }
            if (host.isCursorOutsideClientWindow()) {
                lastOffscreenBiasHandledClickSerial = clickSerial;
                emitOffscreenDebugEvent(
                    tick,
                    "idle_offscreen_watch_mode_outside_hold",
                    details(
                        "tick", tick,
                        "idleContext", normalizeContextName(idleContext),
                        "configuredFishingIdleMode", normalizeFishingIdleMode(configuredFishingIdleMode),
                        "resolvedFishingIdleMode", normalizeFishingIdleMode(fishingIdleMode),
                        "watchModeOffscreenBiased", watchModeOffscreenBiased
                    )
                );
                emitOffscreenGateSnapshot(
                    tick,
                    "already_outside_window_watch",
                    idleContext,
                    configuredFishingIdleMode,
                    fishingIdleMode,
                    watchModeOffscreenBiased,
                    details("lastInteractionClickSerial", clickSerial)
                );
                return IdleActionOutcome.noop();
            }
            host.emitIdleEvent(
                "offscreen_attempt_selected",
                details(
                    "tick", tick,
                    "idleContext", normalizeContextName(idleContext),
                    "configuredFishingIdleMode", normalizeFishingIdleMode(configuredFishingIdleMode),
                    "resolvedFishingIdleMode", normalizeFishingIdleMode(fishingIdleMode),
                    "watchModeOffscreenBiased", watchModeOffscreenBiased,
                    "mode", "watch_only",
                    "offscreenActionReason", offscreenMoveReason,
                    "fishingInventoryFullAfkActive", fishingInventoryFullAfkActive,
                    "fishingInventoryFullAfkRemainingMs", Math.max(0L, host.fishingInventoryFullAfkRemainingMs())
                )
            );
            Optional<Point> offscreenTarget = attemptOffscreenMoveAvoidingRepeat(
                tick,
                offscreenMoveReason,
                host::resolveIdleOffscreenTargetScreenPoint
            );
            if (offscreenTarget.isPresent()) {
                idleMotionBurstActions++;
                if (hasUnconsumedClick) {
                    lastOffscreenBiasHandledClickSerial = clickSerial;
                }
                JsonObject details = targetDetails(offscreenTarget.get());
                details.addProperty("forcedByMode", true);
                details.addProperty("mode", "watch_only");
                details.addProperty(
                    "trigger",
                    fishingInventoryFullAfkActive && !hasUnconsumedClick ? "inventory_full_afk" : "click_serial"
                );
                details.addProperty("triggeredByClickSerial", clickSerial);
                details.addProperty("offscreenActionReason", offscreenMoveReason);
                return IdleActionOutcome.actedWithDetails(offscreenMoveReason, details);
            }
            emitOffscreenDebugEvent(
                tick,
                "idle_offscreen_watch_mode_target_unavailable",
                details(
                    "idleContext", normalizeContextName(idleContext),
                    "fishingIdleMode", normalizeFishingIdleMode(resolveFishingIdleMode(idleContext)),
                    "forcedByMode", true,
                    "mode", "watch_only"
                )
            );
            emitOffscreenGateSnapshot(
                tick,
                "target_unavailable_watch",
                idleContext,
                configuredFishingIdleMode,
                fishingIdleMode,
                watchModeOffscreenBiased,
                details("offscreenActionReason", offscreenMoveReason)
            );
            return IdleActionOutcome.noop();
        }
        IdleBehaviorProfile profile = resolveBehaviorProfile(idleContext, fishingIdleMode, idleCadenceTuning);
        profile = applyBlendToProfile(profile, blendMode, offscreenOnlyMode);
        if (shouldAttemptParking(profile)) {
            IdleActionOutcome parked = performHandParking(tick);
            if (parked.acted) {
                parkAfterSessionCompletion = false;
                parkRetryFailStreak = 0;
                idleMotionBurstActions = 0;
                return parked;
            }
        }
        int hoverWeight = adjustedActionWeight("idle_hover_move", profile.hoverChancePercent());
        int driftWeight = adjustedActionWeight("idle_drift_move", profile.driftChancePercent());
        int offscreenWeight =
            offscreenOnlyMode
                ? adjustedActionWeight(offscreenMoveReason, profile.offscreenParkChancePercent())
                : 0;
        int cameraWeight = adjustedActionWeight("idle_camera_micro_adjust", profile.cameraChancePercent());
        int noopWeight = Math.max(0, profile.noopChancePercent());
        int totalPercent = Math.max(1, hoverWeight + driftWeight + offscreenWeight + cameraWeight + noopWeight);
        int roll = ThreadLocalRandom.current().nextInt(totalPercent);
        int threshold = hoverWeight;
        if (roll < threshold) {
            Optional<Point> hoverTarget = attemptCanvasMoveAvoidingRepeat(
                tick,
                "idle_hover_move",
                host::resolveIdleHoverTargetCanvasPoint
            );
            if (hoverTarget.isPresent()) {
                idleMotionBurstActions++;
                return IdleActionOutcome.acted("idle_hover_move", hoverTarget.get());
            }
            Optional<Point> driftFallback = attemptCanvasMoveAvoidingRepeat(
                tick,
                "idle_drift_move",
                host::resolveIdleDriftTargetCanvasPoint
            );
            if (driftFallback.isPresent()) {
                idleMotionBurstActions++;
                JsonObject details = targetDetails(driftFallback.get());
                details.addProperty("fallbackFrom", "hover");
                return IdleActionOutcome.actedWithDetails("idle_drift_move", details);
            }
            return IdleActionOutcome.noop();
        }
        threshold += driftWeight;
        if (roll < threshold) {
            Optional<Point> driftTarget = attemptCanvasMoveAvoidingRepeat(
                tick,
                "idle_drift_move",
                host::resolveIdleDriftTargetCanvasPoint
            );
            if (driftTarget.isPresent()) {
                idleMotionBurstActions++;
                return IdleActionOutcome.acted("idle_drift_move", driftTarget.get());
            }
            Optional<Point> hoverFallback = attemptCanvasMoveAvoidingRepeat(
                tick,
                "idle_hover_move",
                host::resolveIdleHoverTargetCanvasPoint
            );
            if (hoverFallback.isPresent()) {
                idleMotionBurstActions++;
                JsonObject details = targetDetails(hoverFallback.get());
                details.addProperty("fallbackFrom", "drift");
                return IdleActionOutcome.actedWithDetails("idle_hover_move", details);
            }
            return IdleActionOutcome.noop();
        }
        threshold += offscreenWeight;
        if (roll < threshold) {
            if (host.isCursorOutsideClientWindow()) {
                emitOffscreenDebugEvent(
                    tick,
                    "idle_offscreen_already_outside_window",
                    details(
                        "tick", tick,
                        "idleContext", normalizeContextName(idleContext),
                        "configuredFishingIdleMode", normalizeFishingIdleMode(configuredFishingIdleMode),
                        "resolvedFishingIdleMode", normalizeFishingIdleMode(fishingIdleMode),
                        "watchModeOffscreenBiased", watchModeOffscreenBiased
                    )
                );
                emitOffscreenGateSnapshot(
                    tick,
                    "already_outside_window_weighted",
                    idleContext,
                    configuredFishingIdleMode,
                    fishingIdleMode,
                    watchModeOffscreenBiased,
                    details("offscreenActionReason", offscreenMoveReason)
                );
                return IdleActionOutcome.noop();
            }
            host.emitIdleEvent(
                "offscreen_attempt_selected",
                details(
                    "tick", tick,
                    "idleContext", normalizeContextName(idleContext),
                    "configuredFishingIdleMode", normalizeFishingIdleMode(configuredFishingIdleMode),
                    "resolvedFishingIdleMode", normalizeFishingIdleMode(fishingIdleMode),
                    "watchModeOffscreenBiased", watchModeOffscreenBiased,
                    "offscreenActionReason", offscreenMoveReason
                )
            );
            Optional<Point> offscreenTarget = attemptOffscreenMoveAvoidingRepeat(
                tick,
                offscreenMoveReason,
                host::resolveIdleOffscreenTargetScreenPoint
            );
            if (offscreenTarget.isPresent()) {
                idleMotionBurstActions++;
                JsonObject details = targetDetails(offscreenTarget.get());
                details.addProperty("forcedByMode", offscreenOnlyMode);
                details.addProperty("offscreenActionReason", offscreenMoveReason);
                return IdleActionOutcome.actedWithDetails(offscreenMoveReason, details);
            }
            emitOffscreenDebugEvent(
                tick,
                "idle_offscreen_biased_target_unavailable",
                details(
                    "idleContext", normalizeContextName(idleContext),
                    "fishingIdleMode", normalizeFishingIdleMode(resolveFishingIdleMode(idleContext)),
                    "forcedByMode", offscreenOnlyMode
                )
            );
            host.emitIdleEvent(
                "offscreen_target_unavailable",
                details(
                    "tick", tick,
                    "idleContext", normalizeContextName(idleContext),
                    "configuredFishingIdleMode", normalizeFishingIdleMode(configuredFishingIdleMode),
                    "resolvedFishingIdleMode", normalizeFishingIdleMode(fishingIdleMode),
                    "watchModeOffscreenBiased", watchModeOffscreenBiased,
                    "offscreenActionReason", offscreenMoveReason,
                    "failureReason", "runtime_offscreen_branch_no_target"
                )
            );
            emitOffscreenGateSnapshot(
                tick,
                "target_unavailable_weighted",
                idleContext,
                configuredFishingIdleMode,
                fishingIdleMode,
                watchModeOffscreenBiased,
                details("offscreenActionReason", offscreenMoveReason)
            );
            return IdleActionOutcome.noop();
        }
        threshold += cameraWeight;
        if (IDLE_CAMERA_MICRO_ADJUST_ENABLED && roll < threshold && host.performIdleCameraMicroAdjust()) {
            idleMotionBurstActions++;
            return IdleActionOutcome.acted("idle_camera_micro_adjust", null);
        }
        return IdleActionOutcome.noop();
    }

    private boolean shouldAttemptParking(IdleBehaviorProfile profile) {
        if (parkAfterSessionCompletion) {
            return true;
        }
        int burstMinActions = Math.max(0, profile.parkAfterBurstMinActions());
        if (idleMotionBurstActions < burstMinActions) {
            return false;
        }
        int parkChancePercent = Math.max(0, Math.min(100, profile.parkAfterBurstChancePercent()));
        if (parkChancePercent <= 0) {
            return false;
        }
        return ThreadLocalRandom.current().nextInt(100) < parkChancePercent;
    }

    private IdleActionOutcome performHandParking(int tick) {
        Optional<Point> parkTarget = attemptCanvasMoveAvoidingRepeat(
            tick,
            "idle_hand_park_move",
            host::resolveIdleParkingTargetCanvasPoint
        );
        if (parkTarget.isEmpty()) {
            return IdleActionOutcome.noop();
        }
        Point target = parkTarget.get();
        int dwellTicks = ThreadLocalRandom.current().nextInt(PARK_DWELL_MIN_TICKS, PARK_DWELL_MAX_TICKS + 1);
        parkedUntilTick = tick + Math.max(1, dwellTicks);
        JsonObject details = new JsonObject();
        details.addProperty("targetX", target.x);
        details.addProperty("targetY", target.y);
        details.addProperty("dwellTicks", dwellTicks);
        return IdleActionOutcome.parked("idle_hand_park_move", details);
    }

    private IdleBlendMode resolveIdleBlendMode(
        int tick,
        IdleSkillContext idleContext,
        FishingIdleMode fishingIdleMode,
        boolean cameraOnlyMode,
        FatigueSnapshot fatigue
    ) {
        if (cameraOnlyMode) {
            activeBlendMode = IdleBlendMode.CAMERA_FOCUS;
            blendModeUntilTick = tick + sampleBlendDurationTicks();
            lastBlendTransitionReason = "camera_only_forced";
            return activeBlendMode;
        }
        if (fishingIdleMode == FishingIdleMode.OFFSCREEN_BIASED) {
            activeBlendMode = IdleBlendMode.OFFSCREEN_FOCUS;
            blendModeUntilTick = tick + sampleBlendDurationTicks();
            lastBlendTransitionReason = "offscreen_forced";
            return activeBlendMode;
        }
        if (tick < blendModeUntilTick) {
            return activeBlendMode;
        }
        IdleBlendMode[] candidates = resolveBlendCandidates(idleContext);
        if (candidates.length <= 0) {
            activeBlendMode = IdleBlendMode.BASELINE;
            blendModeUntilTick = tick + sampleBlendDurationTicks();
            lastBlendTransitionReason = "candidate_fallback";
            return activeBlendMode;
        }
        IdleBlendMode previousMode = activeBlendMode;
        int transitionPressure = blendTransitionPressure();
        IdleBlendMode nextMode = pickWeightedBlendMode(candidates, previousMode, fatigue, transitionPressure);
        activeBlendMode = nextMode;
        blendModeUntilTick = tick + sampleBlendDurationTicks();
        lastBlendTransitionReason = previousMode == nextMode
            ? "weighted_stay"
            : "weighted_shift_" + transitionPressure;
        return activeBlendMode;
    }

    private IdleBehaviorProfile applyBlendToProfile(
        IdleBehaviorProfile baseProfile,
        IdleBlendMode blendMode,
        boolean offscreenOnlyMode
    ) {
        IdleBehaviorProfile base = baseProfile == null ? FALLBACK_BEHAVIOR_PROFILE : baseProfile;
        int hover = base.hoverChancePercent();
        int drift = base.driftChancePercent();
        int offscreen = base.offscreenParkChancePercent();
        int camera = base.cameraChancePercent();
        int noop = base.noopChancePercent();
        int parkMinActions = base.parkAfterBurstMinActions();
        int parkChance = base.parkAfterBurstChancePercent();
        switch (blendMode) {
            case HOVER_FOCUS:
                hover += 12;
                drift -= 6;
                camera -= 2;
                noop -= 4;
                parkChance -= 6;
                break;
            case DRIFT_FOCUS:
                hover -= 6;
                drift += 12;
                camera -= 2;
                noop -= 4;
                parkChance -= 6;
                break;
            case CAMERA_FOCUS:
                hover -= 4;
                drift -= 3;
                camera += 10;
                noop -= 2;
                break;
            case PARK_FOCUS:
                hover -= 4;
                drift -= 3;
                noop += 8;
                parkMinActions = Math.max(0, parkMinActions - 1);
                parkChance += 14;
                break;
            case OFFSCREEN_FOCUS:
                hover -= 5;
                drift -= 4;
                offscreen += 14;
                noop -= 3;
                parkChance -= 4;
                break;
            case BASELINE:
            default:
                break;
        }
        if (offscreenOnlyMode) {
            offscreen = Math.max(offscreen, 86);
            hover = Math.min(hover, 6);
            drift = Math.min(drift, 6);
        }
        return new IdleBehaviorProfile(
            clampPercent(hover),
            clampPercent(drift),
            clampPercent(offscreen),
            clampPercent(camera),
            clampPercent(noop),
            Math.max(0, parkMinActions),
            clampPercent(parkChance)
        );
    }

    private static IdleBlendMode[] resolveBlendCandidates(IdleSkillContext idleContext) {
        if (idleContext == IdleSkillContext.FISHING || idleContext == IdleSkillContext.WOODCUTTING) {
            return FISHING_BLEND_MODES;
        }
        return GLOBAL_BLEND_MODES;
    }

    private IdleCadenceEnvelope resolveCadenceEnvelope(
        int tick,
        IdleSkillContext idleContext,
        FishingIdleMode fishingIdleMode,
        IdleBlendMode blendMode,
        FatigueSnapshot fatigue,
        boolean acted
    ) {
        if (fishingIdleMode == FishingIdleMode.OFFSCREEN_BIASED) {
            activeCadenceEnvelope = IdleCadenceEnvelope.PATIENT;
            cadenceEnvelopeUntilTick = tick + sampleCadenceEnvelopeDurationTicks();
            lastCadenceEnvelopeReason = "offscreen_forced";
            return activeCadenceEnvelope;
        }
        if (tick < cadenceEnvelopeUntilTick) {
            return activeCadenceEnvelope;
        }
        int pressure = blendTransitionPressure();
        IdleCadenceEnvelope nextEnvelope = pickWeightedCadenceEnvelope(
            blendMode,
            fatigue,
            pressure,
            acted,
            idleContext
        );
        IdleCadenceEnvelope previousEnvelope = activeCadenceEnvelope;
        activeCadenceEnvelope = nextEnvelope;
        cadenceEnvelopeUntilTick = tick + sampleCadenceEnvelopeDurationTicks();
        lastCadenceEnvelopeReason = previousEnvelope == nextEnvelope
            ? "weighted_stay"
            : "weighted_shift_" + pressure;
        return activeCadenceEnvelope;
    }

    private static int sampleBlendDurationTicks() {
        return ThreadLocalRandom.current().nextInt(IDLE_BLEND_WINDOW_MIN_TICKS, IDLE_BLEND_WINDOW_MAX_TICKS + 1);
    }

    private static int sampleCadenceEnvelopeDurationTicks() {
        return ThreadLocalRandom.current().nextInt(
            IDLE_CADENCE_ENVELOPE_WINDOW_MIN_TICKS,
            IDLE_CADENCE_ENVELOPE_WINDOW_MAX_TICKS + 1
        );
    }

    private void resetIdleBlendState() {
        activeBlendMode = IdleBlendMode.BASELINE;
        blendModeUntilTick = Integer.MIN_VALUE;
        lastBlendTransitionReason = "reset";
        activeCadenceEnvelope = IdleCadenceEnvelope.STEADY;
        cadenceEnvelopeUntilTick = Integer.MIN_VALUE;
        lastCadenceEnvelopeReason = "reset";
    }

    private void resetIdleStrategyState() {
        resetIdleBlendState();
        lastScheduledIdleDeltaTicks = Integer.MIN_VALUE;
        lastScheduledIdleDeltaRepeatCount = 0;
        recentActionWriteIndex = 0;
        recentActionHistoryCount = 0;
        for (int i = 0; i < recentActionHistory.length; i++) {
            recentActionHistory[i] = "";
        }
    }

    private void recordActionHistory(String reason) {
        String normalized = reason == null ? "" : reason.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || recentActionHistory.length <= 0) {
            return;
        }
        recentActionHistory[recentActionWriteIndex] = normalized;
        recentActionWriteIndex = (recentActionWriteIndex + 1) % recentActionHistory.length;
        recentActionHistoryCount = Math.min(recentActionHistory.length, recentActionHistoryCount + 1);
    }

    private void scheduleNextTick(
        int tick,
        boolean acted,
        boolean parked,
        IdleSkillContext idleContext,
        FatigueSnapshot fatigue,
        FishingIdleMode fishingIdleMode,
        IdleBlendMode blendMode,
        IdleCadenceEnvelope cadenceEnvelope,
        IdleCadenceTuning idleCadenceTuning
    ) {
        ActivityIdlePolicy activityPolicy = resolveActivityIdlePolicy(idleContext);
        ActivityIdleCadenceWindow cadenceWindow = activityPolicy.resolveCadenceWindow(fishingIdleMode);
        int min;
        int max;
        if (usesSkillingCadenceOverrides(idleContext)) {
            if (acted) {
                min = idleCadenceTuning.resolveFishingDbParityIdleMinIntervalTicks(
                    cadenceWindow.actedMinIntervalTicks()
                );
                max = idleCadenceTuning.resolveFishingDbParityIdleMaxIntervalTicks(
                    cadenceWindow.actedMaxIntervalTicks(),
                    min
                );
            } else {
                min = idleCadenceTuning.resolveFishingDbParityIdleRetryMinIntervalTicks(
                    cadenceWindow.retryMinIntervalTicks()
                );
                max = idleCadenceTuning.resolveFishingDbParityIdleRetryMaxIntervalTicks(
                    cadenceWindow.retryMaxIntervalTicks(),
                    min
                );
            }
        } else {
            min = acted ? cadenceWindow.actedMinIntervalTicks() : cadenceWindow.retryMinIntervalTicks();
            max = acted ? cadenceWindow.actedMaxIntervalTicks() : cadenceWindow.retryMaxIntervalTicks();
        }
        int cadenceMinShift = 0;
        int cadenceMaxShift = 0;
        switch (blendMode) {
            case HOVER_FOCUS:
                cadenceMinShift = acted ? -2 : -1;
                cadenceMaxShift = acted ? -3 : -2;
                break;
            case DRIFT_FOCUS:
                cadenceMinShift = acted ? -1 : 0;
                cadenceMaxShift = acted ? -2 : -1;
                break;
            case CAMERA_FOCUS:
                cadenceMinShift = acted ? 1 : 1;
                cadenceMaxShift = acted ? 2 : 3;
                break;
            case PARK_FOCUS:
                cadenceMinShift = acted ? 2 : 1;
                cadenceMaxShift = acted ? 4 : 3;
                break;
            case OFFSCREEN_FOCUS:
                cadenceMinShift = acted ? 1 : 0;
                cadenceMaxShift = acted ? 3 : 2;
                break;
            case BASELINE:
            default:
                break;
        }
        min = Math.max(1, min + cadenceMinShift);
        max = Math.max(min, max + cadenceMaxShift);
        int envelopeMinShift = 0;
        int envelopeMaxShift = 0;
        int envelopeExtraTicksMax = 0;
        switch (cadenceEnvelope) {
            case LOOSE:
                envelopeMinShift = acted ? -1 : -1;
                envelopeMaxShift = acted ? 3 : 2;
                envelopeExtraTicksMax = 2;
                break;
            case BURSTY:
                envelopeMinShift = acted ? -3 : -2;
                envelopeMaxShift = acted ? 1 : 1;
                envelopeExtraTicksMax = 3;
                break;
            case PATIENT:
                envelopeMinShift = acted ? 2 : 1;
                envelopeMaxShift = acted ? 5 : 4;
                envelopeExtraTicksMax = 5;
                break;
            case STEADY:
            default:
                break;
        }
        min = Math.max(1, min + envelopeMinShift);
        max = Math.max(min, max + envelopeMaxShift);
        int fatigueExtraTicksMax =
            fishingIdleMode == FishingIdleMode.OFFSCREEN_BIASED
                ? 1
                : ((idleContext == IdleSkillContext.FISHING || idleContext == IdleSkillContext.WOODCUTTING) ? 2 : 3);
        int fatigueExtraTicksCap = fatigue.idleIntervalExtraTicks(fatigueExtraTicksMax);
        int delta = ThreadLocalRandom.current().nextInt(min, max + 1);
        if (envelopeExtraTicksMax > 0) {
            delta += ThreadLocalRandom.current().nextInt(envelopeExtraTicksMax + 1);
            if (cadenceEnvelope == IdleCadenceEnvelope.BURSTY && ThreadLocalRandom.current().nextInt(100) < 16) {
                delta += ThreadLocalRandom.current().nextInt(2, 6);
            }
        }
        if (fatigueExtraTicksCap > 0) {
            delta += ThreadLocalRandom.current().nextInt(fatigueExtraTicksCap + 1);
        }
        delta = applySkillingIntervalVarianceGuard(
            delta,
            min,
            max,
            idleContext,
            fishingIdleMode,
            acted
        );
        if (idleContext == IdleSkillContext.WOODCUTTING && fishingIdleMode != FishingIdleMode.OFFSCREEN_BIASED) {
            delta = applyWoodcutCadenceEntropy(delta, min, max, acted, blendMode, cadenceEnvelope);
            lastScheduledIdleDeltaTicks = delta;
        }
        nextIdleTick = tick + Math.max(1, delta);
        if (parked) {
            nextIdleTick = Math.max(nextIdleTick, parkedUntilTick);
        }
    }

    private int applySkillingIntervalVarianceGuard(
        int delta,
        int min,
        int max,
        IdleSkillContext idleContext,
        FishingIdleMode fishingIdleMode,
        boolean acted
    ) {
        int safeDelta = Math.max(1, delta);
        if (!usesSkillingCadenceOverrides(idleContext) || fishingIdleMode == FishingIdleMode.OFFSCREEN_BIASED) {
            lastScheduledIdleDeltaTicks = safeDelta;
            lastScheduledIdleDeltaRepeatCount = 0;
            return safeDelta;
        }
        int widenedMin = Math.max(1, min - (acted ? 1 : 2));
        int widenedMax = Math.max(widenedMin, max + (acted ? 2 : 3));
        int jitter = ThreadLocalRandom.current().nextInt(-2, 3);
        safeDelta = clampInt(safeDelta + jitter, widenedMin, widenedMax);

        if (safeDelta == lastScheduledIdleDeltaTicks) {
            lastScheduledIdleDeltaRepeatCount++;
            if (lastScheduledIdleDeltaRepeatCount >= 2) {
                int nudge = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
                if (ThreadLocalRandom.current().nextInt(100) < 32) {
                    nudge *= 2;
                }
                safeDelta = clampInt(safeDelta + nudge, widenedMin, widenedMax);
                if (safeDelta == lastScheduledIdleDeltaTicks && widenedMax > widenedMin) {
                    safeDelta = safeDelta >= widenedMax ? safeDelta - 1 : safeDelta + 1;
                }
                lastScheduledIdleDeltaRepeatCount = 0;
            }
        } else {
            lastScheduledIdleDeltaRepeatCount = 0;
        }
        lastScheduledIdleDeltaTicks = safeDelta;
        return safeDelta;
    }

    private int applyWoodcutCadenceEntropy(
        int delta,
        int min,
        int max,
        boolean acted,
        IdleBlendMode blendMode,
        IdleCadenceEnvelope cadenceEnvelope
    ) {
        int widenedMin = Math.max(1, min - (acted ? 2 : 3));
        int widenedMax = Math.max(widenedMin, max + (acted ? 7 : 9));
        int adjusted = clampInt(Math.max(1, delta), widenedMin, widenedMax);

        int hesitationChance = WOODCUT_CADENCE_HESITATION_BASE_CHANCE_PERCENT;
        int microBurstChance = WOODCUT_CADENCE_MICRO_BURST_BASE_CHANCE_PERCENT;
        if (cadenceEnvelope == IdleCadenceEnvelope.PATIENT) {
            hesitationChance += 10;
        } else if (cadenceEnvelope == IdleCadenceEnvelope.BURSTY) {
            microBurstChance += 8;
        } else if (cadenceEnvelope == IdleCadenceEnvelope.LOOSE) {
            hesitationChance += 4;
            microBurstChance += 4;
        }
        if (blendMode == IdleBlendMode.PARK_FOCUS || blendMode == IdleBlendMode.CAMERA_FOCUS) {
            hesitationChance += 6;
        } else if (blendMode == IdleBlendMode.HOVER_FOCUS || blendMode == IdleBlendMode.DRIFT_FOCUS) {
            microBurstChance += 5;
        }
        if (!acted) {
            hesitationChance += 6;
        }
        hesitationChance = clampPercent(hesitationChance);
        microBurstChance = clampPercent(Math.min(100 - hesitationChance, microBurstChance));

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < hesitationChance) {
            int extra = ThreadLocalRandom.current().nextInt(2, acted ? 9 : 12);
            adjusted = clampInt(adjusted + extra, widenedMin, widenedMax);
        } else if (roll < (hesitationChance + microBurstChance)) {
            int reduce = ThreadLocalRandom.current().nextInt(1, 4);
            adjusted = clampInt(adjusted - reduce, widenedMin, widenedMax);
        }

        if (adjusted == lastScheduledIdleDeltaTicks
            && ThreadLocalRandom.current().nextInt(100) < WOODCUT_CADENCE_REPEAT_BREAK_CHANCE_PERCENT
            && widenedMax > widenedMin) {
            int nudge = ThreadLocalRandom.current().nextBoolean()
                ? ThreadLocalRandom.current().nextInt(1, 4)
                : -ThreadLocalRandom.current().nextInt(1, 4);
            adjusted = clampInt(adjusted + nudge, widenedMin, widenedMax);
            if (adjusted == lastScheduledIdleDeltaTicks) {
                adjusted = adjusted >= widenedMax ? adjusted - 1 : adjusted + 1;
            }
        }
        return Math.max(1, adjusted);
    }

    private Optional<Point> attemptCanvasMoveAvoidingRepeat(
        int tick,
        String reason,
        Supplier<Optional<Point>> targetSupplier
    ) {
        return attemptPointMoveAvoidingRepeat(
            tick,
            reason,
            targetSupplier,
            host::performIdleCursorMove
        );
    }

    private Optional<Point> attemptOffscreenMoveAvoidingRepeat(
        int tick,
        String reason,
        Supplier<Optional<Point>> targetSupplier
    ) {
        return attemptPointMoveAvoidingRepeat(
            tick,
            reason,
            targetSupplier,
            host::performIdleOffscreenCursorMove
        );
    }

    private Optional<Point> attemptPointMoveAvoidingRepeat(
        int tick,
        String reason,
        Supplier<Optional<Point>> targetSupplier,
        Predicate<Point> moveAction
    ) {
        if (targetSupplier == null || moveAction == null) {
            return Optional.empty();
        }
        int blockedRepeatCount = 0;
        int attempts = Math.max(1, IDLE_REPEAT_RETRY_ATTEMPTS);
        for (int i = 0; i < attempts; i++) {
            Optional<Point> targetOpt = targetSupplier.get();
            if (targetOpt.isEmpty()) {
                continue;
            }
            Point candidate = targetOpt.get();
            if (candidate == null) {
                continue;
            }
            if (repetitionGuard.isRepeated(tick, reason, candidate)) {
                blockedRepeatCount++;
                continue;
            }
            if (moveAction.test(candidate)) {
                repetitionGuard.recordAction(tick, reason, candidate);
                emitPatternDetectionEvents(
                    patternDetector.onMoveExecuted(
                        tick,
                        reason,
                        candidate,
                        blockedRepeatCount,
                        false
                    )
                );
                return Optional.of(new Point(candidate));
            }
        }
        return Optional.empty();
    }

    private IdleBlendMode pickWeightedBlendMode(
        IdleBlendMode[] candidates,
        IdleBlendMode currentMode,
        FatigueSnapshot fatigue,
        int transitionPressure
    ) {
        if (candidates == null || candidates.length <= 0) {
            return IdleBlendMode.BASELINE;
        }
        int[] weights = new int[candidates.length];
        int total = 0;
        for (int i = 0; i < candidates.length; i++) {
            IdleBlendMode mode = candidates[i];
            int weight = baseBlendWeight(mode, currentMode, fatigue, transitionPressure);
            weights[i] = Math.max(1, weight);
            total += weights[i];
        }
        if (total <= 0) {
            return candidates[ThreadLocalRandom.current().nextInt(candidates.length)];
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cursor = 0;
        for (int i = 0; i < candidates.length; i++) {
            cursor += weights[i];
            if (roll < cursor) {
                return candidates[i];
            }
        }
        return candidates[candidates.length - 1];
    }

    private int baseBlendWeight(
        IdleBlendMode candidate,
        IdleBlendMode currentMode,
        FatigueSnapshot fatigue,
        int transitionPressure
    ) {
        int weight = 28;
        if (candidate == currentMode) {
            weight += 6;
            if (transitionPressure >= 5) {
                weight -= 20;
            } else if (transitionPressure >= 3) {
                weight -= 12;
            }
        } else if (transitionPressure >= 4) {
            weight += 8;
        }
        String fatigueBand = fatigue == null ? "low" : safeString(fatigue.band()).toLowerCase(Locale.ROOT);
        switch (candidate) {
            case HOVER_FOCUS:
            case DRIFT_FOCUS:
                if ("low".equals(fatigueBand)) {
                    weight += 7;
                }
                if ("high".equals(fatigueBand)) {
                    weight -= 4;
                }
                break;
            case CAMERA_FOCUS:
                if ("medium".equals(fatigueBand)) {
                    weight += 4;
                }
                if ("high".equals(fatigueBand)) {
                    weight += 6;
                }
                break;
            case PARK_FOCUS:
                if ("high".equals(fatigueBand)) {
                    weight += 10;
                }
                if (parkAfterSessionCompletion) {
                    weight += 16;
                }
                break;
            case OFFSCREEN_FOCUS:
                weight -= 12;
                break;
            case BASELINE:
            default:
                if (transitionPressure <= 1) {
                    weight += 6;
                }
                break;
        }
        String dominantRecent = dominantRecentActionReason();
        if ("idle_hover_move".equals(dominantRecent) && candidate == IdleBlendMode.HOVER_FOCUS) {
            weight -= 9;
        }
        if ("idle_drift_move".equals(dominantRecent) && candidate == IdleBlendMode.DRIFT_FOCUS) {
            weight -= 9;
        }
        if ("idle_camera_micro_adjust".equals(dominantRecent) && candidate == IdleBlendMode.CAMERA_FOCUS) {
            weight -= 6;
        }
        if ("idle_hand_park_move".equals(dominantRecent) && candidate == IdleBlendMode.PARK_FOCUS) {
            weight -= 8;
        }
        return Math.max(1, weight);
    }

    private IdleCadenceEnvelope pickWeightedCadenceEnvelope(
        IdleBlendMode blendMode,
        FatigueSnapshot fatigue,
        int transitionPressure,
        boolean acted,
        IdleSkillContext idleContext
    ) {
        int steadyWeight = 28;
        int looseWeight = 20;
        int burstyWeight = acted ? 16 : 12;
        int patientWeight = 14;
        String fatigueBand = fatigue == null ? "low" : safeString(fatigue.band()).toLowerCase(Locale.ROOT);
        if ("high".equals(fatigueBand)) {
            patientWeight += 12;
            burstyWeight -= 6;
            looseWeight -= 3;
        } else if ("low".equals(fatigueBand)) {
            burstyWeight += 6;
            looseWeight += 4;
        }
        if (transitionPressure >= 4) {
            looseWeight += 8;
            burstyWeight += 8;
            steadyWeight -= 6;
        } else if (transitionPressure <= 1) {
            steadyWeight += 10;
        }
        if (blendMode == IdleBlendMode.PARK_FOCUS || parkAfterSessionCompletion) {
            patientWeight += 10;
        }
        if (blendMode == IdleBlendMode.HOVER_FOCUS || blendMode == IdleBlendMode.DRIFT_FOCUS) {
            burstyWeight += 5;
        }
        if (idleContext == IdleSkillContext.FISHING || idleContext == IdleSkillContext.WOODCUTTING) {
            looseWeight += 3;
            burstyWeight += 2;
        }
        int[] weights = new int[] {
            Math.max(1, steadyWeight),
            Math.max(1, looseWeight),
            Math.max(1, burstyWeight),
            Math.max(1, patientWeight)
        };
        int total = weights[0] + weights[1] + weights[2] + weights[3];
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        int cursor = weights[0];
        if (roll < cursor) {
            return IdleCadenceEnvelope.STEADY;
        }
        cursor += weights[1];
        if (roll < cursor) {
            return IdleCadenceEnvelope.LOOSE;
        }
        cursor += weights[2];
        if (roll < cursor) {
            return IdleCadenceEnvelope.BURSTY;
        }
        return IdleCadenceEnvelope.PATIENT;
    }

    private int blendTransitionPressure() {
        int pressure = 0;
        if (lastIdleActionReasonStreak >= IDLE_ACTION_STREAK_HARD_LIMIT) {
            pressure += 3;
        } else if (lastIdleActionReasonStreak >= IDLE_ACTION_STREAK_SOFT_LIMIT + 1) {
            pressure += 2;
        }
        int diversity = recentActionDiversityScore();
        if (recentActionHistoryCount >= 4 && diversity <= 2) {
            pressure += 2;
        } else if (recentActionHistoryCount >= 4 && diversity <= 3) {
            pressure += 1;
        }
        return Math.max(0, Math.min(7, pressure));
    }

    private int recentActionDiversityScore() {
        if (recentActionHistoryCount <= 0) {
            return 0;
        }
        int unique = 0;
        String[] seen = new String[IDLE_ACTION_HISTORY_SIZE];
        int seenCount = 0;
        int count = Math.min(recentActionHistoryCount, recentActionHistory.length);
        for (int i = 0; i < count; i++) {
            String reason = recentActionHistory[i];
            if (reason == null || reason.isBlank()) {
                continue;
            }
            boolean exists = false;
            for (int j = 0; j < seenCount; j++) {
                if (reason.equals(seen[j])) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                seen[seenCount] = reason;
                seenCount++;
                unique++;
            }
        }
        return unique;
    }

    private String dominantRecentActionReason() {
        int count = Math.min(recentActionHistoryCount, recentActionHistory.length);
        if (count <= 0) {
            return "";
        }
        String dominant = "";
        int dominantCount = 0;
        for (int i = 0; i < count; i++) {
            String reason = recentActionHistory[i];
            if (reason == null || reason.isBlank()) {
                continue;
            }
            int localCount = 0;
            for (int j = 0; j < count; j++) {
                if (reason.equals(recentActionHistory[j])) {
                    localCount++;
                }
            }
            if (localCount > dominantCount) {
                dominant = reason;
                dominantCount = localCount;
            }
        }
        return dominant;
    }

    private int adjustedActionWeight(String reason, int baseWeight) {
        int weight = Math.max(0, baseWeight);
        if (weight <= 0 || lastIdleActionReasonStreak <= 1) {
            return weight;
        }
        String normalizedReason = reason == null ? "" : reason.trim().toLowerCase(Locale.ROOT);
        if (normalizedReason.isEmpty()) {
            return weight;
        }
        int streak = Math.max(0, lastIdleActionReasonStreak);
        if (normalizedReason.equals(lastIdleActionReason)) {
            int softOver = Math.max(0, streak - IDLE_ACTION_STREAK_SOFT_LIMIT);
            int hardOver = Math.max(0, streak - IDLE_ACTION_STREAK_HARD_LIMIT);
            int penalty = (softOver * IDLE_ACTION_STREAK_SOFT_PENALTY_STEP_PERCENT)
                + (hardOver * IDLE_ACTION_STREAK_HARD_PENALTY_STEP_PERCENT)
                + ThreadLocalRandom.current().nextInt(0, 5);
            penalty = Math.min(IDLE_ACTION_STREAK_PENALTY_CAP_PERCENT, Math.max(0, penalty));
            int adjusted = (int) Math.round(weight * ((100.0 - penalty) / 100.0));
            return Math.max(1, adjusted);
        }
        int rebound = Math.min(
            IDLE_ACTION_STREAK_REBOUND_CAP_PERCENT,
            Math.max(0, streak - IDLE_ACTION_STREAK_SOFT_LIMIT) * IDLE_ACTION_STREAK_REBOUND_STEP_PERCENT
        );
        if (rebound <= 0) {
            return weight;
        }
        int bonus = (int) Math.round(weight * (rebound / 100.0));
        return Math.max(1, weight + Math.max(1, bonus));
    }

    private void noteIdleActionExecuted(String reason) {
        String normalizedReason = reason == null ? "" : reason.trim().toLowerCase(Locale.ROOT);
        if (normalizedReason.isEmpty()) {
            return;
        }
        recordActionHistory(normalizedReason);
        if (normalizedReason.equals(lastIdleActionReason)) {
            lastIdleActionReasonStreak = Math.min(12, lastIdleActionReasonStreak + 1);
            return;
        }
        lastIdleActionReason = normalizedReason;
        lastIdleActionReasonStreak = 1;
    }

    private void coolDownIdleActionStreakOnNoop() {
        recordActionHistory("idle_noop_sampled");
        if (lastIdleActionReasonStreak <= 0) {
            return;
        }
        lastIdleActionReasonStreak = Math.max(0, lastIdleActionReasonStreak - 1);
        if (lastIdleActionReasonStreak == 0) {
            lastIdleActionReason = "";
        }
    }

    private void resetIdlePatternState() {
        repetitionGuard.reset();
        patternDetector.reset();
        lastIdleActionReason = "";
        lastIdleActionReasonStreak = 0;
        resetIdleStrategyState();
    }

    private void emitPatternDetectionEvents(List<IdleActionPatternDetector.DetectionEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (IdleActionPatternDetector.DetectionEvent event : events) {
            if (event == null || event.reason == null || event.reason.isBlank()) {
                continue;
            }
            JsonObject payload = event.details == null ? new JsonObject() : event.details.deepCopy();
            if (!payload.has("nextIdleTick")) {
                payload.addProperty("nextIdleTick", nextIdleTick);
            }
            host.emitIdleEvent(event.reason, payload);
        }
    }

    private static final class IdleActionOutcome {
        private final boolean acted;
        private final boolean parked;
        private final String reason;
        private final JsonObject details;

        private IdleActionOutcome(boolean acted, boolean parked, String reason, JsonObject details) {
            this.acted = acted;
            this.parked = parked;
            this.reason = reason;
            this.details = details;
        }

        private static IdleActionOutcome noop() {
            return new IdleActionOutcome(false, false, null, null);
        }

        private static IdleActionOutcome acted(String reason, Point target) {
            return actedWithDetails(reason, targetDetails(target));
        }

        private static IdleActionOutcome actedWithDetails(String reason, JsonObject details) {
            return new IdleActionOutcome(true, false, reason, details);
        }

        private static IdleActionOutcome parked(String reason, JsonObject details) {
            return new IdleActionOutcome(true, true, reason, details);
        }
    }

    private IdleSkillContext resolveSkillContext() {
        IdleSkillContext context = host.resolveIdleSkillContext();
        return context == null ? IdleSkillContext.GLOBAL : context;
    }

    private IdleBehaviorProfile resolveBehaviorProfile(
        IdleSkillContext context,
        FishingIdleMode fishingIdleMode,
        IdleCadenceTuning idleCadenceTuning
    ) {
        ActivityIdlePolicy activityPolicy = resolveActivityIdlePolicy(context);
        IdleBehaviorProfile resolved = activityPolicy.resolveBehaviorProfile(fishingIdleMode);
        if (usesSkillingCadenceOverrides(context)) {
            return idleCadenceTuning.applyFishingProfile(resolved);
        }
        return resolved;
    }

    private static boolean usesSkillingCadenceOverrides(IdleSkillContext context) {
        return context == IdleSkillContext.FISHING || context == IdleSkillContext.WOODCUTTING;
    }

    private ActivityIdlePolicy resolveActivityIdlePolicy(IdleSkillContext context) {
        ActivityIdlePolicy resolved = host.resolveActivityIdlePolicy(context);
        if (resolved != null) {
            return resolved;
        }
        return FALLBACK_IDLE_POLICY_REGISTRY.resolveForContext(context);
    }

    private static String normalizeContextName(IdleSkillContext context) {
        if (context == null) {
            return "global";
        }
        return context.name().toLowerCase(Locale.ROOT);
    }

    private FishingIdleMode resolveFishingIdleMode(IdleSkillContext context) {
        FishingIdleMode configuredMode = host.resolveFishingIdleMode(context);
        if (configuredMode == null) {
            ActivityIdlePolicy policy = resolveActivityIdlePolicy(context);
            configuredMode = policy == null ? FishingIdleMode.STANDARD : policy.fishingIdleMode();
        }
        if (configuredMode == FishingIdleMode.OFF) {
            return FishingIdleMode.OFF;
        }
        if (configuredMode == FishingIdleMode.OFFSCREEN_BIASED) {
            return FishingIdleMode.OFFSCREEN_BIASED;
        }
        return FishingIdleMode.STANDARD;
    }

    private static String normalizeFishingIdleMode(FishingIdleMode mode) {
        FishingIdleMode safeMode = mode == null ? FishingIdleMode.STANDARD : mode;
        return safeMode.name().toLowerCase(Locale.ROOT);
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static int clampInt(int value, int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        return Math.max(lo, Math.min(hi, value));
    }

    private void maybeEmitIdleGateEvent(int tick, String reason, JsonObject details) {
        String safeReason = reason == null ? "" : reason.trim();
        if (safeReason.isEmpty()) {
            return;
        }
        boolean reasonChanged = !safeReason.equals(lastIdleGateTelemetryReason);
        int elapsedTicks = elapsedTicksSince(tick, lastIdleGateTelemetryTick);
        if (!reasonChanged && elapsedTicks < IDLE_GATE_TELEMETRY_MIN_INTERVAL_TICKS) {
            return;
        }
        lastIdleGateTelemetryTick = tick;
        lastIdleGateTelemetryReason = safeReason;
        JsonObject payload = details == null ? new JsonObject() : details;
        payload.addProperty("tick", tick);
        payload.addProperty("nextIdleTick", nextIdleTick);
        payload.addProperty("parkAfterSessionCompletion", parkAfterSessionCompletion);
        host.emitIdleEvent(safeReason, payload);
    }

    private void maybeEmitIdleNoopEvent(
        int tick,
        IdleSkillContext idleContext,
        FishingIdleMode fishingIdleMode,
        FatigueSnapshot fatigue,
        IdleBlendMode blendMode,
        IdleCadenceEnvelope cadenceEnvelope
    ) {
        int elapsedTicks = elapsedTicksSince(tick, lastIdleNoopTelemetryTick);
        if (elapsedTicks < IDLE_NOOP_TELEMETRY_MIN_INTERVAL_TICKS) {
            return;
        }
        lastIdleNoopTelemetryTick = tick;
        JsonObject details = new JsonObject();
        details.addProperty("tick", tick);
        details.addProperty("nextIdleTick", nextIdleTick);
        details.addProperty("idleContext", normalizeContextName(idleContext));
        details.addProperty("fishingIdleMode", normalizeFishingIdleMode(fishingIdleMode));
        details.addProperty("blendMode", blendMode.telemetryName());
        details.addProperty("blendModeUntilTick", blendModeUntilTick);
        details.addProperty("blendTransitionReason", lastBlendTransitionReason);
        details.addProperty("cadenceEnvelope", cadenceEnvelope.telemetryName());
        details.addProperty("cadenceEnvelopeUntilTick", cadenceEnvelopeUntilTick);
        details.addProperty("cadenceEnvelopeReason", lastCadenceEnvelopeReason);
        details.addProperty("parkAfterSessionCompletion", parkAfterSessionCompletion);
        details.addProperty("fatigueLoad", fatigue.loadPercent());
        details.addProperty("fatigueBand", fatigue.band());
        details.addProperty("recentActionDiversity", recentActionDiversityScore());
        host.emitIdleEvent("idle_noop_sampled", details);
    }

    private FatigueSnapshot fatigueSnapshot() {
        FatigueSnapshot snapshot = host.fatigueSnapshot();
        return snapshot == null ? FatigueSnapshot.neutral() : snapshot;
    }

    private IdleCadenceTuning resolveIdleCadenceTuning() {
        IdleCadenceTuning tuning = host.activeIdleCadenceTuning();
        return tuning == null ? IdleCadenceTuning.none() : tuning;
    }

    private static JsonObject targetDetails(Point target) {
        JsonObject details = new JsonObject();
        if (target != null) {
            details.addProperty("targetX", target.x);
            details.addProperty("targetY", target.y);
        }
        return details;
    }

    private static JsonObject details(Object... kvPairs) {
        JsonObject out = new JsonObject();
        if (kvPairs == null) {
            return out;
        }
        int count = kvPairs.length - (kvPairs.length % 2);
        for (int i = 0; i < count; i += 2) {
            String key = kvPairs[i] == null ? "" : String.valueOf(kvPairs[i]);
            if (key.isBlank()) {
                continue;
            }
            Object value = kvPairs[i + 1];
            if (value == null) {
                out.addProperty(key, (String) null);
            } else if (value instanceof Number) {
                out.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                out.addProperty(key, (Boolean) value);
            } else if (value instanceof Character) {
                out.addProperty(key, (Character) value);
            } else {
                out.addProperty(key, String.valueOf(value));
            }
        }
        return out;
    }

    private static boolean shouldHoldMotorLeaseForOutcome(String reason) {
        String safe = reason == null ? "" : reason.trim();
        return "idle_hover_move".equals(safe)
            || "idle_drift_move".equals(safe)
            || "idle_hand_park_move".equals(safe)
            || "idle_fishing_offscreen_park_move".equals(safe)
            || "idle_woodcut_offscreen_park_move".equals(safe);
    }

    private static String offscreenMoveReason(IdleSkillContext idleContext) {
        if (idleContext == IdleSkillContext.WOODCUTTING) {
            return "idle_woodcut_offscreen_park_move";
        }
        return "idle_fishing_offscreen_park_move";
    }

    private static String offscreenHoldSuppressedReason(IdleSkillContext idleContext) {
        if (idleContext == IdleSkillContext.WOODCUTTING) {
            return "idle_woodcut_offscreen_hold_suppressed";
        }
        return "idle_fishing_offscreen_hold_suppressed";
    }

    private void emitOffscreenDebugEvent(int tick, String reason, JsonObject details) {
        if (!IDLE_OFFSCREEN_DEBUG_TELEMETRY_ENABLED) {
            return;
        }
        maybeEmitIdleGateEvent(tick, reason, details);
    }

    private void emitOffscreenGateSnapshot(
        int tick,
        String gateReasonCode,
        IdleSkillContext idleContext,
        FishingIdleMode configuredFishingIdleMode,
        FishingIdleMode resolvedFishingIdleMode,
        boolean watchModeOffscreenBiased,
        JsonObject details
    ) {
        if (!IDLE_OFFSCREEN_DEBUG_TELEMETRY_ENABLED) {
            return;
        }
        String normalizedCode = safeString(gateReasonCode).toLowerCase(Locale.ROOT);
        if (normalizedCode.isEmpty()) {
            return;
        }
        boolean codeChanged = !normalizedCode.equals(lastOffscreenGateSnapshotCode);
        int elapsedTicks = elapsedTicksSince(tick, lastOffscreenGateSnapshotTick);
        if (!codeChanged && elapsedTicks < IDLE_GATE_TELEMETRY_MIN_INTERVAL_TICKS) {
            return;
        }
        lastOffscreenGateSnapshotTick = tick;
        lastOffscreenGateSnapshotCode = normalizedCode;
        JsonObject payload = details == null ? new JsonObject() : details.deepCopy();
        payload.addProperty("tick", tick);
        payload.addProperty("nextIdleTick", nextIdleTick);
        payload.addProperty("parkAfterSessionCompletion", parkAfterSessionCompletion);
        payload.addProperty("gateReasonCode", normalizedCode);
        payload.addProperty("idleContext", normalizeContextName(idleContext));
        payload.addProperty("configuredFishingIdleMode", normalizeFishingIdleMode(configuredFishingIdleMode));
        payload.addProperty("resolvedFishingIdleMode", normalizeFishingIdleMode(resolvedFishingIdleMode));
        payload.addProperty("watchModeOffscreenBiased", watchModeOffscreenBiased);
        host.emitIdleEvent("idle_offscreen_gate_snapshot", payload);
    }

    private static String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private static int elapsedTicksSince(int nowTick, int lastTick) {
        if (lastTick == Integer.MIN_VALUE) {
            return Integer.MAX_VALUE;
        }
        long elapsed = (long) nowTick - (long) lastTick;
        if (elapsed <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (elapsed >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) elapsed;
    }

    private enum IdleCadenceEnvelope {
        STEADY("steady"),
        LOOSE("loose"),
        BURSTY("bursty"),
        PATIENT("patient");

        private final String telemetryName;

        IdleCadenceEnvelope(String telemetryName) {
            this.telemetryName = telemetryName == null ? "steady" : telemetryName;
        }

        private String telemetryName() {
            return telemetryName;
        }
    }

    private enum IdleBlendMode {
        BASELINE("baseline"),
        HOVER_FOCUS("hover_focus"),
        DRIFT_FOCUS("drift_focus"),
        CAMERA_FOCUS("camera_focus"),
        PARK_FOCUS("park_focus"),
        OFFSCREEN_FOCUS("offscreen_focus");

        private final String telemetryName;

        IdleBlendMode(String telemetryName) {
            this.telemetryName = telemetryName == null ? "baseline" : telemetryName;
        }

        private String telemetryName() {
            return telemetryName;
        }
    }
}
