package com.xptool.executor;

import com.xptool.motion.MotionProfile;

final class MoveOnlyMotorProfileSpec {
    final String owner;
    final String clickType;
    final MotionProfile fallbackMotionProfile;
    final MotorMenuValidationMode menuValidationMode;
    final int maxStepsPerTick;
    final long postGestureCooldownMs;

    MoveOnlyMotorProfileSpec(
        String owner,
        String clickType,
        MotionProfile fallbackMotionProfile,
        MotorMenuValidationMode menuValidationMode,
        int maxStepsPerTick,
        long postGestureCooldownMs
    ) {
        this.owner = safeString(owner);
        this.clickType = safeString(clickType);
        this.fallbackMotionProfile = fallbackMotionProfile == null ? MotionProfile.GENERIC_INTERACT : fallbackMotionProfile;
        this.menuValidationMode = menuValidationMode == null ? MotorMenuValidationMode.NONE : menuValidationMode;
        this.maxStepsPerTick = Math.max(1, maxStepsPerTick);
        this.postGestureCooldownMs = Math.max(1L, postGestureCooldownMs);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
