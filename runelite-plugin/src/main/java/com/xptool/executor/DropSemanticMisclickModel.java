package com.xptool.executor;

import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;

final class DropSemanticMisclickModel {
    private long nextAllowedAtMs = 0L;
    private int sessionDispatchAttempts = 0;

    void reset() {
        nextAllowedAtMs = 0L;
        sessionDispatchAttempts = 0;
    }

    OptionalInt maybeSelectAdjacentSlot(int intendedSlot, FatigueSnapshot fatigue) {
        if (intendedSlot < 0 || intendedSlot >= 28) {
            return OptionalInt.empty();
        }
        sessionDispatchAttempts++;
        if (sessionDispatchAttempts <= ExecutorDropProfile.DROP_SEMANTIC_MISCLICK_WARMUP_DISPATCHES) {
            return OptionalInt.empty();
        }
        if (ExecutorDropProfile.DROP_SEMANTIC_MISCLICK_SUPPRESS_TOP_ROW && intendedSlot < 4) {
            return OptionalInt.empty();
        }
        long now = System.currentTimeMillis();
        if (now < nextAllowedAtMs) {
            return OptionalInt.empty();
        }
        int chancePercent = clampPercent(
            ExecutorDropProfile.DROP_SEMANTIC_MISCLICK_BASE_CHANCE_PERCENT
                + (fatigue == null ? 0 : fatigue.dropHesitationChanceBiasPercent(
                    ExecutorDropProfile.DROP_SEMANTIC_MISCLICK_FATIGUE_BONUS_MAX_PERCENT
                ))
        );
        if (chancePercent <= 0 || ThreadLocalRandom.current().nextInt(100) >= chancePercent) {
            return OptionalInt.empty();
        }

        int chosen = preferredAdjacentSlot(intendedSlot);
        if (chosen < 0 || chosen >= 28) {
            return OptionalInt.empty();
        }
        nextAllowedAtMs = now + randomLongInclusive(
            ExecutorDropProfile.DROP_SEMANTIC_MISCLICK_MIN_GAP_MS,
            ExecutorDropProfile.DROP_SEMANTIC_MISCLICK_MAX_GAP_MS
        );
        return OptionalInt.of(chosen);
    }

    private static int preferredAdjacentSlot(int slot) {
        int row = slot / 4;
        int col = slot % 4;
        if ((row & 1) == 0) {
            if (col < 3) {
                return slot + 1;
            }
            return slot - 1;
        } else {
            if (col > 0) {
                return slot - 1;
            }
            return slot + 1;
        }
    }

    private static long randomLongInclusive(long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        if (lo == hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
