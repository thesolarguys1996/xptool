package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleBehaviorProfile;
import com.xptool.sessions.idle.IdleSkillContext;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdleRuntimeTest {
    @Test
    void doesNotReleaseIdleOwnershipWhenSessionConflictBlocksBeforeAcquire() {
        TestHost host = new TestHost();
        host.hasActiveSession = true;
        host.hasActiveSessionOtherThanInteraction = true;
        host.activeSessionName = Optional.of("drop_sweep");

        IdleRuntime runtime = new IdleRuntime(host);
        runtime.onGameTick(100);

        assertTrue(host.emittedReasons.contains("idle_gate_session_conflict"));
        assertEquals(0, host.acquireOrRenewIdleMotorOwnershipCalls);
        assertEquals(0, host.releaseIdleMotorOwnershipCalls);
    }

    @Test
    void releasesHeldIdleOwnershipWhenSessionConflictAppears() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.FISHING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.lastInteractionClickSerial = 0L;
        host.offscreenTarget = Optional.of(new Point(1600, 900));
        host.offscreenMoveResult = true;

        IdleRuntime runtime = new IdleRuntime(host);

        host.lastInteractionClickSerial = 1L;
        runtime.onGameTick(101);

        assertEquals(1, host.acquireOrRenewIdleMotorOwnershipCalls);
        assertEquals(0, host.releaseIdleMotorOwnershipCalls);

        host.hasActiveSession = true;
        host.hasActiveSessionOtherThanInteraction = true;
        host.activeSessionName = Optional.of("drop_sweep");

        runtime.onGameTick(102);

        assertTrue(host.emittedReasons.contains("idle_gate_session_conflict"));
        assertEquals(1, host.releaseIdleMotorOwnershipCalls);
    }

    @Test
    void offscreenWatchModeWaitsForNewClickSignal() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.FISHING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.lastInteractionClickSerial = 5L;

        IdleRuntime runtime = new IdleRuntime(host);
        runtime.onGameTick(120);

        assertTrue(host.emittedReasons.contains("idle_offscreen_watch_mode_waiting_for_new_click"));
        assertTrue(host.emittedReasons.contains("idle_offscreen_gate_snapshot"));
        JsonObject details = host.lastDetailsByReason.get("idle_offscreen_gate_snapshot");
        assertEquals("waiting_for_new_click", details.get("gateReasonCode").getAsString());
    }

    @Test
    void offscreenWatchModeMovesWhenNewClickSignalArrives() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.FISHING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.lastInteractionClickSerial = 0L;

        IdleRuntime runtime = new IdleRuntime(host);

        host.lastInteractionClickSerial = 1L;
        host.offscreenTarget = Optional.of(new Point(1600, 900));
        host.offscreenMoveResult = true;

        runtime.onGameTick(140);

        assertTrue(host.emittedReasons.contains("offscreen_attempt_selected"));
        assertTrue(host.emittedReasons.contains("idle_fishing_offscreen_park_move"));
        assertEquals(1, host.acquireOrRenewIdleMotorOwnershipCalls);
        assertEquals(0, host.releaseIdleMotorOwnershipCalls);
    }

    @Test
    void woodcutOffscreenWatchModeEmitsWoodcutReasonLabels() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.WOODCUTTING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.lastInteractionClickSerial = 0L;

        IdleRuntime runtime = new IdleRuntime(host);

        host.lastInteractionClickSerial = 1L;
        host.offscreenTarget = Optional.of(new Point(1600, 900));
        host.offscreenMoveResult = true;

        runtime.onGameTick(141);

        assertTrue(host.emittedReasons.contains("offscreen_attempt_selected"));
        assertTrue(host.emittedReasons.contains("idle_woodcut_offscreen_park_move"));
        assertFalse(host.emittedReasons.contains("idle_fishing_offscreen_park_move"));
    }

    @Test
    void offscreenWatchModeDefersWhenFishingHoldSuppressionIsActive() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.FISHING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.lastInteractionClickSerial = 0L;

        IdleRuntime runtime = new IdleRuntime(host);

        host.lastInteractionClickSerial = 1L;
        host.offscreenTarget = Optional.of(new Point(1600, 900));
        host.offscreenMoveResult = true;
        host.fishingOffscreenIdleSuppressed = true;
        host.fishingOffscreenIdleSuppressionRemainingMs = 3200L;
        runtime.onGameTick(145);

        assertTrue(host.emittedReasons.contains("idle_fishing_offscreen_hold_suppressed"));
        assertFalse(host.emittedReasons.contains("idle_fishing_offscreen_park_move"));
    }

    @Test
    void woodcutOffscreenSuppressionPreservesClickSignalUntilHoldEnds() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.WOODCUTTING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.lastInteractionClickSerial = 0L;

        IdleRuntime runtime = new IdleRuntime(host);

        host.lastInteractionClickSerial = 1L;
        host.fishingOffscreenIdleSuppressed = true;
        host.fishingOffscreenIdleSuppressionRemainingMs = 3200L;
        runtime.onGameTick(146);

        assertTrue(host.emittedReasons.contains("idle_woodcut_offscreen_hold_suppressed"));
        assertFalse(host.emittedReasons.contains("idle_woodcut_offscreen_park_move"));

        host.fishingOffscreenIdleSuppressed = false;
        host.fishingOffscreenIdleSuppressionRemainingMs = 0L;
        host.offscreenTarget = Optional.of(new Point(1600, 900));
        host.offscreenMoveResult = true;
        for (int tick = 147; tick <= 260; tick++) {
            runtime.onGameTick(tick);
            if (host.emittedReasons.contains("idle_woodcut_offscreen_park_move")) {
                break;
            }
        }

        assertTrue(host.emittedReasons.contains("idle_woodcut_offscreen_park_move"));
    }

    @Test
    void woodcutOffscreenUnavailableDoesNotFallbackToInWindowIdleMoves() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.WOODCUTTING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.lastInteractionClickSerial = 0L;
        host.offscreenTarget = Optional.empty();
        host.offscreenMoveResult = false;

        IdleRuntime runtime = new IdleRuntime(host);

        host.lastInteractionClickSerial = 1L;
        runtime.onGameTick(148);

        assertTrue(host.emittedReasons.contains("offscreen_attempt_selected"));
        assertFalse(host.emittedReasons.contains("idle_woodcut_offscreen_park_move"));
        assertFalse(host.emittedReasons.contains("idle_hover_move"));
        assertFalse(host.emittedReasons.contains("idle_drift_move"));
    }

    @Test
    void offscreenWatchModeRunsDuringDropSessionWhenFishingInventoryFullAfkIsActive() {
        TestHost host = new TestHost();
        host.hasActiveSession = true;
        host.hasActiveSessionOtherThanInteraction = true;
        host.activeSessionName = Optional.of("drop_sweep");
        host.hasActiveDropSweepSession = true;
        host.idleSkillContext = IdleSkillContext.FISHING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.fishingInventoryFullAfkActive = true;
        host.fishingInventoryFullAfkRemainingMs = 62_000L;
        host.lastInteractionClickSerial = 0L;
        host.offscreenTarget = Optional.of(new Point(1600, 900));
        host.offscreenMoveResult = true;

        IdleRuntime runtime = new IdleRuntime(host);
        runtime.onGameTick(149);

        assertTrue(host.emittedReasons.contains("idle_gate_session_conflict_fishing_afk_override"));
        assertTrue(host.emittedReasons.contains("idle_fishing_offscreen_park_move"));
    }

    @Test
    void actedIdleEventsIncludeBlendTelemetryFields() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.FISHING;
        host.fishingIdleMode = FishingIdleMode.OFFSCREEN_BIASED;
        host.lastInteractionClickSerial = 0L;

        IdleRuntime runtime = new IdleRuntime(host);

        host.lastInteractionClickSerial = 1L;
        host.offscreenTarget = Optional.of(new Point(1590, 902));
        host.offscreenMoveResult = true;
        runtime.onGameTick(150);

        JsonObject details = host.lastDetailsByReason.get("idle_fishing_offscreen_park_move");
        assertTrue(details != null && details.has("blendMode"));
        assertEquals("offscreen_focus", details.get("blendMode").getAsString());
        assertTrue(details.has("blendModeUntilTick"));
        assertTrue(details.has("cadenceEnvelope"));
        assertEquals("patient", details.get("cadenceEnvelope").getAsString());
        assertTrue(details.has("blendTransitionReason"));
        assertTrue(details.has("cadenceEnvelopeReason"));
    }

    @Test
    void noopTelemetryIncludesCadenceAndStrategyFields() {
        TestHost host = new TestHost();
        host.hoverMoveResult = false;
        host.offscreenMoveResult = false;

        IdleRuntime runtime = new IdleRuntime(host);
        runtime.onGameTick(200);

        JsonObject details = host.lastDetailsByReason.get("idle_noop_sampled");
        assertTrue(details != null && details.has("blendMode"));
        assertTrue(details.has("cadenceEnvelope"));
        assertTrue(details.has("blendTransitionReason"));
        assertTrue(details.has("cadenceEnvelopeReason"));
        assertTrue(details.has("recentActionDiversity"));
    }

    @Test
    void woodcutIdleDoesNotRequireNewInteractionClickSignal() {
        TestHost host = new TestHost();
        host.idleSkillContext = IdleSkillContext.WOODCUTTING;
        ActivityIdlePolicy woodcutPolicy = ActivityIdlePolicy.of(
            "DB_PARITY",
            FishingIdleMode.STANDARD,
            new IdleBehaviorProfile(100, 0, 0, 0, 0, 0, 0),
            ActivityIdleCadenceWindow.of(1, 1, 1, 1)
        );
        host.idlePolicyRegistry = ActivityIdlePolicyRegistry.of(
            woodcutPolicy,
            Map.of(ActivityIdlePolicyRegistry.ACTIVITY_WOODCUTTING, woodcutPolicy)
        );

        IdleRuntime runtime = new IdleRuntime(host);

        runtime.onGameTick(10);
        assertTrue(woodcutMoveCount(host) >= 1L);

        for (int tick = 11; tick <= 30; tick++) {
            runtime.onGameTick(tick);
        }
        assertTrue(woodcutMoveCount(host) > 1L);
    }

    private static long woodcutMoveCount(TestHost host) {
        return host.emittedReasons
            .stream()
            .filter(reason -> "idle_hover_move".equals(reason) || "idle_drift_move".equals(reason))
            .count();
    }

    private static final class TestHost implements IdleRuntime.Host {
        private boolean hasActiveSession = false;
        private boolean hasActiveSessionOtherThanInteraction = false;
        private Optional<String> activeSessionName = Optional.empty();
        private boolean hasActiveDropSweepSession = false;
        private int releaseIdleMotorOwnershipCalls = 0;
        private boolean idleInterActionWindowOpen = true;
        private IdleSkillContext idleSkillContext = IdleSkillContext.GLOBAL;
        private boolean idleActionWindowOpen = true;
        private boolean idleCameraWindowOpen = false;
        private boolean idleAnimationActiveNow = false;
        private boolean idleInteractionDelaySatisfied = true;
        private boolean idleCameraInteractionDelaySatisfied = true;
        private long lastInteractionClickSerial = 0L;
        private boolean cursorOutsideClientWindow = false;
        private boolean acquireOrRenewIdleMotorOwnership = true;
        private int acquireOrRenewIdleMotorOwnershipCalls = 0;
        private boolean canPerformIdleMotorActionNow = true;
        private boolean idleCameraMicroAdjustResult = false;
        private Optional<Point> hoverTarget = Optional.of(new Point(250, 250));
        private boolean hoverMoveResult = true;
        private Optional<Point> driftTarget = Optional.of(new Point(270, 260));
        private Optional<Point> offscreenTarget = Optional.empty();
        private boolean offscreenMoveResult = false;
        private Optional<Point> parkingTarget = Optional.of(new Point(300, 300));
        private FishingIdleMode fishingIdleMode = FishingIdleMode.STANDARD;
        private IdleCadenceTuning activeIdleCadenceTuning = IdleCadenceTuning.none();
        private FatigueSnapshot fatigueSnapshot = FatigueSnapshot.neutral();
        private ActivityIdlePolicyRegistry idlePolicyRegistry = ActivityIdlePolicyRegistry.defaults();
        private boolean fishingOffscreenIdleSuppressed = false;
        private long fishingOffscreenIdleSuppressionRemainingMs = 0L;
        private boolean fishingInventoryFullAfkActive = false;
        private long fishingInventoryFullAfkRemainingMs = 0L;
        private final List<String> emittedReasons = new ArrayList<>();
        private final Map<String, JsonObject> lastDetailsByReason = new HashMap<>();

        @Override
        public boolean hasActiveSession() {
            return hasActiveSession;
        }

        @Override
        public boolean hasActiveSessionOtherThan(String sessionName) {
            return hasActiveSessionOtherThanInteraction;
        }

        @Override
        public Optional<String> activeSessionName() {
            return activeSessionName;
        }

        @Override
        public boolean hasActiveDropSweepSession() {
            return hasActiveDropSweepSession;
        }

        @Override
        public void releaseIdleMotorOwnership() {
            releaseIdleMotorOwnershipCalls++;
        }

        @Override
        public boolean isIdleInterActionWindowOpen() {
            return idleInterActionWindowOpen;
        }

        @Override
        public IdleSkillContext resolveIdleSkillContext() {
            return idleSkillContext;
        }

        @Override
        public boolean isIdleActionWindowOpen() {
            return idleActionWindowOpen;
        }

        @Override
        public boolean isIdleCameraWindowOpen() {
            return idleCameraWindowOpen;
        }

        @Override
        public JsonObject idleWindowGateSnapshot() {
            return new JsonObject();
        }

        @Override
        public boolean isIdleAnimationActiveNow() {
            return idleAnimationActiveNow;
        }

        @Override
        public boolean isIdleInteractionDelaySatisfied() {
            return idleInteractionDelaySatisfied;
        }

        @Override
        public boolean isIdleCameraInteractionDelaySatisfied() {
            return idleCameraInteractionDelaySatisfied;
        }

        @Override
        public long lastInteractionClickSerial() {
            return lastInteractionClickSerial;
        }

        @Override
        public boolean isCursorOutsideClientWindow() {
            return cursorOutsideClientWindow;
        }

        @Override
        public boolean acquireOrRenewIdleMotorOwnership() {
            acquireOrRenewIdleMotorOwnershipCalls++;
            return acquireOrRenewIdleMotorOwnership;
        }

        @Override
        public boolean canPerformIdleMotorActionNow() {
            return canPerformIdleMotorActionNow;
        }

        @Override
        public boolean performIdleCameraMicroAdjust() {
            return idleCameraMicroAdjustResult;
        }

        @Override
        public Optional<Point> resolveIdleHoverTargetCanvasPoint() {
            return hoverTarget.map(Point::new);
        }

        @Override
        public boolean performIdleCursorMove(Point canvasTarget) {
            return hoverMoveResult;
        }

        @Override
        public Optional<Point> resolveIdleDriftTargetCanvasPoint() {
            return driftTarget.map(Point::new);
        }

        @Override
        public Optional<Point> resolveIdleOffscreenTargetScreenPoint() {
            return offscreenTarget.map(Point::new);
        }

        @Override
        public boolean performIdleOffscreenCursorMove(Point screenTarget) {
            return offscreenMoveResult;
        }

        @Override
        public Optional<Point> resolveIdleParkingTargetCanvasPoint() {
            return parkingTarget.map(Point::new);
        }

        @Override
        public FishingIdleMode resolveFishingIdleMode(IdleSkillContext context) {
            return fishingIdleMode;
        }

        @Override
        public ActivityIdlePolicy resolveActivityIdlePolicy(IdleSkillContext context) {
            return idlePolicyRegistry.resolveForContext(context);
        }

        @Override
        public IdleCadenceTuning activeIdleCadenceTuning() {
            return activeIdleCadenceTuning;
        }

        @Override
        public FatigueSnapshot fatigueSnapshot() {
            return fatigueSnapshot;
        }

        @Override
        public boolean isFishingOffscreenIdleSuppressed() {
            return fishingOffscreenIdleSuppressed;
        }

        @Override
        public long fishingOffscreenIdleSuppressionRemainingMs() {
            return fishingOffscreenIdleSuppressionRemainingMs;
        }

        @Override
        public boolean isFishingInventoryFullAfkActive() {
            return fishingInventoryFullAfkActive;
        }

        @Override
        public long fishingInventoryFullAfkRemainingMs() {
            return fishingInventoryFullAfkRemainingMs;
        }

        @Override
        public void emitIdleEvent(String reason, JsonObject details) {
            String safeReason = reason == null ? "" : reason;
            emittedReasons.add(safeReason);
            lastDetailsByReason.put(safeReason, details == null ? new JsonObject() : details.deepCopy());
        }
    }
}
