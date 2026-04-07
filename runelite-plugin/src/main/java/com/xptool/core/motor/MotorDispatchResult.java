package com.xptool.core.motor;

public final class MotorDispatchResult {
    private final long id;
    private final MotorDispatchStatus status;
    private final String reason;

    public MotorDispatchResult(long id, MotorDispatchStatus status, String reason) {
        this.id = id;
        this.status = status == null ? MotorDispatchStatus.FAILED : status;
        this.reason = reason == null ? "" : reason;
    }

    public long getId() {
        return id;
    }

    public MotorDispatchStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
