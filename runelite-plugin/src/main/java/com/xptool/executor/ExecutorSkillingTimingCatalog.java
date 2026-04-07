package com.xptool.executor;

final class ExecutorSkillingTimingCatalog {
    static final SkillTimingProfile WOODCUT_TIMING_PROFILE =
        new SkillTimingProfile(900L, 1500L, 0L);
    static final long WOODCUT_APPROACH_BASE_WAIT_MS = 1100L;
    static final long WOODCUT_APPROACH_WAIT_PER_TILE_MS = 240L;
    static final long WOODCUT_APPROACH_MAX_WAIT_MS = 4200L;
    static final long WOODCUT_APPROACH_MIN_HOLD_MS = 90L;
    static final long WOODCUT_SAME_TARGET_RECLICK_COOLDOWN_MS = 2200L;

    static final SkillTimingProfile MINING_TIMING_PROFILE =
        new SkillTimingProfile(900L, 1500L, 2200L);

    static final SkillTimingProfile FISHING_TIMING_PROFILE =
        new SkillTimingProfile(760L, 1120L, 1200L);
    static final long FISHING_APPROACH_BASE_WAIT_MS = 300L;
    static final long FISHING_APPROACH_WAIT_PER_TILE_MS = 72L;
    static final long FISHING_APPROACH_MAX_WAIT_MS = 1120L;
    static final long FISHING_SAME_TARGET_RECLICK_COOLDOWN_MS = 1600L;

    static final SkillTimingProfile COMBAT_TIMING_PROFILE =
        new SkillTimingProfile(850L, 1500L, 1800L);

    private ExecutorSkillingTimingCatalog() {
    }
}
