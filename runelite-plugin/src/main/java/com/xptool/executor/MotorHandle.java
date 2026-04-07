package com.xptool.executor;

final class MotorHandle {
    final long id;
    final MotorGestureStatus status;
    final String reason;

    MotorHandle(long id, MotorGestureStatus status, String reason) {
        this.id = id;
        this.status = status == null ? MotorGestureStatus.FAILED : status;
        this.reason = safeString(reason);
    }

    static MotorHandle failed(long id, MotorGestureType type, String reason) {
        return new MotorHandle(id, MotorGestureStatus.FAILED, reason);
    }

    static MotorHandle inFlight(long id, String reason) {
        return new MotorHandle(id, MotorGestureStatus.IN_FLIGHT, reason);
    }

    boolean isTerminal() {
        return status == MotorGestureStatus.COMPLETE
            || status == MotorGestureStatus.FAILED
            || status == MotorGestureStatus.CANCELLED;
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
