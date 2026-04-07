package com.xptool.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoginRuntimeTest {
    @Test
    void submitFailuresEnterCooldownThenHardStopAfterRetryLimit() {
        TestHost host = new TestHost();
        host.loginFormVisible = true;
        host.submitLoginResult = false;
        LoginRuntime runtime = newRuntime(host);
        LoginProfile profile = new LoginProfile(1, 1, 1, 5, 5, 0);

        runtime.requestStart("", "", profile, TypingProfile.defaults());
        runTicks(runtime, 80);

        assertEquals(LoginRuntimeState.FAILED_HARD_STOP, runtime.state());
        assertEquals("submit_failed", runtime.lastFailureReason());
        assertTrue(host.emittedReasons.contains("login_retry_cooldown"));
        assertTrue(host.emittedReasons.contains("login_hard_stop"));
    }

    @Test
    void waitResultTimeoutTransitionsToRetryCooldown() {
        TestHost host = new TestHost();
        host.loginFormVisible = true;
        host.submitLoginResult = true;
        host.loggedIn = false;
        host.primarySubmitPromptVisible = false;
        host.secondarySubmitPromptVisible = false;
        LoginRuntime runtime = newRuntime(host);
        LoginProfile profile = new LoginProfile(3, 1, 1, 1, 1, 0);

        runtime.requestStart("", "", profile, TypingProfile.defaults());
        boolean reachedTimeoutCooldown = false;
        for (int tick = 1; tick <= 40; tick++) {
            runtime.onGameTick(tick);
            if (runtime.state() == LoginRuntimeState.FAILED_RETRY_COOLDOWN
                && "result_timeout".equals(runtime.lastFailureReason())) {
                reachedTimeoutCooldown = true;
                break;
            }
        }

        assertTrue(reachedTimeoutCooldown);
        assertEquals("result_timeout", runtime.lastFailureReason());
        assertTrue(host.emittedReasons.contains("login_submit"));
        assertTrue(host.emittedReasons.contains("login_retry_cooldown"));
    }

    private static LoginRuntime newRuntime(TestHost host) {
        LoginScreenStateResolver resolver = new LoginScreenStateResolver(host);
        HumanTypingEngine typingEngine = new HumanTypingEngine(host);
        return new LoginRuntime(host, resolver, typingEngine);
    }

    private static void runTicks(LoginRuntime runtime, int count) {
        for (int tick = 1; tick <= Math.max(1, count); tick++) {
            runtime.onGameTick(tick);
        }
    }

    private static final class TestHost implements
        LoginRuntime.Host,
        LoginScreenStateResolver.Host,
        HumanTypingEngine.Host {

        private boolean loggedIn = false;
        private boolean loginFormVisible = false;
        private boolean worldSelectVisible = false;
        private boolean authenticatorPromptVisible = false;
        private boolean disconnectedDialogVisible = false;
        private boolean loginErrorVisible = false;
        private boolean primarySubmitPromptVisible = false;
        private boolean secondarySubmitPromptVisible = false;
        private boolean submitLoginResult = true;
        private long nowMs = 10_000L;
        private final List<String> emittedReasons = new ArrayList<>();

        @Override
        public boolean isLoggedIn() {
            return loggedIn;
        }

        @Override
        public boolean isPrimarySubmitPromptVisible() {
            return primarySubmitPromptVisible;
        }

        @Override
        public boolean isSecondarySubmitPromptVisible() {
            return secondarySubmitPromptVisible;
        }

        @Override
        public boolean focusUsernameField() {
            return true;
        }

        @Override
        public boolean focusPasswordField() {
            return true;
        }

        @Override
        public boolean submitLogin() {
            return submitLoginResult;
        }

        @Override
        public boolean openWorldSelect() {
            return true;
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
        public void emitLoginEvent(String reason, JsonObject details) {
            emittedReasons.add(reason == null ? "" : reason);
        }

        @Override
        public boolean isLoginFormVisible() {
            return loginFormVisible;
        }

        @Override
        public boolean isWorldSelectVisible() {
            return worldSelectVisible;
        }

        @Override
        public boolean isAuthenticatorPromptVisible() {
            return authenticatorPromptVisible;
        }

        @Override
        public boolean isDisconnectedDialogVisible() {
            return disconnectedDialogVisible;
        }

        @Override
        public boolean isLoginErrorVisible() {
            return loginErrorVisible;
        }

        @Override
        public long nowMs() {
            nowMs += 120L;
            return nowMs;
        }

        @Override
        public boolean ensureTypingFocus() {
            return true;
        }

        @Override
        public boolean pressKey(int keyCode, boolean holdShift, int holdMs) {
            return true;
        }

        @Override
        public void emitTypingEvent(String reason, JsonObject details) {
            // No-op for runtime tests.
        }
    }
}
