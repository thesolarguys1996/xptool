package com.xptool.executor;

final class ExecutorDropProfile {
    static final double DROP_HOVER_TARGET_TOLERANCE_PX = 3.0;
    static final int DROP_SWEEP_NO_PROGRESS_LIMIT = 8;
    static final int DROP_SWEEP_DISPATCH_FAIL_LIMIT = 12;
    static final long DROP_DEBUG_EMIT_MIN_INTERVAL_MS = 250L;
    static final int DROP_SEMANTIC_MISCLICK_BASE_CHANCE_PERCENT = 0;
    static final int DROP_SEMANTIC_MISCLICK_FATIGUE_BONUS_MAX_PERCENT = 1;
    static final long DROP_SEMANTIC_MISCLICK_MIN_GAP_MS = 8000L;
    static final long DROP_SEMANTIC_MISCLICK_MAX_GAP_MS = 18000L;
    static final int DROP_SEMANTIC_MISCLICK_WARMUP_DISPATCHES = 8;
    static final boolean DROP_SEMANTIC_MISCLICK_SUPPRESS_TOP_ROW = true;
    static final long DROP_WRAP_RECOVERY_COOLDOWN_MIN_MS = 180L;
    static final long DROP_WRAP_RECOVERY_COOLDOWN_MAX_MS = 360L;
    static final int DROP_WRAP_RECOVERY_FROM_ROW_MIN = 4;
    static final int DROP_WRAP_RECOVERY_TO_ROW_MAX = 1;
    static final int DROP_WRAP_RECOVERY_MIN_SLOT_DISTANCE = 12;

    private ExecutorDropProfile() {
    }
}
