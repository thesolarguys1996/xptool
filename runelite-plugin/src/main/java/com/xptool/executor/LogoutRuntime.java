package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

final class LogoutRuntime {
    private static final double SESSION_RETRY_SCALE_MIN = 0.88;
    private static final double SESSION_RETRY_SCALE_MAX = 1.24;
    private static final double SESSION_WAIT_SCALE_MIN = 0.90;
    private static final double SESSION_WAIT_SCALE_MAX = 1.22;
    private static final int RETRY_TICK_JITTER_CAP = 2;
    private static final double WAIT_TICK_JITTER_RATIO = 0.10;

    interface Host {
        boolean isLoggedIn();
        LogoutInteractionController.AttemptStatus attemptLogout();
        JsonObject details(Object... kvPairs);
        void emitLogoutEvent(String reason, JsonObject details);
    }

    private final Host host;

    private LogoutRuntimeState state = LogoutRuntimeState.IDLE;
    private LogoutProfile profile = LogoutProfile.defaults();
    private int retries = 0;
    private int waitUntilTick = Integer.MIN_VALUE;
    private int currentTick = Integer.MIN_VALUE;
    private String lastFailureReason = "";
    private double sessionRetryScale = 1.0;
    private double sessionWaitScale = 1.0;

    LogoutRuntime(Host host) {
        this.host = host;
    }

    void requestStart(LogoutProfile logoutProfile) {
        profile = logoutProfile == null ? LogoutProfile.defaults() : logoutProfile;
        retries = 0;
        waitUntilTick = Integer.MIN_VALUE;
        lastFailureReason = "";
        sessionRetryScale = sampleScale(SESSION_RETRY_SCALE_MIN, SESSION_RETRY_SCALE_MAX);
        sessionWaitScale = sampleScale(SESSION_WAIT_SCALE_MIN, SESSION_WAIT_SCALE_MAX);
        if (!host.isLoggedIn()) {
            state = LogoutRuntimeState.SUCCESS;
            emit("logout_already_logged_out");
            return;
        }
        state = LogoutRuntimeState.ATTEMPT;
        emit("logout_start");
    }

    void requestStop() {
        resetToIdle();
        emit("logout_stop");
    }

    boolean onGameTick(int tick) {
        currentTick = tick;
        if (state != LogoutRuntimeState.IDLE && !host.isLoggedIn()) {
            if (state != LogoutRuntimeState.SUCCESS) {
                state = LogoutRuntimeState.SUCCESS;
                emit("logout_success");
                return true;
            }
            return false;
        }
        switch (state) {
            case IDLE:
            case SUCCESS:
            case FAILED_HARD_STOP:
                return false;
            case ATTEMPT:
                return handleAttempt();
            case WAIT_RESULT:
                return handleWaitResult();
            case RETRY_COOLDOWN:
                if (tick >= waitUntilTick) {
                    state = LogoutRuntimeState.ATTEMPT;
                    emit("logout_retry_resume");
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    boolean isActive() {
        return state == LogoutRuntimeState.ATTEMPT
            || state == LogoutRuntimeState.WAIT_RESULT
            || state == LogoutRuntimeState.RETRY_COOLDOWN;
    }

    boolean isSuccessful() {
        return state == LogoutRuntimeState.SUCCESS;
    }

    LogoutRuntimeState state() {
        return state;
    }

    String lastFailureReason() {
        return lastFailureReason;
    }

    private boolean handleAttempt() {
        if (!host.isLoggedIn()) {
            state = LogoutRuntimeState.SUCCESS;
            emit("logout_success");
            return true;
        }
        LogoutInteractionController.AttemptStatus status = host.attemptLogout();
        if (status == LogoutInteractionController.AttemptStatus.ALREADY_LOGGED_OUT) {
            state = LogoutRuntimeState.SUCCESS;
            emit("logout_success");
            return true;
        }
        if (status == LogoutInteractionController.AttemptStatus.FAILED) {
            return failOrRetry("logout_attempt_failed");
        }
        waitUntilTick = currentTick + rollWaitResultTimeoutTicks();
        state = LogoutRuntimeState.WAIT_RESULT;
        emit("logout_attempt_dispatched");
        return true;
    }

    private boolean handleWaitResult() {
        if (!host.isLoggedIn()) {
            state = LogoutRuntimeState.SUCCESS;
            emit("logout_success");
            return true;
        }
        if (currentTick >= waitUntilTick) {
            return failOrRetry("logout_result_timeout");
        }
        return false;
    }

    private boolean failOrRetry(String reason) {
        retries++;
        lastFailureReason = safeString(reason);
        if (retries > profile.maxRetries()) {
            state = LogoutRuntimeState.FAILED_HARD_STOP;
            emit("logout_failed_hard_stop");
            return true;
        }
        waitUntilTick = currentTick + rollRetryCooldownTicks();
        state = LogoutRuntimeState.RETRY_COOLDOWN;
        emit("logout_retry_scheduled");
        return true;
    }

    private void resetToIdle() {
        state = LogoutRuntimeState.IDLE;
        retries = 0;
        waitUntilTick = Integer.MIN_VALUE;
        lastFailureReason = "";
        sessionRetryScale = 1.0;
        sessionWaitScale = 1.0;
    }

    private void emit(String reason) {
        if (host == null || reason == null || reason.isBlank()) {
            return;
        }
        host.emitLogoutEvent(
            reason,
            host.details(
                "tick", currentTick,
                "state", state.name().toLowerCase(Locale.ROOT),
                "retries", retries,
                "maxRetries", profile.maxRetries(),
                "sessionRetryScale", sessionRetryScale,
                "sessionWaitScale", sessionWaitScale,
                "waitUntilTick", waitUntilTick,
                "lastFailureReason", safeString(lastFailureReason)
            )
        );
    }

    private int rollRetryCooldownTicks() {
        int base = Math.max(1, profile.rollRetryCooldownTicks());
        int scaled = clampTicks((int) Math.round(base * sessionRetryScale), 1, 320);
        int jitterRange = Math.min(RETRY_TICK_JITTER_CAP, Math.max(1, retries));
        int jitter = ThreadLocalRandom.current().nextInt(-jitterRange, jitterRange + 1);
        return Math.max(1, scaled + jitter);
    }

    private int rollWaitResultTimeoutTicks() {
        int base = Math.max(1, profile.rollWaitResultTimeoutTicks());
        int scaled = clampTicks((int) Math.round(base * sessionWaitScale), 1, 900);
        int jitterSpan = Math.max(1, (int) Math.round(scaled * WAIT_TICK_JITTER_RATIO));
        int jitter = ThreadLocalRandom.current().nextInt(-jitterSpan, jitterSpan + 1);
        return Math.max(1, scaled + jitter);
    }

    private static double sampleScale(double min, double max) {
        double low = Math.min(min, max);
        double high = Math.max(min, max);
        return ThreadLocalRandom.current().nextDouble(low, high);
    }

    private static int clampTicks(int value, int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        return Math.max(lo, Math.min(hi, value));
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }
}
