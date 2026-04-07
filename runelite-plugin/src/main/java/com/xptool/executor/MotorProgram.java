package com.xptool.executor;

final class MotorProgram {
    final long id;
    final CanvasPoint targetCanvas;
    final MotorGestureType type;
    final MotorProfile profile;
    final long createdAtMs;
    MotorGestureStatus status = MotorGestureStatus.SCHEDULED;
    MotorProgramPhase phase = MotorProgramPhase.WAITING_START;
    String resultReason = "scheduled";
    java.awt.Point fromScreen = null;
    java.awt.Point toScreen = null;
    java.awt.Point controlScreen = null;
    java.util.List<java.awt.Point> scriptedPathScreen = null;
    int totalSteps = 0;
    int stepIndex = 0;
    int hoverSettleTicksRemaining = 0;
    int menuValidationTicks = 0;
    MotorClickPhase clickPhase = MotorClickPhase.PREPARE;
    long clickPhaseReadyAtMs = 0L;
    long clickButtonDownDurationMs = 0L;
    int terminalAlignmentRetryCount = 0;
    boolean firstMouseMutationObserved = false;
    long firstMouseMutationAtMs = Long.MIN_VALUE;

    MotorProgram(long id, CanvasPoint targetCanvas, MotorGestureType type, MotorProfile profile) {
        this.id = id;
        this.targetCanvas = targetCanvas;
        this.type = type;
        this.profile = profile;
        this.createdAtMs = System.currentTimeMillis();
    }

    MotorHandle toHandle() {
        return new MotorHandle(id, status, resultReason);
    }

    boolean matches(CanvasPoint target, MotorGestureType gestureType, MotorProfile reqProfile) {
        if (target == null || gestureType == null || reqProfile == null) {
            return false;
        }
        return target.equals(targetCanvas)
            && gestureType == type
            && safeString(reqProfile.owner).equals(safeString(profile.owner))
            && reqProfile.menuValidationMode == profile.menuValidationMode
            && safeString(reqProfile.clickType).equals(safeString(profile.clickType));
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
