package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogoutRuntimeTest {
    @Test
    void failedAttemptsProgressToHardStopAfterRetryLimit() {
        TestHost host = new TestHost();
        host.loggedIn = true;
        host.attemptStatus = LogoutInteractionController.AttemptStatus.FAILED;
        LogoutRuntime runtime = new LogoutRuntime(host);
        LogoutProfile profile = new LogoutProfile(1, 1, 1, 5, 5);

        runtime.requestStart(profile);
        runTicks(runtime, 40);

        assertEquals(LogoutRuntimeState.FAILED_HARD_STOP, runtime.state());
        assertEquals("logout_attempt_failed", runtime.lastFailureReason());
        assertTrue(host.emittedReasons.contains("logout_retry_scheduled"));
        assertTrue(host.emittedReasons.contains("logout_failed_hard_stop"));
    }

    @Test
    void waitResultTimeoutSchedulesRetryCooldown() {
        TestHost host = new TestHost();
        host.loggedIn = true;
        host.attemptStatus = LogoutInteractionController.AttemptStatus.ACTION_DISPATCHED;
        LogoutRuntime runtime = new LogoutRuntime(host);
        LogoutProfile profile = new LogoutProfile(3, 1, 1, 1, 1);

        runtime.requestStart(profile);
        boolean reachedTimeoutCooldown = false;
        for (int tick = 1; tick <= 24; tick++) {
            runtime.onGameTick(tick);
            if (runtime.state() == LogoutRuntimeState.RETRY_COOLDOWN
                && "logout_result_timeout".equals(runtime.lastFailureReason())) {
                reachedTimeoutCooldown = true;
                break;
            }
        }

        assertTrue(reachedTimeoutCooldown);
        assertEquals("logout_result_timeout", runtime.lastFailureReason());
        assertTrue(host.emittedReasons.contains("logout_attempt_dispatched"));
        assertTrue(host.emittedReasons.contains("logout_retry_scheduled"));
    }

    @Test
    void startWhenAlreadyLoggedOutSucceedsImmediately() {
        TestHost host = new TestHost();
        host.loggedIn = false;
        LogoutRuntime runtime = new LogoutRuntime(host);

        runtime.requestStart(LogoutProfile.defaults());

        assertEquals(LogoutRuntimeState.SUCCESS, runtime.state());
        assertTrue(runtime.isSuccessful());
        assertTrue(host.emittedReasons.contains("logout_already_logged_out"));
    }

    @Test
    void hardStopReconcilesToSuccessWhenLoginStateTurnsLoggedOut() {
        TestHost host = new TestHost();
        host.loggedIn = true;
        host.attemptStatus = LogoutInteractionController.AttemptStatus.FAILED;
        LogoutRuntime runtime = new LogoutRuntime(host);
        LogoutProfile profile = new LogoutProfile(1, 1, 1, 1, 1);

        runtime.requestStart(profile);
        runTicks(runtime, 24);
        assertEquals(LogoutRuntimeState.FAILED_HARD_STOP, runtime.state());

        host.loggedIn = false;
        runtime.onGameTick(25);

        assertEquals(LogoutRuntimeState.SUCCESS, runtime.state());
        assertTrue(runtime.isSuccessful());
        assertTrue(host.emittedReasons.contains("logout_success"));
    }

    private static void runTicks(LogoutRuntime runtime, int count) {
        for (int tick = 1; tick <= Math.max(1, count); tick++) {
            runtime.onGameTick(tick);
        }
    }

    private static final class TestHost implements LogoutRuntime.Host {
        private boolean loggedIn = true;
        private LogoutInteractionController.AttemptStatus attemptStatus =
            LogoutInteractionController.AttemptStatus.ACTION_DISPATCHED;
        private final List<String> emittedReasons = new ArrayList<>();

        @Override
        public boolean isLoggedIn() {
            return loggedIn;
        }

        @Override
        public LogoutInteractionController.AttemptStatus attemptLogout() {
            return attemptStatus;
        }

        @Override
        public JsonObject details(Object... kvPairs) {
            JsonObject out = new JsonObject();
            if (kvPairs == null) {
                return out;
            }
            int count = kvPairs.length - (kvPairs.length % 2);
            for (int i = 0; i < count; i += 2) {
                String key = kvPairs[i] == null ? "" : String.valueOf(kvPairs[i]);
                if (key.isBlank()) {
                    continue;
                }
                Object value = kvPairs[i + 1];
                if (value == null) {
                    out.addProperty(key, "");
                } else if (value instanceof Number) {
                    out.addProperty(key, (Number) value);
                } else if (value instanceof Boolean) {
                    out.addProperty(key, (Boolean) value);
                } else {
                    out.addProperty(key, String.valueOf(value));
                }
            }
            return out;
        }

        @Override
        public void emitLogoutEvent(String reason, JsonObject details) {
            emittedReasons.add(reason == null ? "" : reason);
        }
    }
}
