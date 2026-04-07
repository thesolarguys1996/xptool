package com.xptool.executor;

final class BrutusConstants {
    static final String ENCOUNTER_PROFILE = "brutus";
    static final long DODGE_DEBOUNCE_MS = 520L;
    static final long POST_DODGE_HOLD_MS = 1350L;
    static final int REPEAT_TELEGRAPH_GUARD_TICKS = 2;
    static final int EAT_PRIORITY_WINDOW_TICKS = 5;
    static final long DODGE_PROGRESS_CHECK_MS = 850L;
    static final long DODGE_STUCK_TIMEOUT_MS = 2200L;
    static final long DODGE_TILE_SUPPRESS_MS = 2400L;
    static final int MIN_ESCAPE_EXITS = 2;
    static final int MIN_ESCAPE_EXITS_STRICT = 3;
    static final int DODGE_THREAT_RANGE_TILES = 12;
    static final int NEARBY_SCAN_RANGE_TILES = DODGE_THREAT_RANGE_TILES + 4;
    static final int NO_SAFE_TILE_RECOVERY_WINDOW_TICKS = 8;

    private BrutusConstants() {
    }
}
