package com.xptool.executor;

import com.google.gson.JsonObject;
import java.util.concurrent.ThreadLocalRandom;

final class LoginRuntime {
    private static final int SECONDARY_SUBMIT_DELAY_MIN_TICKS = 1;
    private static final int SECONDARY_SUBMIT_DELAY_MAX_TICKS = 3;
    private static final long SECONDARY_SUBMIT_DELAY_MIN_MS = 220L;
    private static final long SECONDARY_SUBMIT_DELAY_MAX_MS = 700L;
    private static final long SECONDARY_SUBMIT_HARD_FALLBACK_AFTER_READY_MS = 60L;
    private static final long SECONDARY_SUBMIT_LOGGED_IN_SETTLE_MIN_MS = 260L;
    private static final int SECONDARY_PROMPT_VISIBILITY_SETTLE_TICKS = 2;
    private static final double SESSION_RETRY_SCALE_MIN = 0.86;
    private static final double SESSION_RETRY_SCALE_MAX = 1.22;
    private static final double SESSION_WAIT_SCALE_MIN = 0.88;
    private static final double SESSION_WAIT_SCALE_MAX = 1.24;
    private static final int RETRY_TICK_JITTER_CAP = 2;
    private static final double WAIT_TICK_JITTER_RATIO = 0.12;

    interface Host {
        boolean isLoggedIn();
        boolean isPrimarySubmitPromptVisible();
        boolean isSecondarySubmitPromptVisible();
        boolean focusUsernameField();
        boolean focusPasswordField();
        boolean submitLogin();
        boolean openWorldSelect();
        JsonObject details(Object... kvPairs);
        void emitLoginEvent(String reason, JsonObject details);
    }

    private final Host host;
    private final LoginScreenStateResolver screenStateResolver;
    private final HumanTypingEngine typingEngine;

    private LoginRuntimeState state = LoginRuntimeState.IDLE;
    private LoginProfile profile = LoginProfile.defaults();
    private TypingProfile typingProfile = TypingProfile.defaults();
    private String username = "";
    private String password = "";
    private boolean requireSecondarySubmit = false;
    private int retries = 0;
    private int submitAttempts = 0;
    private int waitUntilTick = Integer.MIN_VALUE;
    private int earliestSecondarySubmitTick = Integer.MIN_VALUE;
    private long earliestSecondarySubmitAtMs = Long.MIN_VALUE;
    private int secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
    private long secondaryLoggedInObservedAtMs = Long.MIN_VALUE;
    private int currentTick = Integer.MIN_VALUE;
    private String lastFailureReason = "";
    private LoginRuntimeState activeTypingState = null;
    private double sessionRetryScale = 1.0;
    private double sessionWaitScale = 1.0;

    LoginRuntime(Host host, LoginScreenStateResolver resolver, HumanTypingEngine typingEngine) {
        this.host = host;
        this.screenStateResolver = resolver;
        this.typingEngine = typingEngine;
    }

    void requestStart(String username, String password, LoginProfile loginProfile, TypingProfile typingProfile) {
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.profile = loginProfile == null ? LoginProfile.defaults() : loginProfile;
        this.typingProfile = typingProfile == null ? TypingProfile.defaults() : typingProfile;
        this.requireSecondarySubmit = credentialsPrefilledMode();
        this.retries = 0;
        this.submitAttempts = 0;
        this.waitUntilTick = Integer.MIN_VALUE;
        this.earliestSecondarySubmitTick = Integer.MIN_VALUE;
        this.earliestSecondarySubmitAtMs = Long.MIN_VALUE;
        this.secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
        this.secondaryLoggedInObservedAtMs = Long.MIN_VALUE;
        this.lastFailureReason = "";
        this.activeTypingState = null;
        this.sessionRetryScale = sampleScale(SESSION_RETRY_SCALE_MIN, SESSION_RETRY_SCALE_MAX);
        this.sessionWaitScale = sampleScale(SESSION_WAIT_SCALE_MIN, SESSION_WAIT_SCALE_MAX);
        this.state = LoginRuntimeState.DETECT_SCREEN;
        emit("login_start");
    }

    void requestStop() {
        typingEngine.cancel();
        resetToIdle();
        emit("login_stop");
    }

    boolean onClientTick(int tick) {
        currentTick = tick;
        return typingEngine.onClientTick(tick);
    }

    boolean onGameTick(int tick) {
        currentTick = tick;
        typingEngine.onGameTick(tick);

        switch (state) {
            case IDLE:
            case SUCCESS:
            case FAILED_HARD_STOP:
                return false;
            case DETECT_SCREEN:
                return handleDetectScreen();
            case FOCUS_USERNAME:
                return transition(host.focusUsernameField(), LoginRuntimeState.TYPE_USERNAME, "login_focus_username");
            case TYPE_USERNAME:
                return handleTypingStage(username, LoginRuntimeState.FOCUS_PASSWORD, "login_type_username_start");
            case FOCUS_PASSWORD:
                return transition(host.focusPasswordField(), LoginRuntimeState.TYPE_PASSWORD, "login_focus_password");
            case TYPE_PASSWORD:
                return handleTypingStage(password, LoginRuntimeState.SUBMIT, "login_type_password_start");
            case SUBMIT:
                if (host.submitLogin()) {
                    submitAttempts++;
                    waitUntilTick = tick + rollWaitResultTimeoutTicks();
                    if (submitAttempts == 1) {
                        int rawDelayTicks = ThreadLocalRandom.current().nextInt(
                            SECONDARY_SUBMIT_DELAY_MIN_TICKS,
                            SECONDARY_SUBMIT_DELAY_MAX_TICKS + 1
                        );
                        int delayTicks = clampTicks(
                            (int) Math.round(rawDelayTicks * sessionWaitScale),
                            SECONDARY_SUBMIT_DELAY_MIN_TICKS,
                            SECONDARY_SUBMIT_DELAY_MAX_TICKS + 3
                        );
                        earliestSecondarySubmitTick = tick + Math.max(1, delayTicks);
                        long rawDelayMs = ThreadLocalRandom.current().nextLong(
                            SECONDARY_SUBMIT_DELAY_MIN_MS,
                            SECONDARY_SUBMIT_DELAY_MAX_MS + 1L
                        );
                        long delayMs = clampMillis(
                            Math.round(rawDelayMs * sessionWaitScale),
                            150L,
                            1_250L
                        );
                        earliestSecondarySubmitAtMs = System.currentTimeMillis() + Math.max(1L, delayMs);
                    } else {
                        earliestSecondarySubmitTick = Integer.MIN_VALUE;
                        earliestSecondarySubmitAtMs = Long.MIN_VALUE;
                    }
                    secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
                    secondaryLoggedInObservedAtMs = Long.MIN_VALUE;
                    state = LoginRuntimeState.WAIT_RESULT;
                    emit("login_submit");
                    return true;
                }
                return failOrRetry("submit_failed");
            case WAIT_RESULT:
                return handleWaitResult();
            case WORLD_SELECT:
                return handleWorldSelect();
            case FAILED_RETRY_COOLDOWN:
                if (tick >= waitUntilTick) {
                    state = LoginRuntimeState.DETECT_SCREEN;
                    emit("login_retry_resume");
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    boolean isActive() {
        return state != LoginRuntimeState.IDLE
            && state != LoginRuntimeState.SUCCESS
            && state != LoginRuntimeState.FAILED_HARD_STOP;
    }

    boolean isSuccessful() {
        return state == LoginRuntimeState.SUCCESS;
    }

    LoginRuntimeState state() {
        return state;
    }

    String lastFailureReason() {
        return lastFailureReason;
    }

    private boolean handleDetectScreen() {
        LoginScreenState screenState = screenStateResolver == null
            ? LoginScreenState.UNKNOWN
            : screenStateResolver.detect();
        emit("login_detect_screen");
        switch (screenState) {
            case LOGGED_IN:
                if (requireSecondarySubmit && submitAttempts == 1) {
                    state = LoginRuntimeState.WAIT_RESULT;
                    return false;
                }
                state = LoginRuntimeState.SUCCESS;
                emit("login_success");
                return true;
            case LOGIN_FORM:
                if (credentialsPrefilledMode()) {
                    state = LoginRuntimeState.SUBMIT;
                    emit("login_prefilled_submit_mode");
                    return true;
                }
                state = LoginRuntimeState.FOCUS_USERNAME;
                return true;
            case WORLD_SELECT:
                state = LoginRuntimeState.WORLD_SELECT;
                return true;
            case AUTHENTICATOR:
            case ERROR:
            case DISCONNECTED:
            case UNKNOWN:
            default:
                return failOrRetry("screen_unavailable");
        }
    }

    private boolean handleWaitResult() {
        long nowMs = System.currentTimeMillis();
        boolean loggedInNow = host.isLoggedIn();
        boolean primaryPromptVisible = host.isPrimarySubmitPromptVisible();
        boolean secondaryPromptVisible = host.isSecondarySubmitPromptVisible();
        boolean submitPromptVisible = primaryPromptVisible || secondaryPromptVisible;
        boolean secondaryPending = requireSecondarySubmit && submitAttempts == 1;
        LoginScreenState screenState = screenStateResolver == null
            ? LoginScreenState.UNKNOWN
            : screenStateResolver.detect();
        if (screenState == LoginScreenState.WORLD_SELECT) {
            secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
            state = LoginRuntimeState.WORLD_SELECT;
            return true;
        }

        if (secondaryPending) {
            if (loggedInNow) {
                if (secondaryLoggedInObservedAtMs == Long.MIN_VALUE) {
                    secondaryLoggedInObservedAtMs = nowMs;
                }
            } else {
                secondaryLoggedInObservedAtMs = Long.MIN_VALUE;
            }
            if (secondaryPromptVisible) {
                if (secondaryPromptFirstSeenTick == Integer.MIN_VALUE) {
                    secondaryPromptFirstSeenTick = currentTick;
                }
            } else {
                secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
            }
            boolean secondaryPromptSettled =
                secondaryPromptFirstSeenTick != Integer.MIN_VALUE
                    && currentTick >= (secondaryPromptFirstSeenTick + SECONDARY_PROMPT_VISIBILITY_SETTLE_TICKS);
            boolean tickGateReady =
                earliestSecondarySubmitTick == Integer.MIN_VALUE
                    || currentTick >= earliestSecondarySubmitTick;
            boolean timeGateReady =
                earliestSecondarySubmitAtMs == Long.MIN_VALUE
                    || nowMs >= earliestSecondarySubmitAtMs;
            boolean gateReady = tickGateReady || timeGateReady;
            boolean promptDrivenReady = secondaryPromptSettled && gateReady;
            boolean loggedInSettledReady =
                secondaryLoggedInObservedAtMs != Long.MIN_VALUE
                    && (nowMs - secondaryLoggedInObservedAtMs) >= SECONDARY_SUBMIT_LOGGED_IN_SETTLE_MIN_MS;
            boolean loggedInDrivenReady = loggedInSettledReady && gateReady && !primaryPromptVisible;
            boolean hardFallbackReady =
                (secondaryPromptVisible || loggedInNow)
                    && earliestSecondarySubmitAtMs != Long.MIN_VALUE
                    && nowMs >= (earliestSecondarySubmitAtMs + SECONDARY_SUBMIT_HARD_FALLBACK_AFTER_READY_MS);
            if (promptDrivenReady || loggedInDrivenReady || hardFallbackReady) {
                secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
                secondaryLoggedInObservedAtMs = Long.MIN_VALUE;
                state = LoginRuntimeState.SUBMIT;
                emit("login_wait_form_submit_retry");
                return true;
            }
        } else {
            secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
            secondaryLoggedInObservedAtMs = Long.MIN_VALUE;
        }

        if (loggedInNow && !submitPromptVisible && !secondaryPending) {
            state = LoginRuntimeState.SUCCESS;
            emit("login_success");
            return true;
        }
        if (currentTick >= waitUntilTick) {
            return failOrRetry("result_timeout");
        }
        return false;
    }

    private boolean handleWorldSelect() {
        int chance = profile.worldSelectChancePct();
        if (ThreadLocalRandom.current().nextInt(100) >= chance) {
            return false;
        }
        if (!host.openWorldSelect()) {
            return failOrRetry("world_select_failed");
        }
        state = LoginRuntimeState.DETECT_SCREEN;
        emit("login_world_select");
        return true;
    }

    private boolean handleTypingStage(String text, LoginRuntimeState nextState, String reason) {
        LoginRuntimeState stage = state;
        if (text == null || text.isEmpty()) {
            activeTypingState = null;
            state = nextState;
            emit(reason + "_empty");
            return true;
        }
        if (activeTypingState != stage) {
            activeTypingState = stage;
            typingEngine.startTyping(text, typingProfile);
            emit(reason);
            return true;
        }
        if (typingEngine.isTyping()) {
            if (typingEngine.state() == TypingRuntimeState.CANCELLED) {
                activeTypingState = null;
                return failOrRetry("typing_cancelled");
            }
            return false;
        }
        activeTypingState = null;
        state = nextState;
        emit(reason + "_complete");
        return true;
    }

    private boolean transition(boolean success, LoginRuntimeState nextState, String reason) {
        if (!success) {
            return failOrRetry(reason + "_failed");
        }
        state = nextState;
        emit(reason);
        return true;
    }

    private boolean failOrRetry(String reason) {
        retries++;
        lastFailureReason = reason == null ? "" : reason;
        submitAttempts = 0;
        earliestSecondarySubmitTick = Integer.MIN_VALUE;
        earliestSecondarySubmitAtMs = Long.MIN_VALUE;
        secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
        secondaryLoggedInObservedAtMs = Long.MIN_VALUE;
        if (retries > profile.maxRetries()) {
            state = LoginRuntimeState.FAILED_HARD_STOP;
            emit("login_hard_stop");
            return true;
        }
        waitUntilTick = currentTick + rollRetryCooldownTicks();
        state = LoginRuntimeState.FAILED_RETRY_COOLDOWN;
        emit("login_retry_cooldown");
        return true;
    }

    private void resetToIdle() {
        state = LoginRuntimeState.IDLE;
        retries = 0;
        submitAttempts = 0;
        waitUntilTick = Integer.MIN_VALUE;
        earliestSecondarySubmitTick = Integer.MIN_VALUE;
        earliestSecondarySubmitAtMs = Long.MIN_VALUE;
        secondaryPromptFirstSeenTick = Integer.MIN_VALUE;
        secondaryLoggedInObservedAtMs = Long.MIN_VALUE;
        requireSecondarySubmit = false;
        lastFailureReason = "";
        activeTypingState = null;
        sessionRetryScale = 1.0;
        sessionWaitScale = 1.0;
    }

    private boolean credentialsPrefilledMode() {
        return username == null || username.isBlank()
            ? (password == null || password.isBlank())
            : false;
    }

    private void emit(String reason) {
        if (host == null || reason == null || reason.isBlank()) {
            return;
        }
        host.emitLoginEvent(
            reason,
            host.details(
                "tick", currentTick,
                "state", state.name().toLowerCase(),
                "retries", retries,
                "submitAttempts", submitAttempts,
                "requireSecondarySubmit", requireSecondarySubmit,
                "maxRetries", profile.maxRetries(),
                "sessionRetryScale", sessionRetryScale,
                "sessionWaitScale", sessionWaitScale,
                "lastFailureReason", lastFailureReason
            )
        );
    }

    private int rollRetryCooldownTicks() {
        int base = Math.max(1, profile.rollRetryCooldownTicks());
        int scaled = clampTicks((int) Math.round(base * sessionRetryScale), 1, 600);
        int jitterRange = Math.min(RETRY_TICK_JITTER_CAP, Math.max(1, retries));
        int jitter = ThreadLocalRandom.current().nextInt(-jitterRange, jitterRange + 1);
        return Math.max(1, scaled + jitter);
    }

    private int rollWaitResultTimeoutTicks() {
        int base = Math.max(1, profile.rollWaitResultTimeoutTicks());
        int scaled = clampTicks((int) Math.round(base * sessionWaitScale), 1, 2_000);
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

    private static long clampMillis(long value, long min, long max) {
        long lo = Math.min(min, max);
        long hi = Math.max(min, max);
        return Math.max(lo, Math.min(hi, value));
    }
}
