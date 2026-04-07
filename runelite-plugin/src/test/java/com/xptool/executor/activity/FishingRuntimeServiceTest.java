package com.xptool.executor.activity;

import net.runelite.api.NPC;
import net.runelite.api.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FishingRuntimeServiceTest {
    @Test
    void extendRetryWindowUsesSampledDuration() {
        CapturingFishingRuntime runtime = new CapturingFishingRuntime();
        FishingRuntimeService service = new FishingRuntimeService(
            runtime,
            (baseMs, jitterMs, spread) -> 1234L,
            900L,
            1200L,
            1000L,
            100L,
            1500L,
            0.12,
            0.08,
            0.18
        );

        service.extendRetryWindow();

        assertEquals(1234L, runtime.lastRetryWindowMs);
    }

    @Test
    void clearInteractionWindowsPreserveDispatchSignalDelegates() {
        CapturingFishingRuntime runtime = new CapturingFishingRuntime();
        FishingRuntimeService service = new FishingRuntimeService(
            runtime,
            (baseMs, jitterMs, spread) -> baseMs,
            900L,
            1200L,
            1000L,
            100L,
            1500L,
            0.12,
            0.08,
            0.18
        );

        service.clearInteractionWindowsPreserveDispatchSignal();

        assertEquals(1, runtime.preserveClearCalls);
    }

    private static final class CapturingFishingRuntime implements FishingActivityRuntime {
        long lastRetryWindowMs = -1L;
        int preserveClearCalls = 0;

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
            Player localPlayer,
            NPC targetNpc,
            long approachBaseWaitMs,
            long approachWaitPerTileMs,
            long approachMaxWaitMs,
            long outcomeWaitWindowMs
        ) {
        }

        @Override
        public void clearTargetAttempt() {
        }

        @Override
        public void noteDispatchAttempt(NPC targetNpc, long nowMs) {
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
