package com.xptool.executor;

import com.google.gson.JsonObject;

final class ResumePlanner {
    interface Host {
        boolean isGameReadyForRuntime();
        JsonObject details(Object... kvPairs);
        void emitResumeEvent(String reason, JsonObject details);
    }

    private final Host host;
    private ResumePlannerState state = ResumePlannerState.IDLE;
    private int waitUntilTick = Integer.MIN_VALUE;
    private int currentTick = Integer.MIN_VALUE;

    ResumePlanner(Host host) {
        this.host = host;
    }

    void begin() {
        state = ResumePlannerState.WAIT_LOGIN_SUCCESS;
        waitUntilTick = Integer.MIN_VALUE;
        emit("resume_begin");
    }

    void notifyLoginSuccess() {
        if (state == ResumePlannerState.IDLE || state == ResumePlannerState.READY) {
            return;
        }
        state = ResumePlannerState.WAIT_GAME_READY;
        emit("resume_login_success");
    }

    void beginCooldown(int cooldownTicks) {
        state = ResumePlannerState.COOLDOWN;
        waitUntilTick = currentTick + Math.max(0, cooldownTicks);
        emit("resume_cooldown_begin");
    }

    void fail(String reason) {
        state = ResumePlannerState.FAILED;
        emit(reason == null || reason.isBlank() ? "resume_failed" : reason);
    }

    void cancel() {
        state = ResumePlannerState.IDLE;
        waitUntilTick = Integer.MIN_VALUE;
        emit("resume_cancel");
    }

    boolean onGameTick(int tick) {
        currentTick = tick;
        switch (state) {
            case IDLE:
            case READY:
            case FAILED:
                return false;
            case WAIT_LOGIN_SUCCESS:
                return false;
            case WAIT_GAME_READY:
                if (host != null && host.isGameReadyForRuntime()) {
                    state = ResumePlannerState.READY;
                    emit("resume_ready");
                    return true;
                }
                return false;
            case COOLDOWN:
                if (tick >= waitUntilTick) {
                    state = ResumePlannerState.READY;
                    emit("resume_ready");
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    boolean isReady() {
        return state == ResumePlannerState.READY;
    }

    ResumePlannerState state() {
        return state;
    }

    private void emit(String reason) {
        if (host == null || reason == null || reason.isBlank()) {
            return;
        }
        host.emitResumeEvent(reason, host.details("tick", currentTick, "state", state.name().toLowerCase()));
    }
}

