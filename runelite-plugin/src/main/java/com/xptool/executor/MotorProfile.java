package com.xptool.executor;

import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.motion.MotionProfile.MotorGestureMode;
import net.runelite.api.TileObject;

final class MotorProfile {
    final String owner;
    final String clickType;
    final MotorGestureMode gestureMode;
    final boolean allowActivationClick;
    final boolean enforceMutationBudget;
    final ClickMotionSettings clickSettings;
    final MotorMenuValidationMode menuValidationMode;
    final TileObject menuTargetObject;
    final int hoverSettleTicks;
    final int maxMenuValidationTicks;
    final int maxMoveStepsPerTick;
    final long postGestureCooldownMs;

    MotorProfile(
        String owner,
        String clickType,
        MotorGestureMode gestureMode,
        boolean allowActivationClick,
        boolean enforceMutationBudget,
        ClickMotionSettings clickSettings,
        MotorMenuValidationMode menuValidationMode,
        TileObject menuTargetObject,
        int hoverSettleTicks,
        int maxMenuValidationTicks,
        int maxMoveStepsPerTick,
        long postGestureCooldownMs
    ) {
        this.owner = safeString(owner);
        this.clickType = safeString(clickType);
        this.gestureMode = gestureMode == null ? MotorGestureMode.GENERAL : gestureMode;
        this.allowActivationClick = allowActivationClick;
        this.enforceMutationBudget = enforceMutationBudget;
        this.clickSettings = clickSettings == null ? MotionProfile.GENERIC_INTERACT.directClickSettings : clickSettings;
        this.menuValidationMode = menuValidationMode == null ? MotorMenuValidationMode.NONE : menuValidationMode;
        this.menuTargetObject = menuTargetObject;
        this.hoverSettleTicks = Math.max(0, hoverSettleTicks);
        this.maxMenuValidationTicks = Math.max(1, maxMenuValidationTicks);
        this.maxMoveStepsPerTick = Math.max(1, maxMoveStepsPerTick);
        this.postGestureCooldownMs = Math.max(1L, postGestureCooldownMs);
    }

    MotorProfile copy() {
        return new MotorProfile(
            owner,
            clickType,
            gestureMode,
            allowActivationClick,
            enforceMutationBudget,
            clickSettings,
            menuValidationMode,
            menuTargetObject,
            hoverSettleTicks,
            maxMenuValidationTicks,
            maxMoveStepsPerTick,
            postGestureCooldownMs
        );
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
