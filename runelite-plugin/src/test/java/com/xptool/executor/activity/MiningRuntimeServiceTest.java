package com.xptool.executor.activity;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningRuntimeServiceTest {
    @Test
    void extendRetryWindowUsesSampledDuration() {
        CapturingMiningRuntime runtime = new CapturingMiningRuntime();
        MiningRuntimeService service = new MiningRuntimeService(
            runtime,
            (baseMs, jitterMs, spread) -> 2222L,
            900L,
            1200L,
            0.12,
            0.08
        );

        service.extendRetryWindow();

        assertEquals(2222L, runtime.lastRetryWindowMs);
    }

    @Test
    void suppressRockTargetDelegatesToRuntime() {
        CapturingMiningRuntime runtime = new CapturingMiningRuntime();
        MiningRuntimeService service = new MiningRuntimeService(
            runtime,
            (baseMs, jitterMs, spread) -> baseMs,
            900L,
            1200L,
            0.12,
            0.08
        );
        WorldPoint rock = new WorldPoint(3200, 3200, 0);

        service.suppressRockTarget(rock, 1500L);

        assertEquals(rock, runtime.lastSuppressedRock);
        assertEquals(1500L, runtime.lastSuppressionDurationMs);
        assertTrue(service.isRockSuppressed(rock));
    }

    private static final class CapturingMiningRuntime implements MiningActivityRuntime {
        long lastRetryWindowMs = -1L;
        WorldPoint lastSuppressedRock = null;
        long lastSuppressionDurationMs = -1L;

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
        public void clearInteractionWindows() {
        }

        @Override
        public void suppressRockTarget(WorldPoint worldPoint, long durationMs) {
            lastSuppressedRock = worldPoint;
            lastSuppressionDurationMs = durationMs;
        }

        @Override
        public boolean isRockSuppressed(WorldPoint worldPoint) {
            return worldPoint != null && worldPoint.equals(lastSuppressedRock);
        }

        @Override
        public void pruneRockSuppression() {
        }
    }
}
