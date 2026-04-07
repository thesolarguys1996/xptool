package com.xptool.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

final class DropRuntime {
    private static final String MOTOR_REASON_TERMINAL_ALIGNMENT_DEFERRED =
        "motor_move_terminal_alignment_deferred";
    private static final String MOTOR_REASON_TERMINAL_ALIGNMENT_RETRY_EXHAUSTED =
        "motor_move_terminal_alignment_retry_exhausted";
    private static final String DROP_CADENCE_PROFILE_PAYLOAD_KEY = "dropCadenceProfile";
    private static final String DROP_CADENCE_TUNING_PAYLOAD_KEY = "dropCadenceTuning";
    private static final long DROP_OUTCOME_CONFIRM_WAIT_MIN_MS = 160L;
    private static final long DROP_OUTCOME_CONFIRM_WAIT_MAX_MS = 320L;
    private static final long DROP_OUTCOME_RECHECK_WAIT_MIN_MS = 100L;
    private static final long DROP_OUTCOME_RECHECK_WAIT_MAX_MS = 220L;
    private static final long DROP_OUTCOME_MISS_CONFIRM_MIN_TOTAL_MS = 620L;
    private static final int DROP_SLOT_UNAVAILABLE_CONFIRM_STREAK = 2;
    private static final int DROP_OUTCOME_MAX_RECHECKS = 1;
    private static final long DROP_MISS_RECOVERY_COOLDOWN_MIN_MS = 300L;
    private static final long DROP_MISS_RECOVERY_COOLDOWN_MAX_MS = 760L;
    private static final long DROP_MISS_RECOVERY_STREAK_STEP_MS = 120L;
    private static final long DROP_MISS_RECOVERY_STREAK_CAP_MS = 920L;
    private static final int DROP_MISS_IMMEDIATE_RETRY_BASE_CHANCE_PERCENT = 58;
    private static final int DROP_MISS_IMMEDIATE_RETRY_FATIGUE_BONUS_MAX_PERCENT = 16;
    private static final int DROP_MISS_IMMEDIATE_RETRY_STREAK_PENALTY_STEP_PERCENT = 10;
    private static final int DROP_MISS_IMMEDIATE_RETRY_STREAK_PENALTY_CAP_PERCENT = 34;
    private static final long FISHING_INVENTORY_FULL_AFK_SHORT_MIN_MS = 10_000L;
    private static final long FISHING_INVENTORY_FULL_AFK_SHORT_MAX_MS = 45_000L;
    private static final long FISHING_INVENTORY_FULL_AFK_MEDIUM_MIN_MS = 45_000L;
    private static final long FISHING_INVENTORY_FULL_AFK_MEDIUM_MAX_MS = 90_000L;
    private static final long FISHING_INVENTORY_FULL_AFK_LONG_MIN_MS = 90_000L;
    private static final long FISHING_INVENTORY_FULL_AFK_LONG_MAX_MS = 133_000L;
    private static final int FISHING_INVENTORY_FULL_AFK_SHORT_WEIGHT_PERCENT = 68;
    private static final int FISHING_INVENTORY_FULL_AFK_MEDIUM_WEIGHT_PERCENT = 24;
    private static final int FISHING_INVENTORY_FULL_AFK_LONG_WEIGHT_PERCENT = 8;
    private static final DropDispatchTuning DROP_DISPATCH_TUNING_DB_PARITY =
        new DropDispatchTuning(
            26L, 48L,
            4L, 14L,
            12L, 34L,
            0, 0,
            36,
            3,
            2, 8,
            8
        );

    interface Host {
        int currentExecutorTick();
        int currentPlayerAnimation();
        boolean isAnimationActive(int animation);

        boolean isDropSweepSessionActive();
        int dropSweepItemId();
        int dropSweepNextSlot();
        int dropSweepLastDispatchTick();
        int dropSweepDispatchFailStreak();
        boolean dropSweepAwaitingFirstCursorSync();

        void setDropSweepNextSlot(int slot);
        void setDropSweepLastDispatchTick(int tick);
        void setDropSweepAwaitingFirstCursorSync(boolean awaiting);
        void setDropSweepProgressCheckPending(boolean pending);

        void beginDropSweepSession(int itemId, Set<Integer> itemIds);
        void endDropSweepSession();

        boolean updateDropSweepProgressState(int itemId);
        boolean noteDropSweepDispatchFailure();
        void noteDropSweepDispatchSuccess();

        Optional<Integer> findInventorySlotFrom(int itemId, int startSlot);
        Optional<Point> resolveInventorySlotPoint(int slot);
        Optional<Point> resolveInventorySlotBasePoint(int slot);
        Optional<Point> centerOfDropSweepRegionCanvas();
        boolean isCursorNearDropTarget(Point canvasPoint);
        MotorHandle scheduleDropMoveGesture(Point canvasPoint);

        boolean acquireOrRenewDropMotorOwner();
        boolean isLoggedInAndBankClosed();
        boolean dispatchInventoryDropAction(int slot, int expectedItemId, Point preparedCanvasPoint);

        void applyDropPerceptionDelay();
        void incrementClicksDispatched();
        FatigueSnapshot fatigueSnapshot();
        void onDropCadenceProfileSelected(String profileKey);
        void onIdleCadenceTuningSelected(IdleCadenceTuning tuning);
        IdleSkillContext resolveIdleSkillContext();
        FishingIdleMode resolveFishingIdleMode(IdleSkillContext context);

        JsonObject details(Object... kvPairs);
        void emitDropDebug(String reason, JsonObject details);
        CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject details);
        CommandExecutor.CommandDecision rejectDecision(String reason);
    }

    private final Host host;
    private DropDispatchTuning activeDropDispatchTuning = DROP_DISPATCH_TUNING_DB_PARITY;
    private DropCadenceTuningOverrides activeDropCadenceTuningOverrides = DropCadenceTuningOverrides.none();
    private long dropLocalCooldownUntilMs = 0L;
    private int dropDispatchTick = Integer.MIN_VALUE;
    private int dropDispatchesThisTick = 0;
    private int nextAllowedSecondDispatchTick = Integer.MIN_VALUE;
    private int rhythmPauseUntilTick = Integer.MIN_VALUE;
    private int dispatchBurstLength = 0;
    private int sessionCooldownBiasMs = 0;
    private int targetCycleClicksMedian = 0;
    private int targetCycleDurationMsMedian = 0;
    private long activeCycleStartedAtMs = 0L;
    private long activeCycleLastDispatchAtMs = 0L;
    private int activeCycleDispatchCount = 0;
    private int activeCycleTargetClicks = 0;
    private int activeCycleTargetDurationMs = 0;
    private DropTraversalProfile activeDropTraversalProfile = DropTraversalProfile.SERPENTINE;
    private int dropTraversalStrideSlots = 1;
    private int dropTraversalHopChancePercent = 0;
    private int dropTraversalStartSlot = 0;
    private long firstCursorSyncNotBeforeMs = 0L;
    private final DropNoticeDelayController dropNoticeDelayController = new DropNoticeDelayController();
    private final DropSemanticMisclickModel dropSemanticMisclickModel = new DropSemanticMisclickModel();
    private int preparedDropSlot = -1;
    private Point preparedDropTargetCanvas = null;
    private int pendingOutcomeSlot = -1;
    private int pendingOutcomeItemId = -1;
    private int pendingOutcomeDispatchTick = Integer.MIN_VALUE;
    private long pendingOutcomeResolveNotBeforeMs = 0L;
    private long pendingOutcomeDispatchedAtMs = 0L;
    private int pendingOutcomeRecheckCount = 0;
    private int slotUnavailableStreak = 0;
    private long fishingInventoryFullAfkUntilMs = 0L;
    private long fishingInventoryFullAfkArmedAtMs = 0L;
    private boolean fishingInventoryFullAfkArmedForSession = false;
    private int lastFishingInventoryFullAfkDebugTick = Integer.MIN_VALUE;

    DropRuntime(Host host) {
        this.host = host;
        initializeDropSessionProfile();
    }

    boolean isFishingInventoryFullAfkActiveNow() {
        return fishingInventoryFullAfkRemainingMs() > 0L;
    }

    long fishingInventoryFullAfkRemainingMs() {
        long now = System.currentTimeMillis();
        long remaining = fishingInventoryFullAfkUntilMs - now;
        if (remaining <= 0L) {
            fishingInventoryFullAfkUntilMs = 0L;
            return 0L;
        }
        return remaining;
    }

    CommandExecutor.CommandDecision executeStartDropSession(JsonObject payload) {
        int itemId = asInt(payload == null ? null : payload.get("itemId"), -1);
        if (itemId <= 0) {
            return host.rejectDecision("invalid_item_id");
        }
        Set<Integer> itemIds = parseItemIds(payload, itemId);
        host.beginDropSweepSession(itemId, itemIds);
        armFishingInventoryFullAfkWindowIfEligible(itemId, true);
        applyDropCadenceProfile(payload);
        resetDispatchPacingState();
        initializeDropSessionProfile();
        dropLocalCooldownUntilMs = 0L;
        dropSemanticMisclickModel.reset();
        clearPreparedDropTarget();
        clearRepeatSlotGuard();
        clearPendingDropOutcome();
        resetCycleProgressState();
        clearSlotUnavailableState();
        firstCursorSyncNotBeforeMs = 0L;
        host.setDropSweepNextSlot(resolveSessionStartSlot(itemId));
        host.setDropSweepLastDispatchTick(host.currentExecutorTick());
        return host.acceptDecision(
            "drop_session_start_dispatched",
            host.details(
                "itemId", itemId,
                "itemIds", itemIds.toString(),
                "sessionActive", host.isDropSweepSessionActive(),
                "traversalProfile", activeDropTraversalProfile.name(),
                "traversalStartSlot", dropTraversalStartSlot,
                "traversalStrideSlots", dropTraversalStrideSlots,
                "traversalHopChancePercent", dropTraversalHopChancePercent
            )
        );
    }

    CommandExecutor.CommandDecision executeStopDropSession(JsonObject payload) {
        int itemId = asInt(payload == null ? null : payload.get("itemId"), -1);
        boolean wasActive = host.isDropSweepSessionActive();
        host.endDropSweepSession();
        clearFishingInventoryFullAfkState();
        dropNoticeDelayController.clearActiveDelay();
        resetDispatchPacingState();
        dropLocalCooldownUntilMs = 0L;
        firstCursorSyncNotBeforeMs = 0L;
        dropSemanticMisclickModel.reset();
        clearPreparedDropTarget();
        clearRepeatSlotGuard();
        clearPendingDropOutcome();
        resetCycleProgressState();
        clearSlotUnavailableState();
        return host.acceptDecision(
            "drop_session_stop_dispatched",
            host.details("itemId", itemId, "wasActive", wasActive)
        );
    }

    CommandExecutor.CommandDecision executeDropItem(JsonObject payload) {
        if (!host.isDropSweepSessionActive()) {
            clearFishingInventoryFullAfkState();
        }
        int itemId = asInt(payload == null ? null : payload.get("itemId"), -1);
        if (itemId <= 0) {
            return host.rejectDecision("invalid_item_id");
        }
        int activeAnimation = host.currentPlayerAnimation();
        if (host.isAnimationActive(activeAnimation)) {
            host.endDropSweepSession();
            clearPreparedDropTarget();
            clearRepeatSlotGuard();
            clearPendingDropOutcome();
            clearSlotUnavailableState();
            return host.acceptDecision(
                "woodcut_drop_wait_animation_end",
                host.details("itemId", itemId, "animation", activeAnimation)
            );
        }
        boolean wasSessionActive = host.isDropSweepSessionActive();

        Set<Integer> itemIds = parseItemIds(payload, itemId);
        int previousSessionItemId = host.dropSweepItemId();
        host.beginDropSweepSession(itemId, itemIds);
        boolean newSession = !wasSessionActive || previousSessionItemId != itemId;
        if (newSession) {
            armFishingInventoryFullAfkWindowIfEligible(itemId, true);
            applyDropCadenceProfile(payload);
            resetDispatchPacingState();
            initializeDropSessionProfile();
            dropLocalCooldownUntilMs = 0L;
            dropSemanticMisclickModel.reset();
            clearPreparedDropTarget();
            clearRepeatSlotGuard();
            clearPendingDropOutcome();
            resetCycleProgressState();
            clearSlotUnavailableState();
            firstCursorSyncNotBeforeMs = 0L;
            host.setDropSweepNextSlot(resolveSessionStartSlot(itemId));
        }
        CommandExecutor.CommandDecision afkWaitDecision = maybeAcceptFishingInventoryFullAfkWait(itemId);
        if (afkWaitDecision != null) {
            return afkWaitDecision;
        }
        Optional<CommandExecutor.CommandDecision> pendingOutcomeDecision = resolvePendingDropOutcomeForCommand(itemId);
        if (pendingOutcomeDecision.isPresent()) {
            return pendingOutcomeDecision.get();
        }
        FatigueSnapshot fatigue = fatigueSnapshot();
        if (!host.updateDropSweepProgressState(itemId)) {
            host.emitDropDebug(
                "drop_session_abort_no_progress",
                host.details(
                    "itemId", itemId,
                    "slot", host.dropSweepNextSlot(),
                    "failureStreak", host.dropSweepDispatchFailStreak()
                )
            );
            host.endDropSweepSession();
            clearPreparedDropTarget();
            clearRepeatSlotGuard();
            clearPendingDropOutcome();
            return host.rejectDecision("drop_no_progress_stop");
        }

        SlotSelection slotSelection = resolveDispatchSlot(itemId);
        if (!slotSelection.available) {
            host.endDropSweepSession();
            clearPreparedDropTarget();
            clearRepeatSlotGuard();
            clearPendingDropOutcome();
            return host.acceptDecision("drop_all_complete", host.details("itemId", itemId, "remainingSlots", 0));
        }

        int slot = slotSelection.slot;
        DropDispatchPlan dispatchPlan = resolveDropDispatchPlan(slot, itemId, fatigue);
        DropCursorState cursorState = prepareCursorForDrop(dispatchPlan.preparationSlot, dispatchPlan.targetCanvasPoint);
        if (!cursorState.ready) {
            String reason = resolveDropCursorDeferredReason(cursorState.motorReason, cursorState.motorFailed);
            return host.acceptDecision(
                reason,
                host.details(
                    "itemId", itemId,
                    "slot", slot,
                    "sessionActive", host.isDropSweepSessionActive(),
                    "motorStatus", cursorState.motorStatus,
                    "motorReason", cursorState.motorReason
                )
            );
        }
        if (isDropLocalCooldownActive()) {
            return host.acceptDecision(
                "drop_local_cooldown_deferred",
                host.details(
                    "itemId", itemId,
                    "slot", slot,
                    "waitMs", Math.max(1L, dropLocalCooldownUntilMs - System.currentTimeMillis())
                )
            );
        }
        int currentTick = host.currentExecutorTick();
        if (!allowDispatchThisTick(currentTick)) {
            return host.acceptDecision(
                "drop_per_tick_dispatch_deferred",
                host.details(
                    "itemId", itemId,
                    "slot", slot,
                    "currentTick", currentTick,
                    "dispatchesThisTick", dropDispatchesThisTick,
                    "rhythmPauseUntilTick", rhythmPauseUntilTick
                )
            );
        }
        String dispatchMode = "left_click_canvas_slot";
        if (dispatchPlan.semanticMisclick) {
            dispatchMode = "semantic_misclick_retry";
        }
        if (!host.dispatchInventoryDropAction(slot, itemId, cursorState.targetCanvasPoint)) {
            if (!host.noteDropSweepDispatchFailure()) {
                host.emitDropDebug(
                    "drop_session_abort_dispatch_fail_limit",
                    host.details(
                        "itemId", itemId,
                        "slot", slot,
                        "failureStreak", host.dropSweepDispatchFailStreak()
                    )
                );
                host.endDropSweepSession();
                clearPreparedDropTarget();
                clearRepeatSlotGuard();
                clearPendingDropOutcome();
                resetCycleProgressState();
                return host.rejectDecision("drop_dispatch_stuck_stop");
            }
            return host.acceptDecision(
                "drop_dispatch_deferred",
                host.details(
                    "itemId", itemId,
                    "slot", slot,
                    "sessionActive", host.isDropSweepSessionActive(),
                    "failureStreak", host.dropSweepDispatchFailStreak()
                )
            );
        }
        clearPreparedDropTarget();
        noteDispatchSuccessForTick(currentTick);
        noteCycleDispatchIssued();
        if (host.dropSweepAwaitingFirstCursorSync()) {
            host.setDropSweepAwaitingFirstCursorSync(false);
            firstCursorSyncNotBeforeMs = 0L;
        }

        host.applyDropPerceptionDelay();
        host.incrementClicksDispatched();
        armDropLocalCooldown();
        host.setDropSweepLastDispatchTick(currentTick);
        host.setDropSweepProgressCheckPending(true);
        armPendingDropOutcome(slot, itemId, currentTick, fatigue);
        return host.acceptDecision(
            "drop_item_dispatched_pending_outcome",
            host.details(
                "itemId", itemId,
                "slot", slot,
                "dropsDispatched", 1,
                "cursorMovedForDrop", cursorState.cursorReady,
                "sessionActive", host.isDropSweepSessionActive(),
                "mode", dispatchMode,
                "fatigueLoad", fatigue.loadPercent(),
                "fatigueBand", fatigue.band(),
                "outcomePending", true
            )
        );
    }

    void advanceTick(int tick) {
        if (!host.isDropSweepSessionActive() || host.dropSweepItemId() <= 0) {
            clearFishingInventoryFullAfkState();
            clearSlotUnavailableState();
            return;
        }
        if (isFishingInventoryFullAfkActiveNow()) {
            maybeEmitFishingInventoryFullAfkWaitDebug(tick, host.dropSweepItemId());
            return;
        }
        if (!host.acquireOrRenewDropMotorOwner()) {
            host.emitDropDebug("drop_gate_owner_unavailable", host.details("tick", tick));
            return;
        }
        if (resolvePendingDropOutcomeForTick(host.dropSweepItemId(), tick)) {
            return;
        }
        if (!host.updateDropSweepProgressState(host.dropSweepItemId())) {
            host.emitDropDebug(
                "drop_session_abort_no_progress",
                host.details(
                    "itemId", host.dropSweepItemId(),
                    "slot", host.dropSweepNextSlot(),
                    "failureStreak", host.dropSweepDispatchFailStreak()
                )
            );
            host.endDropSweepSession();
            clearPreparedDropTarget();
            clearRepeatSlotGuard();
            clearPendingDropOutcome();
            resetCycleProgressState();
            return;
        }
        if (!host.isLoggedInAndBankClosed()) {
            host.emitDropDebug(
                "drop_session_stop_not_logged_in_or_bank_open",
                host.details(
                    "itemId", host.dropSweepItemId(),
                    "slot", host.dropSweepNextSlot(),
                    "failureStreak", host.dropSweepDispatchFailStreak()
                )
            );
            host.endDropSweepSession();
            clearPreparedDropTarget();
            clearRepeatSlotGuard();
            clearPendingDropOutcome();
            resetCycleProgressState();
            clearSlotUnavailableState();
            return;
        }

        SlotSelection slotSelection = resolveDispatchSlot(host.dropSweepItemId());
        if (!slotSelection.available) {
            if (shouldDeferNoSlotAvailable(host.dropSweepItemId())) {
                return;
            }
            host.endDropSweepSession();
            clearPreparedDropTarget();
            clearRepeatSlotGuard();
            clearPendingDropOutcome();
            resetCycleProgressState();
            clearSlotUnavailableState();
            return;
        }
        clearSlotUnavailableState();

        int slot = slotSelection.slot;
        DropDispatchPlan dispatchPlan = resolveDropDispatchPlan(slot, host.dropSweepItemId(), fatigueSnapshot());
        DropCursorState cursorState = prepareCursorForDrop(dispatchPlan.preparationSlot, dispatchPlan.targetCanvasPoint);
        if (!cursorState.ready) {
            if (cursorState.motorFailed) {
                host.emitDropDebug(
                    resolveDropCursorDebugReason(cursorState.motorReason),
                    host.details(
                        "itemId", host.dropSweepItemId(),
                        "slot", slot,
                        "motorStatus", cursorState.motorStatus,
                        "motorReason", cursorState.motorReason
                    )
                );
            }
            return;
        }
        if (isDropLocalCooldownActive()) {
            return;
        }
        if (!allowDispatchThisTick(tick)) {
            return;
        }

        if (!host.dispatchInventoryDropAction(slot, host.dropSweepItemId(), cursorState.targetCanvasPoint)) {
            if (!host.noteDropSweepDispatchFailure()) {
                host.emitDropDebug(
                    "drop_session_abort_dispatch_fail_limit",
                    host.details(
                        "itemId", host.dropSweepItemId(),
                        "slot", slot,
                        "failureStreak", host.dropSweepDispatchFailStreak()
                    )
                );
                host.endDropSweepSession();
                clearPreparedDropTarget();
                clearRepeatSlotGuard();
                clearPendingDropOutcome();
                resetCycleProgressState();
                clearSlotUnavailableState();
            }
            return;
        }
        clearPreparedDropTarget();
        noteDispatchSuccessForTick(tick);
        noteCycleDispatchIssued();
        if (host.dropSweepAwaitingFirstCursorSync()) {
            host.setDropSweepAwaitingFirstCursorSync(false);
            firstCursorSyncNotBeforeMs = 0L;
        }
        host.applyDropPerceptionDelay();
        host.incrementClicksDispatched();
        armDropLocalCooldown();
        host.setDropSweepLastDispatchTick(tick);
        host.setDropSweepProgressCheckPending(true);
        armPendingDropOutcome(slot, host.dropSweepItemId(), tick, fatigueSnapshot());
    }

    private void armFishingInventoryFullAfkWindowIfEligible(int itemId, boolean forceNewSession) {
        if (!forceNewSession) {
            return;
        }
        clearFishingInventoryFullAfkState();
        if (itemId <= 0 || !isFishingInventoryFullAfkEligible()) {
            return;
        }
        long now = System.currentTimeMillis();
        long waitMs = sampleFishingInventoryFullAfkWaitMs();
        if (waitMs <= 0L) {
            return;
        }
        fishingInventoryFullAfkArmedAtMs = now;
        fishingInventoryFullAfkUntilMs = now + waitMs;
        fishingInventoryFullAfkArmedForSession = true;
        host.emitDropDebug(
            "fishing_inventory_full_afk_armed",
            host.details(
                "itemId", itemId,
                "waitMs", waitMs,
                "waitUntilMs", fishingInventoryFullAfkUntilMs,
                "idleContext", safeIdleContextName(host.resolveIdleSkillContext()),
                "fishingIdleMode", safeFishingIdleModeName(host.resolveFishingIdleMode(host.resolveIdleSkillContext()))
            )
        );
    }

    private CommandExecutor.CommandDecision maybeAcceptFishingInventoryFullAfkWait(int itemId) {
        long remainingMs = fishingInventoryFullAfkRemainingMs();
        if (!fishingInventoryFullAfkArmedForSession || remainingMs <= 0L) {
            return null;
        }
        return host.acceptDecision(
            "fishing_inventory_full_afk_wait",
            host.details(
                "itemId", itemId,
                "waitMsRemaining", remainingMs,
                "armedAtMs", fishingInventoryFullAfkArmedAtMs,
                "waitUntilMs", fishingInventoryFullAfkUntilMs
            )
        );
    }

    private void maybeEmitFishingInventoryFullAfkWaitDebug(int tick, int itemId) {
        if (tick == lastFishingInventoryFullAfkDebugTick) {
            return;
        }
        lastFishingInventoryFullAfkDebugTick = tick;
        host.emitDropDebug(
            "fishing_inventory_full_afk_wait_active",
            host.details(
                "tick", tick,
                "itemId", itemId,
                "waitMsRemaining", fishingInventoryFullAfkRemainingMs(),
                "armedAtMs", fishingInventoryFullAfkArmedAtMs,
                "waitUntilMs", fishingInventoryFullAfkUntilMs
            )
        );
    }

    private boolean isFishingInventoryFullAfkEligible() {
        IdleSkillContext context = host.resolveIdleSkillContext();
        if (context != IdleSkillContext.FISHING) {
            return false;
        }
        FishingIdleMode mode = host.resolveFishingIdleMode(context);
        return mode == FishingIdleMode.OFFSCREEN_BIASED;
    }

    private void clearFishingInventoryFullAfkState() {
        fishingInventoryFullAfkUntilMs = 0L;
        fishingInventoryFullAfkArmedAtMs = 0L;
        fishingInventoryFullAfkArmedForSession = false;
        lastFishingInventoryFullAfkDebugTick = Integer.MIN_VALUE;
    }

    private static long sampleFishingInventoryFullAfkWaitMs() {
        int totalWeight = Math.max(
            1,
            FISHING_INVENTORY_FULL_AFK_SHORT_WEIGHT_PERCENT
                + FISHING_INVENTORY_FULL_AFK_MEDIUM_WEIGHT_PERCENT
                + FISHING_INVENTORY_FULL_AFK_LONG_WEIGHT_PERCENT
        );
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int shortThreshold = FISHING_INVENTORY_FULL_AFK_SHORT_WEIGHT_PERCENT;
        int mediumThreshold = shortThreshold + FISHING_INVENTORY_FULL_AFK_MEDIUM_WEIGHT_PERCENT;
        int longThreshold = mediumThreshold + FISHING_INVENTORY_FULL_AFK_LONG_WEIGHT_PERCENT;
        if (roll < shortThreshold) {
            return randomLongInclusive(
                FISHING_INVENTORY_FULL_AFK_SHORT_MIN_MS,
                FISHING_INVENTORY_FULL_AFK_SHORT_MAX_MS
            );
        }
        if (roll < mediumThreshold) {
            return randomLongInclusive(
                FISHING_INVENTORY_FULL_AFK_MEDIUM_MIN_MS,
                FISHING_INVENTORY_FULL_AFK_MEDIUM_MAX_MS
            );
        }
        if (roll < longThreshold) {
            return randomLongInclusive(
                FISHING_INVENTORY_FULL_AFK_LONG_MIN_MS,
                FISHING_INVENTORY_FULL_AFK_LONG_MAX_MS
            );
        }
        return randomLongInclusive(
            FISHING_INVENTORY_FULL_AFK_LONG_MIN_MS,
            FISHING_INVENTORY_FULL_AFK_LONG_MAX_MS
        );
    }

    private static String safeIdleContextName(IdleSkillContext context) {
        if (context == null) {
            return "";
        }
        return context.name().toLowerCase();
    }

    private static String safeFishingIdleModeName(FishingIdleMode mode) {
        if (mode == null) {
            return "";
        }
        return mode.name().toLowerCase();
    }

    private DropDispatchPlan resolveDropDispatchPlan(int intendedSlot, int itemId, FatigueSnapshot fatigue) {
        OptionalInt neighborSlot = dropSemanticMisclickModel.maybeSelectAdjacentSlot(intendedSlot, fatigue);
        if (neighborSlot.isEmpty()) {
            return DropDispatchPlan.intended(intendedSlot, null);
        }
        int selectedSlot = neighborSlot.getAsInt();
        Optional<Point> intendedPoint = host.resolveInventorySlotBasePoint(intendedSlot);
        Optional<Point> neighborPoint = host.resolveInventorySlotBasePoint(selectedSlot);
        if (intendedPoint.isEmpty() || neighborPoint.isEmpty()) {
            return DropDispatchPlan.intended(intendedSlot, null);
        }
        Point gutterPoint = midpoint(intendedPoint.get(), neighborPoint.get());
        host.emitDropDebug(
            "drop_semantic_misclick_simulated",
            host.details(
                "itemId", itemId,
                "intendedSlot", intendedSlot,
                "neighborSlot", selectedSlot,
                "targetX", gutterPoint.x,
                "targetY", gutterPoint.y,
                "fatigueLoad", fatigue == null ? 0 : fatigue.loadPercent(),
                "fatigueBand", fatigue == null ? "low" : fatigue.band()
            )
        );
        return DropDispatchPlan.semanticMisclick(intendedSlot, gutterPoint);
    }

    private static Point midpoint(Point left, Point right) {
        return new Point(
            (int) Math.round((left.x + right.x) / 2.0),
            (int) Math.round((left.y + right.y) / 2.0)
        );
    }

    private DropCursorState prepareCursorForDrop(int slot, Point overrideTargetCanvasPoint) {
        long now = System.currentTimeMillis();
        Optional<Point> slotPointOpt = Optional.empty();
        if (overrideTargetCanvasPoint != null) {
            slotPointOpt = Optional.of(new Point(overrideTargetCanvasPoint));
            preparedDropSlot = slot;
            preparedDropTargetCanvas = new Point(overrideTargetCanvasPoint);
        } else if (preparedDropSlot == slot && preparedDropTargetCanvas != null) {
            slotPointOpt = Optional.of(new Point(preparedDropTargetCanvas));
        } else {
            slotPointOpt = host.resolveInventorySlotPoint(slot);
            if (slotPointOpt.isPresent()) {
                preparedDropSlot = slot;
                preparedDropTargetCanvas = new Point(slotPointOpt.get());
            } else {
                clearPreparedDropTarget();
            }
        }
        if (slotPointOpt.isEmpty()) {
            if (host.dropSweepAwaitingFirstCursorSync()) {
                return DropCursorState.pending("", "", false, null);
            }
            return DropCursorState.ready(false, null);
        }

        Point slotPoint = slotPointOpt.get();
        boolean awaitingFirstSync = host.dropSweepAwaitingFirstCursorSync();
        if (host.isCursorNearDropTarget(slotPoint)) {
            if (awaitingFirstSync && now < firstCursorSyncNotBeforeMs) {
                return DropCursorState.pending("SYNC_WAIT", "drop_first_sync_settle", false, slotPoint);
            }
            return DropCursorState.ready(true, slotPoint);
        }

        MotorHandle handle = host.scheduleDropMoveGesture(slotPoint);
        String motorStatus = handle == null || handle.status == null ? "UNKNOWN" : handle.status.name();
        String motorReason = handle == null ? "" : safeString(handle.reason);
        boolean motorFailed = handle == null
            || handle.status == MotorGestureStatus.FAILED
            || handle.status == MotorGestureStatus.CANCELLED;
        if (motorFailed) {
            return DropCursorState.pending(motorStatus, motorReason, true, slotPoint);
        }
        if (!host.isCursorNearDropTarget(slotPoint)) {
            return DropCursorState.pending(motorStatus, motorReason, false, slotPoint);
        }
        if (awaitingFirstSync && now < firstCursorSyncNotBeforeMs) {
            return DropCursorState.pending(motorStatus, "drop_first_sync_settle", false, slotPoint);
        }
        return DropCursorState.ready(true, slotPoint);
    }

    private static String resolveDropCursorDeferredReason(String motorReason, boolean motorFailed) {
        String normalized = safeString(motorReason);
        if (MOTOR_REASON_TERMINAL_ALIGNMENT_DEFERRED.equals(normalized)) {
            return "drop_cursor_terminal_alignment_deferred";
        }
        if (MOTOR_REASON_TERMINAL_ALIGNMENT_RETRY_EXHAUSTED.equals(normalized)) {
            return motorFailed
                ? "drop_cursor_terminal_alignment_failed_deferred"
                : "drop_cursor_terminal_alignment_retry_exhausted_deferred";
        }
        return motorFailed
            ? "drop_cursor_move_failed_deferred"
            : "drop_cursor_move_pending";
    }

    private static String resolveDropCursorDebugReason(String motorReason) {
        String normalized = safeString(motorReason);
        if (MOTOR_REASON_TERMINAL_ALIGNMENT_DEFERRED.equals(normalized)) {
            return "drop_cursor_terminal_alignment_deferred";
        }
        if (MOTOR_REASON_TERMINAL_ALIGNMENT_RETRY_EXHAUSTED.equals(normalized)) {
            return "drop_cursor_terminal_alignment_retry_exhausted";
        }
        return "drop_cursor_move_failed";
    }

    private Optional<CommandExecutor.CommandDecision> resolvePendingDropOutcomeForCommand(int sessionItemId) {
        PendingOutcomeResolution resolution = resolvePendingDropOutcome(sessionItemId, host.currentExecutorTick());
        if (resolution == PendingOutcomeResolution.NONE) {
            return Optional.empty();
        }
        if (resolution == PendingOutcomeResolution.OUTCOME_PENDING) {
            return Optional.of(
                host.acceptDecision(
                    "drop_outcome_pending",
                    host.details(
                        "itemId", sessionItemId,
                        "slot", pendingOutcomeSlot,
                        "waitMs", Math.max(1L, pendingOutcomeResolveNotBeforeMs - System.currentTimeMillis()),
                        "recheckCount", pendingOutcomeRecheckCount
                    )
                )
            );
        }
        if (resolution == PendingOutcomeResolution.SESSION_ABORTED) {
            return Optional.of(host.rejectDecision("drop_dispatch_stuck_stop"));
        }
        if (resolution == PendingOutcomeResolution.SESSION_COMPLETED) {
            return Optional.of(
                host.acceptDecision(
                    "drop_all_complete",
                    host.details(
                        "itemId", sessionItemId,
                        "remainingSlots", 0
                    )
                )
            );
        }
        if (resolution == PendingOutcomeResolution.MISS_RETRY_ARMED) {
            return Optional.of(
                host.acceptDecision(
                    "drop_outcome_miss_retry_armed",
                    host.details(
                        "itemId", sessionItemId,
                        "slot", host.dropSweepNextSlot(),
                        "waitMs", Math.max(1L, dropLocalCooldownUntilMs - System.currentTimeMillis())
                    )
                )
            );
        }
        return Optional.of(
            host.acceptDecision(
                "drop_outcome_confirmed",
                host.details(
                    "itemId", sessionItemId,
                    "nextSlot", host.dropSweepNextSlot()
                )
            )
        );
    }

    private boolean resolvePendingDropOutcomeForTick(int sessionItemId, int currentTick) {
        PendingOutcomeResolution resolution = resolvePendingDropOutcome(sessionItemId, currentTick);
        return resolution != PendingOutcomeResolution.NONE;
    }

    private PendingOutcomeResolution resolvePendingDropOutcome(int sessionItemId, int currentTick) {
        if (!hasPendingDropOutcome()) {
            return PendingOutcomeResolution.NONE;
        }
        long now = System.currentTimeMillis();
        if (currentTick <= pendingOutcomeDispatchTick || now < pendingOutcomeResolveNotBeforeMs) {
            return PendingOutcomeResolution.OUTCOME_PENDING;
        }
        int resolvedSlot = pendingOutcomeSlot;
        int resolvedItemId = pendingOutcomeItemId;
        int failureStreak = host.dropSweepDispatchFailStreak();
        if (resolvedSlot < 0 || resolvedItemId <= 0 || sessionItemId <= 0) {
            clearPendingDropOutcome();
            host.setDropSweepProgressCheckPending(false);
            return PendingOutcomeResolution.NONE;
        }
        Optional<Integer> slotStillPresentOpt = host.findInventorySlotFrom(resolvedItemId, resolvedSlot);
        boolean slotStillPresent = slotStillPresentOpt.isPresent() && slotStillPresentOpt.get() == resolvedSlot;
        if (slotStillPresent) {
            long elapsedSinceDispatchMs = Math.max(0L, now - pendingOutcomeDispatchedAtMs);
            if (elapsedSinceDispatchMs < DROP_OUTCOME_MISS_CONFIRM_MIN_TOTAL_MS
                && pendingOutcomeRecheckCount < DROP_OUTCOME_MAX_RECHECKS) {
                pendingOutcomeRecheckCount++;
                pendingOutcomeResolveNotBeforeMs =
                    now + randomLongInclusive(DROP_OUTCOME_RECHECK_WAIT_MIN_MS, DROP_OUTCOME_RECHECK_WAIT_MAX_MS);
                host.emitDropDebug(
                    "drop_outcome_recheck_deferred",
                    host.details(
                        "itemId", resolvedItemId,
                        "slot", resolvedSlot,
                        "elapsedMs", elapsedSinceDispatchMs,
                        "recheckCount", pendingOutcomeRecheckCount,
                        "waitMs", Math.max(1L, pendingOutcomeResolveNotBeforeMs - now)
                    )
                );
                return PendingOutcomeResolution.OUTCOME_PENDING;
            }
            clearPendingDropOutcome();
            host.setDropSweepProgressCheckPending(false);
            if (!host.noteDropSweepDispatchFailure()) {
                host.emitDropDebug(
                    "drop_session_abort_dispatch_fail_limit",
                    host.details(
                        "itemId", resolvedItemId,
                        "slot", resolvedSlot,
                        "failureStreak", host.dropSweepDispatchFailStreak(),
                        "failureReason", "drop_outcome_not_confirmed"
                    )
                );
                host.endDropSweepSession();
                clearPreparedDropTarget();
                clearRepeatSlotGuard();
                resetCycleProgressState();
                clearSlotUnavailableState();
                return PendingOutcomeResolution.SESSION_ABORTED;
            }
            long missRecoveryMs = randomLongInclusive(
                DROP_MISS_RECOVERY_COOLDOWN_MIN_MS,
                DROP_MISS_RECOVERY_COOLDOWN_MAX_MS
            );
            missRecoveryMs += Math.min(
                DROP_MISS_RECOVERY_STREAK_CAP_MS,
                (long) Math.max(0, failureStreak) * DROP_MISS_RECOVERY_STREAK_STEP_MS
            );
            FatigueSnapshot fatigue = fatigueSnapshot();
            int immediateRetryChancePercent = resolveDropMissImmediateRetryChancePercent(failureStreak, fatigue);
            boolean immediateRetry = ThreadLocalRandom.current().nextInt(100) < immediateRetryChancePercent;
            int retrySlot = resolvedSlot;
            String retryMode = "immediate_same_slot";
            if (!immediateRetry) {
                retryMode = "cooldown_same_slot";
            }
            dropLocalCooldownUntilMs = Math.max(dropLocalCooldownUntilMs, now + missRecoveryMs);
            host.setDropSweepNextSlot(retrySlot);
            host.setDropSweepLastDispatchTick(currentTick);
            host.emitDropDebug(
                "drop_outcome_not_confirmed_retry",
                host.details(
                    "itemId", resolvedItemId,
                    "slot", resolvedSlot,
                    "retrySlot", retrySlot,
                    "retryMode", retryMode,
                    "immediateRetryChancePercent", immediateRetryChancePercent,
                    "fatigueLoadPercent", fatigue.loadPercent(),
                    "failureStreak", host.dropSweepDispatchFailStreak(),
                    "recoveryMs", missRecoveryMs
                )
            );
            return PendingOutcomeResolution.MISS_RETRY_ARMED;
        }

        clearPendingDropOutcome();
        host.setDropSweepProgressCheckPending(false);
        host.noteDropSweepDispatchSuccess();
        int nextSlot = nextDropSweepStartSlot(resolvedSlot, resolvedItemId);
        emitTraversalSkipTelemetryIfNeeded(resolvedItemId, resolvedSlot, nextSlot, "post_confirm_plan");
        host.setDropSweepNextSlot(nextSlot);
        int effectiveNextSlot = maybeReseedTraversalAfterWrap(resolvedSlot, nextSlot);
        armWrapRecoveryCooldownIfNeeded(resolvedSlot, effectiveNextSlot);
        boolean sessionCompleted = host.findInventorySlotFrom(sessionItemId, effectiveNextSlot).isEmpty();
        if (sessionCompleted) {
            host.endDropSweepSession();
            clearPreparedDropTarget();
            clearRepeatSlotGuard();
            resetCycleProgressState();
            clearSlotUnavailableState();
            return PendingOutcomeResolution.SESSION_COMPLETED;
        }
        return PendingOutcomeResolution.CONFIRMED;
    }

    private boolean shouldDeferNoSlotAvailable(int itemId) {
        if (itemId <= 0) {
            clearSlotUnavailableState();
            return false;
        }
        slotUnavailableStreak = Math.max(0, slotUnavailableStreak) + 1;
        Optional<Integer> reseededSlot = host.findInventorySlotFrom(itemId, 0);
        if (reseededSlot.isPresent()) {
            int slot = reseededSlot.get();
            host.setDropSweepNextSlot(slot);
            host.emitDropDebug(
                "drop_session_slot_unavailable_reseeded",
                host.details(
                    "itemId", itemId,
                    "slot", slot,
                    "streak", slotUnavailableStreak
                )
            );
            clearSlotUnavailableState();
            return true;
        }
        if (slotUnavailableStreak < DROP_SLOT_UNAVAILABLE_CONFIRM_STREAK) {
            host.emitDropDebug(
                "drop_session_slot_unavailable_deferred",
                host.details(
                    "itemId", itemId,
                    "nextSlot", host.dropSweepNextSlot(),
                    "streak", slotUnavailableStreak,
                    "confirmStreak", DROP_SLOT_UNAVAILABLE_CONFIRM_STREAK
                )
            );
            return true;
        }
        host.emitDropDebug(
            "drop_session_slot_unavailable_confirmed_stop",
            host.details(
                "itemId", itemId,
                "nextSlot", host.dropSweepNextSlot(),
                "streak", slotUnavailableStreak
            )
        );
        return false;
    }

    private boolean hasPendingDropOutcome() {
        return pendingOutcomeSlot >= 0
            && pendingOutcomeItemId > 0
            && pendingOutcomeDispatchTick != Integer.MIN_VALUE;
    }

    private void armPendingDropOutcome(int slot, int itemId, int currentTick, FatigueSnapshot fatigue) {
        long now = System.currentTimeMillis();
        pendingOutcomeSlot = slot;
        pendingOutcomeItemId = itemId;
        pendingOutcomeDispatchTick = currentTick;
        pendingOutcomeDispatchedAtMs = now;
        pendingOutcomeRecheckCount = 0;
        long baseWaitMs = randomLongInclusive(DROP_OUTCOME_CONFIRM_WAIT_MIN_MS, DROP_OUTCOME_CONFIRM_WAIT_MAX_MS);
        long fatigueExtraMs = fatigue == null ? 0L : Math.max(0L, fatigue.dropCooldownBiasMs(80));
        pendingOutcomeResolveNotBeforeMs = now + baseWaitMs + fatigueExtraMs;
    }

    private void clearPendingDropOutcome() {
        pendingOutcomeSlot = -1;
        pendingOutcomeItemId = -1;
        pendingOutcomeDispatchTick = Integer.MIN_VALUE;
        pendingOutcomeResolveNotBeforeMs = 0L;
        pendingOutcomeDispatchedAtMs = 0L;
        pendingOutcomeRecheckCount = 0;
    }

    private void clearSlotUnavailableState() {
        slotUnavailableStreak = 0;
    }

    private void clearPreparedDropTarget() {
        preparedDropSlot = -1;
        preparedDropTargetCanvas = null;
    }

    private void clearRepeatSlotGuard() {
        // Repeat-slot guard logic intentionally removed.
    }

    private SlotSelection resolveDispatchSlot(int itemId) {
        int anchorSlot = host.dropSweepNextSlot();
        Optional<Integer> slotOpt = host.findInventorySlotFrom(itemId, anchorSlot);
        if (slotOpt.isEmpty()) {
            return SlotSelection.none();
        }
        int selectedSlot = Math.floorMod(slotOpt.get(), 28);
        int correctedSlot = preferSerpentineDispatchSlot(itemId, anchorSlot, selectedSlot);
        if (correctedSlot != selectedSlot) {
            host.emitDropDebug(
                "drop_dispatch_serpentine_corrected",
                host.details(
                    "itemId", itemId,
                    "anchorSlot", Math.floorMod(anchorSlot, 28),
                    "selectedSlot", selectedSlot,
                    "correctedSlot", correctedSlot
                )
            );
        }
        emitTraversalSkipTelemetryIfNeeded(itemId, anchorSlot, correctedSlot, "dispatch_scan");
        return SlotSelection.available(correctedSlot);
    }

    private int preferSerpentineDispatchSlot(int itemId, int anchorSlot, int selectedSlot) {
        if (itemId <= 0) {
            return Math.floorMod(selectedSlot, 28);
        }
        int normalizedAnchor = Math.floorMod(anchorSlot, 28);
        int normalizedSelected = Math.floorMod(selectedSlot, 28);
        if (normalizedAnchor == normalizedSelected) {
            return normalizedSelected;
        }
        int cursor = normalizedAnchor;
        int traversed = 0;
        while (traversed < 27) {
            cursor = Math.floorMod(nextSerpentineSlot(cursor), 28);
            if (cursor == normalizedSelected) {
                break;
            }
            traversed++;
            if (isTargetItemPresentAtSlot(itemId, cursor)) {
                return cursor;
            }
        }
        return normalizedSelected;
    }

    private boolean isDropLocalCooldownActive() {
        return System.currentTimeMillis() < dropLocalCooldownUntilMs;
    }

    private void resetDispatchPacingState() {
        dropDispatchTick = Integer.MIN_VALUE;
        dropDispatchesThisTick = 0;
        nextAllowedSecondDispatchTick = Integer.MIN_VALUE;
        rhythmPauseUntilTick = Integer.MIN_VALUE;
        dispatchBurstLength = 0;
    }

    private void resetCycleProgressState() {
        activeCycleStartedAtMs = 0L;
        activeCycleLastDispatchAtMs = 0L;
        activeCycleDispatchCount = 0;
        activeCycleTargetClicks = 0;
        activeCycleTargetDurationMs = 0;
    }

    private void noteCycleDispatchIssued() {
        long now = System.currentTimeMillis();
        if (!hasCycleShapeTargets()) {
            return;
        }
        if (activeCycleStartedAtMs <= 0L || activeCycleTargetClicks <= 0 || activeCycleTargetDurationMs <= 0) {
            armNewCycle(now);
        } else if (shouldStartNewCycle(now)) {
            armNewCycle(now);
        }
        activeCycleDispatchCount = Math.max(0, activeCycleDispatchCount) + 1;
        activeCycleLastDispatchAtMs = now;
    }

    private void armNewCycle(long nowMs) {
        activeCycleStartedAtMs = Math.max(0L, nowMs);
        activeCycleLastDispatchAtMs = 0L;
        activeCycleDispatchCount = 0;
        activeCycleTargetClicks = sampleCycleTargetClicks();
        activeCycleTargetDurationMs = sampleCycleTargetDurationMs();
    }

    private boolean shouldStartNewCycle(long nowMs) {
        if (activeCycleStartedAtMs <= 0L) {
            return true;
        }
        long idleGapMs = activeCycleLastDispatchAtMs <= 0L
            ? 0L
            : Math.max(0L, nowMs - activeCycleLastDispatchAtMs);
        if (idleGapMs >= cycleResetGapThresholdMs()) {
            return true;
        }
        long elapsedMs = Math.max(0L, nowMs - activeCycleStartedAtMs);
        int durationTarget = Math.max(1_000, activeCycleTargetDurationMs);
        int clickTarget = Math.max(1, activeCycleTargetClicks);
        boolean reachedClickTarget = activeCycleDispatchCount >= clickTarget;
        boolean reachedDurationFloor = elapsedMs >= Math.round(durationTarget * 0.72);
        boolean exceededCycleWindow = elapsedMs >= Math.round(durationTarget * 1.38);
        boolean clickOvershoot = activeCycleDispatchCount >= Math.max(clickTarget + 3, (int) Math.round(clickTarget * 1.12));
        return (reachedClickTarget && reachedDurationFloor) || exceededCycleWindow || clickOvershoot;
    }

    private long cycleResetGapThresholdMs() {
        if (targetCycleDurationMsMedian <= 0) {
            return 2400L;
        }
        long scaled = Math.round(targetCycleDurationMsMedian * 0.15);
        return Math.max(1700L, Math.min(3300L, scaled));
    }

    private int sampleCycleTargetClicks() {
        int median = Math.max(8, targetCycleClicksMedian);
        int min = Math.max(6, (int) Math.round(median * 0.82));
        int max = Math.max(min + 2, (int) Math.round(median * 1.16));
        return randomIntInclusive(min, max);
    }

    private int sampleCycleTargetDurationMs() {
        int median = Math.max(4_000, targetCycleDurationMsMedian);
        int min = Math.max(3_000, (int) Math.round(median * 0.76));
        int max = Math.max(min + 650, (int) Math.round(median * 1.24));
        return randomIntInclusive(min, max);
    }

    private void ensureDispatchTickWindow(int tick) {
        if (tick != dropDispatchTick) {
            dropDispatchTick = tick;
            dropDispatchesThisTick = 0;
        }
    }

    private boolean allowDispatchThisTick(int tick) {
        ensureDispatchTickWindow(tick);
        if (dropDispatchesThisTick <= 0) {
            return true;
        }
        if (dropDispatchesThisTick >= 2) {
            return false;
        }
        if (tick < nextAllowedSecondDispatchTick) {
            return false;
        }
        int roll = ThreadLocalRandom.current().nextInt(100);
        return roll < dynamicSecondDispatchChance();
    }

    private void noteDispatchSuccessForTick(int tick) {
        ensureDispatchTickWindow(tick);
        dropDispatchesThisTick++;
        dispatchBurstLength++;
        if (dropDispatchesThisTick >= 2) {
            nextAllowedSecondDispatchTick = tick + randomIntInclusive(
                activeDropDispatchTuning.secondDispatchMinGapTicks,
                activeDropDispatchTuning.secondDispatchMaxGapTicks
            );
        }
    }

    private void armDropLocalCooldown() {
        FatigueSnapshot fatigue = fatigueSnapshot();
        long window = sampleLocalCooldownWindowMs();
        window = Math.max(0L, window + sessionCooldownBiasMs + randomLongInclusive(-2L, 4L));
        window += fatigue.dropCooldownBiasMs(7);
        if (dropDispatchesThisTick >= 2) {
            window += randomLongInclusive(
                activeDropDispatchTuning.secondDispatchExtraCooldownMinMs,
                activeDropDispatchTuning.secondDispatchExtraCooldownMaxMs
            );
        }
        int hesitationChancePercent =
            clampPercent(
                activeDropDispatchTuning.microHesitationChancePercent + fatigue.dropHesitationChanceBiasPercent(12)
            );
        if (dispatchBurstLength >= 4
            && ThreadLocalRandom.current().nextInt(100) < hesitationChancePercent) {
            window += randomLongInclusive(
                activeDropDispatchTuning.microHesitationMinMs,
                activeDropDispatchTuning.microHesitationMaxMs
            );
            dispatchBurstLength = Math.max(0, dispatchBurstLength - 2);
        }
        window += cycleShapeCooldownBiasMs(System.currentTimeMillis());
        dropLocalCooldownUntilMs = System.currentTimeMillis() + window;
    }

    private int dynamicSecondDispatchChance() {
        FatigueSnapshot fatigue = fatigueSnapshot();
        int chance = activeDropDispatchTuning.secondDispatchChancePercent;
        if (dispatchBurstLength > 10) {
            chance -= Math.min(14, (dispatchBurstLength - 10) * 2);
        }
        chance += cycleShapeSecondDispatchBiasPercent(System.currentTimeMillis());
        chance -= fatigue.dropSecondDispatchPenaltyPercent(7);
        chance += ThreadLocalRandom.current().nextInt(-3, 4);
        return clampPercent(chance);
    }

    private long cycleShapeCooldownBiasMs(long nowMs) {
        if (!hasCycleShapeTargets() || activeCycleDispatchCount <= 0 || activeCycleTargetClicks <= 0
            || activeCycleTargetDurationMs <= 0) {
            return 0L;
        }
        double lead = cyclePaceLead(nowMs);
        long bias = 0L;
        if (lead > 0.06) {
            bias += randomLongInclusive(18L, 76L);
            bias += Math.min(140L, Math.round((lead - 0.06) * 180.0));
        } else if (lead < -0.10) {
            bias -= Math.min(24L, Math.round((-lead - 0.10) * 70.0));
        }
        if (activeCycleDispatchCount >= Math.max(1, activeCycleTargetClicks - 2)) {
            long elapsed = Math.max(0L, nowMs - activeCycleStartedAtMs);
            long floorDuration = Math.round(activeCycleTargetDurationMs * 0.72);
            if (elapsed < floorDuration) {
                bias += Math.min(240L, floorDuration - elapsed);
            }
        }
        return bias;
    }

    private int cycleShapeSecondDispatchBiasPercent(long nowMs) {
        if (!hasCycleShapeTargets() || activeCycleDispatchCount <= 0 || activeCycleTargetClicks <= 0
            || activeCycleTargetDurationMs <= 0) {
            return 0;
        }
        int bias = 0;
        double lead = cyclePaceLead(nowMs);
        if (lead > 0.08) {
            bias -= Math.min(44, (int) Math.round((lead - 0.08) * 120.0));
        } else if (lead < -0.10) {
            bias += Math.min(26, (int) Math.round((-lead - 0.10) * 90.0));
        }
        if (activeCycleDispatchCount >= activeCycleTargetClicks + 2) {
            bias -= 22;
        }
        return bias;
    }

    private boolean hasCycleShapeTargets() {
        return targetCycleClicksMedian >= 8 && targetCycleDurationMsMedian >= 4_000;
    }

    private double cyclePaceLead(long nowMs) {
        long startedAtMs = activeCycleStartedAtMs > 0L ? activeCycleStartedAtMs : nowMs;
        long elapsedMs = Math.max(1L, nowMs - startedAtMs);
        int clickTarget = Math.max(1, activeCycleTargetClicks);
        int durationTarget = Math.max(1_000, activeCycleTargetDurationMs);
        double clickProgress = (double) Math.max(0, activeCycleDispatchCount) / (double) clickTarget;
        double timeProgress = (double) elapsedMs / (double) durationTarget;
        clickProgress = Math.min(1.7, Math.max(0.0, clickProgress));
        timeProgress = Math.min(1.7, Math.max(0.0, timeProgress));
        return clickProgress - timeProgress;
    }

    private long sampleLocalCooldownWindowMs() {
        long base = randomLongInclusive(
            activeDropDispatchTuning.localCooldownMinMs,
            activeDropDispatchTuning.localCooldownMaxMs
        );
        // Keep tiny variability while avoiding large cadence spikes.
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 22) {
            base += randomLongInclusive(2L, 5L);
        } else if (roll < 36) {
            base = Math.max(0L, base - randomLongInclusive(2L, 4L));
        }
        return Math.max(0L, base);
    }

    private void armWrapRecoveryCooldownIfNeeded(int fromSlot, int nextSlot) {
        // Legacy wrap-recovery pauses have been removed.
    }

    private int maybeReseedTraversalAfterWrap(int fromSlot, int nextSlot) {
        int normalizedNext = Math.floorMod(nextSlot, 28);
        return normalizedNext;
    }

    private int resolveSessionStartSlot(int itemId) {
        if (itemId > 0) {
            Optional<Integer> firstPresent = host.findInventorySlotFrom(itemId, 0);
            if (firstPresent.isPresent()) {
                return Math.floorMod(firstPresent.get(), 28);
            }
        }
        return Math.floorMod(dropTraversalStartSlot, 28);
    }

    private int nextDropSweepStartSlot(int justDroppedSlot, int itemId) {
        int slot = Math.floorMod(justDroppedSlot, 28);
        Optional<Integer> sameRowNext = resolveSameRowTraversalSlot(itemId, slot);
        if (sameRowNext.isPresent()) {
            return Math.floorMod(sameRowNext.get(), 28);
        }
        return Math.floorMod(nextSerpentineSlot(slot), 28);
    }

    private Optional<Integer> resolveSameRowTraversalSlot(int itemId, int droppedSlot) {
        if (itemId <= 0 || droppedSlot < 0 || droppedSlot >= 28) {
            return Optional.empty();
        }
        int row = droppedSlot / 4;
        int col = droppedSlot % 4;
        int[] orderedCols = orderedRowAlternateColumns(row, col);
        for (int candidateCol : orderedCols) {
            if (candidateCol < 0 || candidateCol >= 4 || candidateCol == col) {
                continue;
            }
            int candidateSlot = (row * 4) + candidateCol;
            Optional<Integer> exactMatch = host.findInventorySlotFrom(itemId, candidateSlot);
            if (exactMatch.isPresent() && exactMatch.get() == candidateSlot) {
                return Optional.of(candidateSlot);
            }
        }
        return Optional.empty();
    }

    private static int[] orderedRowAlternateColumns(int row, int col) {
        int[] ordered = new int[3];
        int write = 0;
        boolean evenRow = (row & 1) == 0;
        for (int delta = 1; delta <= 3; delta++) {
            int preferredCol = evenRow ? (col + delta) : (col - delta);
            if (preferredCol >= 0 && preferredCol < 4) {
                ordered[write++] = preferredCol;
            }
        }
        for (int delta = 1; delta <= 3; delta++) {
            int fallbackCol = evenRow ? (col - delta) : (col + delta);
            if (fallbackCol >= 0 && fallbackCol < 4) {
                boolean alreadyIncluded = false;
                for (int i = 0; i < write; i++) {
                    if (ordered[i] == fallbackCol) {
                        alreadyIncluded = true;
                        break;
                    }
                }
                if (!alreadyIncluded) {
                    ordered[write++] = fallbackCol;
                }
            }
        }
        while (write < ordered.length) {
            ordered[write++] = -1;
        }
        return ordered;
    }

    private int nextSerpentineSlot(int slot) {
        int normalized = Math.floorMod(slot, 28);
        int row = normalized / 4;
        int col = normalized % 4;
        if ((row & 1) == 0) {
            if (col < 3) {
                return normalized + 1;
            }
            if (row < 6) {
                return normalized + 4;
            }
            return 0;
        }
        if (col > 0) {
            return normalized - 1;
        }
        if (row < 6) {
            return normalized + 4;
        }
        return 0;
    }

    private void emitTraversalSkipTelemetryIfNeeded(int itemId, int fromSlot, int toSlot, String source) {
        if (itemId <= 0) {
            return;
        }
        int normalizedFrom = Math.floorMod(fromSlot, 28);
        int normalizedTo = Math.floorMod(toSlot, 28);
        if (normalizedFrom == normalizedTo) {
            return;
        }
        int cursor = normalizedFrom;
        int traversed = 0;
        int skippedPresentCount = 0;
        int skippedSameRowCount = 0;
        StringBuilder skippedSlotList = new StringBuilder();
        int fromRow = normalizedFrom / 4;
        while (traversed < 27) {
            cursor = Math.floorMod(nextSerpentineSlot(cursor), 28);
            if (cursor == normalizedTo) {
                break;
            }
            traversed++;
            if (!isTargetItemPresentAtSlot(itemId, cursor)) {
                continue;
            }
            skippedPresentCount++;
            if ((cursor / 4) == fromRow) {
                skippedSameRowCount++;
            }
            if (skippedPresentCount <= 8) {
                if (skippedSlotList.length() > 0) {
                    skippedSlotList.append(',');
                }
                skippedSlotList.append(cursor);
            }
        }
        if (cursor != normalizedTo || skippedPresentCount <= 0) {
            return;
        }
        host.emitDropDebug(
            "drop_traversal_skip_detected",
            host.details(
                "itemId", itemId,
                "source", safeString(source),
                "fromSlot", normalizedFrom,
                "toSlot", normalizedTo,
                "skippedPresentSlots", skippedPresentCount,
                "skippedSameRowSlots", skippedSameRowCount,
                "skippedSlotList", skippedSlotList.toString()
            )
        );
    }

    private boolean isTargetItemPresentAtSlot(int itemId, int slot) {
        if (itemId <= 0 || slot < 0 || slot >= 28) {
            return false;
        }
        Optional<Integer> slotOpt = host.findInventorySlotFrom(itemId, slot);
        return slotOpt.isPresent() && slotOpt.get() == slot;
    }

    private void initializeDropSessionProfile() {
        sessionCooldownBiasMs = activeDropCadenceTuningOverrides.resolveSessionCooldownBiasMs(0);
        targetCycleClicksMedian = activeDropCadenceTuningOverrides.resolveTargetCycleClicksMedian(0);
        targetCycleDurationMsMedian = activeDropCadenceTuningOverrides.resolveTargetCycleDurationMsMedian(0);
        activeDropTraversalProfile = DropTraversalProfile.SERPENTINE;
        dropTraversalStrideSlots = 1;
        dropTraversalHopChancePercent = 0;
        dropTraversalStartSlot = 0;
        resetCycleProgressState();
    }

    private int resolveDropMissImmediateRetryChancePercent(int failureStreak, FatigueSnapshot fatigue) {
        int chance = DROP_MISS_IMMEDIATE_RETRY_BASE_CHANCE_PERCENT;
        if (fatigue != null) {
            chance += fatigue.dropHesitationChanceBiasPercent(DROP_MISS_IMMEDIATE_RETRY_FATIGUE_BONUS_MAX_PERCENT);
        }
        int streakPenalty = Math.min(
            DROP_MISS_IMMEDIATE_RETRY_STREAK_PENALTY_CAP_PERCENT,
            Math.max(0, failureStreak) * DROP_MISS_IMMEDIATE_RETRY_STREAK_PENALTY_STEP_PERCENT
        );
        chance -= streakPenalty;
        chance += randomIntInclusive(-5, 5);
        return clampPercent(chance);
    }

    private void applyDropCadenceProfile(JsonObject payload) {
        DropCadenceProfile profile = DropCadenceProfile.fromPayload(payload);
        activeDropDispatchTuning = DROP_DISPATCH_TUNING_DB_PARITY;
        activeDropCadenceTuningOverrides = DropCadenceTuningOverrides.fromPayload(payload);
        activeDropDispatchTuning = activeDropCadenceTuningOverrides.applyDispatchTuning(activeDropDispatchTuning);
        dropNoticeDelayController.configureProfile(profile.profileKey);
        host.onDropCadenceProfileSelected(profile.profileKey);
        host.onIdleCadenceTuningSelected(IdleCadenceTuning.fromPayload(payload));
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static int randomIntInclusive(int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private static long randomLongInclusive(long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static int asInt(JsonElement element, int fallback) {
        if (element == null) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Integer asIntBounded(JsonElement element, int min, int max) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            int value = element.getAsInt();
            int lo = Math.min(min, max);
            int hi = Math.max(min, max);
            return Math.max(lo, Math.min(hi, value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject asJsonObject(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return null;
        }
        try {
            return element.getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String asString(JsonElement element, String fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            String value = element.getAsString();
            return value == null ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Set<Integer> parseItemIds(JsonObject payload, int fallbackItemId) {
        Set<Integer> out = new LinkedHashSet<>();
        if (payload != null) {
            JsonElement itemIdsElement = payload.get("itemIds");
            if (itemIdsElement != null && !itemIdsElement.isJsonNull()) {
                try {
                    if (itemIdsElement.isJsonArray()) {
                        for (JsonElement item : itemIdsElement.getAsJsonArray()) {
                            int parsed = asInt(item, -1);
                            if (parsed > 0) {
                                out.add(parsed);
                            }
                        }
                    } else {
                        int parsed = asInt(itemIdsElement, -1);
                        if (parsed > 0) {
                            out.add(parsed);
                        }
                    }
                } catch (Exception ignored) {
                    // Keep tolerant to malformed payloads.
                }
            }
        }
        if (fallbackItemId > 0) {
            out.add(fallbackItemId);
        }
        return out;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private FatigueSnapshot fatigueSnapshot() {
        FatigueSnapshot snapshot = host.fatigueSnapshot();
        return snapshot == null ? FatigueSnapshot.neutral() : snapshot;
    }

    private enum PendingOutcomeResolution {
        NONE,
        OUTCOME_PENDING,
        CONFIRMED,
        MISS_RETRY_ARMED,
        SESSION_COMPLETED,
        SESSION_ABORTED
    }

    private static final class DropCursorState {
        private final boolean ready;
        private final boolean cursorReady;
        private final String motorStatus;
        private final String motorReason;
        private final boolean motorFailed;
        private final Point targetCanvasPoint;

        private DropCursorState(
            boolean ready,
            boolean cursorReady,
            String motorStatus,
            String motorReason,
            boolean motorFailed,
            Point targetCanvasPoint
        ) {
            this.ready = ready;
            this.cursorReady = cursorReady;
            this.motorStatus = safeString(motorStatus);
            this.motorReason = safeString(motorReason);
            this.motorFailed = motorFailed;
            this.targetCanvasPoint = targetCanvasPoint == null ? null : new Point(targetCanvasPoint);
        }

        private static DropCursorState ready(boolean cursorReady, Point targetCanvasPoint) {
            return new DropCursorState(true, cursorReady, "", "", false, targetCanvasPoint);
        }

        private static DropCursorState pending(
            String motorStatus,
            String motorReason,
            boolean motorFailed,
            Point targetCanvasPoint
        ) {
            return new DropCursorState(false, false, motorStatus, motorReason, motorFailed, targetCanvasPoint);
        }
    }

    private static final class DropDispatchPlan {
        private final int preparationSlot;
        private final Point targetCanvasPoint;
        private final boolean semanticMisclick;

        private DropDispatchPlan(int preparationSlot, Point targetCanvasPoint, boolean semanticMisclick) {
            this.preparationSlot = preparationSlot;
            this.targetCanvasPoint = targetCanvasPoint == null ? null : new Point(targetCanvasPoint);
            this.semanticMisclick = semanticMisclick;
        }

        private static DropDispatchPlan intended(int slot, Point targetCanvasPoint) {
            return new DropDispatchPlan(slot, targetCanvasPoint, false);
        }

        private static DropDispatchPlan semanticMisclick(int slot, Point targetCanvasPoint) {
            return new DropDispatchPlan(slot, targetCanvasPoint, true);
        }
    }

    private static final class SlotSelection {
        private final boolean available;
        private final int slot;

        private SlotSelection(boolean available, int slot) {
            this.available = available;
            this.slot = slot;
        }

        private static SlotSelection available(int slot) {
            return new SlotSelection(true, slot);
        }

        private static SlotSelection none() {
            return new SlotSelection(false, -1);
        }
    }

    private static final class DropDispatchTuning {
        private final long localCooldownMinMs;
        private final long localCooldownMaxMs;
        private final long secondDispatchExtraCooldownMinMs;
        private final long secondDispatchExtraCooldownMaxMs;
        private final long microHesitationMinMs;
        private final long microHesitationMaxMs;
        private final int secondDispatchMinGapTicks;
        private final int secondDispatchMaxGapTicks;
        private final int secondDispatchChancePercent;
        private final int microHesitationChancePercent;
        private final int rhythmPauseChanceMinPercent;
        private final int rhythmPauseChanceMaxPercent;
        private final int rhythmPauseRampStartDispatches;

        private DropDispatchTuning(
            long localCooldownMinMs,
            long localCooldownMaxMs,
            long secondDispatchExtraCooldownMinMs,
            long secondDispatchExtraCooldownMaxMs,
            long microHesitationMinMs,
            long microHesitationMaxMs,
            int secondDispatchMinGapTicks,
            int secondDispatchMaxGapTicks,
            int secondDispatchChancePercent,
            int microHesitationChancePercent,
            int rhythmPauseChanceMinPercent,
            int rhythmPauseChanceMaxPercent,
            int rhythmPauseRampStartDispatches
        ) {
            this.localCooldownMinMs = Math.max(0L, localCooldownMinMs);
            this.localCooldownMaxMs = Math.max(this.localCooldownMinMs, localCooldownMaxMs);
            this.secondDispatchExtraCooldownMinMs = Math.max(0L, secondDispatchExtraCooldownMinMs);
            this.secondDispatchExtraCooldownMaxMs =
                Math.max(this.secondDispatchExtraCooldownMinMs, secondDispatchExtraCooldownMaxMs);
            this.microHesitationMinMs = Math.max(0L, microHesitationMinMs);
            this.microHesitationMaxMs = Math.max(this.microHesitationMinMs, microHesitationMaxMs);
            this.secondDispatchMinGapTicks = Math.max(0, secondDispatchMinGapTicks);
            this.secondDispatchMaxGapTicks = Math.max(this.secondDispatchMinGapTicks, secondDispatchMaxGapTicks);
            this.secondDispatchChancePercent = Math.max(0, Math.min(100, secondDispatchChancePercent));
            this.microHesitationChancePercent = Math.max(0, Math.min(100, microHesitationChancePercent));
            this.rhythmPauseChanceMinPercent = Math.max(0, Math.min(100, rhythmPauseChanceMinPercent));
            this.rhythmPauseChanceMaxPercent = Math.max(
                this.rhythmPauseChanceMinPercent,
                Math.min(100, rhythmPauseChanceMaxPercent)
            );
            this.rhythmPauseRampStartDispatches = Math.max(1, rhythmPauseRampStartDispatches);
        }
    }

    private static final class DropCadenceTuningOverrides {
        private final Integer localCooldownMinMs;
        private final Integer localCooldownMaxMs;
        private final Integer secondDispatchChancePercent;
        private final Integer rhythmPauseChanceMinPercent;
        private final Integer rhythmPauseChanceMaxPercent;
        private final Integer rhythmPauseRampStartDispatches;
        private final Integer sessionCooldownBiasMs;
        private final Integer targetCycleClicksMedian;
        private final Integer targetCycleDurationMsMedian;

        private DropCadenceTuningOverrides(
            Integer localCooldownMinMs,
            Integer localCooldownMaxMs,
            Integer secondDispatchChancePercent,
            Integer rhythmPauseChanceMinPercent,
            Integer rhythmPauseChanceMaxPercent,
            Integer rhythmPauseRampStartDispatches,
            Integer sessionCooldownBiasMs,
            Integer targetCycleClicksMedian,
            Integer targetCycleDurationMsMedian
        ) {
            this.localCooldownMinMs = localCooldownMinMs;
            this.localCooldownMaxMs = localCooldownMaxMs;
            this.secondDispatchChancePercent = secondDispatchChancePercent;
            this.rhythmPauseChanceMinPercent = rhythmPauseChanceMinPercent;
            this.rhythmPauseChanceMaxPercent = rhythmPauseChanceMaxPercent;
            this.rhythmPauseRampStartDispatches = rhythmPauseRampStartDispatches;
            this.sessionCooldownBiasMs = sessionCooldownBiasMs;
            this.targetCycleClicksMedian = targetCycleClicksMedian;
            this.targetCycleDurationMsMedian = targetCycleDurationMsMedian;
        }

        private static DropCadenceTuningOverrides none() {
            return new DropCadenceTuningOverrides(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }

        private static DropCadenceTuningOverrides fromPayload(JsonObject payload) {
            if (payload == null) {
                return none();
            }
            JsonObject tuning = asJsonObject(payload.get(DROP_CADENCE_TUNING_PAYLOAD_KEY));
            if (tuning == null) {
                return none();
            }
            Integer localCooldownMinMs = asIntBounded(tuning.get("localCooldownMinMs"), 0, 500);
            Integer localCooldownMaxMs = asIntBounded(tuning.get("localCooldownMaxMs"), 0, 700);
            if (localCooldownMinMs != null && localCooldownMaxMs != null && localCooldownMaxMs < localCooldownMinMs) {
                localCooldownMaxMs = localCooldownMinMs;
            }
            Integer secondDispatchChancePercent = asIntBounded(tuning.get("secondDispatchChancePercent"), 0, 100);
            Integer rhythmPauseChanceMinPercent = asIntBounded(tuning.get("rhythmPauseChanceMinPercent"), 0, 100);
            Integer rhythmPauseChanceMaxPercent = asIntBounded(tuning.get("rhythmPauseChanceMaxPercent"), 0, 100);
            if (rhythmPauseChanceMinPercent != null
                && rhythmPauseChanceMaxPercent != null
                && rhythmPauseChanceMaxPercent < rhythmPauseChanceMinPercent) {
                rhythmPauseChanceMaxPercent = rhythmPauseChanceMinPercent;
            }
            Integer rhythmPauseRampStartDispatches =
                asIntBounded(tuning.get("rhythmPauseRampStartDispatches"), 1, 60);
            Integer sessionCooldownBiasMs = asIntBounded(tuning.get("sessionCooldownBiasMs"), -40, 100);
            Integer targetCycleClicksMedian = asIntBounded(tuning.get("targetCycleClicksMedian"), 1, 99);
            Integer targetCycleDurationMsMedian = asIntBounded(tuning.get("targetCycleDurationMsMedian"), 1_000, 60_000);
            return new DropCadenceTuningOverrides(
                localCooldownMinMs,
                localCooldownMaxMs,
                secondDispatchChancePercent,
                rhythmPauseChanceMinPercent,
                rhythmPauseChanceMaxPercent,
                rhythmPauseRampStartDispatches,
                sessionCooldownBiasMs,
                targetCycleClicksMedian,
                targetCycleDurationMsMedian
            );
        }

        private DropDispatchTuning applyDispatchTuning(DropDispatchTuning base) {
            if (base == null) {
                return null;
            }
            long resolvedLocalCooldownMinMs = this.localCooldownMinMs == null
                ? base.localCooldownMinMs
                : Math.max(0L, this.localCooldownMinMs.longValue());
            long resolvedLocalCooldownMaxMs = this.localCooldownMaxMs == null
                ? base.localCooldownMaxMs
                : Math.max(resolvedLocalCooldownMinMs, this.localCooldownMaxMs.longValue());
            int resolvedSecondDispatchChancePercent = this.secondDispatchChancePercent == null
                ? base.secondDispatchChancePercent
                : clampPercent(this.secondDispatchChancePercent.intValue());
            int resolvedRhythmPauseChanceMinPercent = this.rhythmPauseChanceMinPercent == null
                ? base.rhythmPauseChanceMinPercent
                : clampPercent(this.rhythmPauseChanceMinPercent.intValue());
            int resolvedRhythmPauseChanceMaxPercent = this.rhythmPauseChanceMaxPercent == null
                ? base.rhythmPauseChanceMaxPercent
                : clampPercent(this.rhythmPauseChanceMaxPercent.intValue());
            resolvedRhythmPauseChanceMaxPercent = Math.max(
                resolvedRhythmPauseChanceMinPercent,
                resolvedRhythmPauseChanceMaxPercent
            );
            int resolvedRhythmPauseRampStartDispatches = this.rhythmPauseRampStartDispatches == null
                ? base.rhythmPauseRampStartDispatches
                : Math.max(1, this.rhythmPauseRampStartDispatches.intValue());
            return new DropDispatchTuning(
                resolvedLocalCooldownMinMs,
                resolvedLocalCooldownMaxMs,
                base.secondDispatchExtraCooldownMinMs,
                base.secondDispatchExtraCooldownMaxMs,
                base.microHesitationMinMs,
                base.microHesitationMaxMs,
                base.secondDispatchMinGapTicks,
                base.secondDispatchMaxGapTicks,
                resolvedSecondDispatchChancePercent,
                base.microHesitationChancePercent,
                resolvedRhythmPauseChanceMinPercent,
                resolvedRhythmPauseChanceMaxPercent,
                resolvedRhythmPauseRampStartDispatches
            );
        }

        private int resolveSessionCooldownBiasMs(int fallback) {
            if (sessionCooldownBiasMs == null) {
                return fallback;
            }
            return Math.max(-40, Math.min(100, sessionCooldownBiasMs.intValue()));
        }

        private int resolveTargetCycleClicksMedian(int fallback) {
            if (targetCycleClicksMedian == null) {
                return fallback;
            }
            return Math.max(1, Math.min(99, targetCycleClicksMedian.intValue()));
        }

        private int resolveTargetCycleDurationMsMedian(int fallback) {
            if (targetCycleDurationMsMedian == null) {
                return fallback;
            }
            return Math.max(1_000, Math.min(60_000, targetCycleDurationMsMedian.intValue()));
        }
    }

    private enum DropCadenceProfile {
        DB_PARITY("DB_PARITY");

        private final String profileKey;

        DropCadenceProfile(String profileKey) {
            this.profileKey = profileKey;
        }

        private static DropCadenceProfile fromPayload(JsonObject payload) {
            if (payload == null) {
                return DB_PARITY;
            }
            String raw = asString(payload.get(DROP_CADENCE_PROFILE_PAYLOAD_KEY), "DB_PARITY");
            return fromString(raw);
        }

        private static DropCadenceProfile fromString(String value) {
            return DB_PARITY;
        }
    }

    private enum DropTraversalProfile {
        SERPENTINE,
        COLUMN_BIASED,
        WAVE_BIASED
    }

}
