package com.xptool.executor;

import com.xptool.motion.MotionProfile;

final class MoveAndClickMotorProfileSpec {
    final String owner;
    final String clickType;
    final boolean requireHoverSettleBeforeClick;
    final MotionProfile fallbackMotionProfile;
    final MotorMenuValidationMode menuValidationMode;
    final int hoverSettleTicks;
    final int maxMenuValidationTicks;
    final int maxStepsPerTick;

    MoveAndClickMotorProfileSpec(
        String owner,
        String clickType,
        boolean requireHoverSettleBeforeClick,
        MotionProfile fallbackMotionProfile,
        MotorMenuValidationMode menuValidationMode,
        int hoverSettleTicks,
        int maxMenuValidationTicks,
        int maxStepsPerTick
    ) {
        this.owner = safeString(owner);
        this.clickType = safeString(clickType);
        this.requireHoverSettleBeforeClick = requireHoverSettleBeforeClick;
        this.fallbackMotionProfile = fallbackMotionProfile == null ? MotionProfile.GENERIC_INTERACT : fallbackMotionProfile;
        this.menuValidationMode = menuValidationMode == null ? MotorMenuValidationMode.NONE : menuValidationMode;
        this.hoverSettleTicks = Math.max(0, hoverSettleTicks);
        this.maxMenuValidationTicks = Math.max(1, maxMenuValidationTicks);
        this.maxStepsPerTick = Math.max(1, maxStepsPerTick);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
