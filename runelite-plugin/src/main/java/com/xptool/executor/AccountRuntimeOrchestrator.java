package com.xptool.executor;

import com.google.gson.JsonObject;
import net.runelite.api.GameState;

final class AccountRuntimeOrchestrator {
    interface Host {
        GameState gameState();
        int currentExecutorTick();

        boolean hasManualMetricsRuntimeSignalFor(String consumer, boolean emitWhenMissing);
        LogoutProfile resolveManualMetricsLogoutProfile();
        LoginProfile resolveManualMetricsLoginProfile();
        void maybeEmitManualMetricsRuntimeGateEvent(String consumer, String reason, JsonObject details);
        JsonObject details(Object... kvPairs);

        boolean isLogoutRuntimeActive();
        boolean isLogoutRuntimeSuccessful();
        boolean isLogoutRuntimeFailedHardStop();
        void requestLogoutRuntimeStart(LogoutProfile profile);
        void requestLogoutRuntimeStop();
        void advanceLogoutRuntimeOnObservedTick(int observedTick);

        boolean loginBreakRuntimeEnabled();
        boolean isLoginRuntimeActive();
        void requestLoginRuntimeStop();
        void requestLoginRuntimeStart(LoginProfile profile, TypingProfile typingProfile);
        long currentTimeMs();
        long loginIdleSuppressStartWindowMs();
        void extendSuppressIdleForLoginUntil(long untilMs);
        void suppressIdleMotionForLoginStart();
        void resetLoginSubmitState();

        void notifyBreakRuntimeStopAll();
        void cancelResumePlanner();
        void cancelHumanTyping();

        void stopOperationalRuntimeState(String cancelReason);
        CommandExecutor.CommandDecision acceptDecision(String reason, JsonObject details);
    }

    private final Host host;

    AccountRuntimeOrchestrator(Host host) {
        this.host = host;
    }

    boolean startLogoutRuntime() {
        GameState gameState = host.gameState();
        if (gameState != GameState.LOGGED_IN && gameState != GameState.LOGGING_IN) {
            return true;
        }
        if (host.isLogoutRuntimeActive()) {
            return true;
        }
        if (!host.hasManualMetricsRuntimeSignalFor("logout_runtime", true)) {
            return false;
        }
        LogoutProfile logoutProfile = host.resolveManualMetricsLogoutProfile();
        if (logoutProfile == null) {
            host.maybeEmitManualMetricsRuntimeGateEvent(
                "logout_runtime",
                "logout_profile_unresolved",
                host.details("tick", host.currentExecutorTick())
            );
            return false;
        }
        host.requestLogoutRuntimeStart(logoutProfile);
        return host.isLogoutRuntimeActive() || host.isLogoutRuntimeSuccessful();
    }

    CommandExecutor.CommandDecision stopAllRuntime() {
        host.stopOperationalRuntimeState("stop_all_runtime");
        if (host.isLogoutRuntimeActive() || host.isLogoutRuntimeSuccessful()) {
            host.requestLogoutRuntimeStop();
        }
        if (host.loginBreakRuntimeEnabled()) {
            host.notifyBreakRuntimeStopAll();
            host.requestLoginRuntimeStop();
            host.cancelResumePlanner();
            host.cancelHumanTyping();
        }
        return host.acceptDecision(
            "runtime_stop_all_dispatched",
            host.details("tick", host.currentExecutorTick())
        );
    }

    boolean stopAllRuntimeForBreakStart() {
        host.stopOperationalRuntimeState("break_start_runtime_stop");
        if (host.loginBreakRuntimeEnabled()) {
            if (host.isLoginRuntimeActive()) {
                host.requestLoginRuntimeStop();
            }
            host.cancelResumePlanner();
            host.cancelHumanTyping();
        }
        return true;
    }

    boolean requestLogoutForBreakStart() {
        if (host.gameState() != GameState.LOGGED_IN) {
            return true;
        }
        boolean started = startLogoutRuntime();
        if (!started) {
            return false;
        }
        if (host.isLogoutRuntimeActive()) {
            host.advanceLogoutRuntimeOnObservedTick(host.currentExecutorTick());
        }
        if (host.gameState() != GameState.LOGGED_IN) {
            return true;
        }
        if (host.isLogoutRuntimeFailedHardStop()) {
            return false;
        }
        return host.gameState() != GameState.LOGGED_IN || host.isLogoutRuntimeSuccessful();
    }

    boolean startLoginRuntime() {
        if (!host.loginBreakRuntimeEnabled()) {
            return false;
        }
        if (!host.hasManualMetricsRuntimeSignalFor("login_runtime", true)) {
            return false;
        }
        LoginProfile loginProfile = host.resolveManualMetricsLoginProfile();
        if (loginProfile == null) {
            host.maybeEmitManualMetricsRuntimeGateEvent(
                "login_runtime",
                "login_profile_unresolved",
                host.details("tick", host.currentExecutorTick())
            );
            return false;
        }
        if (host.isLoginRuntimeActive()) {
            return true;
        }
        host.extendSuppressIdleForLoginUntil(host.currentTimeMs() + host.loginIdleSuppressStartWindowMs());
        host.suppressIdleMotionForLoginStart();
        host.resetLoginSubmitState();
        host.requestLoginRuntimeStart(loginProfile, TypingProfile.defaults());
        return true;
    }
}
