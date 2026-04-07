package com.xptool.executor.activity;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WoodcuttingRuntimeServiceTest {
    @Test
    void extendRetryWindowUsesSampledDuration() {
        CapturingWoodcuttingRuntime runtime = new CapturingWoodcuttingRuntime();
        WoodcuttingRuntimeService service = new WoodcuttingRuntimeService(
            runtime,
            (baseMs, jitterMs, spread) -> 1111L,
            900L,
            1200L,
            1000L,
            100L,
            1500L,
            200L,
            0.12,
            0.08,
            0.18
        );

        service.extendRetryWindow();

        assertEquals(1111L, runtime.lastRetryWindowMs);
    }

    @Test
    void clearInteractionWindowsPreserveDispatchSignalDelegates() {
        CapturingWoodcuttingRuntime runtime = new CapturingWoodcuttingRuntime();
        WoodcuttingRuntimeService service = new WoodcuttingRuntimeService(
            runtime,
            (baseMs, jitterMs, spread) -> baseMs,
            900L,
            1200L,
            1000L,
            100L,
            1500L,
            200L,
            0.12,
            0.08,
            0.18
        );

        service.clearInteractionWindowsPreserveDispatchSignal();

        assertEquals(1, runtime.preserveClearCalls);
    }

    @Test
    void noteTargetAttemptSkipsWhenTargetMissingWorldPoint() {
        CapturingWoodcuttingRuntime runtime = new CapturingWoodcuttingRuntime();
        WoodcuttingRuntimeService service = new WoodcuttingRuntimeService(
            runtime,
            (baseMs, jitterMs, spread) -> baseMs,
            900L,
            1200L,
            1000L,
            100L,
            1500L,
            200L,
            0.12,
            0.08,
            0.18
        );

        service.noteTargetAttempt(new WorldPoint(3200, 3200, 0), (TileObject) null);

        assertEquals(0, runtime.noteTargetAttemptCalls);
    }

    private static final class CapturingWoodcuttingRuntime implements WoodcuttingActivityRuntime {
        long lastRetryWindowMs = -1L;
        int preserveClearCalls = 0;
        int noteTargetAttemptCalls = 0;

        @Override
        public void extendRetryWindow(long durationMs) {
            lastRetryWindowMs = durationMs;
        }

        @Override
        public void beginOutcomeWaitWindow(long durationMs) {
        }

        @Override
        public void clearOutcomeWaitWindow() {
        }

        @Override
        public void noteTargetAttempt(
            WorldPoint localWorldPoint,
            WorldPoint targetWorldPoint,
            long approachBaseWaitMs,
            long approachWaitPerTileMs,
            long approachMaxWaitMs,
            long approachMinHoldMs
        ) {
            noteTargetAttemptCalls++;
        }

        @Override
        public void clearTargetAttempt() {
        }

        @Override
        public void noteDispatchAttempt(WorldPoint targetWorldPoint, long nowMs) {
        }

        @Override
        public void clearDispatchAttempt() {
        }

        @Override
        public void clearInteractionWindows() {
        }

        @Override
        public void clearInteractionWindowsPreserveDispatchSignal() {
            preserveClearCalls++;
        }
    }
}
