package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.concurrent.ThreadLocalRandom;

final class BreakRuntime {
    private static final double SESSION_WORK_STEP_SCALE_MIN = 0.84;
    private static final double SESSION_WORK_STEP_SCALE_MAX = 1.20;
    private static final double SESSION_BREAK_STEP_SCALE_MIN = 0.82;
    private static final double SESSION_BREAK_STEP_SCALE_MAX = 1.24;
    private static final int SESSION_BREAK_START_CHANCE_BIAS_MIN = -8;
    private static final int SESSION_BREAK_START_CHANCE_BIAS_MAX = 10;
    private static final int SESSION_BREAK_END_CHANCE_BIAS_MIN = -10;
    private static final int SESSION_BREAK_END_CHANCE_BIAS_MAX = 12;
    private static final double RESUME_COOLDOWN_SCALE_MIN = 0.70;
    private static final double RESUME_COOLDOWN_SCALE_MAX = 1.35;
    private static final int RESUME_COOLDOWN_EXTRA_TICKS_MAX = 3;

    interface Host {
        boolean isLoggedIn();
        boolean requestStopAllRuntime();
        boolean requestLogout();
        boolean requestLoginStart();
        JsonObject details(Object... kvPairs);
        void emitBreakEvent(String reason, JsonObject details);
    }

    private final Host host;
    private final LoginRuntime loginRuntime;
    private final ResumePlanner resumePlanner;

    private BreakRuntimeState state = BreakRuntimeState.DISARMED;
    private BreakProfile profile = BreakProfile.defaults();
    private double workProgressPct = 0.0;
    private double breakProgressPct = 0.0;
    private int cooldownUntilTick = Integer.MIN_VALUE;
    private int currentTick = Integer.MIN_VALUE;
    private double sessionWorkStepScale = 1.0;
    private double sessionBreakStepScale = 1.0;
    private int sessionBreakStartChanceBiasPct = 0;
    private int sessionBreakEndChanceBiasPct = 0;
    private int activeResumeCooldownTicks = 0;

    BreakRuntime(Host host, LoginRuntime loginRuntime, ResumePlanner resumePlanner) {
        this.host = host;
        this.loginRuntime = loginRuntime;
        this.resumePlanner = resumePlanner;
    }

    void arm(BreakProfile profile) {
        this.profile = profile == null ? BreakProfile.defaults() : profile;
        this.state = BreakRuntimeState.WORKING;
        this.workProgressPct = 0.0;
        this.breakProgressPct = 0.0;
        this.cooldownUntilTick = Integer.MIN_VALUE;
        this.activeResumeCooldownTicks = 0;
        sampleSessionBehavior();
        loginRuntime.requestStop();
        resumePlanner.cancel();
        emit("break_armed");
    }

    void disarm() {
        state = BreakRuntimeState.DISARMED;
        workProgressPct = 0.0;
        breakProgressPct = 0.0;
        cooldownUntilTick = Integer.MIN_VALUE;
        activeResumeCooldownTicks = 0;
        sessionWorkStepScale = 1.0;
        sessionBreakStepScale = 1.0;
        sessionBreakStartChanceBiasPct = 0;
        sessionBreakEndChanceBiasPct = 0;
        loginRuntime.requestStop();
        resumePlanner.cancel();
        emit("break_disarmed");
    }

    void notifyStopAllRuntime() {
        disarm();
    }

    boolean onGameTick(int tick) {
        currentTick = tick;
        loginRuntime.onGameTick(tick);
        resumePlanner.onGameTick(tick);

        switch (state) {
            case DISARMED:
                return false;
            case WORKING:
                workProgressPct = Math.min(
                    100.0,
                    workProgressPct + sampleProgressStep(profile.workProgressStepPctPerTick(), sessionWorkStepScale)
                );
                if (workProgressPct >= 100.0) {
                    state = BreakRuntimeState.BREAK_ARMED;
                    emit("break_arm_window_open");
                    return true;
                }
                return false;
            case BREAK_ARMED:
                if (rollWithBias(profile.breakStartChancePct(), sessionBreakStartChanceBiasPct)) {
                    state = BreakRuntimeState.BREAK_STARTING;
                    emit("break_start_requested");
                    return true;
                }
                return false;
            case BREAK_STARTING:
                return handleBreakStarting();
            case ON_BREAK:
                breakProgressPct = Math.min(
                    100.0,
                    breakProgressPct + sampleProgressStep(profile.breakProgressStepPctPerTick(), sessionBreakStepScale)
                );
                if (breakProgressPct >= 100.0 && rollWithBias(profile.breakEndChancePct(), sessionBreakEndChanceBiasPct)) {
                    state = BreakRuntimeState.RESUME_STARTING;
                    resumePlanner.begin();
                    emit("break_resume_requested");
                    return true;
                }
                return false;
            case RESUME_STARTING:
                return handleResumeStarting();
            case RESUME_COOLDOWN:
                if (tick >= cooldownUntilTick) {
                    state = BreakRuntimeState.WORKING;
                    workProgressPct = 0.0;
                    breakProgressPct = 0.0;
                    activeResumeCooldownTicks = 0;
                    emit("break_resume_complete");
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    boolean isBreakActive() {
        return state == BreakRuntimeState.BREAK_ARMED
            || state == BreakRuntimeState.BREAK_STARTING
            || state == BreakRuntimeState.ON_BREAK
            || state == BreakRuntimeState.RESUME_STARTING
            || state == BreakRuntimeState.RESUME_COOLDOWN;
    }

    BreakRuntimeState state() {
        return state;
    }

    private boolean handleBreakStarting() {
        boolean stopped = host.requestStopAllRuntime();
        boolean loggedOut = !profile.logoutOnBreakStart() || !host.isLoggedIn() || host.requestLogout();
        if (!stopped || !loggedOut) {
            emit("break_start_retry");
            return false;
        }
        state = BreakRuntimeState.ON_BREAK;
        breakProgressPct = 0.0;
        loginRuntime.requestStop();
        resumePlanner.cancel();
        emit("break_started");
        return true;
    }

    private boolean handleResumeStarting() {
        if (loginRuntime.isSuccessful()) {
            resumePlanner.notifyLoginSuccess();
            activeResumeCooldownTicks = rollResumeCooldownTicks();
            resumePlanner.beginCooldown(activeResumeCooldownTicks);
            state = BreakRuntimeState.RESUME_COOLDOWN;
            cooldownUntilTick = currentTick + activeResumeCooldownTicks;
            emit("break_resume_ready");
            return true;
        }
        if (!loginRuntime.isActive()) {
            boolean started = host.requestLoginStart();
            emit(started ? "break_resume_login_begin" : "break_resume_login_start_failed");
        }
        return false;
    }

    private boolean roll(int chancePct) {
        int chance = Math.max(0, Math.min(100, chancePct));
        if (chance <= 0) {
            return false;
        }
        if (chance >= 100) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(100) < chance;
    }

    private boolean rollWithBias(int baseChancePct, int biasPct) {
        return roll(clampPercent(baseChancePct + biasPct));
    }

    private void sampleSessionBehavior() {
        sessionWorkStepScale = sampleScale(SESSION_WORK_STEP_SCALE_MIN, SESSION_WORK_STEP_SCALE_MAX);
        sessionBreakStepScale = sampleScale(SESSION_BREAK_STEP_SCALE_MIN, SESSION_BREAK_STEP_SCALE_MAX);
        sessionBreakStartChanceBiasPct = ThreadLocalRandom.current().nextInt(
            SESSION_BREAK_START_CHANCE_BIAS_MIN,
            SESSION_BREAK_START_CHANCE_BIAS_MAX + 1
        );
        sessionBreakEndChanceBiasPct = ThreadLocalRandom.current().nextInt(
            SESSION_BREAK_END_CHANCE_BIAS_MIN,
            SESSION_BREAK_END_CHANCE_BIAS_MAX + 1
        );
    }

    private double sampleProgressStep(double baseStepPct, double sessionScale) {
        double safeBase = clampPositive(baseStepPct, 0.01);
        double microJitter = sampleScale(0.82, 1.20);
        double scaled = safeBase * Math.max(0.45, sessionScale) * microJitter;
        return clampDouble(scaled, 0.01, 3.50);
    }

    private int rollResumeCooldownTicks() {
        int base = Math.max(1, profile.resumeCooldownTicks());
        double scale = sampleScale(RESUME_COOLDOWN_SCALE_MIN, RESUME_COOLDOWN_SCALE_MAX);
        int scaled = Math.max(1, (int) Math.round(base * scale));
        int extra = ThreadLocalRandom.current().nextInt(0, RESUME_COOLDOWN_EXTRA_TICKS_MAX + 1);
        return Math.max(1, scaled + extra);
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double sampleScale(double min, double max) {
        double low = Math.min(min, max);
        double high = Math.max(min, max);
        return ThreadLocalRandom.current().nextDouble(low, high);
    }

    private static double clampDouble(double value, double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return lo;
        }
        return Math.max(lo, Math.min(hi, value));
    }

    private static double clampPositive(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0) {
            return fallback;
        }
        return value;
    }

    private void emit(String reason) {
        if (host == null || reason == null || reason.isBlank()) {
            return;
        }
        host.emitBreakEvent(
            reason,
            host.details(
                "tick", currentTick,
                "state", state.name().toLowerCase(),
                "workProgressPct", Math.round(workProgressPct),
                "breakProgressPct", Math.round(breakProgressPct),
                "cooldownUntilTick", cooldownUntilTick,
                "activeResumeCooldownTicks", activeResumeCooldownTicks,
                "sessionWorkStepScale", sessionWorkStepScale,
                "sessionBreakStepScale", sessionBreakStepScale,
                "sessionBreakStartChanceBiasPct", sessionBreakStartChanceBiasPct,
                "sessionBreakEndChanceBiasPct", sessionBreakEndChanceBiasPct
            )
        );
    }
}

