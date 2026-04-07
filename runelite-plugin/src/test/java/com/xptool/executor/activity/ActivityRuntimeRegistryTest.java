package com.xptool.executor.activity;

import net.runelite.api.NPC;
import net.runelite.api.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityRuntimeRegistryTest {
    @Test
    void resolvesRuntimeByKeyAndType() {
        DummyFishingRuntime fishingRuntime = new DummyFishingRuntime();
        ActivityRuntimeRegistry registry = ActivityRuntimeRegistry.of(fishingRuntime);

        FishingActivityRuntime resolved = registry.require("fishing", FishingActivityRuntime.class);

        assertSame(fishingRuntime, resolved);
    }

    @Test
    void throwsForMissingKey() {
        ActivityRuntimeRegistry registry = ActivityRuntimeRegistry.of(new DummyRuntime("woodcutting"));

        assertThrows(
            IllegalStateException.class,
            () -> registry.require("fishing", FishingActivityRuntime.class)
        );
    }

    @Test
    void throwsForTypeMismatch() {
        ActivityRuntimeRegistry registry = ActivityRuntimeRegistry.of(new DummyRuntime("fishing"));

        assertThrows(
            IllegalStateException.class,
            () -> registry.require("fishing", FishingActivityRuntime.class)
        );
    }

    @Test
    void throwsForDuplicateKeys() {
        DummyRuntime a = new DummyRuntime("fishing");
        DummyRuntime b = new DummyRuntime("fishing");

        assertThrows(IllegalArgumentException.class, () -> ActivityRuntimeRegistry.of(a, b));
    }

    private static final class DummyRuntime implements ActivityRuntime {
        private final String key;

        private DummyRuntime(String key) {
            this.key = key;
        }

        @Override
        public String activityKey() {
            return key;
        }
    }

    private static final class DummyFishingRuntime implements FishingActivityRuntime {
        @Override
        public void extendRetryWindow(long durationMs) {
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
        }
    }
}
